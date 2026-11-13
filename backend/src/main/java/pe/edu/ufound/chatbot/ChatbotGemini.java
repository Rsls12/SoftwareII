package pe.edu.ufound.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class ChatbotGemini {

    public static final int PREGUNTA_MAXIMA = 300;

    public static final String RESPUESTA_FIJA = "Ahora no puedo responder. Acércate a la Oficina de objetos perdidos: "
            + "Pabellón A1, piso 2, de 8:00 a. m. a 6:00 p. m.";

    private static final String INSTRUCCIONES = """
            Eres el asistente de U-Found, el sistema de objetos perdidos y encontrados de la universidad.
            Hablas con estudiantes. Responde en español, con calma, en tono amable y en pocas frases (máximo 3).
            Si alguien perdió algo, tranquilízalo y dile que lo reporte en la opción "Reportar" de la web
            y que revise "Buscar", porque ahí aparecen los objetos que se encuentran en el campus.
            Datos de la Oficina de objetos perdidos: Pabellón A1, piso 2. Atiende de 8:00 a. m. a 6:00 p. m.
            Responde solo sobre objetos perdidos, U-Found y la Oficina. Si preguntan otra cosa o un dato que no está aquí,
            di que no lo sabes y que consulten en la Oficina. No inventes datos.
            """;

    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final Logger log = LoggerFactory.getLogger(ChatbotGemini.class);

    private final String apiKey;
    private final String modelo;
    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    public ChatbotGemini(@Value("${chatbot.api-key:}") String apiKey,
                         @Value("${chatbot.modelo:gemini-3.1-flash-lite}") String modelo) {
        this.apiKey = apiKey == null || apiKey.isBlank() ? null : apiKey.trim();
        this.modelo = modelo.trim();
    }

    public String responder(String pregunta) {
        if (pregunta == null || pregunta.isBlank()) {
            return "Escribe tu pregunta y te ayudo.";
        }
        if (pregunta.length() > PREGUNTA_MAXIMA) {
            return "Tu pregunta es muy larga. Escríbela en menos de " + PREGUNTA_MAXIMA + " caracteres.";
        }
        if (apiKey == null) {
            return RESPUESTA_FIJA;
        }
        try {
            HttpResponse<String> respuesta = client.send(peticion(pregunta.trim()), HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() != 200) {
                log.warn("Chatbot: Gemini respondió {}.", respuesta.statusCode());
                return RESPUESTA_FIJA;
            }
            String texto = leerTexto(json.readTree(respuesta.body()));
            return texto.isBlank() ? RESPUESTA_FIJA : texto;
        } catch (IOException | JacksonException e) {
            log.warn("Chatbot: no se pudo obtener respuesta de Gemini ({}).", e.getMessage());
            return RESPUESTA_FIJA;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RESPUESTA_FIJA;
        }
    }

    private HttpRequest peticion(String pregunta) {
        return HttpRequest.newBuilder()
                .uri(URI.create(URL + modelo + ":generateContent"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo(pregunta)))
                .build();
    }

    private String cuerpo(String pregunta) {
        ObjectNode cuerpo = json.createObjectNode();
        cuerpo.putObject("systemInstruction").putArray("parts").addObject().put("text", INSTRUCCIONES);
        ObjectNode mensaje = cuerpo.putArray("contents").addObject();
        mensaje.put("role", "user");
        mensaje.putArray("parts").addObject().put("text", pregunta);
        return json.writeValueAsString(cuerpo);
    }

    static String leerTexto(JsonNode respuesta) {
        StringBuilder texto = new StringBuilder();
        for (JsonNode parte : respuesta.path("candidates").path(0).path("content").path("parts")) {
            texto.append(parte.path("text").asString(""));
        }
        return texto.toString().trim();
    }
}
