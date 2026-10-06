package pe.edu.ufound.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.ufound.model.CategoriaObjeto;
import pe.edu.ufound.model.Entrega;
import pe.edu.ufound.model.EstadoObjeto;
import pe.edu.ufound.model.Objeto;
import pe.edu.ufound.model.Pabellon;
import pe.edu.ufound.model.TipoObjeto;
import pe.edu.ufound.model.Usuario;

import java.util.Comparator;
import java.util.List;

public interface ObjetoRepository extends JpaRepository<Objeto, Long> {

    List<Objeto> findByActivoTrueOrderByFechaHoraReporteDesc();

    List<Objeto> findByReportadoPorAndActivoTrueOrderByFechaHoraReporteDesc(Usuario reportadoPor);

    // HU-16, HU-17: cada objeto decide si cumple los filtros (Objeto.coincideCon).
    default List<Objeto> buscar(String texto, TipoObjeto tipo, CategoriaObjeto categoria,
                                Pabellon ubicacion, EstadoObjeto estado) {
        return findByActivoTrueOrderByFechaHoraReporteDesc().stream()
                .filter(objeto -> objeto.coincideCon(texto, tipo, categoria, ubicacion, estado))
                .toList();
    }

    @Query("select distinct o from Objeto o join o.entregas")
    List<Objeto> findEntregados();

    // HU-22: historial de entregas, de la más reciente a la más antigua.
    default List<Entrega> entregas() {
        return findEntregados().stream()
                .map(Objeto::getEntrega)
                .sorted(Comparator.comparing(Entrega::getFechaHoraEntrega).reversed())
                .toList();
    }

    // HU-19: los últimos objetos reportados.
    default List<Objeto> recientes(int cantidad) {
        return findByActivoTrueOrderByFechaHoraReporteDesc().stream().limit(cantidad).toList();
    }
}
