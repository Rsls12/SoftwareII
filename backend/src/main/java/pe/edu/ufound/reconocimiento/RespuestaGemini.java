package pe.edu.ufound.reconocimiento;

import pe.edu.ufound.model.CaracteristicasObjeto;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * HU-33: la respuesta de Gemini (generateContent) al pedirle que describa la foto. El modelo responde un JSON
 * con el formato de ReconocimientoGemini.ESQUEMA; aquí se lee y se convierte en CaracteristicasObjeto.
 */
public class RespuestaGemini {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final String problema;
    private final JsonNode analisis;

    public RespuestaGemini(JsonNode respuesta) {
        String bloqueo = respuesta.path("promptFeedback").path("blockReason").asString("");
        JsonNode candidato = respuesta.path("candidates").path(0);
        String motivoDeFin = candidato.path("finishReason").asString("");
        String texto = texto(candidato);

        if (!bloqueo.isBlank()) {
            this.problema = "Gemini bloqueó la foto (" + bloqueo + ")";
            this.analisis = null;
        } else if (candidato.isMissingNode()) {
            this.problema = "Gemini no devolvió ninguna respuesta";
            this.analisis = null;
        } else if (!motivoDeFin.isBlank() && !motivoDeFin.equals("STOP")) {
            // MAX_TOKENS deja el JSON cortado; SAFETY y similares, sin contenido útil.
            this.problema = "la respuesta de Gemini terminó por " + motivoDeFin;
            this.analisis = null;
        } else {
            this.analisis = leerJson(texto);
            this.problema = analisis == null ? "la respuesta de Gemini no tenía el formato pedido" : null;
        }
    }

    public boolean tieneProblema() {
        return problema != null;
    }

    public String getProblema() {
        return problema;
    }

    /** Vacío si hubo un problema o si en la foto no se reconoce un objeto. */
    public Optional<CaracteristicasObjeto> aCaracteristicas(LocalDateTime fechaHoraAnalisis) {
        if (tieneProblema() || !analisis.path("objetoIdentificable").asBoolean(false)) {
            return Optional.empty();
        }
        String objeto = analisis.path("objeto").asString("");
        if (objeto.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new CaracteristicasObjeto(objeto, lista(analisis.path("colores")),
                analisis.path("marca").asString(""), analisis.path("material").asString(""),
                lista(analisis.path("rasgosDistintivos")), fechaHoraAnalisis));
    }

    // El texto de la respuesta, sin las partes de "pensamiento" que algunos modelos pueden incluir.
    private static String texto(JsonNode candidato) {
        JsonNode partes = candidato.path("content").path("parts");
        for (int i = 0; i < partes.size(); i++) {
            JsonNode parte = partes.get(i);
            if (parte.has("text") && !parte.path("thought").asBoolean(false)) {
                return parte.path("text").asString("");
            }
        }
        return "";
    }

    private static JsonNode leerJson(String texto) {
        if (texto.isBlank()) {
            return null;
        }
        try {
            JsonNode leido = JSON.readTree(texto);
            return leido.isObject() ? leido : null;
        } catch (JacksonException e) {
            return null;
        }
    }

    private static List<String> lista(JsonNode valores) {
        List<String> textos = new ArrayList<>();
        for (int i = 0; i < valores.size(); i++) {
            textos.add(valores.get(i).asString(""));
        }
        return textos;
    }
}
