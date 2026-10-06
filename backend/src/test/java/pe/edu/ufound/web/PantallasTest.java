package pe.edu.ufound.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.ufound.model.CategoriaObjeto;
import pe.edu.ufound.model.EstadoObjeto;
import pe.edu.ufound.model.Objeto;
import pe.edu.ufound.model.Pabellon;
import pe.edu.ufound.model.RolUsuario;
import pe.edu.ufound.repositorio.ObjetoRepository;
import pe.edu.ufound.repositorio.UsuarioRepository;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Las pantallas que no cubren SesionTest ni RegistroObjetoPerdidoTest: HU-12 a HU-22. */
@SpringBootTest
@AutoConfigureMockMvc
class PantallasTest {

    private static final String ESTUDIANTE = "ana.torres@universidad.edu.pe";
    private static final String SEGURIDAD = "jorge.quispe@universidad.edu.pe";
    private static final String OFICINA = "lucia.paredes@universidad.edu.pe";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjetoRepository objetos;

    @Autowired
    private UsuarioRepository usuarios;

    @Test
    @WithUserDetails(OFICINA)
    void oficinaCambiaElEstadoYElCambioApareceEnElDetalle() throws Exception {
        Long id = reporteDeAna("Paraguas negro", "Paraguas negro con mango de madera").getIdObjeto();

        mvc.perform(post("/objetos/" + id + "/estado")
                        .param("estado", "EN_REVISION")
                        .param("observacion", "Buscando entre lo encontrado hoy")
                        .with(csrf()))
                .andExpect(redirectedUrl("/objetos/" + id))
                .andExpect(flash().attribute("mensaje", "Estado actualizado a \"En revisión\"."));

        Objeto guardado = objetos.findById(id).orElseThrow();
        assertEquals(EstadoObjeto.EN_REVISION, guardado.getEstado());
        assertEquals(1, guardado.getHistorial().size());

        mvc.perform(get("/objetos/" + id))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Paraguas negro")))
                .andExpect(content().string(containsString("Buscando entre lo encontrado hoy")))
                .andExpect(content().string(containsString("Lucía Paredes Vega")));
    }

