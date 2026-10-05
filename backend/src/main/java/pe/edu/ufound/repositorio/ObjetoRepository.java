package pe.edu.ufound.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.ufound.model.Objeto;
import pe.edu.ufound.model.Usuario;

import java.util.List;

public interface ObjetoRepository extends JpaRepository<Objeto, Long> {

    List<Objeto> findByActivoTrueOrderByFechaHoraReporteDesc();

    List<Objeto> findByReportadoPorAndActivoTrueOrderByFechaHoraReporteDesc(Usuario reportadoPor);
}
