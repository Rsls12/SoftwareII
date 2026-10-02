package pe.edu.ufound.model;

import java.time.LocalDateTime;

/** Entrega de un objeto a su dueño (clase Entrega del UML). */
public class Entrega {

    private final Long idEntrega;
    private final Objeto objeto;
    private final String nombreReclamante;
    private final String documentoReclamante;
    private final LocalDateTime fechaHoraEntrega;
    private final String observacion;
    private final Usuario encargado;

    public Entrega(Long idEntrega, Objeto objeto, String nombreReclamante, String documentoReclamante,
                   LocalDateTime fechaHoraEntrega, String observacion, Usuario encargado) {
        this.idEntrega = idEntrega;
        this.objeto = objeto;
        this.nombreReclamante = nombreReclamante;
        this.documentoReclamante = documentoReclamante;
        this.fechaHoraEntrega = fechaHoraEntrega;
        this.observacion = observacion;
        this.encargado = encargado;
    }

    public Long getIdEntrega() { return idEntrega; }
    public Objeto getObjeto() { return objeto; }
    public String getNombreReclamante() { return nombreReclamante; }
    public String getDocumentoReclamante() { return documentoReclamante; }
    public LocalDateTime getFechaHoraEntrega() { return fechaHoraEntrega; }
    public String getObservacion() { return observacion; }
    public Usuario getEncargado() { return encargado; }
}
