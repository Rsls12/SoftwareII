package pe.edu.ufound.web;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import pe.edu.ufound.almacenamiento.SupabaseFalso;
import pe.edu.ufound.model.CategoriaObjeto;
import pe.edu.ufound.model.EstadoObjeto;
import pe.edu.ufound.model.Objeto;
import pe.edu.ufound.model.Pabellon;
import pe.edu.ufound.model.RolUsuario;
import pe.edu.ufound.repositorio.ObjetoRepository;
import pe.edu.ufound.repositorio.UsuarioRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Caja negra de la edición de un reporte, RutasWeb.guardarEdicion() (HU-21): partición equivalente y valores límite. */
@SpringBootTest
@AutoConfigureMockMvc
class EditarReporteCajaNegraTest {

    private static final String ANA = "ana.torres@universidad.edu.pe";
    private static final String JORGE = "jorge.quispe@universidad.edu.pe";
    private static final String ORIGINAL = "Casaca azul impermeable talla M";
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};

    private static final SupabaseFalso supabase = new SupabaseFalso();

    @DynamicPropertySource
    static void usarSupabaseFalso(DynamicPropertyRegistry propiedades) {
        propiedades.add("supabase.url", supabase::url);
    }

    @AfterAll
    static void cerrar() {
        supabase.cerrar();
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjetoRepository objetos;

    @Autowired
    private UsuarioRepository usuarios;

    private Objeto reporteDeAna() {
        return objetos.save(Objeto.reportarPerdido("Casaca azul", ORIGINAL, CategoriaObjeto.ROPA,
                Pabellon.PABELLON_B, null, usuarios.findFirstByRolOrderByIdUsuarioAsc(RolUsuario.ESTUDIANTE).orElseThrow()));
    }

    private MockMultipartHttpServletRequestBuilder edicion(Long id, String descripcion, String ubicacion) {
        MockMultipartHttpServletRequestBuilder peticion = multipart("/objetos/" + id);
        if (descripcion != null) peticion.param("descripcion", descripcion);
        if (ubicacion != null) peticion.param("ubicacion", ubicacion);
        peticion.with(csrf());
        return peticion;
    }

    private void seGuarda(Long id, String descripcion) throws Exception {
        mvc.perform(edicion(id, descripcion, "PABELLON_E"))
                .andExpect(redirectedUrl("/objetos/" + id))
                .andExpect(flash().attribute("mensaje", "Cambios guardados."));
        Objeto editado = objetos.findById(id).orElseThrow();
        assertEquals(descripcion.trim(), editado.getDescripcion());
        assertEquals(Pabellon.PABELLON_E, editado.getUbicacion());
    }

    private void noCambia(Long id) {
        Objeto objeto = objetos.findById(id).orElseThrow();
        assertEquals(ORIGINAL, objeto.getDescripcion());
        assertEquals(Pabellon.PABELLON_B, objeto.getUbicacion());
    }

    @Test @WithUserDetails(ANA)
    @DisplayName("CN-25: dueña, activo, Reportado, descripción válida, Pabellón E, sin foto -> se guarda")
    void edicionValidaSinFoto() throws Exception {
        seGuarda(reporteDeAna().getIdObjeto(), "Casaca azul con capucha gris");
    }

    @Test @WithUserDetails(ANA)
    @DisplayName("CN-26: igual que CN-25, con una foto PNG -> se guarda con la nueva foto")
    void edicionValidaConFoto() throws Exception {
        Long id = reporteDeAna().getIdObjeto();
        mvc.perform(edicion(id, "Casaca azul con capucha gris", "PABELLON_E")
                        .file(new MockMultipartFile("foto", "casaca.png", "image/png", PNG)))
                .andExpect(redirectedUrl("/objetos/" + id))
                .andExpect(flash().attribute("mensaje", "Cambios guardados."));
        assertTrue(objetos.findById(id).orElseThrow().getImagenUrl()
                .startsWith(supabase.url() + "/storage/v1/object/public/objetos/"));
    }

    @Test @WithUserDetails(JORGE)
    @DisplayName("CN-27: otro usuario edita el reporte -> 403")
    void otroUsuario() throws Exception {
        Long id = reporteDeAna().getIdObjeto();
        mvc.perform(edicion(id, "Casaca azul con capucha gris", "PABELLON_E")).andExpect(status().isForbidden());
        noCambia(id);
    }

    @Test @WithUserDetails(ANA)
    @DisplayName("CN-28: reporte eliminado -> 403")
    void reporteEliminado() throws Exception {
        Objeto objeto = reporteDeAna();
        objeto.desactivar();
        Long id = objetos.save(objeto).getIdObjeto();
        mvc.perform(edicion(id, "Casaca azul con capucha gris", "PABELLON_E")).andExpect(status().isForbidden());
        noCambia(id);
    }

    @Test @WithUserDetails(ANA)
    @DisplayName("CN-29: reporte En revisión -> 403")
    void reporteEnRevision() throws Exception {
        Objeto objeto = reporteDeAna();
        objeto.cambiarEstado(EstadoObjeto.EN_REVISION, "Revisando en almacén",
                usuarios.findFirstByRolOrderByIdUsuarioAsc(RolUsuario.OFICINA_OBJETOS_PERDIDOS).orElseThrow());
        Long id = objetos.save(objeto).getIdObjeto();
        mvc.perform(edicion(id, "Casaca azul con capucha gris", "PABELLON_E")).andExpect(status().isForbidden());
        noCambia(id);
    }

    @Test @WithUserDetails(ANA)
    @DisplayName("CN-30: descripción con solo espacios -> 400")
    void descripcionVacia() throws Exception {
        Long id = reporteDeAna().getIdObjeto();
        mvc.perform(edicion(id, "     ", "PABELLON_E")).andExpect(status().isBadRequest());
        noCambia(id);
    }

    @Test @WithUserDetails(ANA)
    @DisplayName("CN-31 a CN-34: límites de la descripción (9, 10, 500 y 501 caracteres)")
    void limitesDeDescripcion() throws Exception {
        Long id = reporteDeAna().getIdObjeto();
        mvc.perform(edicion(id, "x".repeat(9), "PABELLON_E")).andExpect(status().isBadRequest());
        noCambia(id);
        seGuarda(reporteDeAna().getIdObjeto(), "x".repeat(10));
        seGuarda(reporteDeAna().getIdObjeto(), "x".repeat(500));
        Long otro = reporteDeAna().getIdObjeto();
        mvc.perform(edicion(otro, "x".repeat(501), "PABELLON_E")).andExpect(status().isBadRequest());
        noCambia(otro);
    }

    @Test @WithUserDetails(ANA)
    @DisplayName("CN-35 y CN-36: sin ubicación o con una que no es pabellón -> 400")
    void ubicacionInvalida() throws Exception {
        Long id = reporteDeAna().getIdObjeto();
        mvc.perform(edicion(id, "Casaca azul con capucha gris", null)).andExpect(status().isBadRequest());
        mvc.perform(edicion(id, "Casaca azul con capucha gris", "PABELLON_Z")).andExpect(status().isBadRequest());
        noCambia(id);
    }

    @Test @WithUserDetails(ANA)
    @DisplayName("CN-37: archivo que no es imagen -> 400")
    void archivoNoEsImagen() throws Exception {
        Long id = reporteDeAna().getIdObjeto();
        mvc.perform(edicion(id, "Casaca azul con capucha gris", "PABELLON_E")
                        .file(new MockMultipartFile("foto", "virus.png", "image/png", "no soy imagen".getBytes())))
                .andExpect(status().isBadRequest());
        noCambia(id);
    }
}
