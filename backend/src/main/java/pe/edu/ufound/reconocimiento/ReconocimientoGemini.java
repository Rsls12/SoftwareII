package pe.edu.ufound.reconocimiento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.ufound.almacenamiento.TipoImagen;
import pe.edu.ufound.model.CaracteristicasObjeto;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * HU-33: reconoce las características de la foto con Gemini (API de Google AI Studio, plan gratuito), pidiéndole
 * la respuesta en JSON con un esquema fijo. Sin GEMINI_API_KEY queda desactivado y el registro sigue igual.
 */
@Service
public class ReconocimientoGemini implements ReconocimientoImagenes {

    // Por modelo: si uno no contesta a tiempo se prueba el siguiente, así la persona no espera de más.
    private static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(20);

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

    // El formato de la respuesta (JSON Schema): el modelo solo puede responder con estos campos.
    static final String ESQUEMA = """
            {"type": "object",
             "properties": {
               "objetoIdentificable": {"type": "boolean", "description": "true si la foto muestra un objeto reconocible"},
               "objeto": {"type": "string", "description": "Qué objeto es, en pocas palabras. Ej.: Mochila, Laptop"},
               "colores": {"type": "array", "items": {"type": "string"}, "description": "Colores principales"},
               "marca": {"type": "string", "description": "Marca si se lee o reconoce con claridad; si no, vacío"},
               "material": {"type": "string", "description": "Material principal si se distingue; si no, vacío"},
               "rasgosDistintivos": {"type": "array", "items": {"type": "string"},
                                     "description": "Detalles que lo diferencian: stickers, rayones, llaveros"}},
             "required": ["objetoIdentificable", "objeto", "colores", "marca", "material", "rasgosDistintivos"]}
            """;

    private static final Logger log = LoggerFactory.getLogger(ReconocimientoGemini.class);

    private final String apiKey;
    // En orden de preferencia: si uno está saturado (algo común en el plan gratuito), se prueba el siguiente.
    private final List<String> modelos = new ArrayList<>();
    private final String url;
    private final HttpClient client;
    private final JsonMapper json = JsonMapper.builder().build();

    public ReconocimientoGemini(@Value("${gemini.api-key:}") String apiKey,
                                @Value("${gemini.modelos:gemini-3.1-flash-lite,gemini-3.5-flash-lite}") List<String> modelos,
                                @Value("${gemini.url:https://generativelanguage.googleapis.com}") String url) {
        this.apiKey = apiKey == null || apiKey.isBlank() ? null : apiKey.trim();
        for (String modelo : modelos) {
            if (modelo != null && !modelo.isBlank()) {
                this.modelos.add(modelo.trim());
            }
        }
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        if (this.apiKey == null) {
            log.info("GEMINI_API_KEY no está configurada: el análisis de fotos (HU-33) queda desactivado.");
        }
    }

    public boolean estaDisponible() {
        return apiKey != null && !modelos.isEmpty();
    }

    @Override
    public Optional<CaracteristicasObjeto> analizar(MultipartFile foto) {
        if (!estaDisponible() || foto == null || foto.isEmpty()) {
            return Optional.empty();
        }
        byte[] datos;
        try {
            datos = foto.getBytes();
        } catch (IOException e) {
            log.warn("Análisis de foto omitido: no se pudo leer la foto ({}).", e.getMessage());
            return Optional.empty();
        }
        Optional<TipoImagen> tipo = TipoImagen.detectar(datos);
        if (tipo.isEmpty() || tipoDeMedio(tipo.get()) == null) {
            return Optional.empty();
        }
        String cuerpo = cuerpo(datos, tipoDeMedio(tipo.get()));
        for (String modelo : modelos) {
            try {
                HttpResponse<String> respuesta = client.send(peticion(modelo, cuerpo),
                        HttpResponse.BodyHandlers.ofString());
                if (respuesta.statusCode() == 200) {
                    RespuestaGemini gemini = new RespuestaGemini(json.readTree(respuesta.body()));
                    if (gemini.tieneProblema()) {
                        log.warn("Análisis de foto omitido: {}.", gemini.getProblema());
                    }
                    return gemini.aCaracteristicas(LocalDateTime.now());
                }
                log.warn("Gemini ({}) respondió {} ({}).", modelo, respuesta.statusCode(), resumen(respuesta.body()));
                if (!esPasajero(respuesta.statusCode())) {
                    return Optional.empty();
                }
            } catch (HttpTimeoutException e) {
                log.warn("Gemini ({}) no respondió en {} segundos.", modelo, TIEMPO_MAXIMO.toSeconds());
            } catch (IOException e) {
                log.warn("No se pudo conectar con Gemini ({}): {}.", modelo, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            } catch (JacksonException e) {
                log.warn("Análisis de foto omitido: la respuesta de Gemini no era JSON válido.");
                return Optional.empty();
            }
        }
        log.warn("Análisis de foto omitido: ningún modelo de Gemini estuvo disponible ({}).", modelos);
        return Optional.empty();
    }

    // Saturado (503), límite del plan gratuito (429) u otro error del servidor: puede funcionar con otro modelo.
    // Una clave inválida (403) o una petición rechazada (400) fallaría igual con cualquiera.
    private static boolean esPasajero(int estado) {
        return estado == 429 || estado >= 500;
    }

    private HttpRequest peticion(String modelo, String cuerpo) {
        // La clave va en una cabecera y no en la URL, para que no quede en los registros.
        return HttpRequest.newBuilder()
                .uri(URI.create(url + "/v1beta/models/" + modelo + ":generateContent"))
                .timeout(TIEMPO_MAXIMO)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();
    }

    private String cuerpo(byte[] datos, String tipoDeMedio) {
        ObjectNode cuerpo = json.createObjectNode();
        cuerpo.putObject("systemInstruction").putArray("parts").addObject().put("text", INSTRUCCIONES);
        ObjectNode mensaje = cuerpo.putArray("contents").addObject();
        mensaje.put("role", "user");
        ArrayNode partes = mensaje.putArray("parts");
        ObjectNode imagen = partes.addObject().putObject("inlineData");
        imagen.put("mimeType", tipoDeMedio);
        imagen.put("data", Base64.getEncoder().encodeToString(datos));
        partes.addObject().put("text", "Describe el objeto de esta foto.");
        ObjectNode configuracion = cuerpo.putObject("generationConfig");
        configuracion.put("responseMimeType", "application/json");
        configuracion.set("responseJsonSchema", json.readTree(ESQUEMA));
        return json.writeValueAsString(cuerpo);
    }

    // Gemini acepta JPG, PNG y WEBP; los GIF se registran igual, pero sin análisis.
    private static String tipoDeMedio(TipoImagen tipo) {
        return switch (tipo) {
            case JPEG -> "image/jpeg";
            case PNG -> "image/png";
            case WEBP -> "image/webp";
            case GIF -> null;
        };
    }

    private static String resumen(String cuerpo) {
        if (cuerpo == null) {
            return "";
        }
        String limpio = cuerpo.replaceAll("\\s+", " ").trim();
        return limpio.length() <= 200 ? limpio : limpio.substring(0, 200) + "…";
    }
}
