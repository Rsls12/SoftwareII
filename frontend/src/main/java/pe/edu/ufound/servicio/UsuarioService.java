package pe.edu.ufound.servicio;

import org.springframework.stereotype.Service;
import pe.edu.ufound.model.RolUsuario;
import pe.edu.ufound.model.Usuario;

import java.util.List;

/**
 * MOCK (Nivel 1, solo frontend).
 * El backend reemplaza esta clase: el usuario actual lo dará Spring Security tras el login.
 */
@Service
public class UsuarioService {

    private final List<Usuario> usuarios = List.of(
            new Usuario(1L, "Ana", "Torres Ríos", "ana.torres@universidad.edu.pe", RolUsuario.ESTUDIANTE),
            new Usuario(2L, "Jorge", "Quispe Mamani", "jorge.quispe@universidad.edu.pe", RolUsuario.SEGURIDAD),
            new Usuario(3L, "Lucía", "Paredes Vega", "lucia.paredes@universidad.edu.pe", RolUsuario.OFICINA_OBJETOS_PERDIDOS)
    );

    // Nota: un solo usuario para toda la app (sin sesiones); Spring Security lo resuelve por sesión.
    private Usuario usuarioActual = usuarios.get(0);

    public Usuario usuarioActual() {
        return usuarioActual;
    }

    /** Solo para revisar la vista de cada rol durante el Nivel 1. */
    public void usarRol(RolUsuario rol) {
        usuarioActual = buscarPorRol(rol);
    }

    public Usuario buscarPorRol(RolUsuario rol) {
        return usuarios.stream().filter(u -> u.tieneRol(rol)).findFirst().orElseThrow();
    }
}
