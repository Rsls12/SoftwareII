package pe.edu.ufound.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pe.edu.ufound.model.RolUsuario;
import pe.edu.ufound.model.Usuario;
import pe.edu.ufound.repositorio.UsuarioRepository;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static pe.edu.ufound.DatosDePrueba.CLAVE_ESTUDIANTE;
import static pe.edu.ufound.DatosDePrueba.CLAVE_OFICINA;
import static pe.edu.ufound.DatosDePrueba.CLAVE_SEGURIDAD;

/** Login con dos accesos (usuarios y Oficina) y creación de cuentas de estudiante. */
@SpringBootTest
@AutoConfigureMockMvc
class RegistroYAccesoTest {

    private static final String ESTUDIANTE = "ana.torres@universidad.edu.pe";
    private static final String SEGURIDAD = "jorge.quispe@universidad.edu.pe";
    private static final String OFICINA = "lucia.paredes@universidad.edu.pe";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PasswordEncoder cifrador;

    // ---------- Dos accesos ----------

    @Test
    void elLoginMuestraLosDosAccesosYElEnlaceParaCrearCuenta() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Acceso de usuarios")))
                .andExpect(content().string(containsString("Oficina de objetos perdidos")))
                .andExpect(content().string(containsString("/registro")));
        mvc.perform(get("/login").param("acceso", "oficina"))
                .andExpect(content().string(containsString("Acceso de la Oficina")))
                .andExpect(content().string(containsString("value=\"oficina\"")));
    }

    @Test
    void cadaAccesoSoloDejaEntrarASusRoles() throws Exception {
        ingresar(ESTUDIANTE, CLAVE_ESTUDIANTE, "usuario").andExpect(redirectedUrl("/")).andExpect(authenticated());
        ingresar(SEGURIDAD, CLAVE_SEGURIDAD, "usuario").andExpect(redirectedUrl("/")).andExpect(authenticated());
        ingresar(OFICINA, CLAVE_OFICINA, "oficina").andExpect(redirectedUrl("/")).andExpect(authenticated());

        ingresar(OFICINA, CLAVE_OFICINA, "usuario")
                .andExpect(redirectedUrl("/login?acceso=usuario&error=acceso")).andExpect(unauthenticated());
        ingresar(ESTUDIANTE, CLAVE_ESTUDIANTE, "oficina")
                .andExpect(redirectedUrl("/login?acceso=oficina&error=acceso")).andExpect(unauthenticated());
        ingresar(SEGURIDAD, CLAVE_SEGURIDAD, "oficina")
                .andExpect(redirectedUrl("/login?acceso=oficina&error=acceso")).andExpect(unauthenticated());
    }

    @Test
    void conClaveIncorrectaVuelveAlMismoAcceso() throws Exception {
        ingresar(OFICINA, "otra-clave", "oficina")
                .andExpect(redirectedUrl("/login?error&acceso=oficina")).andExpect(unauthenticated());
        ingresar(OFICINA, "otra-clave", "<script>")
                .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
    }

    // ---------- Crear cuenta de estudiante ----------

    @Test
    void unEstudianteCreaSuCuentaYLuegoIniciaSesion() throws Exception {
        mvc.perform(get("/registro")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Crear cuenta de estudiante")));

        registrar("Carla", "Ruiz Soto", "20231234@aloe.ulima.edu.pe", "clave-segura", "clave-segura")
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("mensaje", "Tu cuenta quedó creada. Ya puedes iniciar sesión."));

        Usuario carla = usuarios.findByCorreoIgnoreCase("20231234@aloe.ulima.edu.pe").orElseThrow();
        assertEquals(RolUsuario.ESTUDIANTE, carla.getRol());
        assertNotEquals("clave-segura", carla.getContrasena());
        assertTrue(cifrador.matches("clave-segura", carla.getContrasena()));
        ingresar("20231234@aloe.ulima.edu.pe", "clave-segura", "usuario").andExpect(authenticated());
    }

    @Test
    void unCorreoRepetidoNoCreaOtraCuenta() throws Exception {
        long antes = usuarios.count();

        registrar("Ana", "Torres", ESTUDIANTE.toUpperCase(), "clave-segura", "clave-segura")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Ya existe una cuenta con ese correo")));

        assertEquals(antes, usuarios.count());
    }

    @Test
    void conDatosInvalidosMuestraElErrorYConservaLoEscrito() throws Exception {
        registrar("Diego", "Lara", "dlara@aloe.ulima.edu.pe", "clave-segura", "otra-clave")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Las contraseñas no coinciden.")))
                .andExpect(content().string(containsString("value=\"Diego\"")));
        registrar("Diego", "Lara", "dlara@gmail.com", "clave-segura", "clave-segura")
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("correo institucional")));

        assertFalse(usuarios.existsByCorreoIgnoreCase("dlara@aloe.ulima.edu.pe"));
    }

    @Test
    void unaCuentaDesactivadaNoPuedeIniciarSesion() throws Exception {
        Usuario usuario = usuarios.save(Usuario.registrarEstudiante("Eva", "Paz", "epaz@aloe.ulima.edu.pe",
                "clave-segura", "clave-segura", cifrador::encode));
        usuario.desactivar();
        usuarios.save(usuario);

        ingresar("epaz@aloe.ulima.edu.pe", "clave-segura", "usuario")
                .andExpect(redirectedUrl("/login?error&acceso=usuario")).andExpect(unauthenticated());
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void quienYaInicioSesionNoVeElRegistro() throws Exception {
        mvc.perform(get("/registro")).andExpect(redirectedUrl("/"));
    }

    private ResultActions ingresar(String correo, String clave, String acceso) throws Exception {
        return mvc.perform(post("/login").param("username", correo).param("password", clave)
                .param("acceso", acceso).with(csrf()));
    }

    private ResultActions registrar(String nombres, String apellidos, String correo, String clave,
                                    String confirmacion) throws Exception {
        return mvc.perform(post("/registro").param("nombres", nombres).param("apellidos", apellidos)
                .param("correo", correo).param("contrasena", clave).param("confirmacion", confirmacion)
                .with(csrf()));
    }
}
