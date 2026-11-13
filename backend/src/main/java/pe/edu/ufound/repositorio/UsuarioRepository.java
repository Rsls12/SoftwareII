package pe.edu.ufound.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.ufound.model.RolUsuario;
import pe.edu.ufound.model.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    Optional<Usuario> findFirstByRolOrderByIdUsuarioAsc(RolUsuario rol);

    // HU-28: usuarios por rol.
    long countByRol(RolUsuario rol);
}
