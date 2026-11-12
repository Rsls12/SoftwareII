package pe.edu.ufound.reconocimiento;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servidor local que hace de API de Gemini en las pruebas (HU-33): responde lo que la prueba le indica
 * y anota cada petición que recibe. Así las pruebas no usan red ni una clave real.
 */
public class GeminiFalso {

    public record Peticion(String metodo, String ruta, String consulta, String apiKey, String cuerpo) {
    }

    private record Respuesta(int estado, String cuerpo) {
    }

    private final HttpServer servidor;
    private final List<Peticion> peticiones = new CopyOnWriteArrayList<>();
    // Respuestas de un modelo en particular; los demás modelos reciben la respuesta general.
    private final Map<String, Respuesta> porModelo = new ConcurrentHashMap<>();
    private volatile int estado = 200;
    private volatile String respuesta = "{}";

    public GeminiFalso() {
        try {
            servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        servidor.createContext("/", intercambio -> {
            peticiones.add(new Peticion(intercambio.getRequestMethod(), intercambio.getRequestURI().getPath(),
                    intercambio.getRequestURI().getQuery(),
                    intercambio.getRequestHeaders().getFirst("x-goog-api-key"),
                    new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            Respuesta elegida = new Respuesta(estado, respuesta);
            for (Map.Entry<String, Respuesta> especial : porModelo.entrySet()) {
                if (intercambio.getRequestURI().getPath().contains("/models/" + especial.getKey() + ":")) {
                    elegida = especial.getValue();
                }
            }
            byte[] cuerpo = elegida.cuerpo().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(elegida.estado(), cuerpo.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        servidor.start();
    }

    /** Respuesta de generateContent en la que el modelo "escribió" este texto (el JSON del análisis). */
    public void responderTexto(String textoDelModelo) {
        responderTexto(textoDelModelo, "STOP");
    }

    public void responderTexto(String textoDelModelo, String motivoDeFin) {
        estado = 200;
        respuesta = """
                {"candidates": [{"content": {"role": "model", "parts": [{"text": %s}]}, "finishReason": "%s"}],
                 "usageMetadata": {"promptTokenCount": 1290, "candidatesTokenCount": 60}}
                """.formatted(comoTextoJson(textoDelModelo), motivoDeFin);
    }

    public void responder(String json) {
        estado = 200;
        respuesta = json;
    }

    public void responderError(int estadoHttp) {
        estado = estadoHttp;
        respuesta = error(estadoHttp);
    }

    /** Este modelo responde con un error; los demás siguen con la respuesta general. */
    public void responderErrorEnModelo(String modelo, int estadoHttp) {
        porModelo.put(modelo, new Respuesta(estadoHttp, error(estadoHttp)));
    }

    private static String error(int estadoHttp) {
        return """
                {"error": {"code": %d, "message": "This model is currently experiencing high demand.", "status": "UNAVAILABLE"}}
                """.formatted(estadoHttp);
    }

    public String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    public List<Peticion> peticiones() {
        return peticiones;
    }

    public void cerrar() {
        servidor.stop(0);
    }

    private static String comoTextoJson(String texto) {
        return "\"" + texto.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
    }
}
