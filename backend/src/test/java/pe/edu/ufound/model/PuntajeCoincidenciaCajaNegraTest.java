package pe.edu.ufound.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Caja negra de Objeto.puntajeDeCoincidenciaCon() (HU-23): partición equivalente y valores límite. */
class PuntajeCoincidenciaCajaNegraTest {

    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 10, 10, 0);

    private final Usuario estudiante = new Usuario("Ana", "Torres", "ana@u.pe", null, RolUsuario.ESTUDIANTE);
    private final Usuario seguridad = new Usuario("Jorge", "Quispe", "jorge@u.pe", null, RolUsuario.SEGURIDAD);
    private final Usuario oficina = new Usuario("Lucía", "Paredes", "lucia@u.pe", null,
            RolUsuario.OFICINA_OBJETOS_PERDIDOS);

    // Palabras clave del perdido: mochila, negra, totto, llavero, azul.
    private final Objeto perdida = new Objeto("Mochila negra Totto", "Mochila negra Totto con llavero azul",
            TipoObjeto.PERDIDO, CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_A, null, HOY, estudiante);

    private Objeto encontrado(String nombre, String descripcion, CategoriaObjeto categoria, Pabellon lugar, int dias) {
        return new Objeto(nombre, descripcion, TipoObjeto.ENCONTRADO, categoria, lugar, null,
                HOY.plusDays(dias), seguridad);
    }

    private Objeto sinPalabras(int dias) {
        return encontrado("Bolso", "Bolso rojo de cuero",CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_A, dias);
    }

    @Test @DisplayName("CN-10: todo coincide, mismo día y 4 palabras -> 100")
    void todoCoincide() {
        assertEquals(100, perdida.puntajeDeCoincidenciaCon(encontrado("Mochila Totto", "negra con llavero",
                CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_A, 0)));
    }

    @Test @DisplayName("CN-11: otro lugar, 5 días, 1 palabra -> 55")
    void otroLugarCincoDiasUnaPalabra() {
        assertEquals(55, perdida.puntajeDeCoincidenciaCon(encontrado("Mochila", "gris marca Porta",
                CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_E, 5)));
    }

    @Test @DisplayName("CN-12: mismo lugar, 20 días, sin palabras -> 70")
    void mismoLugarVeinteDiasSinPalabras() {
        assertEquals(70, perdida.puntajeDeCoincidenciaCon(sinPalabras(20)));
    }

    @Test @DisplayName("CN-13: dos objetos perdidos -> 0")
    void mismoTipo() {
        Objeto otroPerdido = new Objeto("Mochila negra Totto", "Mochila negra Totto con llavero azul",
                TipoObjeto.PERDIDO, CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_A, null, HOY, estudiante);
        assertEquals(0, perdida.puntajeDeCoincidenciaCon(otroPerdido));
    }

    @Test @DisplayName("CN-14: distinta categoría -> 0")
    void distintaCategoria() {
        assertEquals(0, perdida.puntajeDeCoincidenciaCon(encontrado("Mochila negra Totto", "llavero azul",
                CategoriaObjeto.ROPA, Pabellon.PABELLON_A, 0)));
    }

    @Test @DisplayName("CN-15: el encontrado ya se entregó -> 0")
    void entregado() {
        Objeto encontrado = sinPalabras(0);
        encontrado.entregar("María López", "DNI 71234567", null, oficina);
        assertEquals(0, perdida.puntajeDeCoincidenciaCon(encontrado));
    }

    @Test @DisplayName("CN-16: el encontrado está archivado -> 0")
    void archivado() {
        Objeto encontrado = sinPalabras(0);
        encontrado.cambiarEstado(EstadoObjeto.ARCHIVADO, null, oficina);
        assertEquals(0, perdida.puntajeDeCoincidenciaCon(encontrado));
    }

    @Test @DisplayName("CN-17: el encontrado fue eliminado -> 0")
    void eliminado() {
        Objeto encontrado = sinPalabras(0);
        encontrado.desactivar();
        assertEquals(0, perdida.puntajeDeCoincidenciaCon(encontrado));
    }

    @Test @DisplayName("CN-18 a CN-23: límites de días")
    void limitesDeDias() {
        assertEquals(80, perdida.puntajeDeCoincidenciaCon(sinPalabras(3)));
        assertEquals(75, perdida.puntajeDeCoincidenciaCon(sinPalabras(4)));
        assertEquals(75, perdida.puntajeDeCoincidenciaCon(sinPalabras(7)));
        assertEquals(70, perdida.puntajeDeCoincidenciaCon(sinPalabras(8)));
        assertEquals(70, perdida.puntajeDeCoincidenciaCon(sinPalabras(30)));
        assertEquals(0, perdida.puntajeDeCoincidenciaCon(sinPalabras(31)));
    }

    @Test @DisplayName("CN-24: 5 palabras en común -> el bono se queda en 20 (100)")
    void topeDePalabras() {
        assertEquals(100, perdida.puntajeDeCoincidenciaCon(encontrado("Mochila negra Totto", "llavero azul",
                CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_A, 0)));
    }
}
