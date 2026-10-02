package pe.edu.ufound.model;

/** Categorías de objeto (enum CategoriaObjeto del UML). */
public enum CategoriaObjeto {
    TECNOLOGIA("Tecnología"),
    DOCUMENTOS("Documentos"),
    ACCESORIOS("Accesorios"),
    ROPA("Ropa"),
    UTILES("Útiles"),
    BOTELLAS("Botellas"),
    OTROS("Otros");

    private final String etiqueta;

    CategoriaObjeto(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
