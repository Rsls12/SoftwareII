package pe.edu.ufound.model;

import java.time.LocalDateTime;

/** Un cambio de estado de un objeto (clase HistorialEstado del UML). */
public class HistorialEstado {

    private final Long idHistorial;
    private final Objeto objeto;
    private final EstadoObjeto estadoAnterior;
    private final EstadoObjeto estadoNuevo;
    private final LocalDateTime fechaHora;
    private final String observacion;
    private final Usuario realizadoPor;

    public HistorialEstado(Long idHistorial, Objeto objeto, EstadoObjeto estadoAnterior, EstadoObjeto estadoNuevo,
                           LocalDateTime fechaHora, String observacion, Usuario realizadoPor) {
        this.idHistorial = idHistorial;
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
}
