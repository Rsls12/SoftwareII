package pe.edu.ufound.model;

import org.junit.jupiter.api.Test;
import pe.edu.ufound.excepcion.DatosInvalidosException;

import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/** Reglas para crear una cuenta de estudiante desde el login. */
class UsuarioTest {

    private static final UnaryOperator<String> CIFRAR = clave -> "hash:" + clave;

    @Test
    void elRegistroCreaUnEstudianteConLaContrasenaCifrada() {
        Usuario nuevo = Usuario.registrarEstudiante("  Carla ", " Ruiz Soto ", " 20231234@ALOE.ulima.edu.pe ",
                "clave-segura", "clave-segura", CIFRAR);

        assertEquals(RolUsuario.ESTUDIANTE, nuevo.getRol());
        assertEquals("Carla Ruiz Soto", nuevo.getNombreCompleto());
        assertEquals("20231234@aloe.ulima.edu.pe", nuevo.getCorreo());
        assertEquals("hash:clave-segura", nuevo.getContrasena());
        assertTrue(nuevo.isActivo());
    }

    @Test
    void soloAceptaCorreosInstitucionales() {
        for (String correo : new String[]{"carla@gmail.com", "carla@aloe.ulima.edu.pe.com", "@ulima.edu.pe",
                "carla@@ulima.edu.pe", "car la@ulima.edu.pe", ""}) {
            assertEquals("correo", campoInvalido("Carla", "Ruiz", correo, "clave-segura", "clave-segura"), correo);
        }
        assertDoesNotThrow(() -> Usuario.registrarEstudiante("Carla", "Ruiz", "cruiz@ulima.edu.pe",
                "clave-segura", "clave-segura", CIFRAR));
    }

    @Test
    void validaLaContrasenaYSuConfirmacion() {
        assertEquals("contrasena", campoInvalido("Carla", "Ruiz", "c@ulima.edu.pe", "1234567", "1234567"));
        assertEquals("contrasena", campoInvalido("Carla", "Ruiz", "c@ulima.edu.pe", "ñ".repeat(37), "ñ".repeat(37)));
        assertEquals("confirmacion", campoInvalido("Carla", "Ruiz", "c@ulima.edu.pe", "clave-segura", "clave-segur"));
        assertDoesNotThrow(() -> Usuario.registrarEstudiante("Carla", "Ruiz", "c@ulima.edu.pe",
                "12345678", "12345678", CIFRAR));
    }

    @Test
    void nombresYApellidosSonObligatorios() {
        assertEquals("nombres", campoInvalido("  ", "Ruiz", "c@ulima.edu.pe", "clave-segura", "clave-segura"));
        assertEquals("apellidos", campoInvalido("Carla", "x".repeat(101), "c@ulima.edu.pe", "clave-segura", "clave-segura"));
    }

    private static String campoInvalido(String nombres, String apellidos, String correo, String clave, String confirmacion) {
        return assertThrows(DatosInvalidosException.class,
                () -> Usuario.registrarEstudiante(nombres, apellidos, correo, clave, confirmacion, CIFRAR)).getCampo();
    }
}
