package pe.edu.ufound.reconocimiento;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import pe.edu.ufound.model.CaracteristicasObjeto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** HU-33: la respuesta del modelo de IA, en el formato JSON que se le pide (salida estructurada). */
public record AnalisisDeFoto(
        @JsonPropertyDescription("true si la foto muestra un objeto que se puede reconocer; false si no")
        boolean objetoIdentificable,
        @JsonPropertyDescription("Qué objeto es, en pocas palabras. Ej.: Mochila, Laptop, Botella de agua")
        String objeto,
        @JsonPropertyDescription("Colores principales del objeto")
        List<String> colores,
        @JsonPropertyDescription("Marca, solo si se lee o se reconoce con claridad; si no, cadena vacía")
        String marca,
        @JsonPropertyDescription("Material principal, solo si se distingue; si no, cadena vacía")
        String material,
        @JsonPropertyDescription("Detalles que lo diferencian de otros parecidos: stickers, rayones, llaveros, estampados")
        List<String> rasgosDistintivos) {

    Optional<CaracteristicasObjeto> aCaracteristicas(LocalDateTime fechaHoraAnalisis) {
        if (!objetoIdentificable || objeto == null || objeto.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new CaracteristicasObjeto(objeto, colores, marca, material, rasgosDistintivos,
                fechaHoraAnalisis));
    }
}
