package pe.edu.ufound.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.ufound.repositorio.UsuarioRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Caja blanca de Objeto.puedeEditarlo() (HU-21): un caso por cada camino independiente. */
@SpringBootTest
@Transactional
class PuedeEditarloCaminosTest {

    @Autowired
    private UsuarioRepository usuarios;

    // esDe() compara ids, así que los usuarios salen de los datos de prueba de la BD.
    private Usuario conRol(RolUsuario rol) {
        return usuarios.findFirstByRolOrderByIdUsuarioAsc(rol).orElseThrow();
    }

    private Objeto reporteDe(Usuario dueno) {
        return Objeto.reportarPerdido("Casaca azul", "Casaca azul impermeable talla M",
                CategoriaObjeto.ROPA, Pabellon.PABELLON_B, null, dueno);
    }

    @Test
    @DisplayName("CB-16: 1-2-7-8, reporte eliminado -> false")
    void reporteEliminado() {
        Usuario ana = conRol(RolUsuario.ESTUDIANTE);
        Objeto objeto = reporteDe(ana);
        objeto.desactivar();
        assertFalse(objeto.puedeEditarlo(ana));
    }

    @Test
    @DisplayName("CB-17: 1-2-3-7-8, sin usuario -> false")
    void sinUsuario() {
        assertFalse(reporteDe(conRol(RolUsuario.ESTUDIANTE)).puedeEditarlo(null));
    }

    @Test
    @DisplayName("CB-18: 1-2-3-4-7-8, otro usuario -> false")
    void otroUsuario() {
        assertFalse(reporteDe(conRol(RolUsuario.ESTUDIANTE)).puedeEditarlo(conRol(RolUsuario.SEGURIDAD)));
    }

    @Test
    @DisplayName("CB-19: 1-2-3-4-5-7-8, el dueño con el reporte en revisión -> false")
    void reporteEnRevision() {
        Usuario ana = conRol(RolUsuario.ESTUDIANTE);
        Objeto objeto = reporteDe(ana);
        objeto.cambiarEstado(EstadoObjeto.EN_REVISION, "Revisando en almacén",
                conRol(RolUsuario.OFICINA_OBJETOS_PERDIDOS));
        assertFalse(objeto.puedeEditarlo(ana));
    }

    @Test
    @DisplayName("CB-20: 1-2-3-4-5-6-8, el dueño con el reporte en Reportado -> true")
    void duenoConReporteReportado() {
        Usuario ana = conRol(RolUsuario.ESTUDIANTE);
        assertTrue(reporteDe(ana).puedeEditarlo(ana));
    }
}
