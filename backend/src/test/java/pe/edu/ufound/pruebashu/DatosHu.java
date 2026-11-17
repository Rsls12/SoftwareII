package pe.edu.ufound.pruebashu;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.ufound.model.CategoriaObjeto;
import pe.edu.ufound.model.Entrega;
import pe.edu.ufound.model.Objeto;
import pe.edu.ufound.model.Pabellon;
import pe.edu.ufound.model.RolUsuario;
import pe.edu.ufound.model.TipoObjeto;
import pe.edu.ufound.model.Usuario;
import pe.edu.ufound.repositorio.CoincidenciaRepository;
import pe.edu.ufound.repositorio.NotificacionRepository;
import pe.edu.ufound.repositorio.ObjetoRepository;
import pe.edu.ufound.repositorio.UsuarioRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/** Datos de prueba para las pruebas de integración. */
public class DatosHu {

    public static final String ANA = "ana.torres@universidad.edu.pe";
    public static final String JORGE = "jorge.quispe@universidad.edu.pe";
    public static final String LUCIA = "lucia.paredes@universidad.edu.pe";
    public static final String BETO = "beto.prueba@universidad.edu.pe";
    public static final String CLAVE_BETO = "prueba-beto-2026";

    private final ObjetoRepository objetos;
    private final UsuarioRepository usuarios;
    private final CoincidenciaRepository coincidencias;
    private final NotificacionRepository notificaciones;
    private final PasswordEncoder cifrador;
    private final JdbcTemplate jdbc;

    public DatosHu(ObjetoRepository objetos, UsuarioRepository usuarios, CoincidenciaRepository coincidencias,
                   NotificacionRepository notificaciones, PasswordEncoder cifrador, JdbcTemplate jdbc) {
        this.objetos = objetos;
        this.usuarios = usuarios;
        this.coincidencias = coincidencias;
        this.notificaciones = notificaciones;
        this.cifrador = cifrador;
        this.jdbc = jdbc;
    }

    public void limpiar() {
        notificaciones.deleteAll();
        coincidencias.deleteAll();
        objetos.deleteAll();
        if (usuarios.findByCorreoIgnoreCase(BETO).isEmpty()) {
            usuarios.save(new Usuario("Beto", "Prueba Ramos", BETO, cifrador.encode(CLAVE_BETO),
                    RolUsuario.ESTUDIANTE));
        }
    }

    public Usuario usuario(String correo) {
        return usuarios.findByCorreoIgnoreCase(correo).orElseThrow();
    }

    public Usuario estudianteNuevo(String correo) {
        return usuarios.save(new Usuario("Carla", "Sin Reportes", correo, cifrador.encode("prueba-carla-2026"),
                RolUsuario.ESTUDIANTE));
    }

    public Objeto objeto(String nombre, TipoObjeto tipo, CategoriaObjeto categoria, LocalDateTime fecha,
                         String correoAutor) {
        return objetos.save(new Objeto(nombre, "Descripción de prueba: " + nombre, tipo, categoria,
                Pabellon.PABELLON_B, null, fecha, usuario(correoAutor)));
    }

    public Objeto guardar(Objeto objeto) {
        return objetos.save(objeto);
    }

    public Objeto recargar(Long id) {
        return objetos.findById(id).orElseThrow();
    }

    public Entrega entregar(Objeto objeto, String reclamante, String documento, String observacion,
                            LocalDateTime fecha) {
        objeto.entregar(reclamante, documento, observacion, usuario(LUCIA));
        Objeto guardado = objetos.save(objeto);
        Entrega entrega = guardado.getEntrega();
        jdbc.update("update entrega set fecha_hora_entrega = ? where id_entrega = ?",
                Timestamp.valueOf(fecha), entrega.getIdEntrega());
        return entrega;
    }

    public long cantidadDeEntregas() {
        return objetos.entregas().size();
    }
}
