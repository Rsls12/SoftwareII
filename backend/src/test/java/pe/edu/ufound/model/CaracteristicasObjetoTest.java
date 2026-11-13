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

    // ---------- Sugerencias para el formulario (HU-33 + HU-35) ----------

    @Test
    void sugiereLosDatosDelFormularioConTodoLoReconocido() {
        CaracteristicasObjeto lentes = new CaracteristicasObjeto("Lentes", List.of("Negro", "Rosado"), null, "Plástico",
                List.of("Montura rectangular de marco grueso", "Línea rosada en las patillas."), AHORA);

        assertEquals("Lentes negro", lentes.sugerirNombre());
        assertEquals("Lentes, de color negro, rosado, de plástico. Montura rectangular de marco grueso; "
                + "Línea rosada en las patillas.", lentes.sugerirDescripcion());
        assertEquals(CategoriaObjeto.ACCESORIOS, lentes.sugerirCategoria());
    }

    @Test
    void siLaIaRespondeEnMinusculasElNombreYLasOracionesEmpiezanConMayuscula() {
        CaracteristicasObjeto lentes = new CaracteristicasObjeto("lentes", List.of("negro", "rojo"), null, "plástico",
                List.of("marco rectangular", "patillas con una línea roja"), AHORA);

        assertEquals("Lentes", lentes.getObjetoDetectado());
        assertEquals("Lentes negro", lentes.sugerirNombre());
        assertEquals("Lentes, de color negro, rojo, de plástico. Marco rectangular; patillas con una línea roja.",
                lentes.sugerirDescripcion());
    }

    @Test
    void laMarcaVaEnElNombreYEnLaDescripcion() {
        CaracteristicasObjeto mouse = new CaracteristicasObjeto("Mouse inalámbrico", List.of("negro"), "Logitech",
                null, null, AHORA);

        assertEquals("Mouse inalámbrico Logitech negro", mouse.sugerirNombre());
        assertEquals("Mouse inalámbrico marca Logitech, de color negro.", mouse.sugerirDescripcion());
        assertEquals(CategoriaObjeto.TECNOLOGIA, mouse.sugerirCategoria());
    }

    @Test
    void laCategoriaSeReconoceAunqueElNombreLleveTildes() {
        assertEquals(CategoriaObjeto.TECNOLOGIA, conObjeto("Audífonos").sugerirCategoria());
        assertEquals(CategoriaObjeto.DOCUMENTOS, conObjeto("Carné universitario").sugerirCategoria());
        assertEquals(CategoriaObjeto.OTROS, conObjeto("Peluche").sugerirCategoria());
    }

    @Test
    void lasSugerenciasRespetanLosLimitesDelFormulario() {
        // Una descripción muy corta se completa hasta el mínimo; un nombre largo se corta al máximo.
        assertTrue(conObjeto("Gorra").sugerirDescripcion().length() >= Objeto.DESCRIPCION_MINIMA);
        CaracteristicasObjeto largo = new CaracteristicasObjeto("o".repeat(70), List.of("verde"),
                "m".repeat(40), null, null, AHORA);
        assertEquals(Objeto.NOMBRE_MAXIMO, largo.sugerirNombre().length());
    }

    private static CaracteristicasObjeto conObjeto(String objeto) {
        return new CaracteristicasObjeto(objeto, null, null, null, null, AHORA);
    }

    @Test
    void unTextoJustoEnElLimiteNoSeRecorta() {
        String marca = "m".repeat(CaracteristicasObjeto.MARCA_MAXIMA);

        assertEquals(marca, new CaracteristicasObjeto("Gorra", null, marca, null, null, AHORA).getMarca());
    }
}
