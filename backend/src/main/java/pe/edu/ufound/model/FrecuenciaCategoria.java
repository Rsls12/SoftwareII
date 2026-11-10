package pe.edu.ufound.model;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** HU-39: cuántos objetos hay de una categoría, separados en perdidos y encontrados. */
public record FrecuenciaCategoria(CategoriaObjeto categoria, long perdidos, long encontrados, int porcentaje) {

    public long total() {
        return perdidos + encontrados;
    }

    // HU-39: solo las categorías con objetos, de la más frecuente a la menos; el porcentaje es sobre el total.
    public static List<FrecuenciaCategoria> de(List<Objeto> objetos) {
        Map<CategoriaObjeto, List<Objeto>> porCategoria = objetos.stream().collect(Collectors.groupingBy(
                Objeto::getCategoria, () -> new EnumMap<>(CategoriaObjeto.class), Collectors.toList()));
        return porCategoria.entrySet().stream()
                .map(grupo -> {
                    long perdidos = grupo.getValue().stream().filter(Objeto::esPerdido).count();
                    int porcentaje = (int) Math.round(100.0 * grupo.getValue().size() / objetos.size());
                    return new FrecuenciaCategoria(grupo.getKey(), perdidos, grupo.getValue().size() - perdidos,
                            porcentaje);
                })
                // Orden estable: ante un empate queda primero la categoría que va antes en CategoriaObjeto.
                .sorted(Comparator.comparingLong(FrecuenciaCategoria::total).reversed())
                .toList();
    }
}
