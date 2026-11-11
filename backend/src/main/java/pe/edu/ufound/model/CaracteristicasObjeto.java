package pe.edu.ufound.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import pe.edu.ufound.excepcion.DatosInvalidosException;

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
        this.objetoDetectado = objeto;
        this.colores = limpiar(unir(colores, ", "), COLORES_MAXIMO);
        this.marca = limpiar(marca, MARCA_MAXIMA);
        this.material = limpiar(material, MATERIAL_MAXIMO);
        this.rasgosDistintivos = limpiar(unir(rasgosDistintivos, "; "), RASGOS_MAXIMO);
        this.fechaHoraAnalisis = fechaHoraAnalisis;
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
