package pe.edu.ufound.chatbot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Prueba unitaria de ChatbotGemini.leerTexto(): saca el texto de la respuesta JSON de Gemini (HU-29 y HU-30). */
class LeerTextoTest {

    final JsonMapper json = JsonMapper.builder().build();

    String leer(String respuesta) {
        return ChatbotGemini.leerTexto(json.readTree(respuesta));
    }

    @Test
    @DisplayName("PU-01: una parte con texto -> devuelve ese texto")
    void unaParte() {
        assertEquals("La Oficina está en el Pabellón A1.", leer("""
                {"candidates": [{"content": {"parts": [{"text": "La Oficina está en el Pabellón A1."}]}}]}
                """));
    }

    @Test
    @DisplayName("PU-02: varias partes -> las une en un solo texto")
    void variasPartes() {
        assertEquals("Repórtalo en la web y revisa Buscar.", leer("""
            {"candidates": [{"content": {"parts": [{"text": "Repórtalo en la web "},
                                                    {"text": "y revisa Buscar."}]}}]}
            """));
    }

    @Test
    @DisplayName("PU-03: texto con espacios y saltos de línea al borde -> lo devuelve recortado")
    void recortaEspacios() {
        assertEquals("Hola", leer("""
                {"candidates": [{"content": {"parts": [{"text": "  Hola\\n"}]}}]}
                """));
    }

    @Test
    @DisplayName("PU-04: sin candidatos -> devuelve texto vacío")
    void sinCandidatos() {
        assertEquals("", leer("{\"candidates\": []}"));
    }

    @Test
    @DisplayName("PU-05: una parte sin el campo text -> devuelve texto vacío")
    void parteSinTexto() {
        assertEquals("", leer("""
                {"candidates": [{"content": {"parts": [{"inlineData": {"mimeType": "image/png"}}]}}]}
                """));
    }

    @Test
    @DisplayName("PU-06: respuesta de error sin candidates -> devuelve texto vacío")
    void respuestaDeError() {
        assertEquals("", leer("{\"error\": {\"code\": 429, \"message\": \"Quota exceeded\"}}"));
    }
}
