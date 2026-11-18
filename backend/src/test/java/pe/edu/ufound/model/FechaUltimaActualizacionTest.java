package pe.edu.ufound.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueba unitaria de Objeto.getFechaUltimaActualizacion() (HU-15): fecha del último movimiento del caso. */
class FechaUltimaActualizacionTest {

    private static final LocalDateTime REPORTE = LocalDateTime.of(2026, 11, 1, 9, 30);

    private final Usuario ana = new Usuario("Ana", "Torres", "ana@u.pe", null, RolUsuario.ESTUDIANTE);
    private final Usuario oficina = new Usuario("Lucía", "Paredes", "lucia@u.pe", null,
            RolUsuario.OFICINA_OBJETOS_PERDIDOS);
    private final Objeto objeto = new Objeto("Casaca azul", "Casaca azul impermeable talla M", TipoObjeto.PERDIDO,
            CategoriaObjeto.ROPA, Pabellon.PABELLON_B, null, REPORTE, ana);

    @Test
    @DisplayName("PU-13: sin cambios de estado -> la fecha del reporte")
    void sinCambios() {
        assertEquals(REPORTE, objeto.getFechaUltimaActualizacion());
    }

    @Test
    @DisplayName("PU-14: un cambio de estado -> la fecha de ese cambio")
    void unCambio() {
        objeto.cambiarEstado(EstadoObjeto.EN_REVISION, "Revisando en almacén", oficina);
        LocalDateTime cambio = objeto.getHistorial().get(0).getFechaHora();
        assertEquals(cambio, objeto.getFechaUltimaActualizacion());
        assertTrue(cambio.isAfter(REPORTE));
    }

    @Test
    @DisplayName("PU-15: varios cambios -> la fecha del más reciente")
    void variosCambios() {
        objeto.cambiarEstado(EstadoObjeto.EN_REVISION, null, oficina);
        objeto.cambiarEstado(EstadoObjeto.COINCIDENCIA_DETECTADA, null, oficina);
        objeto.cambiarEstado(EstadoObjeto.DISPONIBLE_EN_OFICINA, "Ya está en la Oficina", oficina);
        HistorialEstado ultimo = objeto.getHistorial().get(0);
        assertEquals(EstadoObjeto.DISPONIBLE_EN_OFICINA, ultimo.getEstadoNuevo());
        assertEquals(ultimo.getFechaHora(), objeto.getFechaUltimaActualizacion());
    }

    @Test
    @DisplayName("PU-16: objeto entregado -> la fecha de la entrega")
    void entregado() {
        objeto.cambiarEstado(EstadoObjeto.DISPONIBLE_EN_OFICINA, null, oficina);
        objeto.entregar("María López", "DNI 71234567", null, oficina);
        HistorialEstado ultimo = objeto.getHistorial().get(0);
        assertEquals(EstadoObjeto.ENTREGADO, ultimo.getEstadoNuevo());
        assertEquals(ultimo.getFechaHora(), objeto.getFechaUltimaActualizacion());
    }

    @Test
    @DisplayName("PU-17: el dueño edita el reporte sin cambiar el estado -> sigue la fecha del reporte")
    void edicionNoCuenta() {
        objeto.editar("Casaca azul con capucha gris", Pabellon.PABELLON_E, null);
        assertEquals(REPORTE, objeto.getFechaUltimaActualizacion());
    }
}
