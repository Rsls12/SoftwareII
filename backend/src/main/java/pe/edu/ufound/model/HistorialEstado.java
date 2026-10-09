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

import java.time.LocalDateTime;

/** Un cambio de estado de un objeto (clase HistorialEstado del UML). Lo crea Objeto.cambiarEstado. */
@Entity
@Table(name = "historial_estado")
public class HistorialEstado {

    public static final int OBSERVACION_MAXIMA = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idHistorial;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_objeto", nullable = false, updatable = false)
    private Objeto objeto;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, updatable = false, length = 30)
    private EstadoObjeto estadoAnterior;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, updatable = false, length = 30)
    private EstadoObjeto estadoNuevo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(updatable = false, length = OBSERVACION_MAXIMA)
    private String observacion;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_realiza", nullable = false, updatable = false)
    private Usuario realizadoPor;

    protected HistorialEstado() {
    }

    public HistorialEstado(Objeto objeto, EstadoObjeto estadoAnterior, EstadoObjeto estadoNuevo,
                           LocalDateTime fechaHora, String observacion, Usuario realizadoPor) {
        if (observacion != null && observacion.length() > OBSERVACION_MAXIMA) {
            throw new DatosInvalidosException("observacion");
        }
        if (realizadoPor == null) {
            throw new DatosInvalidosException("realizadoPor");
        }
        this.objeto = objeto;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.fechaHora = fechaHora;
        this.observacion = observacion;
        this.realizadoPor = realizadoPor;
    }

    public Long getIdHistorial() { return idHistorial; }
    public Objeto getObjeto() { return objeto; }
    public EstadoObjeto getEstadoAnterior() { return estadoAnterior; }
    public EstadoObjeto getEstadoNuevo() { return estadoNuevo; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getObservacion() { return observacion; }
    public Usuario getRealizadoPor() { return realizadoPor; }

    public String getDescripcionCambio() {
        return "Pasó de " + estadoAnterior.getEtiqueta() + " a " + estadoNuevo.getEtiqueta();
    }

    public boolean tieneObservacion() {
        return observacion != null && !observacion.isBlank();
    }
}
