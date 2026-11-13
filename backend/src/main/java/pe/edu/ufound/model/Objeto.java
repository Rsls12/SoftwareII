package pe.edu.ufound.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.edu.ufound.excepcion.DatosInvalidosException;
import pe.edu.ufound.excepcion.OperacionNoPermitidaException;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "objeto")
public class Objeto {

    public static final int NOMBRE_MAXIMO = 80;
    public static final int DESCRIPCION_MINIMA = 10;
    public static final int DESCRIPCION_MAXIMA = 500;
    public static final int IMAGEN_URL_MAXIMA = 255;
    public static final int DIAS_MAXIMOS_COINCIDENCIA = 30;
    public static final int DIAS_SIN_MOVIMIENTO = 7;
    // Palabras que aparecen en casi cualquier descripción y no ayudan a distinguir un objeto de otro.
    private static final Set<String> PALABRAS_COMUNES = Set.of("para", "esta", "este", "tiene", "color", "como",
            "sobre", "desde", "entre", "cerca", "donde", "pero", "creo", "deje", "olvide", "perdi");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idObjeto;

    @Column(nullable = false, length = NOMBRE_MAXIMO)
    private String nombre;

    @Column(nullable = false, length = DESCRIPCION_MAXIMA)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoObjeto tipo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private CategoriaObjeto categoria;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Pabellon ubicacion;

    @Column(length = IMAGEN_URL_MAXIMA)
    private String imagenUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaHoraReporte;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private EstadoObjeto estado = EstadoObjeto.REPORTADO;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_reporta", nullable = false, updatable = false)
    private Usuario reportadoPor;

