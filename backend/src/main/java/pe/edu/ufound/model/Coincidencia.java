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
    // HU-25: desde este puntaje se avisa al dueño del objeto perdido.
    public static final int PUNTAJE_RELEVANTE = 75;
    // HU-24: desde este puntaje el nivel es ALTO (coinciden en casi todo: categoría, lugar, fecha y palabras).
    public static final int PUNTAJE_ALTO = 90;

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

    public boolean esRelevante() {
        return puntaje >= PUNTAJE_RELEVANTE;
    }

    // HU-24: el puntaje (0-100, calculado en Objeto.puntajeDeCoincidenciaCon) traducido a un nivel
    // que la Oficina entiende de un vistazo. No se guarda aparte: siempre depende del mismo puntaje.
    public NivelCoincidencia getNivelCoincidencia() {
        if (puntaje >= PUNTAJE_ALTO) {
            return NivelCoincidencia.ALTO;
        }
        if (puntaje >= PUNTAJE_RELEVANTE) {
            return NivelCoincidencia.MEDIO;
        }
        return NivelCoincidencia.BAJO;
    }

    public Long getIdCoincidencia() { return idCoincidencia; }
    public Objeto getObjetoPerdido() { return objetoPerdido; }
    public Objeto getObjetoEncontrado() { return objetoEncontrado; }
    public int getPuntaje() { return puntaje; }
    public LocalDateTime getFechaHoraDeteccion() { return fechaHoraDeteccion; }
}
