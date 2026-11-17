package pe.edu.ufound.pruebashu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import pe.edu.ufound.model.CategoriaObjeto;
import pe.edu.ufound.model.EstadoObjeto;
import pe.edu.ufound.model.Objeto;
import pe.edu.ufound.model.TipoObjeto;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static pe.edu.ufound.pruebashu.DatosHu.ANA;
import static pe.edu.ufound.pruebashu.DatosHu.JORGE;

/** Pruebas de HU-38: estadísticas. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:hu38-it;DB_CLOSE_DELAY=-1")
class Hu38EstadisticasIT extends BaseHuIT {

    @BeforeEach
    void fixtures() {
        datos.objeto("HU38 P1", TipoObjeto.PERDIDO, CategoriaObjeto.TECNOLOGIA, LocalDateTime.of(2026, 10, 5, 12, 0), ANA);
        Objeto p2 = datos.objeto("HU38 P2", TipoObjeto.PERDIDO, CategoriaObjeto.BOTELLAS, LocalDateTime.of(2026, 11, 1, 0, 0), ANA);
        p2.cambiarEstado(EstadoObjeto.EN_REVISION);
        datos.guardar(p2);
        datos.objeto("HU38 P3", TipoObjeto.PERDIDO, CategoriaObjeto.TECNOLOGIA, LocalDateTime.of(2026, 11, 30, 23, 59), ANA);
        Objeto e1 = datos.objeto("HU38 E1", TipoObjeto.ENCONTRADO, CategoriaObjeto.ACCESORIOS, LocalDateTime.of(2026, 11, 15, 12, 0), JORGE);
        e1.cambiarEstado(EstadoObjeto.DISPONIBLE_EN_OFICINA);
        datos.guardar(e1);
        Objeto p4 = datos.objeto("HU38 P4", TipoObjeto.PERDIDO, CategoriaObjeto.ROPA, LocalDateTime.of(2026, 11, 16, 12, 0), ANA);
        p4.desactivar();
        datos.guardar(p4);
        Objeto p5 = datos.objeto("HU38 P5", TipoObjeto.PERDIDO, CategoriaObjeto.UTILES, LocalDateTime.of(2026, 12, 1, 0, 0), ANA);
        datos.entregar(p5, "Reclamante P5", "DNI 55555555", null, LocalDateTime.of(2026, 12, 2, 9, 0));
    }

    private String estadisticas(String consulta) throws Exception {
        return html("/estadisticas" + consulta, oficina());
    }

    @Test
    @DisplayName("La Oficina ve el enlace a Estadísticas")
    void navegacion() throws Exception {
        assertTrue(html("/dashboard", oficina()).contains("href=\"/estadisticas\""));
        assertTrue(html("/", oficina()).contains("href=\"/estadisticas\""));
    }

    @Test
    @DisplayName("CN-02: sin fechas cuenta 5 objetos")
    void sinFiltro() throws Exception {
        String pagina = estadisticas("");

        assertTrue(pagina.contains("5 objetos en el periodo elegido."));
        assertEquals(Map.of("Reportado", "2 (40 %)", "En revisión", "1 (20 %)", "Coincidencia", "0 (0 %)",
                        "En oficina", "1 (20 %)", "Entregado", "1 (20 %)", "Archivado", "0 (0 %)"),
                HtmlHu.barras(HtmlHu.seccion(pagina, "titulo-estado")));
        assertEquals(Map.of("Tecnología", "2 (40 %)", "Botellas", "1 (20 %)", "Accesorios", "1 (20 %)",
                        "Útiles", "1 (20 %)"),
                HtmlHu.barras(HtmlHu.seccion(pagina, "titulo-categoria")));
        assertEquals(java.util.List.of("oct 2026", "nov 2026", "dic 2026"),
                java.util.List.copyOf(HtmlHu.barras(HtmlHu.seccion(pagina, "titulo-mes")).keySet()),
                "orden cronológico");
        assertEquals(Map.of("oct 2026", "1 (20 %)", "nov 2026", "3 (60 %)", "dic 2026", "1 (20 %)"),
                HtmlHu.barras(HtmlHu.seccion(pagina, "titulo-mes")));
        assertFalse(pagina.contains("HU38 P4"));
    }

    @Test
    @DisplayName("CN-01: noviembre cuenta 3 objetos")
    void noviembre() throws Exception {
        String pagina = estadisticas("?desde=2026-11-01&hasta=2026-11-30");

        assertTrue(pagina.contains("3 objetos en el periodo elegido."));
        assertEquals(Map.of("Botellas", "1 (33 %)", "Tecnología", "1 (33 %)", "Accesorios", "1 (33 %)"),
                HtmlHu.barras(HtmlHu.seccion(pagina, "titulo-categoria")));
        assertEquals(Map.of("Reportado", "1 (33 %)", "En revisión", "1 (33 %)", "Coincidencia", "0 (0 %)",
                        "En oficina", "1 (33 %)", "Entregado", "0 (0 %)", "Archivado", "0 (0 %)"),
                HtmlHu.barras(HtmlHu.seccion(pagina, "titulo-estado")));
        assertEquals(Map.of("nov 2026", "3 (100 %)"), HtmlHu.barras(HtmlHu.seccion(pagina, "titulo-mes")));
    }

    @Test
    @DisplayName("Filtros parciales")
    void filtrosParciales() throws Exception {
        String soloDesde = estadisticas("?desde=2026-11-15");
        assertTrue(soloDesde.contains("3 objetos en el periodo elegido."), "E1, P3 y P5");
        assertTrue(soloDesde.contains("value=\"2026-11-15\""));

        String soloHasta = estadisticas("?hasta=2026-10-31");
        assertTrue(soloHasta.contains("1 objeto en el periodo elegido."), "solo P1");
        assertTrue(soloHasta.contains("value=\"2026-10-31\""));

        assertTrue(estadisticas("?desde=2026-11-15&hasta=2026-11-15").contains("1 objeto en el periodo elegido."));
        assertTrue(estadisticas("").contains("5 objetos en el periodo elegido."), "Ver todo restaura el conjunto");
    }

    @Test
    @DisplayName("Sin datos")
    void sinDatos() throws Exception {
        String periodoVacio = estadisticas("?desde=2020-01-01&hasta=2020-01-31");
        assertTrue(periodoVacio.contains("No hay objetos reportados en esas fechas."));
        assertFalse(periodoVacio.contains("ranking__fila"));
        assertFalse(periodoVacio.contains("NaN"));

        datos.limpiar();
        String baseVacia = estadisticas("");
        assertTrue(baseVacia.contains("0 objetos en el periodo elegido."));
        assertTrue(baseVacia.contains("No hay objetos reportados en esas fechas."));
        assertFalse(baseVacia.contains("ranking__fila"));
    }

    @Test
    @DisplayName("CN-04: solo la Oficina entra")
    void soloOficina() throws Exception {
        mvc.perform(get("/estadisticas").with(estudiante(ANA))).andExpect(status().isForbidden());
        mvc.perform(get("/estadisticas").with(seguridad())).andExpect(status().isForbidden());
        mvc.perform(get("/estadisticas")).andExpect(status().isFound()).andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("CN-03: fecha inválida da 400")
    void fechasInvalidas() throws Exception {
        mvc.perform(get("/estadisticas").param("desde", "abc").with(oficina())).andExpect(status().isBadRequest());

        String invertido = estadisticas("?desde=2026-11-30&hasta=2026-11-01");
        assertTrue(invertido.contains("0 objetos en el periodo elegido."));
        assertTrue(invertido.contains("No hay objetos reportados en esas fechas."));
    }
}
