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

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 150)
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
