package pe.edu.ufound.almacenamiento;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

/** Caja blanca de AlmacenamientoSupabase.guardar(): un caso por cada camino independiente posible. */
class GuardarCaminosTest {

    static final byte[] PNG = AlmacenamientoSupabaseTest.PNG;

    @Test
    @DisplayName("CB C1: 1-2-4-10, archivo nulo -> null")
    void archivoNulo() {
        assertNull(new AlmacenamientoSupabase("http://127.0.0.1:1", "clave-falsa", "objetos").guardar(null));
    }

    @Test
    @DisplayName("CB C2: 1-2-3-4-10, archivo vacío -> null")
    void archivoVacio() {
        assertNull(new AlmacenamientoSupabase("http://127.0.0.1:1", "clave-falsa", "objetos")
                .guardar(new MockMultipartFile("foto", new byte[0])));
    }

    @Test
    @DisplayName("CB C4: 1-2-3-5-6-7-8-10, Supabase responde 404 -> IllegalStateException")
    void supabaseResponde404() throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext("/", intercambio -> {
            intercambio.getRequestBody().readAllBytes();
            intercambio.sendResponseHeaders(404, -1);
            intercambio.close();
        });
        servidor.start();
        try {
            AlmacenamientoSupabase almacenamiento = new AlmacenamientoSupabase(
                    "http://127.0.0.1:" + servidor.getAddress().getPort(), "clave-falsa", "objetos");
            assertThrows(IllegalStateException.class,
                    () -> almacenamiento.guardar(new MockMultipartFile("foto", "x.png", "image/png", PNG)));
        } finally {
            servidor.stop(0);
        }
    }

    @Test
    @DisplayName("CB C5: 1-2-3-5-6-7-9-10, Supabase responde 200 -> URL pública")
    void supabaseResponde200() {
        SupabaseFalso supabase = new SupabaseFalso();
        try {
            String url = new AlmacenamientoSupabase(supabase.url(), "clave-falsa", "objetos")
                    .guardar(new MockMultipartFile("foto", "x.png", "image/png", PNG));
            assertTrue(url.startsWith(supabase.url() + "/storage/v1/object/public/objetos/"));
            assertTrue(url.endsWith(".png"));
        } finally {
            supabase.cerrar();
        }
    }
}
