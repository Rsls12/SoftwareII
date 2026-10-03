package pe.edu.ufound.model;

import java.time.LocalDateTime;

/** Objeto perdido o encontrado (clase Objeto del UML). */
public class Objeto {

    private final Long idObjeto;
    private final String nombre;
    private String descripcion;
    private final TipoObjeto tipo;
    private final CategoriaObjeto categoria;
    private Pabellon ubicacion;
    private String imagenUrl;
    private final LocalDateTime fechaHoraReporte;
    private EstadoObjeto estado = EstadoObjeto.REPORTADO;
    private boolean activo = true;
    private final Usuario reportadoPor;

    public Objeto(Long idObjeto, String nombre, String descripcion, TipoObjeto tipo, CategoriaObjeto categoria,
                  Pabellon ubicacion, String imagenUrl, LocalDateTime fechaHoraReporte, Usuario reportadoPor) {
        this.idObjeto = idObjeto;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.categoria = categoria;
        this.ubicacion = ubicacion;
        this.imagenUrl = imagenUrl;
        this.fechaHoraReporte = fechaHoraReporte;
        this.reportadoPor = reportadoPor;
    }

    /** Código corto que ve el usuario, por ejemplo UF-0007. */
    public String getCodigo() {
        return String.format("UF-%04d", idObjeto);
    }

    public void cambiarEstado(EstadoObjeto nuevoEstado) {
        this.estado = nuevoEstado;
    }

    /** Solo cambia lo que permite HU-21: descripción, ubicación e imagen (si se subió una nueva). */
    public void editar(String descripcion, Pabellon ubicacion, String imagenUrl) {
        this.descripcion = descripcion;
        this.ubicacion = ubicacion;
        if (imagenUrl != null) {
            this.imagenUrl = imagenUrl;
        }
    }

    /** Retira el objeto del catálogo sin borrarlo (HU-20). */
    public void desactivar() {
        this.activo = false;
    }

    public boolean esDe(Usuario usuario) {
        return reportadoPor.getIdUsuario().equals(usuario.getIdUsuario());
    }

    /** Se puede editar mientras siga en REPORTADO, es decir, antes de que la oficina lo procese. */
    public boolean puedeEditarlo(Usuario usuario) {
        return activo && esDe(usuario) && estado == EstadoObjeto.REPORTADO;
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
}
