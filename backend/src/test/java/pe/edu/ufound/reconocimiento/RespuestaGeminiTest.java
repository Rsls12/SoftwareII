package pe.edu.ufound.reconocimiento;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** HU-33: cómo se lee la respuesta de Gemini y cuándo no sirve para guardar características. */
class RespuestaGeminiTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void ignoraLasPartesDePensamientoDelModelo() {
        RespuestaGemini respuesta = leer("""
                {"candidates": [{"finishReason": "STOP", "content": {"parts": [
                  {"text": "Veo una mochila...", "thought": true},
                  {"text": "{\\"objetoIdentificable\\": true, \\"objeto\\": \\"Mochila\\", \\"colores\\": [\\"azul\\"]}"}]}}]}
                """);

        assertFalse(respuesta.tieneProblema());
        assertEquals("Mochila", respuesta.aCaracteristicas(LocalDateTime.now()).orElseThrow().getObjetoDetectado());
    }

    @Test
    void explicaPorQueNoHayCaracteristicas() {
        assertEquals("Gemini bloqueó la foto (SAFETY)",
                leer("{\"promptFeedback\": {\"blockReason\": \"SAFETY\"}}").getProblema());
        assertEquals("Gemini no devolvió ninguna respuesta", leer("{}").getProblema());
        assertEquals("la respuesta de Gemini terminó por MAX_TOKENS", leer("""
                {"candidates": [{"finishReason": "MAX_TOKENS", "content": {"parts": [{"text": "{\\"objeto"}]}}]}
                """).getProblema());
        assertEquals("la respuesta de Gemini no tenía el formato pedido", leer("""
                {"candidates": [{"finishReason": "STOP", "content": {"parts": [{"text": "[1, 2]"}]}}]}
                """).getProblema());
    }

    @Test
    void sinObjetoNoHayCaracteristicasAunqueDigaQueEsIdentificable() {
        RespuestaGemini respuesta = leer("""
                {"candidates": [{"finishReason": "STOP", "content": {"parts": [{"text":
                  "{\\"objetoIdentificable\\": true, \\"objeto\\": \\"  \\", \\"colores\\": [\\"rojo\\"]}"}]}}]}
                """);

        assertFalse(respuesta.tieneProblema());
        assertTrue(respuesta.aCaracteristicas(LocalDateTime.now()).isEmpty());
    }

    private static RespuestaGemini leer(String json) {
        return new RespuestaGemini(JSON.readTree(json));
    }
}
