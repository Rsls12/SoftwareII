package pe.edu.ufound.model;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** HU-40: en qué pabellones se pierden más objetos, para que la Oficina identifique zonas críticas. */
public record FrecuenciaUbicacion(Pabellon ubicacion, long cantidad, int porcentaje) {

    // HU-40: solo cuenta objetos PERDIDOS (la "frecuencia de pérdida"); un objeto ENCONTRADO no perdió nada.
    // De la zona con más pérdidas a la que menos; un pabellón sin pérdidas no aparece en el ranking.
    public static List<FrecuenciaUbicacion> de(List<Objeto> objetos) {
        List<Objeto> perdidos = objetos.stream().filter(Objeto::esPerdido).toList();
        if (perdidos.isEmpty()) {
            return List.of();
        }
        Map<Pabellon, Long> porPabellon = perdidos.stream().collect(Collectors.groupingBy(
                Objeto::getUbicacion, () -> new EnumMap<>(Pabellon.class), Collectors.counting()));
        return porPabellon.entrySet().stream()
                .map(grupo -> new FrecuenciaUbicacion(grupo.getKey(), grupo.getValue(),
                        (int) Math.round(100.0 * grupo.getValue() / perdidos.size())))
                // Orden estable: ante un empate queda primero el pabellón que va antes en Pabellon.
                .sorted(Comparator.comparingLong(FrecuenciaUbicacion::cantidad).reversed())
                .toList();
    }
}
