package pe.edu.ufound.model;

/**
 * Usuario del sistema (clase Usuario del UML).
 * La contraseña no está aquí a propósito: el login lo maneja el backend con Spring Security.
 */
public class Usuario {

    private final Long idUsuario;
    private final String nombres;
    private final String apellidos;
    private final String correo;
    private final RolUsuario rol;

    public Usuario(Long idUsuario, String nombres, String apellidos, String correo, RolUsuario rol) {
        this.idUsuario = idUsuario;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
        this.rol = rol;
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

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }

    public Long getIdUsuario() { return idUsuario; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getCorreo() { return correo; }
    public RolUsuario getRol() { return rol; }
}
