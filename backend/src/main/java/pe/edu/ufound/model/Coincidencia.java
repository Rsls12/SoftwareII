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

/** HU-23: un objeto perdido y uno encontrado que podrían ser el mismo. La crea Objeto.detectarCoincidencias. */
@Entity
@Table(name = "coincidencia")
public class Coincidencia {

    // Misma categoría y al menos otra pista fuerte (lugar, fecha cercana o palabras en común).
    public static final int PUNTAJE_MINIMO = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCoincidencia;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_objeto_perdido", nullable = false, updatable = false)
    private Objeto objetoPerdido;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_objeto_encontrado", nullable = false, updatable = false)
    private Objeto objetoEncontrado;

    @Column(nullable = false)
    private int puntaje;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaHoraDeteccion;

    protected Coincidencia() {
    }

    Coincidencia(Objeto objetoPerdido, Objeto objetoEncontrado, int puntaje, LocalDateTime fechaHoraDeteccion) {
        this.objetoPerdido = objetoPerdido;
        this.objetoEncontrado = objetoEncontrado;
        this.puntaje = puntaje;
        this.fechaHoraDeteccion = fechaHoraDeteccion;
    }

    public Long getIdCoincidencia() { return idCoincidencia; }
    public Objeto getObjetoPerdido() { return objetoPerdido; }
    public Objeto getObjetoEncontrado() { return objetoEncontrado; }
    public int getPuntaje() { return puntaje; }
    public LocalDateTime getFechaHoraDeteccion() { return fechaHoraDeteccion; }
}
