package pe.edu.ufound.almacenamiento;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

public enum TipoImagen {
    JPEG("jpg", datos -> empiezaCon(datos, 0, 0xFF, 0xD8, 0xFF)),
    PNG("png", datos -> empiezaCon(datos, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)),
    GIF("gif", datos -> empiezaCon(datos, 0, 0x47, 0x49, 0x46, 0x38)),
    WEBP("webp", datos -> empiezaCon(datos, 0, 0x52, 0x49, 0x46, 0x46) && empiezaCon(datos, 8, 0x57, 0x45, 0x42, 0x50));

    private final String extension;
    private final Predicate<byte[]> firma;

    TipoImagen(String extension, Predicate<byte[]> firma) {
        this.extension = extension;
        this.firma = firma;
    }

    public static Optional<TipoImagen> detectar(byte[] datos) {
        if (datos == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(tipo -> tipo.firma.test(datos)).findFirst();
    }

    public String getExtension() {
        return extension;
    }

    private static boolean empiezaCon(byte[] datos, int desde, int... bytes) {
        if (datos.length < desde + bytes.length) {
            return false;
        }
        for (int i = 0; i < bytes.length; i++) {
            if ((datos[desde + i] & 0xFF) != bytes[i]) {
                return false;
            }
        }
        return true;
    }
}
