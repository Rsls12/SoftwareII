package pe.edu.ufound.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** HU-36: las coincidencias se ordenan por nivel de similitud, de la más relevante a la menos. */
class PriorizarCoincidenciasTest {

    private static final LocalDateTime AHORA = LocalDateTime.now();
    private static final Usuario ANA = new Usuario("Ana", "Torres", "ana@universidad.edu.pe", null, RolUsuario.ESTUDIANTE);
    private static final Usuario JORGE = new Usuario("Jorge", "Quispe", "jorge@universidad.edu.pe", null, RolUsuario.SEGURIDAD);
    private static final Objeto PERDIDO = objeto("Mochila negra", TipoObjeto.PERDIDO, ANA);

    @Test
    void lasDeMayorNivelDeSimilitudVanPrimero() {
        Coincidencia baja = coincidencia("Mochila azul", 62, AHORA);
        Coincidencia alta = coincidencia("Mochila negra Totto", 95, AHORA);
        Coincidencia media = coincidencia("Mochila gris", 80, AHORA);

        List<Coincidencia> priorizadas = Coincidencia.priorizar(List.of(baja, alta, media));

        assertEquals(List.of(alta, media, baja), priorizadas);
        assertEquals(NivelCoincidencia.ALTO, priorizadas.get(0).getNivelCoincidencia());
        assertEquals(NivelCoincidencia.MEDIO, priorizadas.get(1).getNivelCoincidencia());
        assertEquals(NivelCoincidencia.BAJO, priorizadas.get(2).getNivelCoincidencia());
    }

    @Test
    void dentroDelMismoNivelVaPrimeroLaDeMayorPuntaje() {
        Coincidencia media76 = coincidencia("Mochila gris", 76, AHORA);
        Coincidencia media88 = coincidencia("Mochila negra", 88, AHORA);

        assertEquals(List.of(media88, media76), Coincidencia.priorizar(List.of(media76, media88)));
    }

    @Test
    void aIgualPuntajeVaPrimeroLaDetectadaMasRecientemente() {
        Coincidencia antigua = coincidencia("Mochila gris", 80, AHORA.minusHours(5));
        Coincidencia reciente = coincidencia("Mochila negra", 80, AHORA);

        assertEquals(List.of(reciente, antigua), Coincidencia.priorizar(List.of(antigua, reciente)));
        assertTrue(reciente.compareTo(antigua) < 0);
        assertTrue(antigua.compareTo(reciente) > 0);
        assertEquals(0, reciente.compareTo(reciente));
    }

    @Test
    void noCambiaLaListaQueRecibe() {
        Coincidencia baja = coincidencia("Mochila azul", 62, AHORA);
        Coincidencia alta = coincidencia("Mochila negra Totto", 95, AHORA);
        List<Coincidencia> original = new java.util.ArrayList<>(List.of(baja, alta));

        Coincidencia.priorizar(original);

        assertEquals(List.of(baja, alta), original);
    }

    @Test
    void sinCoincidenciasOConUnaSolaNoHayNadaQueOrdenar() {
        Coincidencia unica = coincidencia("Mochila negra", 70, AHORA);

        assertTrue(Coincidencia.priorizar(List.of()).isEmpty());
        assertEquals(List.of(unica), Coincidencia.priorizar(List.of(unica)));
    }

    private static Coincidencia coincidencia(String encontrado, int puntaje, LocalDateTime deteccion) {
        return new Coincidencia(PERDIDO, objeto(encontrado, TipoObjeto.ENCONTRADO, JORGE), puntaje, deteccion);
    }

    private static Objeto objeto(String nombre, TipoObjeto tipo, Usuario autor) {
        return new Objeto(nombre, "Descripción de prueba para la HU-36", tipo, CategoriaObjeto.ACCESORIOS,
                Pabellon.PABELLON_A, null, AHORA, autor);
    }
}