    @Test
    @WithUserDetails(OFICINA)
    void cambiarEstadoSinEstadoNoGuardaNada() throws Exception {
        Long id = reporteDeAna("Gorra roja", "Gorra roja con visera curva").getIdObjeto();

        mvc.perform(post("/objetos/" + id + "/estado").with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(post("/objetos/" + id + "/estado").param("estado", "PERDIDISIMO").with(csrf()))
                .andExpect(status().isBadRequest());

        assertTrue(objetos.findById(id).orElseThrow().getHistorial().isEmpty());
    }

    @Test
    @WithUserDetails(OFICINA)
    void oficinaEntregaElObjetoConLosDatosDeQuienReclama() throws Exception {
        Long id = reporteDeAna("Reloj plateado", "Reloj plateado con correa negra").getIdObjeto();

        mvc.perform(get("/objetos/" + id))
                .andExpect(content().string(containsString("name=\"nombreReclamante\"")))
                .andExpect(content().string(containsString("name=\"documentoReclamante\"")));

        mvc.perform(post("/objetos/" + id + "/estado")
                        .param("estado", "ENTREGADO")
                        .param("nombreReclamante", "Carlos Ruiz Soto")
                        .param("documentoReclamante", "DNI 70112233")
                        .param("observacion", "Mostró su carné universitario")
                        .with(csrf()))
                .andExpect(redirectedUrl("/objetos/" + id))
                .andExpect(flash().attribute("mensaje",
                        "Entrega registrada. El objeto ya figura en el historial de entregas."));

        Objeto guardado = objetos.findById(id).orElseThrow();
        assertEquals(EstadoObjeto.ENTREGADO, guardado.getEstado());
        assertEquals("DNI 70112233", guardado.getEntrega().getDocumentoReclamante());
        assertEquals(OFICINA, guardado.getEntrega().getEncargado().getCorreo());
        assertEquals(1, guardado.getHistorial().size());

        mvc.perform(get("/entregas"))
                .andExpect(content().string(containsString("Reloj plateado")))
                .andExpect(content().string(containsString("Carlos Ruiz Soto")))
                .andExpect(content().string(containsString("DNI 70112233")));

        // Caso cerrado: el detalle muestra la entrega y ya no ofrece cambiar el estado.
        mvc.perform(get("/objetos/" + id))
                .andExpect(content().string(containsString("Caso cerrado")))
                .andExpect(content().string(containsString("Carlos Ruiz Soto")))
                .andExpect(content().string(not(containsString("name=\"estado\""))));
        mvc.perform(post("/objetos/" + id + "/estado").param("estado", "EN_REVISION").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(OFICINA)
    void sinDocumentoNoSeEntregaNiCambiaElEstado() throws Exception {
        Long id = reporteDeAna("Lentes de sol", "Lentes de sol con estuche azul").getIdObjeto();

        mvc.perform(post("/objetos/" + id + "/estado")
                        .param("estado", "ENTREGADO")
                        .param("nombreReclamante", "Carlos Ruiz Soto")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        Objeto guardado = objetos.findById(id).orElseThrow();
        assertEquals(EstadoObjeto.REPORTADO, guardado.getEstado());
        assertFalse(guardado.fueEntregado());
        assertTrue(guardado.getHistorial().isEmpty());
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void elAvisoPendienteSeMuestraEnLaPantallaSiguiente() throws Exception {
        mvc.perform(get("/mis-publicaciones").flashAttr("mensaje", "Reporte eliminado."))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reporte eliminado.")));
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void misPublicacionesMuestraSoloLosReportesActivosDelEstudiante() throws Exception {
        Objeto retirado = reporteDeAna("Polo blanco", "Polo blanco talla S con logo");
        retirado.desactivar();
        objetos.save(retirado);

        mvc.perform(get("/mis-publicaciones"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Laptop Lenovo ThinkPad gris")))
                .andExpect(content().string(containsString("En revisión")))
                .andExpect(content().string(not(containsString("Polo blanco"))))
                .andExpect(content().string(not(containsString("Llaves con llavero de Pikachu"))));
    }

    @Test
    @WithUserDetails(SEGURIDAD)
    void elInicioMuestraLosObjetosRecientes() throws Exception {
        reporteDeAna("Termo negro", "Termo negro de acero con tapa gris");

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Objetos recientes")))
                .andExpect(content().string(containsString("Termo negro")));
    }

    @Test
    @WithUserDetails(SEGURIDAD)
    void misPublicacionesEsSoloParaEstudiantes() throws Exception {
        mvc.perform(get("/mis-publicaciones")).andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void losFormulariosLlevanElTokenCsrf() throws Exception {
        mvc.perform(get("/objetos/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void buscarFiltraPorTextoYMantieneElFiltro() throws Exception {
        mvc.perform(get("/buscar").param("q", "stanley").param("tipo", "").param("categoria", "BOTELLAS"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Botella Stanley verde")))
                .andExpect(content().string(containsString("1 resultado")))
                .andExpect(content().string(not(containsString("Laptop Lenovo"))))
                .andExpect(content().string(containsString("value=\"stanley\"")));

        mvc.perform(get("/buscar").param("tipo", "ROBADO")).andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void estudianteEditaYEliminaSuReporte() throws Exception {
        Long id = reporteDeAna("Tomatodo azul", "Tomatodo azul de medio litro").getIdObjeto();

        mvc.perform(get("/objetos/" + id + "/editar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Editar reporte")));

        mvc.perform(multipart("/objetos/" + id)
                        .param("descripcion", "Tomatodo azul con tapa blanca")
                        .param("ubicacion", "PABELLON_E")
                        .with(csrf()))
                .andExpect(redirectedUrl("/objetos/" + id))
                .andExpect(flash().attribute("mensaje", "Cambios guardados."));

        Objeto editado = objetos.findById(id).orElseThrow();
        assertEquals("Tomatodo azul con tapa blanca", editado.getDescripcion());
        assertEquals(Pabellon.PABELLON_E, editado.getUbicacion());

        mvc.perform(post("/objetos/" + id + "/eliminar").with(csrf()))
                .andExpect(redirectedUrl("/mis-publicaciones"));

        assertFalse(objetos.findById(id).orElseThrow().isActivo());
    }

    @Test
    @WithUserDetails(SEGURIDAD)
    void nadieMasPuedeEliminarElReporteDeOtro() throws Exception {
        Long id = reporteDeAna("Casco de bici", "Casco de bici blanco con luz trasera").getIdObjeto();

        mvc.perform(get("/objetos/" + id)).andExpect(content().string(not(containsString("Eliminar reporte"))));
        mvc.perform(post("/objetos/" + id + "/eliminar").with(csrf())).andExpect(status().isForbidden());

        assertTrue(objetos.findById(id).orElseThrow().isActivo());
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void unObjetoQueNoExisteDa404() throws Exception {
        mvc.perform(get("/objetos/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/objetos/abc")).andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(OFICINA)
    void oficinaVeElHistorialDeEntregas() throws Exception {
        mvc.perform(get("/entregas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("María Fernández López")))
                .andExpect(content().string(containsString("Llaves con llavero de Pikachu")));
    }

    private Objeto reporteDeAna(String nombre, String descripcion) {
        return objetos.save(Objeto.reportar(nombre, descripcion, CategoriaObjeto.OTROS, Pabellon.PABELLON_A, null,
                usuarios.findFirstByRolOrderByIdUsuarioAsc(RolUsuario.ESTUDIANTE).orElseThrow()));
    }
}
