package pe.edu.ufound.servicio;

import org.junit.jupiter.api.Test;
import pe.edu.ufound.model.*;

import static org.junit.jupiter.api.Assertions.*;

/** Prueba lo que muestran las pantallas: búsqueda, registro y cambio de estado del mock. */
class ObjetoServiceTest {

    private final UsuarioService usuarios = new UsuarioService();
    private final ObjetoService servicio = new ObjetoService(usuarios);

    @Test
    void filtraPorCategoriaYTexto() {
        var tecnologia = servicio.buscar(null, null, CategoriaObjeto.TECNOLOGIA, null, null);
        assertFalse(tecnologia.isEmpty());
        assertTrue(tecnologia.stream().allMatch(o -> o.getCategoria() == CategoriaObjeto.TECNOLOGIA));

        assertEquals(1, servicio.buscar("STANLEY", null, null, null, null).size());
    }

    @Test
    void seguridadRegistraEncontradoYEstudiantePerdido() {
        Usuario guardia = usuarios.buscarPorRol(RolUsuario.SEGURIDAD);
        Objeto encontrado = servicio.registrar("Mochila", CategoriaObjeto.OTROS, Pabellon.PABELLON_A,
                "Mochila negra con llavero", null, guardia);

        assertEquals(TipoObjeto.ENCONTRADO, encontrado.getTipo());
        assertEquals(EstadoObjeto.REPORTADO, encontrado.getEstado());
        assertNotNull(encontrado.getFechaHoraReporte());
    }

    @Test
    void cambiarEstadoQuedaEnHistorialYBloqueaEdicion() {
        Usuario ana = usuarios.buscarPorRol(RolUsuario.ESTUDIANTE);
        Objeto objeto = servicio.registrar("Billetera", CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_B,
                "Billetera marrón de cuero", null, ana);
        assertTrue(objeto.puedeEditarlo(ana));

        servicio.cambiarEstado(objeto.getIdObjeto(), EstadoObjeto.EN_REVISION, "Revisando",
                usuarios.buscarPorRol(RolUsuario.OFICINA_OBJETOS_PERDIDOS));

        assertEquals(1, servicio.historialDe(objeto).size());
        assertFalse(objeto.puedeEditarlo(ana));
    }

    @Test
    void eliminarLoSacaDeLasBusquedas() {
        Usuario ana = usuarios.buscarPorRol(RolUsuario.ESTUDIANTE);
        int antes = servicio.deUsuario(ana).size();
        servicio.eliminar(servicio.deUsuario(ana).get(0).getIdObjeto());
        assertEquals(antes - 1, servicio.deUsuario(ana).size());
    }
}
