package pe.edu.ufound.model;

/** Ubicación del objeto: solo se registra el pabellón (enum Pabellon del UML). */
public enum Pabellon {
    PABELLON_A("Pabellón A"),
    PABELLON_B("Pabellón B"),
    PABELLON_C("Pabellón C"),
    PABELLON_D("Pabellón D"),
    PABELLON_E("Pabellón E"),
    OTRO("Otro lugar");

    private final String etiqueta;

    Pabellon(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
