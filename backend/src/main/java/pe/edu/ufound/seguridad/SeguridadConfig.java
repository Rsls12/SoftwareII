package pe.edu.ufound.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import pe.edu.ufound.model.RolUsuario;
import pe.edu.ufound.model.Usuario;
import pe.edu.ufound.repositorio.UsuarioRepository;

/**
 * Reglas de acceso del Sprint 1: inicio de sesión (HU-01), cierre de sesión (HU-02)
 * y qué rutas puede abrir cada rol (HU-03).
 */
@Configuration
public class SeguridadConfig {

    @Bean
    public SecurityFilterChain reglasDeAcceso(HttpSecurity http) throws Exception {
        String estudiante = RolUsuario.ESTUDIANTE.name();
        String seguridad = RolUsuario.SEGURIDAD.name();
        String oficina = RolUsuario.OFICINA_OBJETOS_PERDIDOS.name();

        return http
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/login", "/css/**").permitAll()
                        // Las mismas reglas con las que la vista muestra u oculta cada opción.
                        .requestMatchers("/entregas").hasRole(oficina)
                        .requestMatchers("/mis-publicaciones").hasRole(estudiante)
                        .requestMatchers(HttpMethod.POST, "/objetos/*/estado").hasRole(oficina)
                        .requestMatchers("/objetos/nuevo").hasAnyRole(estudiante, seguridad)
                        .requestMatchers(HttpMethod.POST, "/objetos").hasAnyRole(estudiante, seguridad)
                        .anyRequest().authenticated())
                // HU-01: siempre lleva al inicio, que se arma según el rol (HU-03).
                .formLogin(login -> login.loginPage("/login").defaultSuccessUrl("/", true))
                // HU-02: POST /logout cierra la sesión y vuelve al login.
                .logout(salida -> salida.logoutSuccessUrl("/login?logout"))
                .build();
    }

    @Bean
    public PasswordEncoder cifrador() {
        return new BCryptPasswordEncoder();
    }

    // HU-01: el correo es el nombre de usuario, la contraseña es el hash BCrypt de la tabla usuario
    // y el rol del modelo pasa a ser el rol de la sesión.
    @Bean
    public UserDetailsService usuariosDeLaSesion(UsuarioRepository usuarios) {
        return correo -> usuarios.findByCorreoIgnoreCase(correo.trim())
                .filter(Usuario::tieneContrasena)
                .map(usuario -> User.withUsername(usuario.getCorreo())
                        .password(usuario.getContrasena())
                        .roles(usuario.getRol().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No existe un usuario con ese correo"));
    }
}
