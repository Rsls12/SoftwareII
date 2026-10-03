package pe.edu.ufound.servicio;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pe.edu.ufound.model.RolUsuario;
import pe.edu.ufound.model.Usuario;

import java.util.List;
import java.util.Optional;

/**
 * MOCK: los usuarios están en una lista en memoria.
 * El backend mantiene los mismos métodos y cambia la lista por un repositorio JPA.
 */
@Service
public class UsuarioService {

    private final List<Usuario> usuarios = List.of(
            new Usuario(1L, "Ana", "Torres Ríos", "ana.torres@universidad.edu.pe", RolUsuario.ESTUDIANTE),
            new Usuario(2L, "Jorge", "Quispe Mamani", "jorge.quispe@universidad.edu.pe", RolUsuario.SEGURIDAD),
            new Usuario(3L, "Lucía", "Paredes Vega", "lucia.paredes@universidad.edu.pe", RolUsuario.OFICINA_OBJETOS_PERDIDOS)
    );

    /** El usuario que inició sesión (HU-01). Cada sesión del navegador tiene el suyo. */
    public Usuario usuarioActual() {
        Authentication sesion = SecurityContextHolder.getContext().getAuthentication();
        if (sesion == null) {
            throw new IllegalStateException("No hay una sesión iniciada.");
        }
        return buscarPorCorreo(sesion.getName())
                .orElseThrow(() -> new IllegalStateException("La sesión no corresponde a un usuario del sistema."));
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        if (correo == null) {
            return Optional.empty();
        }
        String buscado = correo.trim();
        return usuarios.stream().filter(u -> u.getCorreo().equalsIgnoreCase(buscado)).findFirst();
    }

    public Usuario buscarPorRol(RolUsuario rol) {
        return usuarios.stream().filter(u -> u.tieneRol(rol)).findFirst().orElseThrow();
    }
}
