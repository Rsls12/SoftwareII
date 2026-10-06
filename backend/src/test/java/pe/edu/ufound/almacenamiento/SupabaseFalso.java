package pe.edu.ufound.almacenamiento;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Servidor local que hace de Supabase Storage en las pruebas: responde 200 y anota lo que recibe. */
public class SupabaseFalso {

    public record Peticion(String metodo, String ruta, String tipoContenido, byte[] cuerpo) {
    }

    private final HttpServer servidor;
    private final List<Peticion> peticiones = new CopyOnWriteArrayList<>();

    public SupabaseFalso() {
        try {
            servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        servidor.createContext("/", intercambio -> {
            peticiones.add(new Peticion(intercambio.getRequestMethod(), intercambio.getRequestURI().getPath(),
                    intercambio.getRequestHeaders().getFirst("Content-Type"),
                    intercambio.getRequestBody().readAllBytes()));
            intercambio.sendResponseHeaders(200, -1);
            intercambio.close();
        });
        servidor.start();
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
}
