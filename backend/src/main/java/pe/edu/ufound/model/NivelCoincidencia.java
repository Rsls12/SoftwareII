package pe.edu.ufound.model;

/**
 * HU-24: qué tan prioritaria es una coincidencia para la Oficina, según su puntaje (enum NivelCoincidencia del UML).
 * claseCss elige el color de la insignia en app.css (.nivel--alto, etc.), igual que EstadoObjeto.claseCss.
 */
public enum NivelCoincidencia {
    BAJO("Baja", "bajo"),
    MEDIO("Media", "medio"),
    ALTO("Alta", "alto");

    private final String etiqueta;
    private final String claseCss;

    NivelCoincidencia(String etiqueta, String claseCss) {
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