    // HU-15: cambios de estado del objeto, en el orden en que ocurrieron.
    @OneToMany(mappedBy = "objeto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("fechaHora ASC, idHistorial ASC")
    private List<HistorialEstado> historial = new ArrayList<>();

    // HU-22: la entrega del objeto a su dueño; como mucho una, y cierra el caso.
    @OneToMany(mappedBy = "objeto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<Entrega> entregas = new ArrayList<>();

    // HU-33: lo que la IA reconoció en la foto; null si no hay foto o no se pudo analizar.
    @Embedded
    private CaracteristicasObjeto caracteristicas;

    protected Objeto() {
    }

    public Objeto(String nombre, String descripcion, TipoObjeto tipo, CategoriaObjeto categoria,
                  Pabellon ubicacion, String imagenUrl, LocalDateTime fechaHoraReporte, Usuario reportadoPor) {
        this.nombre = validarNombre(nombre);
        this.descripcion = validarDescripcion(descripcion);
        this.tipo = requerido(tipo, "tipo");
        this.categoria = requerido(categoria, "categoria");
        this.ubicacion = requerido(ubicacion, "ubicacion");
        this.imagenUrl = validarImagenUrl(imagenUrl);
        this.fechaHoraReporte = requerido(fechaHoraReporte, "fechaHoraReporte");
        this.reportadoPor = requerido(reportadoPor, "reportadoPor");
    }

    // HU-04, HU-10: el estudiante reporta PERDIDO y seguridad registra ENCONTRADO.
    public static Objeto reportar(String nombre, String descripcion, CategoriaObjeto categoria,
                                  Pabellon ubicacion, String imagenUrl, Usuario autor) {
        if (autor != null && autor.esSeguridad()) {
            return reportarEncontrado(nombre, descripcion, categoria, ubicacion, imagenUrl, autor);
        }
        return reportarPerdido(nombre, descripcion, categoria, ubicacion, imagenUrl, autor);
    }

    public static Objeto reportarPerdido(String nombre, String descripcion, CategoriaObjeto categoria,
                                         Pabellon ubicacion, String imagenUrl, Usuario estudiante) {
        if (estudiante == null || !estudiante.esEstudiante()) {
            throw new OperacionNoPermitidaException();
        }
        return new Objeto(nombre, descripcion, TipoObjeto.PERDIDO, categoria, ubicacion, imagenUrl,
                LocalDateTime.now(), estudiante);
    }

    public static Objeto reportarEncontrado(String nombre, String descripcion, CategoriaObjeto categoria,
                                            Pabellon ubicacion, String imagenUrl, Usuario agente) {
        if (agente == null || !agente.esSeguridad()) {
            throw new OperacionNoPermitidaException();
        }
        return new Objeto(nombre, descripcion, TipoObjeto.ENCONTRADO, categoria, ubicacion, imagenUrl,
                LocalDateTime.now(), agente);
    }

    // HU-35: genera nombre, descripción y categoría sugeridos a partir de los datos detectados en la imagen.
    public static String sugerirNombre(String tipoDetectado, String marca, String color) {
        StringBuilder sb = new StringBuilder();
        if (tipoDetectado != null && !tipoDetectado.isBlank()) {
            sb.append(tipoDetectado.trim());
        } else {
            sb.append("Objeto");
        }
        if (marca != null && !marca.isBlank()) {
            sb.append(" ").append(marca.trim());
        }
        if (color != null && !color.isBlank()) {
            sb.append(" ").append(color.trim().toLowerCase());
        }
        String resultado = sb.toString().trim();
        return resultado.length() > NOMBRE_MAXIMO ? resultado.substring(0, NOMBRE_MAXIMO) : resultado;
    }

    public static String sugerirDescripcion(String tipoDetectado, String marca, String color, String detalle) {
        String base = sugerirNombre(tipoDetectado, marca, color);
        StringBuilder sb = new StringBuilder(base);
        if (detalle != null && !detalle.isBlank()) {
            sb.append(" con detalle visible: ").append(detalle.trim());
        } else {
            sb.append(" registrado a partir de la imagen subida");
        }
        sb.append(".");
        String resultado = sb.toString();
        if (resultado.length() < DESCRIPCION_MINIMA) {
            resultado = resultado + " Sin detalles adicionales.";
        }
        return resultado.length() > DESCRIPCION_MAXIMA ? resultado.substring(0, DESCRIPCION_MAXIMA) : resultado;
    }

    public static CategoriaObjeto sugerirCategoria(String textoAnalisis) {
        if (textoAnalisis == null || textoAnalisis.isBlank()) {
            return CategoriaObjeto.OTROS;
        }
        String t = textoAnalisis.toLowerCase();
        if (t.contains("laptop") || t.contains("celular") || t.contains("audifono") || t.contains("audéfono")
                || t.contains("cargador") || t.contains("tablet") || t.contains("mouse")) {
            return CategoriaObjeto.TECNOLOGIA;
        }
        if (t.contains("dni") || t.contains("carne") || t.contains("carné") || t.contains("tarjeta")
                || t.contains("documento") || t.contains("billetera")) {
            return CategoriaObjeto.DOCUMENTOS;
        }
        if (t.contains("botella") || t.contains("tomatodo") || t.contains("termo") || t.contains("stanley")) {
            return CategoriaObjeto.BOTELLAS;
        }
        if (t.contains("casaca") || t.contains("polera") || t.contains("gorra") || t.contains("chompa")
                || t.contains("ropa")) {
            return CategoriaObjeto.ROPA;
        }
        if (t.contains("cuaderno") || t.contains("libro") || t.contains("calculadora") || t.contains("lapicero")
                || t.contains("cartuchera")) {
            return CategoriaObjeto.UTILES;
        }
        if (t.contains("llave") || t.contains("lentes") || t.contains("reloj") || t.contains("mochila")
                || t.contains("paraguas")) {
            return CategoriaObjeto.ACCESORIOS;
        }
        return CategoriaObjeto.OTROS;
    }


    public String getCodigo() {
        return String.format("UF-%04d", idObjeto);
    }

    public void cambiarEstado(EstadoObjeto nuevoEstado) {
        this.estado = requerido(nuevoEstado, "estado");
    }

    // HU-14, HU-15: cambia el estado y deja el cambio en el historial del objeto.
    public void cambiarEstado(EstadoObjeto nuevoEstado, String observacion, Usuario realizadoPor) {
        if (fueEntregado()) {
            throw new OperacionNoPermitidaException();
        }
        // Para pasar a ENTREGADO hacen falta los datos de quien reclama: se usa entregar().
        if (nuevoEstado == EstadoObjeto.ENTREGADO) {
            throw new DatosInvalidosException("entrega");
        }
        registrarCambio(nuevoEstado, observacion, realizadoPor);
    }

    // HU-14, HU-22: entrega el objeto a su dueño; queda ENTREGADO y con su registro de entrega.
    public Entrega entregar(String nombreReclamante, String documentoReclamante, String observacion,
                            Usuario encargado) {
        if (fueEntregado()) {
            throw new OperacionNoPermitidaException();
        }
        Entrega entrega = new Entrega(this, nombreReclamante, documentoReclamante, LocalDateTime.now(),
                observacion, encargado);
        registrarCambio(EstadoObjeto.ENTREGADO, observacion, encargado);
        entregas.add(entrega);
        return entrega;
    }

    private void registrarCambio(EstadoObjeto nuevoEstado, String observacion, Usuario realizadoPor) {
        HistorialEstado cambio = new HistorialEstado(this, estado, requerido(nuevoEstado, "estado"),
                LocalDateTime.now(), observacion, realizadoPor);
        cambiarEstado(nuevoEstado);
        historial.add(cambio);
    }

    public void marcarEnRevision() {
        cambiarEstado(EstadoObjeto.EN_REVISION);
    }

    public void marcarCoincidenciaDetectada() {
        cambiarEstado(EstadoObjeto.COINCIDENCIA_DETECTADA);
    }

    public void marcarDisponibleEnOficina() {
        cambiarEstado(EstadoObjeto.DISPONIBLE_EN_OFICINA);
    }

    public void marcarComoEntregado() {
        cambiarEstado(EstadoObjeto.ENTREGADO);
    }

    public void archivar() {
        cambiarEstado(EstadoObjeto.ARCHIVADO);
    }

    public void adjuntarImagen(String imagenUrl) {
        String nueva = validarImagenUrl(imagenUrl);
        // HU-33: las características describen la foto; con otra foto dejan de valer.
        if (!Objects.equals(nueva, this.imagenUrl)) {
            this.caracteristicas = null;
        }
        this.imagenUrl = nueva;
    }

    // HU-33: guarda lo que la IA reconoció en la foto del objeto; sin foto no hay nada que describir.
    public void registrarCaracteristicas(CaracteristicasObjeto caracteristicas) {
        if (!tieneImagen()) {
            throw new DatosInvalidosException("imagenUrl");
        }
        this.caracteristicas = requerido(caracteristicas, "caracteristicas");
    }

    public void editar(String descripcion, Pabellon ubicacion, String imagenUrl) {
        this.descripcion = validarDescripcion(descripcion);
        this.ubicacion = requerido(ubicacion, "ubicacion");
        if (imagenUrl != null) {
            adjuntarImagen(imagenUrl);
        }
    }

    public void desactivar() {
        this.activo = false;
    }

    public boolean esDe(Usuario usuario) {
        return usuario != null && reportadoPor.getIdUsuario().equals(usuario.getIdUsuario());
    }

    public boolean puedeEditarlo(Usuario usuario) {
        return activo && esDe(usuario) && estado == EstadoObjeto.REPORTADO;
    }

    // HU-16, HU-17: un filtro vacío (null) no descarta al objeto.
    public boolean coincideCon(String texto, TipoObjeto tipo, CategoriaObjeto categoria,
                               Pabellon ubicacion, EstadoObjeto estado) {
        String buscado = texto == null ? "" : texto.trim().toLowerCase();
        return (buscado.isEmpty()
                        || nombre.toLowerCase().contains(buscado)
                        || descripcion.toLowerCase().contains(buscado))
                && (tipo == null || this.tipo == tipo)
                && (categoria == null || this.categoria == categoria)
                && (ubicacion == null || this.ubicacion == ubicacion)
                && (estado == null || this.estado == estado);
    }

    // HU-23: de 0 a 100, qué tanto se parece a otro objeto; 0 si no pueden ser el mismo objeto.
    public int puntajeDeCoincidenciaCon(Objeto otro) {
        if (otro == null || otro == this || tipo == otro.tipo || categoria != otro.categoria
                || !sigueAbierto() || !otro.sigueAbierto()) {
            return 0;
        }
        long dias = Math.abs(Duration.between(fechaHoraReporte, otro.fechaHoraReporte).toDays());
        if (dias > DIAS_MAXIMOS_COINCIDENCIA) {
            return 0;
        }
        int puntaje = 40;
        if (ubicacion == otro.ubicacion) {
            puntaje += 25;
        }
        puntaje += dias <= 3 ? 15 : dias <= 7 ? 10 : 5;
        Set<String> comunes = palabrasClave();
        comunes.retainAll(otro.palabrasClave());
        return puntaje + Math.min(20, 5 * comunes.size());
    }

    // HU-23: las posibles coincidencias con los candidatos que llegan al puntaje mínimo.
    public List<Coincidencia> detectarCoincidencias(List<Objeto> candidatos) {
        LocalDateTime ahora = LocalDateTime.now();
        List<Coincidencia> detectadas = new ArrayList<>();
        for (Objeto otro : candidatos) {
            int puntaje = puntajeDeCoincidenciaCon(otro);
            if (puntaje >= Coincidencia.PUNTAJE_MINIMO) {
                detectadas.add(esPerdido() ? new Coincidencia(this, otro, puntaje, ahora)
                        : new Coincidencia(otro, this, puntaje, ahora));
            }
        }
        return detectadas;
    }

    private boolean sigueAbierto() {
        return activo && !fueEntregado() && !estaArchivado();
    }

    // HU-28: caso abierto sin cambios de estado hace más de DIAS_SIN_MOVIMIENTO días.
    public boolean necesitaAtencion() {
        LocalDateTime limite = LocalDateTime.now().minusDays(DIAS_SIN_MOVIMIENTO);
        return sigueAbierto() && getFechaUltimaActualizacion().isBefore(limite);
    }

    private Set<String> palabrasClave() {
        String sinTildes = Normalizer.normalize((nombre + " " + descripcion).toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return Arrays.stream(sinTildes.split("[^a-z0-9]+"))
                .filter(palabra -> palabra.length() >= 4 && !PALABRAS_COMUNES.contains(palabra))
                .collect(Collectors.toCollection(HashSet::new));
    }

    public boolean tieneImagen() {
        return imagenUrl != null;
    }

    public boolean tieneCaracteristicas() {
        return caracteristicas != null;
    }

    public boolean esPerdido() { return tipo == TipoObjeto.PERDIDO; }
    public boolean esEncontrado() { return tipo == TipoObjeto.ENCONTRADO; }
    public boolean estaArchivado() { return estado == EstadoObjeto.ARCHIVADO; }
    public boolean fueEntregado() { return !entregas.isEmpty(); }

    public Long getIdObjeto() { return idObjeto; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public TipoObjeto getTipo() { return tipo; }
    public CategoriaObjeto getCategoria() { return categoria; }
    public Pabellon getUbicacion() { return ubicacion; }
    public String getImagenUrl() { return imagenUrl; }
    public LocalDateTime getFechaHoraReporte() { return fechaHoraReporte; }
    public EstadoObjeto getEstado() { return estado; }
    public boolean isActivo() { return activo; }
    public Usuario getReportadoPor() { return reportadoPor; }
    public CaracteristicasObjeto getCaracteristicas() { return caracteristicas; }

    public Entrega getEntrega() { return entregas.isEmpty() ? null : entregas.get(0); }

    // HU-15: el historial se muestra del cambio más reciente al más antiguo.
    public List<HistorialEstado> getHistorial() {
        List<HistorialEstado> recientesPrimero = new ArrayList<>(historial);
        Collections.reverse(recientesPrimero);
        return Collections.unmodifiableList(recientesPrimero);
    }

    public boolean tieneHistorial() {
        return !historial.isEmpty();
    }

    public HistorialEstado getUltimoCambio() {
        return historial.isEmpty() ? null : historial.get(historial.size() - 1);
    }

    public LocalDateTime getFechaUltimaActualizacion() {
        return historial.isEmpty() ? fechaHoraReporte : historial.get(historial.size() - 1).getFechaHora();
    }

    private static String validarNombre(String valor) {
        String limpio = texto(valor, "nombre");
        if (limpio.length() > NOMBRE_MAXIMO) {
            throw new DatosInvalidosException("nombre");
        }
        return limpio;
    }

    private static String validarDescripcion(String valor) {
        String limpio = texto(valor, "descripcion");
        if (limpio.length() < DESCRIPCION_MINIMA || limpio.length() > DESCRIPCION_MAXIMA) {
            throw new DatosInvalidosException("descripcion");
        }
        return limpio;
    }

    private static String validarImagenUrl(String valor) {
        if (valor != null && (valor.isBlank() || valor.length() > IMAGEN_URL_MAXIMA)) {
            throw new DatosInvalidosException("imagenUrl");
        }
        return valor;
    }

    private static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DatosInvalidosException(campo);
        }
        return valor.trim();
    }

    private static <T> T requerido(T valor, String campo) {
        if (valor == null) {
            throw new DatosInvalidosException(campo);
        }
        return valor;
    }
}
