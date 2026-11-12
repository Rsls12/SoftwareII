package pe.edu.ufound.model;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** HU-38: una barra del gráfico de estadísticas. */
public record Estadistica(String etiqueta, long cantidad, int porcentaje) {

    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMM yyyy", new Locale("es"));

    public static List<Estadistica> porEstado(List<Objeto> objetos) {
        return Arrays.stream(EstadoObjeto.values())
                .map(estado -> barra(estado.getEtiqueta(),
                        objetos.stream().filter(o -> o.getEstado() == estado).count(), objetos.size()))
                .toList();
    }

    public static List<Estadistica> porMes(List<Objeto> objetos) {
        Map<YearMonth, Long> porMes = objetos.stream().collect(Collectors.groupingBy(
                o -> YearMonth.from(o.getFechaHoraReporte()), TreeMap::new, Collectors.counting()));
        return porMes.entrySet().stream()
                .map(mes -> barra(mes.getKey().format(MES), mes.getValue(), objetos.size()))
                .toList();
    }

    private static Estadistica barra(String etiqueta, long cantidad, int total) {
        int porcentaje = total == 0 ? 0 : (int) Math.round(100.0 * cantidad / total);
        return new Estadistica(etiqueta, cantidad, porcentaje);
    }
}
