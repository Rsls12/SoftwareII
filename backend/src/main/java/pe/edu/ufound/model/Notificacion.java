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

import java.time.LocalDateTime;
import java.util.List;

/** HU-25: aviso al dueño de un objeto perdido cuando aparece una coincidencia relevante. */
@Entity
@Table(name = "notificacion")
public class Notificacion {

    public static final int MENSAJE_MAXIMO = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNotificacion;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario_destino", nullable = false, updatable = false)
    private Usuario destinatario;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_coincidencia", nullable = false, updatable = false)
    private Coincidencia coincidencia;

    @Column(nullable = false, updatable = false, length = MENSAJE_MAXIMO)
    private String mensaje;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private boolean leida = false;

    protected Notificacion() {
    }

    private Notificacion(Coincidencia coincidencia, LocalDateTime fechaHora) {
        Objeto perdido = coincidencia.getObjetoPerdido();
        this.destinatario = perdido.getReportadoPor();
        this.coincidencia = coincidencia;
        this.mensaje = "Se registró un objeto encontrado que podría ser tu \"" + perdido.getNombre() + "\" ("
                + perdido.getCodigo() + "). Acércate a la Oficina de Objetos Perdidos para verificarlo.";
        this.fechaHora = fechaHora;
    }

    // HU-25: una notificación por cada coincidencia relevante, para el estudiante que perdió el objeto.
    public static List<Notificacion> de(List<Coincidencia> coincidencias) {
        LocalDateTime ahora = LocalDateTime.now();
        return coincidencias.stream()
                .filter(Coincidencia::esRelevante)
                .map(coincidencia -> new Notificacion(coincidencia, ahora))
                .toList();
    }

    public Long getIdNotificacion() { return idNotificacion; }
    public Usuario getDestinatario() { return destinatario; }
    public Coincidencia getCoincidencia() { return coincidencia; }
    public String getMensaje() { return mensaje; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public boolean isLeida() { return leida; }
}
