package pe.edu.ufound.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.ufound.model.CategoriaObjeto;
import pe.edu.ufound.model.Entrega;
import pe.edu.ufound.model.EstadoObjeto;
import pe.edu.ufound.model.FrecuenciaCategoria;
import pe.edu.ufound.model.FrecuenciaUbicacion;
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

    List<Objeto> findByTipoAndCategoriaAndActivoTrue(TipoObjeto tipo, CategoriaObjeto categoria);

    // HU-23: solo pueden ser el mismo objeto los del tipo contrario y de la misma categoría.
    default List<Objeto> candidatosDeCoincidencia(Objeto objeto) {
        TipoObjeto contrario = objeto.esPerdido() ? TipoObjeto.ENCONTRADO : TipoObjeto.PERDIDO;
        return findByTipoAndCategoriaAndActivoTrue(contrario, objeto.getCategoria());
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

    // HU-39: ranking de categorías de los objetos activos (los reportes eliminados no cuentan).
    // shortcut: cuenta en memoria como buscar(); pasar a un GROUP BY en la base si hay miles de objetos.
    default List<FrecuenciaCategoria> categoriasMasFrecuentes() {
        return FrecuenciaCategoria.de(findByActivoTrueOrderByFechaHoraReporteDesc());
    }

    // HU-40: ranking de pabellones con más objetos perdidos (mismo shortcut que categoriasMasFrecuentes).
    default List<FrecuenciaUbicacion> zonasConMasPerdidas() {
        return FrecuenciaUbicacion.de(findByActivoTrueOrderByFechaHoraReporteDesc());
    }

    // HU-19: los últimos objetos reportados.
    default List<Objeto> recientes(int cantidad) {
        return findByActivoTrueOrderByFechaHoraReporteDesc().stream().limit(cantidad).toList();
    }

    // ---------- Dashboard ----------

    long countByActivoTrue();

    long countByActivoTrueAndTipo(TipoObjeto tipo);

    long countByActivoTrueAndEstado(EstadoObjeto estado);

    // Casos sin movimiento, el más antiguo primero.
    default List<Objeto> sinMovimiento() {
        return findByActivoTrueOrderByFechaHoraReporteDesc().stream()
                .filter(Objeto::necesitaAtencion)
                .sorted(Comparator.comparing(Objeto::getFechaUltimaActualizacion))
                .toList();
    }

    default List<Entrega> ultimasEntregas(int cantidad) {
        return entregas().stream().limit(cantidad).toList();
    }
}
