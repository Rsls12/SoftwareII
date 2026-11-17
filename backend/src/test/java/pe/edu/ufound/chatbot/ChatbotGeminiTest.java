package pe.edu.ufound.chatbot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.ufound.reconocimiento.GeminiFalso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.edu.ufound.chatbot.ChatbotGemini.RESPUESTA_FIJA;

/**
 * HU-29 y HU-30, caja blanca de ChatbotGemini.responder(): un caso por cada camino independiente
 * del grafo de flujo (V(G) = 10, casos CB-01 a CB-10). Gemini se reemplaza por un servidor local.
 */
class ChatbotGeminiTest {

    static final String MODELO = "gemini-3.1-flash-lite";
    static final String VACIA = "Escribe tu pregunta y te ayudo.";

    GeminiFalso gemini;
    ChatbotGemini chatbot;

    @BeforeEach
    void iniciar() {
        gemini = new GeminiFalso();
        chatbot = new ChatbotGemini("clave-de-prueba", MODELO, gemini.url() + "/v1beta/models/");
    }

    @AfterEach
    void cerrar() {
        gemini.cerrar();
    }

    @Test
    @DisplayName("CB-01 (1-3-17): pregunta nula")
    void preguntaNula() {
        assertEquals(VACIA, chatbot.responder(null));
        assertTrue(gemini.peticiones().isEmpty());
    }

    @Test
    @DisplayName("CB-02 (1-2-3-17): pregunta con solo espacios")
    void preguntaEnBlanco() {
        assertEquals(VACIA, chatbot.responder("    "));
        assertTrue(gemini.peticiones().isEmpty());
    }

    @Test
    @DisplayName("CB-03 (1-2-4-5-17): pregunta de 301 caracteres")
    void preguntaMuyLarga() {
        String respuesta = chatbot.responder("a".repeat(ChatbotGemini.PREGUNTA_MAXIMA + 1));

        assertEquals("Tu pregunta es muy larga. Escríbela en menos de 300 caracteres.", respuesta);
        assertTrue(gemini.peticiones().isEmpty());
    }

    @Test
    @DisplayName("CB-04 (1-2-4-6-7-17): pregunta válida y sin clave de la API")
    void sinClaveDeLaApi() {
        ChatbotGemini sinClave = new ChatbotGemini("", MODELO, gemini.url());

        assertEquals(RESPUESTA_FIJA, sinClave.responder("¿Dónde queda la Oficina?"));
        assertTrue(gemini.peticiones().isEmpty());
    }

    @Test
    @DisplayName("CB-05 (1-2-4-6-8-16-17): el hilo se interrumpe durante la llamada")
    void hiloInterrumpido() {
        Thread.currentThread().interrupt();

        String respuesta = chatbot.responder("¿Dónde queda la Oficina?");

        // Thread.interrupted() también limpia la marca para no afectar a las demás pruebas.
        assertTrue(Thread.interrupted(), "responder() debe volver a marcar el hilo como interrumpido");
        assertEquals(RESPUESTA_FIJA, respuesta);
    }

    @Test
    @DisplayName("CB-06 (1-2-4-6-8-15-17): falla la conexión con Gemini")
    void fallaLaConexion() {
        String direccion = gemini.url();
        gemini.cerrar();
        ChatbotGemini sinServidor = new ChatbotGemini("clave-de-prueba", MODELO, direccion);

        assertEquals(RESPUESTA_FIJA, sinServidor.responder("¿Dónde queda la Oficina?"));
    }

    @Test
    @DisplayName("CB-07 (1-2-4-6-8-9-10-17): Gemini responde 429")
    void geminiRespondeError() {
        gemini.responderError(429);

        assertEquals(RESPUESTA_FIJA, chatbot.responder("¿Dónde queda la Oficina?"));
        assertEquals(1, gemini.peticiones().size());
    }

    @Test
    @DisplayName("CB-08 (1-2-4-6-8-9-11-15-17): Gemini responde 200 con un JSON inválido")
    void geminiRespondeJsonInvalido() {
        gemini.responder("esto no es JSON");

        assertEquals(RESPUESTA_FIJA, chatbot.responder("¿Dónde queda la Oficina?"));
    }

    @Test
    @DisplayName("CB-09 (1-2-4-6-8-9-11-12-13-17): Gemini responde 200 sin texto")
    void geminiRespondeSinTexto() {
        gemini.responder("{\"candidates\": []}");

        assertEquals(RESPUESTA_FIJA, chatbot.responder("¿Dónde queda la Oficina?"));
    }

    @Test
    @DisplayName("CB-10 (1-2-4-6-8-9-11-12-14-17): Gemini responde 200 con texto")
    void geminiRespondeConTexto() {
        gemini.responderTexto("La Oficina está en el Pabellón A1, piso 2.");

        String respuesta = chatbot.responder("¿Dónde queda la Oficina de objetos perdidos?");

        assertEquals("La Oficina está en el Pabellón A1, piso 2.", respuesta);
        assertEquals("/v1beta/models/" + MODELO + ":generateContent", gemini.peticiones().get(0).ruta());
        assertEquals("clave-de-prueba", gemini.peticiones().get(0).apiKey());
    }
}
