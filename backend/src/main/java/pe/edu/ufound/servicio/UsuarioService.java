package pe.edu.ufound.servicio;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pe.edu.ufound.model.RolUsuario;
import pe.edu.ufound.model.Usuario;
import pe.edu.ufound.repositorio.UsuarioRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;

    public UsuarioService(UsuarioRepository repositorio) {
        this.repositorio = repositorio;
        if (repositorio.count() == 0) {
            repositorio.saveAll(List.of(
                    new Usuario("Ana", "Torres Ríos", "ana.torres@universidad.edu.pe", RolUsuario.ESTUDIANTE),
                    new Usuario("Jorge", "Quispe Mamani", "jorge.quispe@universidad.edu.pe", RolUsuario.SEGURIDAD),
                    new Usuario("Lucía", "Paredes Vega", "lucia.paredes@universidad.edu.pe", RolUsuario.OFICINA_OBJETOS_PERDIDOS)
            ));
        }
    }

    public Usuario usuarioActual() {
        Authentication sesion = SecurityContextHolder.getContext().getAuthentication();
        if (sesion == null) {
            throw new IllegalStateException();
        }
        return buscarPorCorreo(sesion.getName()).orElseThrow(IllegalStateException::new);
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        if (correo == null) {
            return Optional.empty();
        }
        return repositorio.findByCorreoIgnoreCase(correo.trim());
    }

    public Usuario buscarPorRol(RolUsuario rol) {
        return repositorio.findFirstByRolOrderByIdUsuarioAsc(rol).orElseThrow();
    }
}
