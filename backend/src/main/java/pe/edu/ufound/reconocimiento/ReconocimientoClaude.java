package pe.edu.ufound.reconocimiento;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicInvalidDataException;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredContentBlock;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredOutputConfig;
import com.anthropic.models.messages.TextBlockParam;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.ufound.almacenamiento.TipoImagen;
import pe.edu.ufound.model.CaracteristicasObjeto;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * HU-33: reconoce las características de la foto con Claude (API de Anthropic), pidiéndole la respuesta
 * en el formato de AnalisisDeFoto. Sin ANTHROPIC_API_KEY queda desactivado y el registro sigue igual.
 */
@Service
public class ReconocimientoClaude implements ReconocimientoImagenes {

    static final String MODELO = "claude-opus-5-5";
    private static final long MAX_TOKENS = 16000L;
    private static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(30);
    // Si el modelo declina la foto por sus filtros de seguridad, la API la reintenta con el modelo que recomienda.
    private static final String BETA_RESPALDO = "server-side-fallback-2026-07-01";

    private static final String INSTRUCCIONES = """
            Eres el asistente de U-Found, el sistema de objetos perdidos y encontrados de una universidad.
            Recibes la foto de un objeto que un estudiante perdió o que el personal de seguridad encontró en el campus.
            Describe solo el objeto, en español y con palabras simples, para que otra persona lo pueda reconocer:
            qué objeto es, sus colores principales, la marca y el material si se distinguen con claridad,
            y los rasgos que lo diferencian de otros parecidos. Si un dato no se ve, deja ese campo vacío: no lo adivines.
            No escribas datos personales aunque aparezcan en la foto (nombres, números de documento, teléfonos,
            correos, direcciones o códigos de alumno) y no describas personas ni rostros.
            Si la foto no muestra un objeto reconocible, responde objetoIdentificable = false.
            """;

    private static final Logger log = LoggerFactory.getLogger(ReconocimientoClaude.class);

    private final AnthropicClient cliente;

    public ReconocimientoClaude(@Value("${ia.api-key:}") String apiKey,
                                @Value("${ia.url:https://api.anthropic.com}") String url) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("ANTHROPIC_API_KEY no está configurada: el análisis de fotos con IA (HU-33) queda desactivado.");
            this.cliente = null;
        } else {
            this.cliente = AnthropicOkHttpClient.builder()
                    .apiKey(apiKey)
                    .baseUrl(url)
                    .timeout(TIEMPO_MAXIMO)
                    .maxRetries(1)
                    .build();
        }
    }

    public boolean estaDisponible() {
        return cliente != null;
    }

    @Override
    public Optional<CaracteristicasObjeto> analizar(MultipartFile foto) {
        if (!estaDisponible() || foto == null || foto.isEmpty()) {
            return Optional.empty();
        }
        try {
            byte[] datos = foto.getBytes();
            Optional<TipoImagen> tipo = TipoImagen.detectar(datos);
            if (tipo.isEmpty()) {
                return Optional.empty();
            }
            return interpretar(cliente.messages().create(peticion(datos, tipo.get())));
        } catch (IOException e) {
            log.warn("No se pudo leer la foto para analizarla: {}", e.getMessage());
        } catch (RateLimitException e) {
            log.warn("Análisis de foto omitido: se alcanzó el límite de uso de la API de Anthropic.");
        } catch (AnthropicServiceException e) {
            log.warn("Análisis de foto omitido: la API de Anthropic respondió {} ({}).", e.statusCode(), e.getMessage());
        } catch (AnthropicIoException e) {
            log.warn("Análisis de foto omitido: no se pudo conectar con la API de Anthropic ({}).", e.getMessage());
        } catch (AnthropicInvalidDataException e) {
            log.warn("Análisis de foto omitido: la respuesta del modelo no tenía el formato esperado ({}).", e.getMessage());
        } catch (AnthropicException e) {
            log.warn("Análisis de foto omitido: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private StructuredMessageCreateParams<AnalisisDeFoto> peticion(byte[] datos, TipoImagen tipo) {
        return MessageCreateParams.builder()
                .model(MODELO)
                .maxTokens(MAX_TOKENS)
                .system(INSTRUCCIONES)
                // Describir una foto es una tarea simple: esfuerzo bajo para responder rápido mientras el usuario espera.
                .outputConfig(StructuredOutputConfig.<AnalisisDeFoto>builder()
                        .format(AnalisisDeFoto.class)
                        .effort(OutputConfig.Effort.LOW)
                        .build())
                .addUserMessageOfContentBlockParams(List.of(
                        ContentBlockParam.ofImage(Base64ImageSource.builder()
                                .mediaType(tipoDeMedio(tipo))
                                .data(Base64.getEncoder().encodeToString(datos))
                                .build()),
                        ContentBlockParam.ofText(TextBlockParam.builder()
                                .text("Describe el objeto de esta foto.")
                                .build())))
                .putAdditionalHeader("anthropic-beta", BETA_RESPALDO)
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .build();
    }

    private Optional<CaracteristicasObjeto> interpretar(StructuredMessage<AnalisisDeFoto> respuesta) {
        Optional<StopReason> motivo = respuesta.stopReason();
        if (motivo.isPresent() && motivo.get().equals(StopReason.REFUSAL)) {
            log.warn("El modelo declinó analizar la foto.");
            return Optional.empty();
        }
        if (motivo.isPresent() && motivo.get().equals(StopReason.MAX_TOKENS)) {
            log.warn("La respuesta del análisis de la foto quedó incompleta.");
            return Optional.empty();
        }
        for (StructuredContentBlock<AnalisisDeFoto> bloque : respuesta.content()) {
            if (bloque.isText()) {
                return bloque.asText().text().aCaracteristicas(LocalDateTime.now());
            }
        }
        return Optional.empty();
    }

    private static Base64ImageSource.MediaType tipoDeMedio(TipoImagen tipo) {
        return switch (tipo) {
            case JPEG -> Base64ImageSource.MediaType.IMAGE_JPEG;
            case PNG -> Base64ImageSource.MediaType.IMAGE_PNG;
            case GIF -> Base64ImageSource.MediaType.IMAGE_GIF;
            case WEBP -> Base64ImageSource.MediaType.IMAGE_WEBP;
        };
    }

    @PreDestroy
    public void cerrar() {
        if (cliente != null) {
            cliente.close();
        }
    }
}
