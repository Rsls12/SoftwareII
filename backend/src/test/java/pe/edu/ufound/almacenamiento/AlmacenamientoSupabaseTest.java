package pe.edu.ufound.almacenamiento;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import pe.edu.ufound.excepcion.ImagenInvalidaException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AlmacenamientoSupabaseTest {

    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};

    SupabaseFalso supabase;
    AlmacenamientoSupabase almacenamiento;

    @BeforeEach
    void iniciar() {
        supabase = new SupabaseFalso();
        almacenamiento = new AlmacenamientoSupabase(supabase.url() + "/", "clave-falsa", "objetos");
    }

    @AfterEach
    void cerrar() {
        supabase.cerrar();
    }

    @Test
    void subeLaFotoYDevuelveSuUrlPublica() {
        String url = almacenamiento.guardar(new MockMultipartFile("foto", "mochila.png", "image/png", PNG));

        String prefijo = supabase.url() + "/storage/v1/object/public/objetos/";
        assertTrue(url.startsWith(prefijo));
        assertTrue(url.endsWith(".png"));

        assertEquals(1, supabase.peticiones().size());
        SupabaseFalso.Peticion subida = supabase.peticiones().get(0);
        assertEquals("POST", subida.metodo());
        assertEquals("/storage/v1/object/objetos/" + url.substring(prefijo.length()), subida.ruta());
        assertEquals("image/png", subida.tipoContenido());
        assertArrayEquals(PNG, subida.cuerpo());
    }

    @Test
    void detectaElTipoPorElContenidoYNoPorElNombre() {
        String url = almacenamiento.guardar(new MockMultipartFile("foto", "foto.png", "image/png", JPEG));

        assertTrue(url.endsWith(".jpg"));
        assertEquals("image/jpeg", supabase.peticiones().get(0).tipoContenido());
    }

    @Test
    void rechazaArchivosQueNoSonImagenesSinSubirlos() {
        MockMultipartFile falso = new MockMultipartFile("foto", "foto.png", "image/png", "hola".getBytes());

        assertThrows(ImagenInvalidaException.class, () -> almacenamiento.guardar(falso));
        assertTrue(supabase.peticiones().isEmpty());
    }

    @Test
    void sinFotoNoSubeNada() {
        assertNull(almacenamiento.guardar(null));
        assertNull(almacenamiento.guardar(new MockMultipartFile("foto", new byte[0])));
        assertTrue(supabase.peticiones().isEmpty());
    }

    @Test
    void eliminaSoloFotosDeSuBucket() {
        String url = almacenamiento.guardar(new MockMultipartFile("foto", "a.png", "image/png", PNG));
        String nombre = url.substring(url.lastIndexOf('/') + 1);

        almacenamiento.eliminar("/imagenes/ajena.png");
        almacenamiento.eliminar(null);
        assertEquals(1, supabase.peticiones().size());

        almacenamiento.eliminar(url);
        SupabaseFalso.Peticion borrado = supabase.peticiones().get(1);
        assertEquals("DELETE", borrado.metodo());
        assertEquals("/storage/v1/object/objetos", borrado.ruta());
        assertTrue(new String(borrado.cuerpo(), StandardCharsets.UTF_8).contains(nombre));
    }
}
