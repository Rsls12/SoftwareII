package pe.edu.ufound.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pe.edu.ufound.excepcion.DatosInvalidosException;
import pe.edu.ufound.excepcion.OperacionNoPermitidaException;

import java.time.LocalDateTime;

/** Entrega de un objeto a su dueño (clase Entrega del UML). La crea Objeto.entregar. */
@Entity
@Table(name = "entrega")
public class Entrega {

    public static final int NOMBRE_MAXIMO = 150;
    public static final int DOCUMENTO_MAXIMO = 30;
    public static final int OBSERVACION_MAXIMA = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEntrega;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_objeto", nullable = false, updatable = false)
    private Objeto objeto;

    @Column(nullable = false, length = NOMBRE_MAXIMO)
    private String nombreReclamante;

    @Column(nullable = false, length = DOCUMENTO_MAXIMO)
    private String documentoReclamante;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaHoraEntrega;

    @Column(length = OBSERVACION_MAXIMA)
    private String observacion;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_encargado", nullable = false, updatable = false)
    private Usuario encargado;

    protected Entrega() {
    }

    // HU-22: solo Oficina entrega objetos, y siempre queda quién lo reclamó y con qué documento.
    Entrega(Objeto objeto, String nombreReclamante, String documentoReclamante,
            LocalDateTime fechaHoraEntrega, String observacion, Usuario encargado) {
        if (encargado == null || !encargado.esOficina()) {
            throw new OperacionNoPermitidaException();
        }
        if (observacion != null && observacion.length() > OBSERVACION_MAXIMA) {
            throw new DatosInvalidosException("observacion");
        }
        this.objeto = objeto;
        this.nombreReclamante = texto(nombreReclamante, "nombreReclamante", NOMBRE_MAXIMO);
        this.documentoReclamante = texto(documentoReclamante, "documentoReclamante", DOCUMENTO_MAXIMO);
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

    private static String texto(String valor, String campo, int maximo) {
        if (valor == null || valor.isBlank() || valor.trim().length() > maximo) {
            throw new DatosInvalidosException(campo);
        }
        return valor.trim();
    }
}
