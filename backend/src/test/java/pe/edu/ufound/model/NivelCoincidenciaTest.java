package pe.edu.ufound.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** HU-24: el puntaje de una coincidencia (HU-23) se traduce en un nivel que la Oficina prioriza. */
class NivelCoincidenciaTest {

    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 10, 10, 0);

    private final Usuario estudiante = new Usuario("Ana", "Torres", "ana@u.pe", null, RolUsuario.ESTUDIANTE);
    private final Usuario seguridad = new Usuario("Jorge", "Quispe", "jorge@u.pe", null, RolUsuario.SEGURIDAD);

    @Test
    void puntajeDesde90EsNivelAlto() {
        // 40 (categoría) + 25 (mismo pabellón) + 15 (menos de 3 días) + 20 (varias palabras en común, tope 20) = 100
        Objeto perdida = perdido("Laptop Lenovo gris", "Laptop Lenovo con sticker de la NASA en la tapa",
                CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_C, HOY);
        Objeto encontrada = encontrado("Laptop gris", "Lenovo gris con sticker NASA en la tapa",
                CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_C, HOY.plusHours(2));

        Coincidencia coincidencia = encontrada.detectarCoincidencias(List.of(perdida)).get(0);

        assertEquals(100, coincidencia.getPuntaje());
        assertEquals(NivelCoincidencia.ALTO, coincidencia.getNivelCoincidencia());
    }

    @Test
    void puntajeEntre75Y89EsNivelMedio() {
        // 40 (categoría) + 0 (distinto pabellón) + 15 (2 días) + 20 (4+ palabras en común) = 75
        Objeto perdida = perdido("Mochila Totto negra", "Mochila escolar negra con cierre plateado",
                CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_A, HOY);
        Objeto encontrada = encontrado("Mochila negra Totto", "Mochila negra con cierre plateado",
                CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_B, HOY.plusDays(2));

        Coincidencia coincidencia = encontrada.detectarCoincidencias(List.of(perdida)).get(0);

        assertEquals(75, coincidencia.getPuntaje());
        assertEquals(NivelCoincidencia.MEDIO, coincidencia.getNivelCoincidencia());
        assertTrue(coincidencia.esRelevante());
    }

    @Test
    void puntajeEntre60Y74EsNivelBajo() {
        // Mismo caso que CoincidenciaTest: 40 (categoría) + 5 (más de una semana) + 15 (palabras en común) = 60
        Objeto perdida = perdido("Cuaderno de Cálculo", "Cuaderno ANILLADO de cálculo",
                CategoriaObjeto.UTILES, Pabellon.PABELLON_A, HOY);
        Objeto encontrado = encontrado("cuaderno calculo", "cuaderno anillado",
                CategoriaObjeto.UTILES, Pabellon.PABELLON_E, HOY.plusDays(10));

        Coincidencia coincidencia = encontrado.detectarCoincidencias(List.of(perdida)).get(0);

        assertEquals(60, coincidencia.getPuntaje());
        assertEquals(NivelCoincidencia.BAJO, coincidencia.getNivelCoincidencia());
        assertFalse(coincidencia.esRelevante());
    }

    private Objeto perdido(String nombre, String descripcion, CategoriaObjeto categoria, Pabellon lugar,
                           LocalDateTime fecha) {
        return new Objeto(nombre, descripcion, TipoObjeto.PERDIDO, categoria, lugar, null, fecha, estudiante);
    }

    private Objeto encontrado(String nombre, String descripcion, CategoriaObjeto categoria, Pabellon lugar,
                              LocalDateTime fecha) {
        return new Objeto(nombre, descripcion, TipoObjeto.ENCONTRADO, categoria, lugar, null, fecha, seguridad);
    }
}
