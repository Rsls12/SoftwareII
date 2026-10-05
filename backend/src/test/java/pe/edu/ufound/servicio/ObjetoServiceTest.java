package pe.edu.ufound.servicio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.ufound.excepcion.DatosInvalidosException;
import pe.edu.ufound.excepcion.OperacionNoPermitidaException;
import pe.edu.ufound.model.*;
import pe.edu.ufound.repositorio.ObjetoRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ObjetoServiceTest {

    @Autowired
    private UsuarioService usuarios;

    @Autowired
    private ObjetoService servicio;

    @Autowired
    private ObjetoRepository repositorio;

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
    void estudianteRegistraObjetoPerdidoYQuedaEnBaseDeDatos() {
        Usuario ana = usuarios.buscarPorRol(RolUsuario.ESTUDIANTE);
        Objeto registrado = servicio.registrarPerdido("Laptop HP", CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_E,
                "Laptop HP plateada con funda azul", "/imagenes/foto.png", ana);

        assertNotNull(registrado.getIdObjeto());
        Objeto guardado = repositorio.findById(registrado.getIdObjeto()).orElseThrow();
        assertEquals("Laptop HP", guardado.getNombre());
        assertEquals(TipoObjeto.PERDIDO, guardado.getTipo());
        assertEquals(EstadoObjeto.REPORTADO, guardado.getEstado());
        assertEquals(CategoriaObjeto.TECNOLOGIA, guardado.getCategoria());
        assertEquals(Pabellon.PABELLON_E, guardado.getUbicacion());
        assertEquals("/imagenes/foto.png", guardado.getImagenUrl());
        assertTrue(guardado.esDe(ana));
        assertTrue(guardado.isActivo());
    }

    @Test
    void soloUnEstudianteRegistraObjetosPerdidos() {
        Usuario guardia = usuarios.buscarPorRol(RolUsuario.SEGURIDAD);
        Usuario oficina = usuarios.buscarPorRol(RolUsuario.OFICINA_OBJETOS_PERDIDOS);

        assertThrows(OperacionNoPermitidaException.class, () -> servicio.registrarPerdido("Mochila",
                CategoriaObjeto.OTROS, Pabellon.PABELLON_A, "Mochila negra con llavero", null, guardia));
        assertThrows(OperacionNoPermitidaException.class, () -> servicio.registrar("Mochila",
                CategoriaObjeto.OTROS, Pabellon.PABELLON_A, "Mochila negra con llavero", null, oficina));
    }

    @Test
    void sinCategoriaNoSeRegistra() {
        Usuario ana = usuarios.buscarPorRol(RolUsuario.ESTUDIANTE);
        long antes = repositorio.count();

        DatosInvalidosException error = assertThrows(DatosInvalidosException.class, () -> servicio.registrarPerdido(
                "Mochila", null, Pabellon.PABELLON_A, "Mochila negra con llavero", null, ana));

        assertEquals("categoria", error.getCampo());
        assertEquals(antes, repositorio.count());
    }

    @Test
    void datosIncompletosNoSeRegistran() {
        Usuario ana = usuarios.buscarPorRol(RolUsuario.ESTUDIANTE);

        assertEquals("nombre", assertThrows(DatosInvalidosException.class, () -> servicio.registrarPerdido(
                "  ", CategoriaObjeto.OTROS, Pabellon.PABELLON_A, "Mochila negra con llavero", null, ana)).getCampo());
        assertEquals("descripcion", assertThrows(DatosInvalidosException.class, () -> servicio.registrarPerdido(
                "Mochila", CategoriaObjeto.OTROS, Pabellon.PABELLON_A, "Negra", null, ana)).getCampo());
        assertEquals("ubicacion", assertThrows(DatosInvalidosException.class, () -> servicio.registrarPerdido(
                "Mochila", CategoriaObjeto.OTROS, null, "Mochila negra con llavero", null, ana)).getCampo());
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
        assertFalse(servicio.obtener(objeto.getIdObjeto()).puedeEditarlo(ana));
    }

    @Test
    void eliminarLoSacaDeLasBusquedas() {
        Usuario ana = usuarios.buscarPorRol(RolUsuario.ESTUDIANTE);
        int antes = servicio.deUsuario(ana).size();
        servicio.eliminar(servicio.deUsuario(ana).get(0).getIdObjeto());
        assertEquals(antes - 1, servicio.deUsuario(ana).size());
    }
}
