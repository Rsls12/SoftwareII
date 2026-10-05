package pe.edu.ufound.almacenamiento;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import pe.edu.ufound.excepcion.ImagenInvalidaException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AlmacenamientoLocalTest {

    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};

    @TempDir
    Path carpeta;

    @Test
    void guardaLaFotoYDevuelveSuUrl() throws Exception {
        AlmacenamientoLocal almacenamiento = new AlmacenamientoLocal(carpeta.toString(), "/imagenes");

        String url = almacenamiento.guardar(new MockMultipartFile("foto", "mochila.png", "image/png", PNG));

        assertTrue(url.startsWith("/imagenes/"));
        assertTrue(url.endsWith(".png"));
        Path archivo = carpeta.resolve(url.substring("/imagenes/".length()));
        assertArrayEquals(PNG, Files.readAllBytes(archivo));
    }

    @Test
    void detectaElTipoPorElContenidoYNoPorElNombre() {
        AlmacenamientoLocal almacenamiento = new AlmacenamientoLocal(carpeta.toString(), "/imagenes");

        String url = almacenamiento.guardar(new MockMultipartFile("foto", "foto.png", "image/png", JPEG));

        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    void rechazaArchivosQueNoSonImagenes() {
        AlmacenamientoLocal almacenamiento = new AlmacenamientoLocal(carpeta.toString(), "/imagenes");
        MockMultipartFile falso = new MockMultipartFile("foto", "foto.png", "image/png", "hola".getBytes());

        assertThrows(ImagenInvalidaException.class, () -> almacenamiento.guardar(falso));
    }

    @Test
    void sinFotoNoGuardaNada() throws Exception {
        AlmacenamientoLocal almacenamiento = new AlmacenamientoLocal(carpeta.toString(), "/imagenes");

        assertNull(almacenamiento.guardar(null));
        assertNull(almacenamiento.guardar(new MockMultipartFile("foto", new byte[0])));
        try (var archivos = Files.list(carpeta)) {
            assertEquals(0, archivos.count());
        }
    }

    @Test
    void eliminaSoloArchivosDeSuCarpeta() throws Exception {
        AlmacenamientoLocal almacenamiento = new AlmacenamientoLocal(carpeta.resolve("imagenes").toString(), "/imagenes");
        Path ajeno = Files.writeString(carpeta.resolve("ajeno.txt"), "x");
        String url = almacenamiento.guardar(new MockMultipartFile("foto", "a.png", "image/png", PNG));

        almacenamiento.eliminar("/imagenes/../ajeno.txt");
        almacenamiento.eliminar(url);

        assertTrue(Files.exists(ajeno));
        try (var archivos = Files.list(carpeta.resolve("imagenes"))) {
            assertEquals(0, archivos.count());
        }
    }
}
