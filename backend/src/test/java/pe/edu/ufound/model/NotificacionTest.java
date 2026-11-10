package pe.edu.ufound.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** HU-25: solo las coincidencias relevantes avisan al dueño del objeto perdido. */
class NotificacionTest {

    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 10, 10, 0);

    private final Usuario ana = new Usuario("Ana", "Torres", "ana@u.pe", null, RolUsuario.ESTUDIANTE);
    private final Usuario jorge = new Usuario("Jorge", "Quispe", "jorge@u.pe", null, RolUsuario.SEGURIDAD);

    @Test
    void unaCoincidenciaRelevanteAvisaAlEstudianteQuePerdioElObjeto() {
        Objeto perdida = new Objeto("Laptop Lenovo gris", "Laptop Lenovo con sticker de la NASA", TipoObjeto.PERDIDO,
                CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_C, null, HOY, ana);
        Objeto encontrada = new Objeto("Laptop gris", "Lenovo gris con sticker NASA", TipoObjeto.ENCONTRADO,
                CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_C, null, HOY.plusHours(2), jorge);

        List<Notificacion> avisos = Notificacion.de(encontrada.detectarCoincidencias(List.of(perdida)));

        assertEquals(1, avisos.size());
        assertSame(ana, avisos.get(0).getDestinatario());
        assertTrue(avisos.get(0).getMensaje().contains("\"Laptop Lenovo gris\""));
        assertTrue(avisos.get(0).getMensaje().length() <= Notificacion.MENSAJE_MAXIMO);
        assertFalse(avisos.get(0).isLeida());
        assertNotNull(avisos.get(0).getFechaHora());
    }

    @Test
    void unaCoincidenciaPosiblePeroNoRelevanteNoAvisa() {
        Objeto perdido = new Objeto("Cuaderno de Cálculo", "Cuaderno anillado de cálculo", TipoObjeto.PERDIDO,
                CategoriaObjeto.UTILES, Pabellon.PABELLON_A, null, HOY, ana);
        Objeto encontrado = new Objeto("Cuaderno calculo", "Cuaderno anillado", TipoObjeto.ENCONTRADO,
                CategoriaObjeto.UTILES, Pabellon.PABELLON_E, null, HOY.plusDays(10), jorge);

        List<Coincidencia> detectadas = encontrado.detectarCoincidencias(List.of(perdido));

        assertEquals(1, detectadas.size());
        assertFalse(detectadas.get(0).esRelevante());
        assertTrue(Notificacion.de(detectadas).isEmpty());
    }
}
