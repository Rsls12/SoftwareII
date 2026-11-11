package pe.edu.ufound.reconocimiento;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.ufound.model.CaracteristicasObjeto;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** HU-33: el análisis de la foto con Claude, contra un servidor falso de la API de Anthropic. */
class ReconocimientoClaudeTest {

    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};

    static final String MOCHILA = """
            {"objetoIdentificable": true, "objeto": "Mochila", "colores": ["negro", "gris"],
             "marca": "Totto", "material": "tela", "rasgosDistintivos": ["llavero de Lima", "cierre roto"]}
            """;

    ClaudeFalso claude;
    ReconocimientoClaude reconocimiento;

    @BeforeEach
    void iniciar() {
        claude = new ClaudeFalso();
        reconocimiento = new ReconocimientoClaude("clave-de-prueba", claude.url());
    }

    @AfterEach
    void cerrar() {
        reconocimiento.cerrar();
        claude.cerrar();
    }

    @Test
    void reconoceLasCaracteristicasDeLaFoto() {
        claude.responderAnalisis(MOCHILA);

        CaracteristicasObjeto caracteristicas = reconocimiento.analizar(foto(PNG)).orElseThrow();

        assertEquals("Mochila", caracteristicas.getObjetoDetectado());
        assertEquals("negro, gris", caracteristicas.getColores());
        assertEquals("Totto", caracteristicas.getMarca());
        assertEquals("tela", caracteristicas.getMaterial());
        assertEquals("llavero de Lima; cierre roto", caracteristicas.getRasgosDistintivos());
        assertNotNull(caracteristicas.getFechaHoraAnalisis());
    }

    @Test
    void leEnviaLaFotoAlModeloYLePideLaRespuestaConFormato() {
        claude.responderAnalisis(MOCHILA);

        reconocimiento.analizar(foto(PNG));

        assertEquals(1, claude.peticiones().size());
        ClaudeFalso.Peticion peticion = claude.peticiones().get(0);
        assertEquals("POST", peticion.metodo());
        assertEquals("/v1/messages", peticion.ruta());
        // El SDK de Anthropic envía la clave en la cabecera Authorization.
        assertEquals("Bearer clave-de-prueba", peticion.autorizacion());
        String cuerpo = peticion.cuerpo().replace(" ", "");
        assertTrue(cuerpo.contains("\"model\":\"" + ReconocimientoClaude.MODELO + "\""));
        assertTrue(cuerpo.contains("\"media_type\":\"image/png\""));
        assertTrue(cuerpo.contains(Base64.getEncoder().encodeToString(PNG)));
        // Salida estructurada con el esquema de AnalisisDeFoto y esfuerzo bajo.
        assertTrue(cuerpo.contains("\"output_config\""));
        assertTrue(cuerpo.contains("\"json_schema\""));
        assertTrue(cuerpo.contains("\"objetoIdentificable\""));
        assertTrue(cuerpo.contains("\"effort\":\"low\""));
        // Respaldo del lado del servidor si el modelo declina la foto.
        assertTrue(cuerpo.contains("\"fallbacks\":\"default\""));
        assertTrue(peticion.beta().contains("server-side-fallback-2026-07-01"));
    }

    @Test
    void detectaElTipoDeImagenPorSuContenido() {
        claude.responderAnalisis(MOCHILA);

        reconocimiento.analizar(new MockMultipartFile("foto", "foto.png", "image/png", JPEG));

        assertTrue(claude.peticiones().get(0).cuerpo().replace(" ", "").contains("\"media_type\":\"image/jpeg\""));
    }

    @Test
    void siLaFotoNoMuestraUnObjetoNoDevuelveCaracteristicas() {
        claude.responderAnalisis("""
                {"objetoIdentificable": false, "objeto": "", "colores": [], "marca": "", "material": "",
                 "rasgosDistintivos": []}
                """);

        assertTrue(reconocimiento.analizar(foto(PNG)).isEmpty());
    }

    @Test
    void losDatosQueNoSeVenQuedanVacios() {
        claude.responderAnalisis("""
                {"objetoIdentificable": true, "objeto": "Botella de agua", "colores": ["verde"],
                 "marca": "", "material": " ", "rasgosDistintivos": []}
                """);

        CaracteristicasObjeto caracteristicas = reconocimiento.analizar(foto(PNG)).orElseThrow();

        assertEquals("Botella de agua", caracteristicas.getObjetoDetectado());
        assertFalse(caracteristicas.tieneMarca());
        assertFalse(caracteristicas.tieneMaterial());
        assertFalse(caracteristicas.tieneRasgosDistintivos());
    }

    @Test
    void siElModeloDeclinaLaFotoNoDevuelveCaracteristicas() {
        claude.responderNegativa();

        assertTrue(reconocimiento.analizar(foto(PNG)).isEmpty());
    }

    @Test
    void unaRespuestaIncompletaNoSeUsa() {
        claude.responderMensaje("{\"objetoIdentificable\": true, \"objeto\": \"Moch", "max_tokens");

        assertTrue(reconocimiento.analizar(foto(PNG)).isEmpty());
    }

    @Test
    void unaRespuestaSinElFormatoPedidoNoRompeNada() {
        claude.responderAnalisis("Es una mochila negra.");

        assertTrue(reconocimiento.analizar(foto(PNG)).isEmpty());
    }

    @Test
    void siLaApiFallaNoDevuelveCaracteristicasNiLanzaError() {
        claude.responderError(401);
        assertTrue(reconocimiento.analizar(foto(PNG)).isEmpty());

        claude.responderError(500);
        assertTrue(reconocimiento.analizar(foto(PNG)).isEmpty());
    }

    @Test
    void sinConexionConLaApiNoDevuelveCaracteristicas() {
        ReconocimientoClaude sinServidor = new ReconocimientoClaude("clave-de-prueba", "http://127.0.0.1:1");

        assertTrue(sinServidor.analizar(foto(PNG)).isEmpty());
        sinServidor.cerrar();
    }

    @Test
    void sinClaveConfiguradaQuedaDesactivadoYNoLlamaALaApi() {
        ReconocimientoClaude sinClave = new ReconocimientoClaude("  ", claude.url());

        assertFalse(sinClave.estaDisponible());
        assertTrue(sinClave.analizar(foto(PNG)).isEmpty());
        assertTrue(claude.peticiones().isEmpty());
    }

    @Test
    void sinFotoOConUnArchivoQueNoEsImagenNoLlamaALaApi() {
        assertTrue(reconocimiento.analizar(null).isEmpty());
        assertTrue(reconocimiento.analizar(new MockMultipartFile("foto", new byte[0])).isEmpty());
        assertTrue(reconocimiento.analizar(
                new MockMultipartFile("foto", "virus.png", "image/png", "no soy imagen".getBytes())).isEmpty());

        assertTrue(claude.peticiones().isEmpty());
    }

    @Test
    void siNoSePuedeLeerLaFotoNoLlamaALaApi() throws IOException {
        // Mockito simula un archivo subido que falla al leerse.
        MultipartFile ilegible = mock(MultipartFile.class);
        when(ilegible.isEmpty()).thenReturn(false);
        when(ilegible.getBytes()).thenThrow(new IOException("disco lleno"));

        Optional<CaracteristicasObjeto> resultado = reconocimiento.analizar(ilegible);

        assertTrue(resultado.isEmpty());
        assertTrue(claude.peticiones().isEmpty());
    }

    private static MockMultipartFile foto(byte[] datos) {
        return new MockMultipartFile("foto", "foto.png", "image/png", datos);
    }
}
