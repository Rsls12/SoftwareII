package pe.edu.ufound.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.ufound.model.Coincidencia;
import pe.edu.ufound.model.Objeto;

import java.util.List;

public interface CoincidenciaRepository extends JpaRepository<Coincidencia, Long> {

    // HU-23: las coincidencias de un objeto, sea el perdido o el encontrado, de la más parecida a la menos.
    List<Coincidencia> findByObjetoPerdidoOrObjetoEncontradoOrderByPuntajeDesc(Objeto perdido, Objeto encontrado);

    default List<Coincidencia> de(Objeto objeto) {
        return findByObjetoPerdidoOrObjetoEncontradoOrderByPuntajeDesc(objeto, objeto);
    }

    // HU-24: todas las coincidencias, de la más parecida a la menos, para que la Oficina priorice su validación.
    List<Coincidencia> findAllByOrderByPuntajeDesc();
}
