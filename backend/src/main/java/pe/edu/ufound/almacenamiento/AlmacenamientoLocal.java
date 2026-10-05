package pe.edu.ufound.almacenamiento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.ufound.excepcion.ImagenInvalidaException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class AlmacenamientoLocal implements AlmacenamientoImagenes {

    private final Path directorio;
    private final String rutaPublica;

    public AlmacenamientoLocal(@Value("${ufound.imagenes.directorio}") String directorio,
                               @Value("${ufound.imagenes.ruta-publica}") String rutaPublica) {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
        this.rutaPublica = rutaPublica.endsWith("/") ? rutaPublica.substring(0, rutaPublica.length() - 1) : rutaPublica;
        try {
            Files.createDirectories(this.directorio);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        byte[] datos = leer(archivo);
        TipoImagen tipo = TipoImagen.detectar(datos).orElseThrow(ImagenInvalidaException::new);
        String nombre = UUID.randomUUID() + "." + tipo.getExtension();
        try {
            Files.write(directorio.resolve(nombre), datos);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return rutaPublica + "/" + nombre;
    }

    @Override
    public void eliminar(String url) {
        if (url == null || !url.startsWith(rutaPublica + "/")) {
            return;
        }
        Path archivo = directorio.resolve(url.substring(rutaPublica.length() + 1)).normalize();
        if (!archivo.startsWith(directorio)) {
            return;
        }
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path getDirectorio() {
        return directorio;
    }

    public String getRutaPublica() {
        return rutaPublica;
    }

    private byte[] leer(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
