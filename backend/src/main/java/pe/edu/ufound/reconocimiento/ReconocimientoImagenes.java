package pe.edu.ufound.reconocimiento;

import org.springframework.web.multipart.MultipartFile;
import pe.edu.ufound.model.CaracteristicasObjeto;

import java.util.Optional;

/** HU-33: reconoce en la foto de un objeto sus características visuales. */
public interface ReconocimientoImagenes {

    /**
     * Las características que se reconocen en la foto. Vacío si no hay foto, si el servicio no está disponible
     * o si en la foto no se reconoce ningún objeto: el análisis ayuda al registro, pero nunca lo impide.
     */
    Optional<CaracteristicasObjeto> analizar(MultipartFile foto);
}
