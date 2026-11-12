package pe.edu.ufound.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import pe.edu.ufound.excepcion.DatosInvalidosException;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HU-33: lo que el análisis con IA reconoció en la foto de un objeto: qué es, sus colores, marca, material
 * y rasgos distintivos. Se guarda dentro del objeto (columnas ia_* de la tabla objeto) con
 * Objeto.registrarCaracteristicas.
 */
@Embeddable
public class CaracteristicasObjeto {

    public static final int OBJETO_MAXIMO = 80;
    public static final int COLORES_MAXIMO = 120;
    public static final int MARCA_MAXIMA = 60;
    public static final int MATERIAL_MAXIMO = 60;
    public static final int RASGOS_MAXIMO = 300;

    @Column(name = "ia_objeto", length = OBJETO_MAXIMO)
    private String objetoDetectado;

    @Column(name = "ia_colores", length = COLORES_MAXIMO)
    private String colores;

    @Column(name = "ia_marca", length = MARCA_MAXIMA)
    private String marca;

    @Column(name = "ia_material", length = MATERIAL_MAXIMO)
    private String material;

    @Column(name = "ia_rasgos", length = RASGOS_MAXIMO)
    private String rasgosDistintivos;

    @Column(name = "ia_fecha_hora_analisis")
    private LocalDateTime fechaHoraAnalisis;

    protected CaracteristicasObjeto() {
    }

    // Lo único obligatorio es saber qué objeto es. Los textos vacíos no se guardan y los que no caben se recortan:
    // la respuesta de la IA no se puede corregir a mano, así que se ajusta en vez de perder el análisis.
    public CaracteristicasObjeto(String objetoDetectado, List<String> colores, String marca, String material,
                                 List<String> rasgosDistintivos, LocalDateTime fechaHoraAnalisis) {
        String objeto = limpiar(objetoDetectado, OBJETO_MAXIMO);
        if (objeto == null) {
            throw new DatosInvalidosException("objetoDetectado");
        }
        if (fechaHoraAnalisis == null) {
            throw new DatosInvalidosException("fechaHoraAnalisis");
        }
        this.objetoDetectado = conMayuscula(objeto);
        this.colores = limpiar(unir(colores, ", "), COLORES_MAXIMO);
        this.marca = limpiar(marca, MARCA_MAXIMA);
        this.material = limpiar(material, MATERIAL_MAXIMO);
        this.rasgosDistintivos = limpiar(unir(rasgosDistintivos, "; "), RASGOS_MAXIMO);
        this.fechaHoraAnalisis = fechaHoraAnalisis;
    }

    // ---------- HU-33 + HU-35: valores que se sugieren en el formulario de registro ----------

    /** Ej.: "Mouse inalámbrico Logitech negro". Usa la misma regla que el autocompletado (Objeto.sugerirNombre). */
    public String sugerirNombre() {
        String nombre = Objeto.sugerirNombre(objetoDetectado, marca, primerColor());
        return nombre.length() <= Objeto.NOMBRE_MAXIMO ? nombre : nombre.substring(0, Objeto.NOMBRE_MAXIMO).trim();
    }

    /** Ej.: "Lentes, de color negro, rosado, de plástico. Montura rectangular de marco grueso." */
    public String sugerirDescripcion() {
        StringBuilder descripcion = new StringBuilder(objetoDetectado);
        if (marca != null) {
            descripcion.append(" marca ").append(marca);
        }
        if (colores != null) {
            descripcion.append(", de color ").append(colores.toLowerCase());
        }
        if (material != null) {
            descripcion.append(", de ").append(material.toLowerCase());
        }
        descripcion.append(".");
        if (rasgosDistintivos != null) {
            descripcion.append(" ").append(conMayuscula(sinPuntoFinal(rasgosDistintivos))).append(".");
        }
        if (descripcion.length() < Objeto.DESCRIPCION_MINIMA) {
            descripcion.append(" Reconocido en la foto.");
        }
        return limpiar(descripcion.toString(), Objeto.DESCRIPCION_MAXIMA);
    }

    /** La categoría según qué objeto es, con la misma regla que el autocompletado (Objeto.sugerirCategoria). */
    public CategoriaObjeto sugerirCategoria() {
        // La regla compara palabras sin tildes ("audifono"): se le pasa el nombre sin tildes.
        String sinTildes = Normalizer.normalize(objetoDetectado, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return Objeto.sugerirCategoria(sinTildes);
    }

    private String primerColor() {
        if (colores == null) {
            return null;
        }
        int coma = colores.indexOf(',');
        return coma < 0 ? colores : colores.substring(0, coma);
    }

    // Algunos modelos responden todo en minúsculas ("lentes"): el nombre y cada oración empiezan con mayúscula.
    private static String conMayuscula(String texto) {
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }

    private static String sinPuntoFinal(String texto) {
        return texto.endsWith(".") ? texto.substring(0, texto.length() - 1) : texto;
    }

    public boolean tieneColores() { return colores != null; }
    public boolean tieneMarca() { return marca != null; }
    public boolean tieneMaterial() { return material != null; }
    public boolean tieneRasgosDistintivos() { return rasgosDistintivos != null; }

    public String getObjetoDetectado() { return objetoDetectado; }
    public String getColores() { return colores; }
    public String getMarca() { return marca; }
    public String getMaterial() { return material; }
    public String getRasgosDistintivos() { return rasgosDistintivos; }
    public LocalDateTime getFechaHoraAnalisis() { return fechaHoraAnalisis; }

    private static String unir(List<String> partes, String separador) {
        if (partes == null) {
            return null;
        }
        StringBuilder texto = new StringBuilder();
        for (String parte : partes) {
            if (parte != null && !parte.isBlank()) {
                if (!texto.isEmpty()) {
                    texto.append(separador);
                }
                texto.append(parte.trim());
            }
        }
        return texto.toString();
    }

    private static String limpiar(String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo - 1).trim() + "…";
    }
}
