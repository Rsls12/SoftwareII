package pe.edu.ufound.model;

import org.junit.jupiter.api.Test;
import pe.edu.ufound.excepcion.DatosInvalidosException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** HU-33: las características que la IA reconoce en la foto se guardan limpias y dentro de su tamaño. */
class CaracteristicasObjetoTest {

    private static final LocalDateTime AHORA = LocalDateTime.now();

    @Test
    void guardaLoQueSeReconocioEnLaFoto() {
        CaracteristicasObjeto caracteristicas = new CaracteristicasObjeto("  Laptop ", List.of("gris", " negro "),
                "Lenovo", "plástico", List.of("sticker de la NASA", "esquina rayada"), AHORA);

        assertEquals("Laptop", caracteristicas.getObjetoDetectado());
        assertEquals("gris, negro", caracteristicas.getColores());
        assertEquals("Lenovo", caracteristicas.getMarca());
        assertEquals("plástico", caracteristicas.getMaterial());
        assertEquals("sticker de la NASA; esquina rayada", caracteristicas.getRasgosDistintivos());
        assertEquals(AHORA, caracteristicas.getFechaHoraAnalisis());
        assertTrue(caracteristicas.tieneColores());
        assertTrue(caracteristicas.tieneMarca());
        assertTrue(caracteristicas.tieneMaterial());
        assertTrue(caracteristicas.tieneRasgosDistintivos());
    }

    @Test
    void losDatosVaciosNoSeGuardan() {
        CaracteristicasObjeto caracteristicas = new CaracteristicasObjeto("Llaves", Arrays.asList(" ", null),
                "", "   ", null, AHORA);

        assertEquals("Llaves", caracteristicas.getObjetoDetectado());
        assertFalse(caracteristicas.tieneColores());
        assertFalse(caracteristicas.tieneMarca());
        assertFalse(caracteristicas.tieneMaterial());
        assertFalse(caracteristicas.tieneRasgosDistintivos());
        assertNull(caracteristicas.getColores());
    }

    @Test
    void sinSaberQueObjetoEsNoHayCaracteristicas() {
        assertEquals("objetoDetectado", assertThrows(DatosInvalidosException.class,
                () -> new CaracteristicasObjeto(null, List.of("rojo"), null, null, null, AHORA)).getCampo());
        assertEquals("objetoDetectado", assertThrows(DatosInvalidosException.class,
                () -> new CaracteristicasObjeto("  ", List.of("rojo"), null, null, null, AHORA)).getCampo());
        assertEquals("fechaHoraAnalisis", assertThrows(DatosInvalidosException.class,
                () -> new CaracteristicasObjeto("Gorra", List.of("rojo"), null, null, null, null)).getCampo());
    }

    @Test
    void losTextosQueNoCabenSeRecortan() {
        CaracteristicasObjeto caracteristicas = new CaracteristicasObjeto("x".repeat(200), null,
                "m".repeat(100), null, List.of("r".repeat(400)), AHORA);

        assertEquals(CaracteristicasObjeto.OBJETO_MAXIMO, caracteristicas.getObjetoDetectado().length());
        assertEquals(CaracteristicasObjeto.MARCA_MAXIMA, caracteristicas.getMarca().length());
        assertEquals(CaracteristicasObjeto.RASGOS_MAXIMO, caracteristicas.getRasgosDistintivos().length());
        assertTrue(caracteristicas.getMarca().endsWith("…"));
    }

    @Test
    void unTextoJustoEnElLimiteNoSeRecorta() {
        String marca = "m".repeat(CaracteristicasObjeto.MARCA_MAXIMA);

        assertEquals(marca, new CaracteristicasObjeto("Gorra", null, marca, null, null, AHORA).getMarca());
    }
}
