package pe.edu.ufound.almacenamiento;

import org.springframework.web.multipart.MultipartFile;

public interface AlmacenamientoImagenes {

    String guardar(MultipartFile archivo);

    void eliminar(String url);
}
