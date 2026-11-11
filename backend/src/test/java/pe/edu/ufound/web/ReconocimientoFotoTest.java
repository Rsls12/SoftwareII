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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.ufound.almacenamiento.SupabaseFalso;
import pe.edu.ufound.model.CaracteristicasObjeto;
import pe.edu.ufound.model.CategoriaObjeto;
import pe.edu.ufound.model.Objeto;
import pe.edu.ufound.model.Pabellon;
import pe.edu.ufound.model.TipoObjeto;
import pe.edu.ufound.reconocimiento.ReconocimientoImagenes;
import pe.edu.ufound.repositorio.ObjetoRepository;
import pe.edu.ufound.repositorio.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HU-33: al registrar o editar un objeto con foto, el sistema reconoce sus características con IA, las guarda
 * con el objeto y las muestra en el detalle. La IA se simula con Mockito; las fotos van a un Supabase falso.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReconocimientoFotoTest {

    private static final String ESTUDIANTE = "ana.torres@universidad.edu.pe";
    private static final String SEGURIDAD = "jorge.quispe@universidad.edu.pe";
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

    @MockitoBean
    private ReconocimientoImagenes reconocimiento;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjetoRepository objetos;

    @Autowired
    private UsuarioRepository usuarios;

    @Test
    @WithUserDetails(ESTUDIANTE)
    void alRegistrarConFotoSeGuardanYSeMuestranLasCaracteristicasReconocidas() throws Exception {
        when(reconocimiento.analizar(any())).thenReturn(Optional.of(new CaracteristicasObjeto("Mochila",
                List.of("negro", "gris"), "Totto", "tela", List.of("llavero de Lima"), LocalDateTime.now())));

        mvc.perform(multipart("/objetos").file(foto())
                        .param("nombre", "Mochila Totto de la biblioteca")
                        .param("categoria", "ACCESORIOS")
                        .param("ubicacion", "PABELLON_B")
                        .param("descripcion", "Mochila con un llavero colgando")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", containsString("En la foto se reconoció: Mochila.")));

        verify(reconocimiento, times(1)).analizar(any());
        Objeto guardado = ultimoReporteDe(ESTUDIANTE);
        assertEquals("Mochila Totto de la biblioteca", guardado.getNombre());
        assertTrue(guardado.tieneCaracteristicas());
        assertEquals("Totto", guardado.getCaracteristicas().getMarca());

        mvc.perform(get("/objetos/" + guardado.getIdObjeto()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Características detectadas en la foto")))
                .andExpect(content().string(containsString("negro, gris")))
                .andExpect(content().string(containsString("Totto")))
                .andExpect(content().string(containsString("llavero de Lima")))
                .andExpect(content().string(containsString("Pueden no ser exactas")));
    }

    @Test
    @WithUserDetails(SEGURIDAD)
    void seguridadTambienObtieneLasCaracteristicasDelObjetoEncontrado() throws Exception {
        when(reconocimiento.analizar(any())).thenReturn(Optional.of(new CaracteristicasObjeto("Audífonos",
                List.of("blanco"), null, "plástico", null, LocalDateTime.now())));

        mvc.perform(multipart("/objetos").file(foto())
                        .param("nombre", "Audífonos inalámbricos")
                        .param("categoria", "TECNOLOGIA")
                        .param("ubicacion", "PABELLON_E")
                        .param("descripcion", "Audífonos blancos en su estuche")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        Objeto encontrado = ultimoReporteDe(SEGURIDAD);
        assertEquals(TipoObjeto.ENCONTRADO, encontrado.getTipo());
        assertEquals("Audífonos", encontrado.getCaracteristicas().getObjetoDetectado());
        assertFalse(encontrado.getCaracteristicas().tieneMarca());
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void siLaIaNoReconoceNadaElObjetoSeRegistraIgual() throws Exception {
        when(reconocimiento.analizar(any())).thenReturn(Optional.empty());

        mvc.perform(multipart("/objetos").file(foto())
                        .param("nombre", "Cuaderno anillado")
                        .param("categoria", "UTILES")
                        .param("ubicacion", "PABELLON_C")
                        .param("descripcion", "Cuaderno azul con apuntes de cálculo")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", not(containsString("En la foto se reconoció"))));

        Objeto guardado = ultimoReporteDe(ESTUDIANTE);
        assertEquals("Cuaderno anillado", guardado.getNombre());
        assertTrue(guardado.tieneImagen());
        assertFalse(guardado.tieneCaracteristicas());
        mvc.perform(get("/objetos/" + guardado.getIdObjeto()))
                .andExpect(content().string(not(containsString("Características detectadas en la foto"))));
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void sinFotoNoSeLlamaALaIa() throws Exception {
        mvc.perform(multipart("/objetos")
                        .param("nombre", "Tomatodo morado")
                        .param("categoria", "BOTELLAS")
                        .param("ubicacion", "PABELLON_A")
                        .param("descripcion", "Tomatodo morado de medio litro")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(reconocimiento, never()).analizar(any());
        assertFalse(ultimoReporteDe(ESTUDIANTE).tieneCaracteristicas());
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void unArchivoQueNoEsImagenSeRechazaSinLlamarALaIa() throws Exception {
        long antes = objetos.count();

        mvc.perform(multipart("/objetos")
                        .file(new MockMultipartFile("foto", "virus.png", "image/png", "no soy imagen".getBytes()))
                        .param("nombre", "Cartuchera")
                        .param("categoria", "UTILES")
                        .param("ubicacion", "PABELLON_A")
                        .param("descripcion", "Cartuchera roja con lapiceros")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(reconocimiento, never()).analizar(any());
        assertEquals(antes, objetos.count());
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void datosInvalidosNoLlamanALaIaNiRegistranNada() throws Exception {
        long antes = objetos.count();

        mvc.perform(multipart("/objetos").file(foto())
                        .param("nombre", "Cartuchera")
                        .param("ubicacion", "PABELLON_A")
                        .param("descripcion", "Cartuchera roja con lapiceros")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(reconocimiento, never()).analizar(any());
        assertEquals(antes, objetos.count());
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void alEditarConOtraFotoSeReconocenDeNuevoYSinFotoSeConservan() throws Exception {
        Objeto reporte = objetos.save(Objeto.reportar("Casaca impermeable", "Casaca azul impermeable talla M",
                CategoriaObjeto.ROPA, Pabellon.PABELLON_A, "/imagenes/casaca.png",
                usuarios.findByCorreoIgnoreCase(ESTUDIANTE).orElseThrow()));
        Long id = reporte.getIdObjeto();
        when(reconocimiento.analizar(any())).thenReturn(Optional.of(new CaracteristicasObjeto("Casaca",
                List.of("azul marino"), "Columbia", "nylon", List.of("capucha desmontable"), LocalDateTime.now())));

        mvc.perform(multipart("/objetos/" + id).file(foto())
                        .param("descripcion", "Casaca azul impermeable talla M con capucha")
                        .param("ubicacion", "PABELLON_A")
                        .with(csrf()))
                .andExpect(redirectedUrl("/objetos/" + id));

        assertEquals("Columbia", objetos.findById(id).orElseThrow().getCaracteristicas().getMarca());
        verify(reconocimiento, times(1)).analizar(any());

        // Una edición sin foto nueva no vuelve a analizar y conserva lo reconocido.
        mvc.perform(multipart("/objetos/" + id)
                        .param("descripcion", "Casaca azul impermeable talla M, capucha gris")
                        .param("ubicacion", "PABELLON_B")
                        .with(csrf()))
                .andExpect(redirectedUrl("/objetos/" + id));

        Objeto editado = objetos.findById(id).orElseThrow();
        assertEquals(Pabellon.PABELLON_B, editado.getUbicacion());
        assertEquals("Columbia", editado.getCaracteristicas().getMarca());
        verify(reconocimiento, times(1)).analizar(any());
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void elFormularioAvisaQueLaFotoSeAnalizaConIa() throws Exception {
        mvc.perform(get("/objetos/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("la IA reconoce sus características")));
    }

    private Objeto ultimoReporteDe(String correo) {
        return objetos.findByReportadoPorAndActivoTrueOrderByFechaHoraReporteDesc(
                usuarios.findByCorreoIgnoreCase(correo).orElseThrow()).get(0);
    }

    private static MockMultipartFile foto() {
        return new MockMultipartFile("foto", "foto.png", "image/png", PNG);
    }
}
