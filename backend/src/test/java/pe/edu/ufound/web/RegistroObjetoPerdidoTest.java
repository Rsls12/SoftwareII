package pe.edu.ufound.web;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.ufound.almacenamiento.SupabaseFalso;
import pe.edu.ufound.model.CategoriaObjeto;
import pe.edu.ufound.model.Objeto;
import pe.edu.ufound.model.TipoObjeto;
import pe.edu.ufound.repositorio.ObjetoRepository;
import pe.edu.ufound.repositorio.UsuarioRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RegistroObjetoPerdidoTest {

    private static final String ESTUDIANTE = "ana.torres@universidad.edu.pe";
    private static final String OFICINA = "lucia.paredes@universidad.edu.pe";
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

    @Test
    @WithUserDetails(ESTUDIANTE)
    void estudianteRegistraObjetoPerdidoConFoto() throws Exception {
        MockMultipartFile foto = new MockMultipartFile("foto", "mochila.png", "image/png", PNG);

        mvc.perform(multipart("/objetos").file(foto)
                        .param("nombre", "Mochila Totto negra")
                        .param("categoria", "ACCESORIOS")
                        .param("ubicacion", "PABELLON_D")
                        .param("descripcion", "Mochila negra con llavero de Lima")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/objetos/*"))
                .andExpect(flash().attributeExists("mensaje"));

        Objeto guardado = objetos.findByReportadoPorAndActivoTrueOrderByFechaHoraReporteDesc(
                usuarios.findByCorreoIgnoreCase(ESTUDIANTE).orElseThrow()).get(0);
        assertEquals("Mochila Totto negra", guardado.getNombre());
        assertEquals(TipoObjeto.PERDIDO, guardado.getTipo());
        assertEquals(CategoriaObjeto.ACCESORIOS, guardado.getCategoria());
        assertTrue(guardado.getImagenUrl().startsWith(supabase.url() + "/storage/v1/object/public/objetos/"));
        assertTrue(guardado.getImagenUrl().endsWith(".png"));
        assertArrayEquals(PNG, supabase.peticiones().get(supabase.peticiones().size() - 1).cuerpo());
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void archivoQueNoEsImagenSeRechazaYNoSeRegistra() throws Exception {
        long antes = objetos.count();
        MockMultipartFile falso = new MockMultipartFile("foto", "virus.png", "image/png", "no soy imagen".getBytes());

        mvc.perform(multipart("/objetos").file(falso)
                        .param("nombre", "Cartuchera")
                        .param("categoria", "UTILES")
                        .param("ubicacion", "PABELLON_A")
                        .param("descripcion", "Cartuchera roja con lapiceros")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        assertEquals(antes, objetos.count());
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void sinCategoriaNoSeRegistraNiSeSubeLaFoto() throws Exception {
        long antes = objetos.count();
        int subidas = supabase.peticiones().size();
        MockMultipartFile foto = new MockMultipartFile("foto", "cartuchera.png", "image/png", PNG);

        mvc.perform(multipart("/objetos").file(foto)
                        .param("nombre", "Cartuchera")
                        .param("ubicacion", "PABELLON_A")
                        .param("descripcion", "Cartuchera roja con lapiceros")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        assertEquals(antes, objetos.count());
        assertEquals(subidas, supabase.peticiones().size());
    }

    @Test
    @WithUserDetails(OFICINA)
    void oficinaNoRegistraObjetos() throws Exception {
        mvc.perform(multipart("/objetos")
                        .param("nombre", "Cartuchera")
                        .param("categoria", "UTILES")
                        .param("ubicacion", "PABELLON_A")
                        .param("descripcion", "Cartuchera roja con lapiceros")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void listaLasCategorias() throws Exception {
        mvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(CategoriaObjeto.values().length))
                .andExpect(jsonPath("$[0].codigo").value("TECNOLOGIA"))
                .andExpect(jsonPath("$[0].etiqueta").value("Tecnología"));
    }
}
