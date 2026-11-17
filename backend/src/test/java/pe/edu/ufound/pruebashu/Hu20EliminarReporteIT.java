package pe.edu.ufound.pruebashu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pe.edu.ufound.model.CategoriaObjeto;
import pe.edu.ufound.model.Objeto;
import pe.edu.ufound.model.TipoObjeto;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static pe.edu.ufound.pruebashu.DatosHu.ANA;
import static pe.edu.ufound.pruebashu.DatosHu.BETO;

/** Pruebas de HU-20: eliminar reporte. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:hu20-it;DB_CLOSE_DELAY=-1")
class Hu20EliminarReporteIT extends BaseHuIT {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 3, 10, 0);

    private Objeto reporteDeAna(String nombre) {
        return datos.objeto(nombre, TipoObjeto.PERDIDO, CategoriaObjeto.OTROS, FECHA, ANA);
    }

    private static List<MockHttpServletRequestBuilder> ambasRutas(Long id) {
        return List.of(delete("/objetos/" + id), post("/objetos/" + id + "/eliminar"));
    }

    @Test
    @DisplayName("El dueño elimina su reporte")
    void elDuenoElimina() throws Exception {
        Objeto reporte = reporteDeAna("HU20 Gorro del dueño");

        mvc.perform(delete("/objetos/" + reporte.getIdObjeto()).with(estudiante(ANA)).with(csrf()))
                .andExpect(redirectedUrl("/mis-publicaciones"))
                .andExpect(flash().attribute("mensaje", "Reporte eliminado."));

        Objeto recargado = datos.recargar(reporte.getIdObjeto());
        assertFalse(recargado.isActivo());
        assertEquals(reporte.getIdObjeto(), recargado.getIdObjeto());
        assertFalse(html("/mis-publicaciones", estudiante(ANA)).contains("HU20 Gorro del dueño"));
        assertFalse(html("/buscar", estudiante(ANA)).contains("HU20 Gorro del dueño"));
        assertFalse(html("/recientes", estudiante(ANA)).contains("HU20 Gorro del dueño"));
    }

    @Test
    @DisplayName("El dueño elimina por POST")
    void rutaAlternativa() throws Exception {
        Objeto reporte = reporteDeAna("HU20 Ruta POST");

        mvc.perform(post("/objetos/" + reporte.getIdObjeto() + "/eliminar").with(estudiante(ANA)).with(csrf()))
                .andExpect(redirectedUrl("/mis-publicaciones"));

        assertFalse(datos.recargar(reporte.getIdObjeto()).isActivo());
    }

    @Test
    @DisplayName("Otro estudiante recibe 403")
    void otroEstudianteNoElimina() throws Exception {
        Objeto reporte = reporteDeAna("HU20 Ajeno");

        for (MockHttpServletRequestBuilder ruta : ambasRutas(reporte.getIdObjeto())) {
            mvc.perform(ruta.with(estudiante(BETO)).with(csrf())).andExpect(status().isForbidden());
        }
        assertTrue(datos.recargar(reporte.getIdObjeto()).isActivo());
    }

    @Test
    @DisplayName("Seguridad y Oficina reciben 403")
    void seguridadYOficinaNoEliminan() throws Exception {
        Objeto reporte = reporteDeAna("HU20 Intento staff");

        for (MockHttpServletRequestBuilder ruta : ambasRutas(reporte.getIdObjeto())) {
            mvc.perform(ruta.with(seguridad()).with(csrf())).andExpect(status().isForbidden());
        }
        for (MockHttpServletRequestBuilder ruta : ambasRutas(reporte.getIdObjeto())) {
            mvc.perform(ruta.with(oficina()).with(csrf())).andExpect(status().isForbidden());
        }
        assertTrue(datos.recargar(reporte.getIdObjeto()).isActivo());
    }

    @Test
    @DisplayName("Límites del ID")
    void limitesDelId() throws Exception {
        Objeto otro = reporteDeAna("HU20 Límite intacto");
        Objeto ultimo = reporteDeAna("HU20 Límite N");
        long n = ultimo.getIdObjeto();

        for (String id : new String[]{String.valueOf(n + 1), "0", "-1"}) {
            mvc.perform(delete("/objetos/" + id).with(estudiante(ANA)).with(csrf())).andExpect(status().isNotFound());
        }
        assertTrue(datos.recargar(n).isActivo());
        assertTrue(datos.recargar(otro.getIdObjeto()).isActivo());

        mvc.perform(delete("/objetos/" + n).with(estudiante(ANA)).with(csrf()))
                .andExpect(redirectedUrl("/mis-publicaciones"));
        assertFalse(datos.recargar(n).isActivo());
    }

    @Test
    @DisplayName("Solicitudes inválidas")
    void solicitudesInvalidas() throws Exception {
        Objeto reporte = reporteDeAna("HU20 Intacto");

        mvc.perform(delete("/objetos/999999").with(estudiante(ANA)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(delete("/objetos/abc").with(estudiante(ANA)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(delete("/objetos/" + reporte.getIdObjeto()).with(csrf()))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
        mvc.perform(delete("/objetos/" + reporte.getIdObjeto()).with(estudiante(ANA)))
                .andExpect(status().isForbidden());

        assertTrue(datos.recargar(reporte.getIdObjeto()).isActivo());
    }
}
