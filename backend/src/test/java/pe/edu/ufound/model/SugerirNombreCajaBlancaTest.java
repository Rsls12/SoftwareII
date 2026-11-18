package pe.edu.ufound.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Sección 10.2.7 - Prueba de caja blanca (camino básico) de Mathias Vegas sobre Objeto.sugerirNombre() (HU-35).
 * Complejidad ciclomática V(G) = 8 (15 nodos, 21 aristas, 7 nodos predicado).
 */
class SugerirNombreCajaBlancaTest {

    @Test
    @DisplayName("CB-01: camino 1-2-5-6-9-12-14-15 -> tipo null, marca null y color null devuelve 'Objeto'")
    void cb01TipoNuloSinMarcaNiColor() {
        assertEquals("Objeto", Objeto.sugerirNombre(null, null, null));
    }

    @Test
    @DisplayName("CB-02: camino 1-2-3-5-6-9-12-14-15 -> tipo en blanco devuelve 'Objeto'")
    void cb02TipoEnBlancoSinMarcaNiColor() {
        assertEquals("Objeto", Objeto.sugerirNombre("   ", null, null));
    }

    @Test
    @DisplayName("CB-03: camino 1-2-3-4-6-9-12-14-15 -> solo tipo válido con espacios laterales")
    void cb03SoloTipoValido() {
        assertEquals("Mochila", Objeto.sugerirNombre("  Mochila  ", null, null));
    }

    @Test
    @DisplayName("CB-04: camino 1-2-3-4-6-7-9-12-14-15 -> tipo válido y marca en blanco")
    void cb04TipoValidoYMarcaEnBlanco() {
        assertEquals("Mochila", Objeto.sugerirNombre("Mochila", "   ", null));
    }

    @Test
    @DisplayName("CB-05: camino 1-2-3-4-6-7-8-9-12-14-15 -> tipo y marca válidos, color nulo")
    void cb05TipoYMarcaValidos() {
        assertEquals("Mochila Totto", Objeto.sugerirNombre("Mochila", " Totto ", null));
    }

    @Test
    @DisplayName("CB-06: camino 1-2-3-4-6-7-8-9-10-12-14-15 -> tipo y marca válidos, color en blanco")
    void cb06TipoYMarcaValidosYColorEnBlanco() {
        assertEquals("Mochila Totto", Objeto.sugerirNombre("Mochila", "Totto", "   "));
    }

    @Test
    @DisplayName("CB-07: camino 1-2-3-4-6-7-8-9-10-11-12-14-15 -> tipo, marca y color válidos (<= 80 chars)")
    void cb07TipoMarcaYColorValidos() {
        assertEquals("Mochila Totto negro", Objeto.sugerirNombre("Mochila", "Totto", " NEGRO "));
    }

    @Test
    @DisplayName("CB-08: camino 1-2-3-4-6-7-8-9-10-11-12-13-15 -> resultado mayor a 80 caracteres se recorta")
    void cb08ResultadoExcedeMaximoYSeRecortaA80() {
        String tipoLargo = "Calculadora cientifica programable de ingenieria";
        String marcaLarga = "TexasInstrumentsEdicionEspecial";
        String colorLargo = "grisplateadooscuro";

        String sugerido = Objeto.sugerirNombre(tipoLargo, marcaLarga, colorLargo);

        assertEquals(Objeto.NOMBRE_MAXIMO, sugerido.length());
    }
}
