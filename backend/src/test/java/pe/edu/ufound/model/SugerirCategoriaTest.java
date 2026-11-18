package pe.edu.ufound.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Sección 10.4.7 - Prueba unitaria automatizada de Mathias Vegas sobre Objeto.sugerirCategoria() (HU-35).
 */
class SugerirCategoriaTest {

    @Test
    @DisplayName("PU-01: texto con artículo electrónico en mayúsculas -> TECNOLOGIA")
    void pu01ReconoceTecnologia() {
        assertEquals(CategoriaObjeto.TECNOLOGIA, Objeto.sugerirCategoria("MOUSE INALAMBRICO LOGITECH"));
    }

    @Test
    @DisplayName("PU-02: texto de documento con tilde -> DOCUMENTOS")
    void pu02ReconoceDocumentos() {
        assertEquals(CategoriaObjeto.DOCUMENTOS, Objeto.sugerirCategoria("Carné universitario encontrado"));
    }

    @Test
    @DisplayName("PU-03: texto de recipiente de bebida por marca -> BOTELLAS")
    void pu03ReconoceBotellas() {
        assertEquals(CategoriaObjeto.BOTELLAS, Objeto.sugerirCategoria("Termo Stanley verde de 1 litro"));
    }

    @Test
    @DisplayName("PU-04: texto con prenda de vestir -> ROPA")
    void pu04ReconoceRopa() {
        assertEquals(CategoriaObjeto.ROPA, Objeto.sugerirCategoria("Casaca impermeable azul talla M"));
    }

    @Test
    @DisplayName("PU-05: texto con material académico -> UTILES")
    void pu05ReconoceUtiles() {
        assertEquals(CategoriaObjeto.UTILES, Objeto.sugerirCategoria("Calculadora científica Casio"));
    }

    @Test
    @DisplayName("PU-06: texto con accesorio personal -> ACCESORIOS")
    void pu06ReconoceAccesorios() {
        assertEquals(CategoriaObjeto.ACCESORIOS, Objeto.sugerirCategoria("Lentes de medida con estuche"));
    }

    @Test
    @DisplayName("PU-07: objeto sin palabras clave conocidas -> OTROS")
    void pu07ObjetoDesconocidoDevuelveOtros() {
        assertEquals(CategoriaObjeto.OTROS, Objeto.sugerirCategoria("Parlante portátil azul"));
    }

    @Test
    @DisplayName("PU-08: entrada nula o en blanco -> OTROS")
    void pu08EntradaNulaOEnBlancoDevuelveOtros() {
        assertEquals(CategoriaObjeto.OTROS, Objeto.sugerirCategoria(null));
        assertEquals(CategoriaObjeto.OTROS, Objeto.sugerirCategoria("   "));
    }
}
