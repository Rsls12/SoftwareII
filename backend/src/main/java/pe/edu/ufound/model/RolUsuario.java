package pe.edu.ufound.model;

/** Roles del sistema (enum RolUsuario del UML). */
public enum RolUsuario {
    ESTUDIANTE("Estudiante"),
    SEGURIDAD("Seguridad"),
    OFICINA_OBJETOS_PERDIDOS("Oficina de objetos perdidos");

    private final String etiqueta;

    RolUsuario(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
