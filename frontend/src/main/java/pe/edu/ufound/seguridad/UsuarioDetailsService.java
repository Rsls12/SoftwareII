package pe.edu.ufound.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pe.edu.ufound.model.Usuario;
import pe.edu.ufound.servicio.UsuarioService;

/**
 * Le presenta a Spring Security los usuarios de U-FOUND (HU-01): el correo es el nombre
 * de usuario y el rol del modelo pasa a ser el rol de la sesión.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioService usuarios;
    private final String claveCifrada;

    public UsuarioDetailsService(UsuarioService usuarios, PasswordEncoder cifrador,
                                 @Value("${ufound.mock.clave}") String claveDeEjemplo) {
        this.usuarios = usuarios;
        // MOCK: los usuarios de ejemplo comparten una clave. Con la BD, cada usuario trae su propio hash.
        this.claveCifrada = cifrador.encode(claveDeEjemplo);
    }

    @Override
    public UserDetails loadUserByUsername(String correo) {
        Usuario usuario = usuarios.buscarPorCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("No existe un usuario con ese correo"));
        return User.withUsername(usuario.getCorreo())
                .password(claveCifrada)
                .roles(usuario.getRol().name())
                .build();
    }
}
