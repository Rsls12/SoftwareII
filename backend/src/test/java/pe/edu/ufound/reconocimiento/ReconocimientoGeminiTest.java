package pe.edu.ufound.reconocimiento;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.ufound.model.CaracteristicasObjeto;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** HU-33: el análisis de la foto con Gemini, contra un servidor falso de la API. */
class ReconocimientoGeminiTest {

    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
    static final byte[] GIF = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00};

    static final String MOCHILA = """
            {"objetoIdentificable": true, "objeto": "Mochila", "colores": ["negro", "gris"],
             "marca": "Totto", "material": "tela", "rasgosDistintivos": ["llavero de Lima", "cierre roto"]}
            """;

    GeminiFalso gemini;
    ReconocimientoGemini reconocimiento;

    @BeforeEach
    void iniciar() {
        gemini = new GeminiFalso();
        reconocimiento = new ReconocimientoGemini("clave-de-prueba", List.of("gemini-3.6-flash"), gemini.url() + "/");
    }

    @AfterEach
    void cerrar() {
        gemini.cerrar();
    }

    @Test
    void reconoceLasCaracteristicasDeLaFoto() {
        gemini.responderTexto(MOCHILA);

        CaracteristicasObjeto caracteristicas = reconocimiento.analizar(foto(PNG)).orElseThrow();

        assertEquals("Mochila", caracteristicas.getObjetoDetectado());
        assertEquals("negro, gris", caracteristicas.getColores());
        assertEquals("Totto", caracteristicas.getMarca());
        assertEquals("tela", caracteristicas.getMaterial());
        assertEquals("llavero de Lima; cierre roto", caracteristicas.getRasgosDistintivos());
        assertNotNull(caracteristicas.getFechaHoraAnalisis());
    }

    @Test
    void leEnviaLaFotoAlModeloYLePideLaRespuestaEnJson() {
        gemini.responderTexto(MOCHILA);

        reconocimiento.analizar(foto(PNG));

        assertEquals(1, gemini.peticiones().size());
        GeminiFalso.Peticion peticion = gemini.peticiones().get(0);
        assertEquals("POST", peticion.metodo());
        assertEquals("/v1beta/models/gemini-3.6-flash:generateContent", peticion.ruta());
        assertEquals("clave-de-prueba", peticion.apiKey());
        // La clave no viaja en la URL.
        assertNull(peticion.consulta());
        String cuerpo = peticion.cuerpo().replace(" ", "");
        assertTrue(cuerpo.contains("\"systemInstruction\""));
        assertTrue(cuerpo.contains("\"mimeType\":\"image/png\""));
        assertTrue(cuerpo.contains("\"data\":\"" + Base64.getEncoder().encodeToString(PNG) + "\""));
        assertTrue(cuerpo.contains("\"responseMimeType\":\"application/json\""));
        assertTrue(cuerpo.contains("\"responseJsonSchema\""));
        assertTrue(cuerpo.contains("\"objetoIdentificable\""));
    }

    @Test
    void elModeloSeCambiaPorConfiguracion() {
        ReconocimientoGemini otroModelo = new ReconocimientoGemini("clave-de-prueba", List.of("gemini-3.8-flash"), gemini.url());
        gemini.responderTexto(MOCHILA);

        otroModelo.analizar(foto(JPEG));

        assertEquals("/v1beta/models/gemini-3.8-flash:generateContent", gemini.peticiones().get(0).ruta());
        assertTrue(gemini.peticiones().get(0).cuerpo().replace(" ", "").contains("\"mimeType\":\"image/jpeg\""));
    }

    @Test
    void siLaFotoNoMuestraUnObjetoNoDevuelveCaracteristicas() {
        gemini.responderTexto("""
                {"objetoIdentificable": false, "objeto": "", "colores": [], "marca": "", "material": "",
                 "rasgosDistintivos": []}
                """);

        assertTrue(reconocimiento.analizar(foto(PNG)).isEmpty());
    }

    @Test
    void losDatosQueNoSeVenQuedanVacios() {
        gemini.responderTexto("""
                {"objetoIdentificable": true, "objeto": "Botella de agua", "colores": ["verde"],
                 "marca": "", "material": " ", "rasgosDistintivos": []}
                """);

        CaracteristicasObjeto caracteristicas = reconocimiento.analizar(foto(PNG)).orElseThrow();

        assertEquals("Botella de agua", caracteristicas.getObjetoDetectado());
        assertEquals("verde", caracteristicas.getColores());
        assertFalse(caracteristicas.tieneMarca());
        assertFalse(caracteristicas.tieneMaterial());
        assertFalse(caracteristicas.tieneRasgosDistintivos());
    }

    @Test
    void siGeminiBloqueaLaFotoNoDevuelveCaracteristicas() {
        gemini.responder("""
                {"promptFeedback": {"blockReason": "SAFETY"}}
                """);

        assertTrue(reconocimiento.analizar(foto(PNG)).isEmpty());
    }

    @Test
    void unaRespuestaCortadaOFrenadaNoSeUsa() {
        gemini.responderTexto("{\"objetoIdentificable\": true, \"objeto\": \"Moch", "MAX_TOKENS");
        assertTrue(reconocimiento.analizar(foto(PNG)).isEmpty());

        gemini.responderTexto(MOCHILA, "SAFETY");
        assertTrue(reconocimiento.analizar(foto(PNG)).isEmpty());
    }

    @Test
    void unaRespuestaSinElFormatoPedidoNoRompeNada() {
        gemini.responderTexto("Es una mochila negra.");
        assertTrue(reconocimiento.analizar(foto(PNG)).isEmpty());

        gemini.responder("<html>Servicio no disponible</html>");
        assertTrue(reconocimiento.analizar(foto(PNG)).isEmpty());
    }

    @Test
    void siLaApiFallaOSeAgotaElLimiteGratuitoNoDevuelveCaracteristicasNiLanzaError() {
        gemini.responderError(429);
        assertTrue(reconocimiento.analizar(foto(PNG)).isEmpty());

        gemini.responderError(403);
        assertTrue(reconocimiento.analizar(foto(PNG)).isEmpty());

        gemini.responderError(500);
        assertTrue(reconocimiento.analizar(foto(PNG)).isEmpty());
    }

    // ---------- Modelos de respaldo: el plan gratuito a veces responde "high demand" (503) ----------

    @Test
    void siElPrimerModeloEstaSaturadoUsaElSiguiente() {
        ReconocimientoGemini conRespaldo = new ReconocimientoGemini("clave-de-prueba",
                List.of("gemini-saturado", "gemini-libre"), gemini.url());
        gemini.responderTexto(MOCHILA);
        gemini.responderErrorEnModelo("gemini-saturado", 503);

        CaracteristicasObjeto caracteristicas = conRespaldo.analizar(foto(PNG)).orElseThrow();

        assertEquals("Mochila", caracteristicas.getObjetoDetectado());
        assertEquals(2, gemini.peticiones().size());
        assertEquals("/v1beta/models/gemini-saturado:generateContent", gemini.peticiones().get(0).ruta());
        assertEquals("/v1beta/models/gemini-libre:generateContent", gemini.peticiones().get(1).ruta());
        // Al siguiente modelo le llega exactamente la misma foto y el mismo pedido.
        assertEquals(gemini.peticiones().get(0).cuerpo(), gemini.peticiones().get(1).cuerpo());
    }

    @Test
    void siElLimiteGratuitoSeAgotaEnUnModeloPruebaElSiguiente() {
        ReconocimientoGemini conRespaldo = new ReconocimientoGemini("clave-de-prueba",
                List.of("gemini-agotado", "gemini-libre"), gemini.url());
        gemini.responderTexto(MOCHILA);
        gemini.responderErrorEnModelo("gemini-agotado", 429);

        assertTrue(conRespaldo.analizar(foto(PNG)).isPresent());
        assertEquals(2, gemini.peticiones().size());
    }

    @Test
    void conUnaClaveInvalidaNoPruebaOtrosModelos() {
        ReconocimientoGemini conRespaldo = new ReconocimientoGemini("clave-de-prueba",
                List.of("gemini-a", "gemini-b"), gemini.url());
        gemini.responderError(403);

        assertTrue(conRespaldo.analizar(foto(PNG)).isEmpty());
        assertEquals(1, gemini.peticiones().size());
    }

    @Test
    void siTodosLosModelosEstanSaturadosNoHayCaracteristicas() {
        ReconocimientoGemini conRespaldo = new ReconocimientoGemini("clave-de-prueba",
                List.of("gemini-a", "gemini-b"), gemini.url());
        gemini.responderError(503);

        assertTrue(conRespaldo.analizar(foto(PNG)).isEmpty());
        assertEquals(2, gemini.peticiones().size());
    }

    @Test
    void sinConexionConGeminiNoDevuelveCaracteristicas() {
        ReconocimientoGemini sinServidor = new ReconocimientoGemini("clave-de-prueba",
                List.of("gemini-a", "gemini-b"), "http://127.0.0.1:1");

        assertTrue(sinServidor.analizar(foto(PNG)).isEmpty());
    }

    @Test
    void sinClaveConfiguradaQuedaDesactivadoYNoLlamaAGemini() {
        ReconocimientoGemini sinClave = new ReconocimientoGemini("  ", List.of("gemini-3.6-flash"), gemini.url());

        assertFalse(sinClave.estaDisponible());
        assertTrue(sinClave.analizar(foto(PNG)).isEmpty());
        assertFalse(new ReconocimientoGemini("clave-de-prueba", List.of(" "), gemini.url()).estaDisponible());
        assertTrue(gemini.peticiones().isEmpty());
    }

    @Test
    void sinFotoConUnArchivoQueNoEsImagenOConUnGifNoLlamaAGemini() {
        assertTrue(reconocimiento.analizar(null).isEmpty());
        assertTrue(reconocimiento.analizar(new MockMultipartFile("foto", new byte[0])).isEmpty());
        assertTrue(reconocimiento.analizar(
                new MockMultipartFile("foto", "virus.png", "image/png", "no soy imagen".getBytes())).isEmpty());
        // Gemini no acepta GIF: el objeto se registra igual, pero sin análisis.
        assertTrue(reconocimiento.analizar(new MockMultipartFile("foto", "foto.gif", "image/gif", GIF)).isEmpty());

        assertTrue(gemini.peticiones().isEmpty());
    }

    @Test
    void siNoSePuedeLeerLaFotoNoLlamaAGemini() throws IOException {
        // Mockito simula un archivo subido que falla al leerse.
        MultipartFile ilegible = mock(MultipartFile.class);
        when(ilegible.isEmpty()).thenReturn(false);
        when(ilegible.getBytes()).thenThrow(new IOException("disco lleno"));

        assertTrue(reconocimiento.analizar(ilegible).isEmpty());
        assertTrue(gemini.peticiones().isEmpty());
    }

    private static MockMultipartFile foto(byte[] datos) {
        return new MockMultipartFile("foto", "foto.png", "image/png", datos);
    }
}
