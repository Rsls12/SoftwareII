package pe.edu.ufound.almacenamiento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.ufound.excepcion.ImagenInvalidaException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Primary
@Service
public class AlmacenamientoSupabase implements AlmacenamientoImagenes {

    private final String url;
    private final String key;
    private final String bucket;
    private final HttpClient client;

    public AlmacenamientoSupabase(@Value("${supabase.url}") String url,
                                  @Value("${supabase.key}") String key,
                                  @Value("${supabase.bucket}") String bucket) {
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.key = key;
        this.bucket = bucket;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        byte[] datos = leer(archivo);
        TipoImagen tipo = TipoImagen.detectar(datos).orElseThrow(ImagenInvalidaException::new);
        String nombre = UUID.randomUUID() + "." + tipo.getExtension();

        String uploadUrl = url + "/storage/v1/object/" + bucket + "/" + nombre;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + key)
                .header("apikey", key)
                .header("Content-Type", obtenerMimeType(tipo))
                .POST(HttpRequest.BodyPublishers.ofByteArray(datos))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Error al subir a Supabase Storage: " + response.statusCode() + " " + response.body());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Subida a Supabase interrumpida", e);
        }

        return url + "/storage/v1/object/public/" + bucket + "/" + nombre;
    }

    @Override
    public void eliminar(String urlImagen) {
        if (urlImagen == null || !urlImagen.contains("/storage/v1/object/public/" + bucket + "/")) {
            return;
        }
        String prefix = url + "/storage/v1/object/public/" + bucket + "/";
        String nombre = urlImagen.substring(prefix.length());

        String deleteUrl = url + "/storage/v1/object/" + bucket;
        String jsonBody = "{\"prefixes\":[\"" + nombre + "\"]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(deleteUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + key)
                .header("apikey", key)
                .header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException ignored) {
        }
    }

    private String obtenerMimeType(TipoImagen tipo) {
        return switch (tipo) {
            case JPEG -> "image/jpeg";
            case PNG -> "image/png";
            case GIF -> "image/gif";
            case WEBP -> "image/webp";
        };
    }

    private byte[] leer(MultipartFile archivo) {
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
