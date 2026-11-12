package pe.edu.ufound.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.edu.ufound.excepcion.DatosInvalidosException;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.function.UnaryOperator;

@Entity
@Table(name = "usuario")
public class Usuario {

    public static final int NOMBRE_MAXIMO = 100;
    public static final int CORREO_MAXIMO = 150;
    public static final int CONTRASENA_MINIMA = 8;
    // BCrypt solo usa los primeros 72 bytes; más largo se rechaza para no aceptar claves que no se verifican.
    public static final int CONTRASENA_MAXIMA_BYTES = 72;
    // shortcut: dominios fijos en el código; pasarlos a application.properties si la universidad agrega otros.
    public static final Set<String> DOMINIOS_PERMITIDOS = Set.of("aloe.ulima.edu.pe", "ulima.edu.pe",
            "universidad.edu.pe");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @Column(nullable = false, length = NOMBRE_MAXIMO)
    private String nombres;

    @Column(nullable = false, length = NOMBRE_MAXIMO)
    private String apellidos;

    @Column(nullable = false, unique = true, length = CORREO_MAXIMO)
    private String correo;

    // HU-01: hash BCrypt de la contraseña; el texto de la contraseña nunca se guarda.
    @Column(length = 100)
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private RolUsuario rol;

    @Column(nullable = false)
    private boolean activo = true;

    protected Usuario() {
    }

    public Usuario(String nombres, String apellidos, String correo, String contrasena, RolUsuario rol) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
        this.contrasena = contrasena;
        this.rol = rol;
    }

    // Registro público: siempre crea una cuenta de ESTUDIANTE; el personal no puede registrarse solo.
    public static Usuario registrarEstudiante(String nombres, String apellidos, String correo, String contrasena,
                                              String confirmacion, UnaryOperator<String> cifrar) {
        String nombresLimpios = texto(nombres, "nombres", NOMBRE_MAXIMO);
        String apellidosLimpios = texto(apellidos, "apellidos", NOMBRE_MAXIMO);
        String correoLimpio = validarCorreo(correo);
        validarContrasena(contrasena, confirmacion);
        return new Usuario(nombresLimpios, apellidosLimpios, correoLimpio, cifrar.apply(contrasena),
                RolUsuario.ESTUDIANTE);
    }

    private static String validarCorreo(String correo) {
        String limpio = texto(correo, "correo", CORREO_MAXIMO).toLowerCase();
        int arroba = limpio.indexOf('@');
        if (arroba < 1 || arroba != limpio.lastIndexOf('@') || limpio.contains(" ")
                || !DOMINIOS_PERMITIDOS.contains(limpio.substring(arroba + 1))) {
            throw new DatosInvalidosException("correo");
        }
        return limpio;
    }

    private static void validarContrasena(String contrasena, String confirmacion) {
        if (contrasena == null || contrasena.length() < CONTRASENA_MINIMA
                || contrasena.getBytes(StandardCharsets.UTF_8).length > CONTRASENA_MAXIMA_BYTES) {
            throw new DatosInvalidosException("contrasena");
        }
        if (!contrasena.equals(confirmacion)) {
            throw new DatosInvalidosException("confirmacion");
        }
    }

    private static String texto(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank() || valor.trim().length() > maximo) {
            throw new DatosInvalidosException(campo);
        }
        return valor.trim();
    }

    // HU-01: sin contraseña registrada no se puede iniciar sesión.
    public boolean tieneContrasena() {
        return contrasena != null && !contrasena.isBlank();
    }

    public boolean tieneRol(RolUsuario rol) {
        return this.rol == rol;
    }

    public boolean esEstudiante() {
        return tieneRol(RolUsuario.ESTUDIANTE);
    }

    public boolean esSeguridad() {
        return tieneRol(RolUsuario.SEGURIDAD);
    }

    public boolean esOficina() {
        return tieneRol(RolUsuario.OFICINA_OBJETOS_PERDIDOS);
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }

    public Long getIdUsuario() { return idUsuario; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getCorreo() { return correo; }
    public String getContrasena() { return contrasena; }
    public RolUsuario getRol() { return rol; }
    public boolean isActivo() { return activo; }
}
