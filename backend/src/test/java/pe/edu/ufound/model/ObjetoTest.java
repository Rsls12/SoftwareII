package pe.edu.ufound.model;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.ufound.excepcion.DatosInvalidosException;
import pe.edu.ufound.excepcion.OperacionNoPermitidaException;
import pe.edu.ufound.repositorio.ObjetoRepository;
import pe.edu.ufound.repositorio.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ObjetoTest {

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ObjetoRepository objetos;

    @Autowired
    private EntityManager bd;

    @Test
    void filtraPorCategoriaYTexto() {
        var tecnologia = objetos.buscar(null, null, CategoriaObjeto.TECNOLOGIA, null, null);

        assertFalse(tecnologia.isEmpty());
        assertTrue(tecnologia.stream().allMatch(o -> o.getCategoria() == CategoriaObjeto.TECNOLOGIA));
        assertEquals(1, objetos.buscar("STANLEY", null, null, null, null).size());
    }

    @Test
    void seguridadRegistraEncontradoYEstudiantePerdido() {
        Objeto encontrado = Objeto.reportar("Mochila", "Mochila negra con llavero", CategoriaObjeto.OTROS,
                Pabellon.PABELLON_A, null, conRol(RolUsuario.SEGURIDAD));
        Objeto perdido = Objeto.reportar("Mochila", "Mochila negra con llavero", CategoriaObjeto.OTROS,
                Pabellon.PABELLON_A, null, conRol(RolUsuario.ESTUDIANTE));

        assertEquals(TipoObjeto.ENCONTRADO, encontrado.getTipo());
        assertEquals(EstadoObjeto.REPORTADO, encontrado.getEstado());
        assertNotNull(encontrado.getFechaHoraReporte());
        assertEquals(TipoObjeto.PERDIDO, perdido.getTipo());
    }

    @Test
    void estudianteRegistraObjetoPerdidoYQuedaEnBaseDeDatos() {
        Usuario ana = conRol(RolUsuario.ESTUDIANTE);

        Objeto registrado = objetos.save(Objeto.reportarPerdido("Laptop HP", "Laptop HP plateada con funda azul",
                CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_E, "/imagenes/foto.png", ana));

        assertNotNull(registrado.getIdObjeto());
        Objeto guardado = recargar(registrado);
        assertEquals("Laptop HP", guardado.getNombre());
        assertEquals(TipoObjeto.PERDIDO, guardado.getTipo());
        assertEquals(EstadoObjeto.REPORTADO, guardado.getEstado());
        assertEquals(CategoriaObjeto.TECNOLOGIA, guardado.getCategoria());
        assertEquals(Pabellon.PABELLON_E, guardado.getUbicacion());
        assertEquals("/imagenes/foto.png", guardado.getImagenUrl());
        assertTrue(guardado.esDe(ana));
        assertTrue(guardado.isActivo());
        assertTrue(guardado.getHistorial().isEmpty());
    }

    @Test
    void soloUnEstudianteRegistraObjetosPerdidos() {
        Usuario guardia = conRol(RolUsuario.SEGURIDAD);
        Usuario oficina = conRol(RolUsuario.OFICINA_OBJETOS_PERDIDOS);

        assertThrows(OperacionNoPermitidaException.class, () -> Objeto.reportarPerdido("Mochila",
                "Mochila negra con llavero", CategoriaObjeto.OTROS, Pabellon.PABELLON_A, null, guardia));
        assertThrows(OperacionNoPermitidaException.class, () -> Objeto.reportar("Mochila",
                "Mochila negra con llavero", CategoriaObjeto.OTROS, Pabellon.PABELLON_A, null, oficina));
    }

    @Test
    void datosIncompletosNoSeRegistran() {
        Usuario ana = conRol(RolUsuario.ESTUDIANTE);
 
        assertEquals("categoria", assertThrows(DatosInvalidosException.class, () -> Objeto.reportarPerdido(
                "Mochila", "Mochila negra con llavero", null, Pabellon.PABELLON_A, null, ana)).getCampo());
        assertEquals("nombre", assertThrows(DatosInvalidosException.class, () -> Objeto.reportarPerdido(
                "  ", "Mochila negra con llavero", CategoriaObjeto.OTROS, Pabellon.PABELLON_A, null, ana)).getCampo());
        assertEquals("descripcion", assertThrows(DatosInvalidosException.class, () -> Objeto.reportarPerdido(
                "Mochila", "Negra", CategoriaObjeto.OTROS, Pabellon.PABELLON_A, null, ana)).getCampo());
        assertEquals("ubicacion", assertThrows(DatosInvalidosException.class, () -> Objeto.reportarPerdido(
                "Mochila", "Mochila negra con llavero", CategoriaObjeto.OTROS, null, null, ana)).getCampo());
    }

    @Test
    void cambiarEstadoQuedaEnElHistorialDelObjetoYBloqueaLaEdicion() {
        Usuario ana = conRol(RolUsuario.ESTUDIANTE);
        Usuario oficina = conRol(RolUsuario.OFICINA_OBJETOS_PERDIDOS);
        Objeto objeto = objetos.save(Objeto.reportar("Billetera", "Billetera marrón de cuero",
                CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_B, null, ana));
        assertTrue(objeto.puedeEditarlo(ana));

        objeto.cambiarEstado(EstadoObjeto.EN_REVISION, "Revisando", oficina);
        objeto.cambiarEstado(EstadoObjeto.DISPONIBLE_EN_OFICINA, "Llegó a la oficina", oficina);
        objetos.save(objeto);

        Objeto guardado = recargar(objeto);
        assertEquals(EstadoObjeto.DISPONIBLE_EN_OFICINA, guardado.getEstado());
        assertFalse(guardado.puedeEditarlo(ana));

        List<HistorialEstado> historial = guardado.getHistorial();
        assertEquals(2, historial.size());
        HistorialEstado ultimo = historial.get(0);
        assertEquals(EstadoObjeto.EN_REVISION, ultimo.getEstadoAnterior());
        assertEquals(EstadoObjeto.DISPONIBLE_EN_OFICINA, ultimo.getEstadoNuevo());
        assertEquals("Llegó a la oficina", ultimo.getObservacion());
        assertEquals(oficina.getIdUsuario(), ultimo.getRealizadoPor().getIdUsuario());
        assertEquals(EstadoObjeto.REPORTADO, historial.get(1).getEstadoAnterior());
    }

    @Test
    void cambioDeEstadoInvalidoNoTocaElObjeto() {
        Usuario oficina = conRol(RolUsuario.OFICINA_OBJETOS_PERDIDOS);
        Objeto objeto = Objeto.reportar("Billetera", "Billetera marrón de cuero", CategoriaObjeto.ACCESORIOS,
                Pabellon.PABELLON_B, null, conRol(RolUsuario.ESTUDIANTE));

        assertEquals("estado", assertThrows(DatosInvalidosException.class,
                () -> objeto.cambiarEstado(null, "Revisando", oficina)).getCampo());
        assertEquals("realizadoPor", assertThrows(DatosInvalidosException.class,
                () -> objeto.cambiarEstado(EstadoObjeto.EN_REVISION, "Revisando", null)).getCampo());
        assertEquals("observacion", assertThrows(DatosInvalidosException.class, () -> objeto.cambiarEstado(
                EstadoObjeto.EN_REVISION, "x".repeat(HistorialEstado.OBSERVACION_MAXIMA + 1), oficina)).getCampo());

        assertEquals(EstadoObjeto.REPORTADO, objeto.getEstado());
        assertTrue(objeto.getHistorial().isEmpty());
    }

    @Test
    void entregarDejaElObjetoEntregadoConSuEntregaYSuHistorial() {
        Usuario oficina = conRol(RolUsuario.OFICINA_OBJETOS_PERDIDOS);
        Objeto objeto = objetos.save(Objeto.reportar("Billetera", "Billetera marrón de cuero",
                CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_B, null, conRol(RolUsuario.ESTUDIANTE)));

        objeto.cambiarEstado(EstadoObjeto.DISPONIBLE_EN_OFICINA, "Recibido de seguridad", oficina);
        objeto.entregar("  Carlos Ruiz Soto ", "DNI 70112233", "Mostró su carné", oficina);
        objetos.save(objeto);

        Objeto guardado = recargar(objeto);
        assertEquals(EstadoObjeto.ENTREGADO, guardado.getEstado());
        assertTrue(guardado.fueEntregado());

        Entrega entrega = guardado.getEntrega();
        assertNotNull(entrega.getIdEntrega());
        assertEquals("Carlos Ruiz Soto", entrega.getNombreReclamante());
        assertEquals("DNI 70112233", entrega.getDocumentoReclamante());
        assertEquals("Mostró su carné", entrega.getObservacion());
        assertEquals(oficina.getIdUsuario(), entrega.getEncargado().getIdUsuario());
        assertNotNull(entrega.getFechaHoraEntrega());

        HistorialEstado ultimo = guardado.getHistorial().get(0);
        assertEquals(EstadoObjeto.DISPONIBLE_EN_OFICINA, ultimo.getEstadoAnterior());
        assertEquals(EstadoObjeto.ENTREGADO, ultimo.getEstadoNuevo());
        assertTrue(objetos.entregas().stream().anyMatch(e -> e.getIdEntrega().equals(entrega.getIdEntrega())));
    }

    @Test
    void sinLosDatosDeQuienReclamaNoSeEntrega() {
        Usuario ana = conRol(RolUsuario.ESTUDIANTE);
        Usuario oficina = conRol(RolUsuario.OFICINA_OBJETOS_PERDIDOS);
        Objeto objeto = Objeto.reportar("Billetera", "Billetera marrón de cuero", CategoriaObjeto.ACCESORIOS,
                Pabellon.PABELLON_B, null, ana);

        assertEquals("entrega", assertThrows(DatosInvalidosException.class,
                () -> objeto.cambiarEstado(EstadoObjeto.ENTREGADO, "Se lo llevó", oficina)).getCampo());
        assertEquals("nombreReclamante", assertThrows(DatosInvalidosException.class,
                () -> objeto.entregar("  ", "DNI 70112233", null, oficina)).getCampo());
        assertEquals("documentoReclamante", assertThrows(DatosInvalidosException.class,
                () -> objeto.entregar("Carlos Ruiz Soto", null, null, oficina)).getCampo());
        assertEquals("documentoReclamante", assertThrows(DatosInvalidosException.class, () -> objeto.entregar(
                "Carlos Ruiz Soto", "9".repeat(Entrega.DOCUMENTO_MAXIMO + 1), null, oficina)).getCampo());
        assertThrows(OperacionNoPermitidaException.class,
                () -> objeto.entregar("Carlos Ruiz Soto", "DNI 70112233", null, ana));

        assertEquals(EstadoObjeto.REPORTADO, objeto.getEstado());
        assertFalse(objeto.fueEntregado());
        assertTrue(objeto.getHistorial().isEmpty());
    }

    @Test
    void unObjetoEntregadoYaNoCambiaDeEstado() {
        Usuario oficina = conRol(RolUsuario.OFICINA_OBJETOS_PERDIDOS);
        Objeto objeto = Objeto.reportar("Billetera", "Billetera marrón de cuero", CategoriaObjeto.ACCESORIOS,
                Pabellon.PABELLON_B, null, conRol(RolUsuario.ESTUDIANTE));
        objeto.entregar("Carlos Ruiz Soto", "DNI 70112233", null, oficina);

        assertThrows(OperacionNoPermitidaException.class,
                () -> objeto.cambiarEstado(EstadoObjeto.ARCHIVADO, null, oficina));
        assertThrows(OperacionNoPermitidaException.class,
                () -> objeto.entregar("Otra Persona", "DNI 40112233", null, oficina));

        assertEquals(EstadoObjeto.ENTREGADO, objeto.getEstado());
        assertEquals(1, objeto.getHistorial().size());
        assertEquals("Carlos Ruiz Soto", objeto.getEntrega().getNombreReclamante());
    }

    @Test
    void desactivarLoSacaDeLasBusquedas() {
        Usuario ana = conRol(RolUsuario.ESTUDIANTE);
        List<Objeto> antes = objetos.findByReportadoPorAndActivoTrueOrderByFechaHoraReporteDesc(ana);

        Objeto eliminado = antes.get(0);
        eliminado.desactivar();
        objetos.save(eliminado);

        assertEquals(antes.size() - 1, objetos.findByReportadoPorAndActivoTrueOrderByFechaHoraReporteDesc(ana).size());
    }

    @Test
    void objetoPerdidoMantieneHistorialYLineaDeTiempo() {
        Usuario ana = conRol(RolUsuario.ESTUDIANTE);
        Usuario oficina = conRol(RolUsuario.OFICINA_OBJETOS_PERDIDOS);
        Objeto objeto = objetos.save(Objeto.reportarPerdido("Audífonos Sony", "Audífonos inalámbricos negros",
                CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_C, null, ana));

        assertFalse(objeto.tieneHistorial());
        assertNull(objeto.getUltimoCambio());
        assertEquals(objeto.getFechaHoraReporte(), objeto.getFechaUltimaActualizacion());

        objeto.cambiarEstado(EstadoObjeto.EN_REVISION, "Revisando en almacén", oficina);
        assertTrue(objeto.tieneHistorial());
        assertNotNull(objeto.getUltimoCambio());
        assertEquals("Pasó de Reportado a En revisión", objeto.getUltimoCambio().getDescripcionCambio());
        assertTrue(objeto.getUltimoCambio().tieneObservacion());
    }

    // ---------- HU-33: características reconocidas en la foto ----------

    @Test
    void lasCaracteristicasDeLaFotoSeGuardanConElObjeto() {
        Objeto objeto = Objeto.reportar("Mochila", "Mochila negra con llavero", CategoriaObjeto.ACCESORIOS,
                Pabellon.PABELLON_D, "/imagenes/mochila.png", conRol(RolUsuario.ESTUDIANTE));
        objeto.registrarCaracteristicas(new CaracteristicasObjeto("Mochila", List.of("negro", "gris"), "Totto",
                "tela", List.of("llavero de Lima"), LocalDateTime.now()));
        objetos.save(objeto);

        Objeto guardado = recargar(objeto);
        assertTrue(guardado.tieneCaracteristicas());
        CaracteristicasObjeto caracteristicas = guardado.getCaracteristicas();
        assertEquals("Mochila", caracteristicas.getObjetoDetectado());
        assertEquals("negro, gris", caracteristicas.getColores());
        assertEquals("Totto", caracteristicas.getMarca());
        assertEquals("tela", caracteristicas.getMaterial());
        assertEquals("llavero de Lima", caracteristicas.getRasgosDistintivos());
        assertNotNull(caracteristicas.getFechaHoraAnalisis());
    }

    @Test
    void unObjetoSinCaracteristicasSeLeeSinCaracteristicas() {
        Objeto objeto = objetos.save(Objeto.reportar("Gorra", "Gorra roja con visera curva",
                CategoriaObjeto.ROPA, Pabellon.PABELLON_A, "/imagenes/gorra.png", conRol(RolUsuario.ESTUDIANTE)));

        Objeto guardado = recargar(objeto);
        assertFalse(guardado.tieneCaracteristicas());
        assertNull(guardado.getCaracteristicas());
    }

    @Test
    void sinFotoNoSeRegistranCaracteristicas() {
        Objeto objeto = Objeto.reportar("Gorra", "Gorra roja con visera curva", CategoriaObjeto.ROPA,
                Pabellon.PABELLON_A, null, conRol(RolUsuario.ESTUDIANTE));
        CaracteristicasObjeto caracteristicas = new CaracteristicasObjeto("Gorra", List.of("rojo"), null, null,
                null, LocalDateTime.now());

        assertEquals("imagenUrl", assertThrows(DatosInvalidosException.class,
                () -> objeto.registrarCaracteristicas(caracteristicas)).getCampo());
        assertEquals("caracteristicas", assertThrows(DatosInvalidosException.class, () -> Objeto.reportar("Gorra",
                "Gorra roja con visera curva", CategoriaObjeto.ROPA, Pabellon.PABELLON_A, "/imagenes/gorra.png",
                conRol(RolUsuario.ESTUDIANTE)).registrarCaracteristicas(null)).getCampo());
        assertFalse(objeto.tieneCaracteristicas());
    }

    @Test
    void conOtraFotoLasCaracteristicasAnterioresDejanDeValer() {
        Objeto objeto = Objeto.reportar("Mochila", "Mochila negra con llavero", CategoriaObjeto.ACCESORIOS,
                Pabellon.PABELLON_D, "/imagenes/mochila.png", conRol(RolUsuario.ESTUDIANTE));
        objeto.registrarCaracteristicas(new CaracteristicasObjeto("Mochila", List.of("negro"), null, null, null,
                LocalDateTime.now()));

        // Editar sin subir otra foto conserva lo reconocido.
        objeto.editar("Mochila negra con llavero de Lima", Pabellon.PABELLON_D, null);
        assertTrue(objeto.tieneCaracteristicas());

        // La misma foto tampoco cambia nada; una foto distinta sí.
        objeto.adjuntarImagen("/imagenes/mochila.png");
        assertTrue(objeto.tieneCaracteristicas());
        objeto.editar("Mochila negra con llavero de Lima", Pabellon.PABELLON_D, "/imagenes/otra.png");
        assertFalse(objeto.tieneCaracteristicas());
    }

    private Usuario conRol(RolUsuario rol) {
        return usuarios.findFirstByRolOrderByIdUsuarioAsc(rol).orElseThrow();
    }

    /** Vuelve a leer el objeto de la base de datos, sin lo que quedó en memoria. */
    private Objeto recargar(Objeto objeto) {
        bd.flush();
        bd.clear();
        return objetos.findById(objeto.getIdObjeto()).orElseThrow();
    }
}
