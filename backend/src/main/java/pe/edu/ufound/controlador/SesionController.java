package pe.edu.ufound.controlador;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Pantalla de inicio de sesión (HU-01). Solo la muestra: el POST /login y el POST /logout
 * los procesa Spring Security (ver SeguridadConfig).
 */
@Controller
public class SesionController {

    @GetMapping("/login")
    public String login(Authentication sesion) {
        // Quien ya inició sesión no necesita ver el formulario otra vez.
        return sesion != null && sesion.isAuthenticated() ? "redirect:/" : "login";
    }
}
