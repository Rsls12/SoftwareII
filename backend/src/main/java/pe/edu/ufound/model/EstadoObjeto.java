package pe.edu.ufound.model;

/**
 * Estados por los que pasa un objeto, en orden (enum EstadoObjeto del UML).
 * claseCss elige el color de la insignia en app.css (.estado--reportado, etc.).
 */
public enum EstadoObjeto {
    REPORTADO("Reportado", "reportado"),
    EN_REVISION("En revisión", "revision"),
    COINCIDENCIA_DETECTADA("Coincidencia", "coincidencia"),
    DISPONIBLE_EN_OFICINA("En oficina", "oficina"),
    ENTREGADO("Entregado", "entregado"),
    ARCHIVADO("Archivado", "archivado");

    private final String etiqueta;
    private final String claseCss;

    EstadoObjeto(String etiqueta, String claseCss) {
        this.etiqueta = etiqueta;
        this.claseCss = claseCss;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getClaseCss() {
        return claseCss;
    }
}
