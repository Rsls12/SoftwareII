package pe.edu.ufound.pruebashu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lee el HTML de las pantallas. */
public final class HtmlHu {

    private static final Pattern ENLACE_OBJETO = Pattern.compile("href=\"/objetos/(\\d+)\"");
    private static final Pattern NOMBRE = Pattern.compile("class=\"ranking__nombre\"[^>]*>([^<]+)</span>");
    private static final Pattern DATO = Pattern.compile("class=\"ranking__dato\"[^>]*>([^<]+)</span>");

    private HtmlHu() {
    }

    public static String seccion(String html, String idTitulo) {
        int inicio = html.indexOf("id=\"" + idTitulo + "\"");
        if (inicio < 0) {
            throw new AssertionError("No existe la sección " + idTitulo);
        }
        int fin = html.indexOf("</section>", inicio);
        return html.substring(inicio, fin < 0 ? html.length() : fin);
    }

    public static String principal(String html) {
        int inicio = html.indexOf("<main");
        int fin = html.indexOf("</main>");
        return html.substring(inicio, fin);
    }

    public static List<Long> objetosEnlazados(String html) {
        List<Long> ids = new ArrayList<>();
        Matcher m = ENLACE_OBJETO.matcher(html);
        while (m.find()) {
            long id = Long.parseLong(m.group(1));
            if (ids.isEmpty() || ids.get(ids.size() - 1) != id) {
                ids.add(id);
            }
        }
        return ids;
    }

    public static Map<String, String> barras(String seccion) {
        List<String> nombres = grupos(NOMBRE, seccion);
        List<String> datos = grupos(DATO, seccion);
        if (nombres.size() != datos.size()) {
            throw new AssertionError("Filas incompletas: " + nombres + " / " + datos);
        }
        Map<String, String> filas = new LinkedHashMap<>();
        for (int i = 0; i < nombres.size(); i++) {
            filas.put(nombres.get(i).trim(), datos.get(i).trim());
        }
        return filas;
    }

    public static int contar(String texto, String buscado) {
        int veces = 0;
        for (int i = texto.indexOf(buscado); i >= 0; i = texto.indexOf(buscado, i + buscado.length())) {
            veces++;
        }
        return veces;
    }

    private static List<String> grupos(Pattern patron, String texto) {
        List<String> valores = new ArrayList<>();
        Matcher m = patron.matcher(texto);
        while (m.find()) {
            valores.add(m.group(1));
        }
        return valores;
    }
}
