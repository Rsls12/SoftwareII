package pe.edu.ufound.reconocimiento;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servidor local que hace de API de Anthropic en las pruebas (HU-33): responde lo que la prueba le indica
 * y anota cada petición que recibe. Así las pruebas no usan red, ni clave real, ni gastan créditos.
 */
public class ClaudeFalso {

    public record Peticion(String metodo, String ruta, String autorizacion, String beta, String cuerpo) {
    }

    private final HttpServer servidor;
    private final List<Peticion> peticiones = new CopyOnWriteArrayList<>();
    private volatile int estado = 200;
    private volatile String respuesta = "{}";

    public ClaudeFalso() {
        try {
            servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        servidor.createContext("/", intercambio -> {
            peticiones.add(new Peticion(intercambio.getRequestMethod(), intercambio.getRequestURI().getPath(),
                    intercambio.getRequestHeaders().getFirst("Authorization"),
                    intercambio.getRequestHeaders().getFirst("anthropic-beta"),
                    new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            byte[] cuerpo = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, cuerpo.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        servidor.start();
    }

    /** Respuesta de la API de mensajes con el JSON que "escribió" el modelo como texto. */
    public void responderAnalisis(String jsonDelModelo) {
        responderMensaje(jsonDelModelo, "end_turn");
    }

    public void responderMensaje(String textoDelModelo, String motivoDeFin) {
        estado = 200;
        respuesta = """
                {"id":"msg_prueba","type":"message","role":"assistant","model":"claude-opus-5-5",
                 "content":[{"type":"text","text":%s}],
                 "stop_reason":"%s","stop_sequence":null,
                 "usage":{"input_tokens":1200,"output_tokens":80}}
                """.formatted(comoTextoJson(textoDelModelo), motivoDeFin);
    }

    /** Una negativa del modelo: HTTP 200 con stop_reason "refusal" y sin contenido. */
    public void responderNegativa() {
        estado = 200;
        respuesta = """
                {"id":"msg_prueba","type":"message","role":"assistant","model":"claude-opus-5-5",
                 "content":[],"stop_reason":"refusal","stop_sequence":null,
                 "usage":{"input_tokens":1200,"output_tokens":0}}
                """;
    }

    public void responderError(int estadoHttp) {
        estado = estadoHttp;
        respuesta = """
                {"type":"error","error":{"type":"api_error","message":"error de prueba"}}
                """;
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
