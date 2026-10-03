package pe.edu.ufound.model;

/** Si el objeto fue perdido (lo reporta un estudiante) o encontrado (lo registra seguridad). */
public enum TipoObjeto {
    PERDIDO("Perdido"),
    ENCONTRADO("Encontrado");

    private final String etiqueta;

    TipoObjeto(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
