package pe.edu.ufound.pruebashu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import pe.edu.ufound.model.CategoriaObjeto;
import pe.edu.ufound.model.TipoObjeto;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.edu.ufound.pruebashu.DatosHu.ANA;

/** Caja blanca de HU-38: caminos de RutasWeb.estadisticas(). */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:hu38-caminos-it;DB_CLOSE_DELAY=-1")
class Hu38EstadisticasCaminosIT extends BaseHuIT {

    private void reportado(int anio, int mes, int dia, int hora, int minuto) {
        datos.objeto("Objeto HU38", TipoObjeto.PERDIDO, CategoriaObjeto.TECNOLOGIA,
                LocalDateTime.of(anio, mes, dia, hora, minuto), ANA);
    }

    private void cuenta(String consulta, String esperado) throws Exception {
        String pagina = html("/estadisticas" + consulta, oficina());
        assertTrue(pagina.contains(esperado + " en el periodo elegido."), esperado);
    }

    @Test
    @DisplayName("CB-01: sin objetos activos")
    void sinObjetos() throws Exception {
        cuenta("", "0 objetos");
    }

    @Test
    @DisplayName("CB-02: sin fechas el objeto se cuenta")
    void sinFechas() throws Exception {
        reportado(2026, 11, 15, 12, 0);
        cuenta("", "1 objeto");
    }

    @Test
    @DisplayName("CB-03: objeto antes de Desde no se cuenta")
    void antesDeDesde() throws Exception {
        reportado(2026, 10, 31, 23, 59);
        cuenta("?desde=2026-11-01", "0 objetos");
    }

    @Test
    @DisplayName("CB-04: objeto en el día de Desde se cuenta")
    void enDesde() throws Exception {
        reportado(2026, 11, 1, 0, 0);
        cuenta("?desde=2026-11-01", "1 objeto");
    }

    @Test
    @DisplayName("CB-05: objeto después de Hasta no se cuenta")
    void despuesDeHasta() throws Exception {
        reportado(2026, 12, 1, 0, 0);
        cuenta("?hasta=2026-11-30", "0 objetos");
    }

    @Test
    @DisplayName("CB-06: objeto en el día de Hasta se cuenta")
    void enHasta() throws Exception {
        reportado(2026, 11, 30, 23, 59);
        cuenta("?hasta=2026-11-30", "1 objeto");
    }
}
