package pe.edu.ufound.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-28, caja negra de Objeto.necesitaAtencion(): partición equivalente y valores límite (casos CN-01 a CN-09).
 * Un caso abierto necesita atención si lleva más de 7 días sin cambios de estado.
 * Cada prueba fija la fecha "actual" para que el límite de 7 días se pueda revisar con exactitud.
 */
class NecesitaAtencionTest {

    static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 17, 10, 0);

    final Usuario ana = new Usuario("Ana", "Torres Ríos", "ana.torres@universidad.edu.pe", null, RolUsuario.ESTUDIANTE);
    final Usuario lucia = new Usuario("Lucía", "Paredes Vega", "lucia.paredes@universidad.edu.pe", null,
            RolUsuario.OFICINA_OBJETOS_PERDIDOS);

    Objeto reportadoEl(LocalDateTime fecha) {
        return new Objeto("Mochila negra", "Mochila Totto negra con un llavero de Lima.", TipoObjeto.PERDIDO,
                CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_A, null, fecha, ana);
    }

    @Test
    @DisplayName("CN-01: activo, reportado hace 10 días y sin cambios de estado -> verdadero")
    void reportadoHaceDiezDiasSinCambios() {
        Objeto objeto = reportadoEl(AHORA.minusDays(10));

        assertTrue(objeto.necesitaAtencion(AHORA));
        // La versión sin parámetros, la que usa el dashboard, toma la fecha real y da el mismo resultado.
        assertTrue(objeto.necesitaAtencion());
    }

    @Test
    @DisplayName("CN-02: activo, en revisión, último cambio de estado hace 8 días -> verdadero")
    void enRevisionConUltimoCambioHaceOchoDias() {
        Objeto objeto = reportadoEl(LocalDateTime.now().minusDays(9));
        objeto.cambiarEstado(EstadoObjeto.EN_REVISION, "Revisando objetos encontrados.", lucia);
        LocalDateTime ultimoCambio = objeto.getFechaUltimaActualizacion();

        assertTrue(objeto.necesitaAtencion(ultimoCambio.plusDays(8)));
    }

    @Test
    @DisplayName("CN-03: activo, reportado hace 3 días -> falso")
    void reportadoHaceTresDias() {
        Objeto objeto = reportadoEl(AHORA.minusDays(3));

        assertFalse(objeto.necesitaAtencion(AHORA));
    }

    @Test
    @DisplayName("CN-04: último cambio de estado hace 7 días exactos (límite) -> falso")
    void ultimoCambioHaceSieteDiasExactos() {
        Objeto objeto = reportadoEl(LocalDateTime.now().minusDays(1));
        objeto.cambiarEstado(EstadoObjeto.EN_REVISION, "Revisando objetos encontrados.", lucia);
        LocalDateTime ultimoCambio = objeto.getFechaUltimaActualizacion();

        assertFalse(objeto.necesitaAtencion(ultimoCambio.plusDays(7)));
    }

    @Test
    @DisplayName("CN-05: último cambio de estado hace 7 días y 1 minuto (límite) -> verdadero")
    void ultimoCambioHaceSieteDiasYUnMinuto() {
        Objeto objeto = reportadoEl(LocalDateTime.now().minusDays(1));
        objeto.cambiarEstado(EstadoObjeto.EN_REVISION, "Revisando objetos encontrados.", lucia);
        LocalDateTime ultimoCambio = objeto.getFechaUltimaActualizacion();

        assertTrue(objeto.necesitaAtencion(ultimoCambio.plusDays(7).plusMinutes(1)));
    }

    @Test
    @DisplayName("CN-06: eliminado por su dueño, reportado hace 10 días -> falso")
    void eliminadoPorSuDueno() {
        Objeto objeto = reportadoEl(AHORA.minusDays(10));
        objeto.desactivar();

        assertFalse(objeto.necesitaAtencion(AHORA));
    }

    @Test
    @DisplayName("CN-07: entregado hace 10 días -> falso")
    void entregadoHaceDiezDias() {
        Objeto objeto = reportadoEl(LocalDateTime.now().minusDays(12));
        objeto.entregar("María Fernández López", "DNI 71234567", "Describió bien la mochila.", lucia);
        LocalDateTime entrega = objeto.getFechaUltimaActualizacion();

        assertFalse(objeto.necesitaAtencion(entrega.plusDays(10)));
    }

    @Test
    @DisplayName("CN-08: archivado hace 10 días -> falso")
    void archivadoHaceDiezDias() {
        Objeto objeto = reportadoEl(LocalDateTime.now().minusDays(12));
        objeto.cambiarEstado(EstadoObjeto.ARCHIVADO, "Nadie lo reclamó.", lucia);
        LocalDateTime archivo = objeto.getFechaUltimaActualizacion();

        assertFalse(objeto.necesitaAtencion(archivo.plusDays(10)));
    }

    @Test
    @DisplayName("CN-09: reportado hace 20 días, con un cambio de estado hace 2 días -> falso")
    void reportadoHaceVeinteDiasConCambioReciente() {
        Objeto objeto = reportadoEl(LocalDateTime.now().minusDays(18));
        objeto.cambiarEstado(EstadoObjeto.EN_REVISION, "Revisando objetos encontrados.", lucia);
        LocalDateTime ultimoCambio = objeto.getFechaUltimaActualizacion();

        assertFalse(objeto.necesitaAtencion(ultimoCambio.plusDays(2)));
    }
}
