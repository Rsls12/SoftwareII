package pe.edu.ufound.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.edu.ufound.excepcion.DatosInvalidosException;
import pe.edu.ufound.excepcion.OperacionNoPermitidaException;

import java.time.LocalDateTime;

@Entity
@Table(name = "objeto")
public class Objeto {

    public static final int NOMBRE_MAXIMO = 80;
    public static final int DESCRIPCION_MINIMA = 10;
    public static final int DESCRIPCION_MAXIMA = 500;
    public static final int IMAGEN_URL_MAXIMA = 255;

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

    public String getCodigo() {
        return String.format("UF-%04d", idObjeto);
    }

    public void cambiarEstado(EstadoObjeto nuevoEstado) {
        this.estado = requerido(nuevoEstado, "estado");
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
        this.imagenUrl = validarImagenUrl(imagenUrl);
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

    public boolean tieneImagen() {
        return imagenUrl != null;
    }

    public boolean esPerdido() { return tipo == TipoObjeto.PERDIDO; }
    public boolean esEncontrado() { return tipo == TipoObjeto.ENCONTRADO; }
    public boolean estaArchivado() { return estado == EstadoObjeto.ARCHIVADO; }

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
