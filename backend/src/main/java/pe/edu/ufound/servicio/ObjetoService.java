package pe.edu.ufound.servicio;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.ufound.model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * MOCK (Nivel 1, solo frontend): guarda todo en listas en memoria y se pierde al reiniciar.
 * El backend mantiene los mismos métodos y cambia las listas por repositorios JPA.
 * Las vistas y el controlador no necesitan cambiar.
 */
@Service
public class ObjetoService {

    private final List<Objeto> objetos = new ArrayList<>();
    private final List<HistorialEstado> historial = new ArrayList<>();
    private final List<Entrega> entregas = new ArrayList<>();
    private long siguienteIdObjeto = 1;
    private long siguienteIdRegistro = 1; // para historial y entregas

    public ObjetoService(UsuarioService usuarios) {
        cargarDatosDeEjemplo(usuarios);
    }

    /** Búsqueda con filtros (HU-16, HU-17). Un filtro en null significa "todos". */
    public List<Objeto> buscar(String texto, TipoObjeto tipo, CategoriaObjeto categoria,
                               Pabellon ubicacion, EstadoObjeto estado) {
        // Nota: no ignora tildes ("revision" no encuentra "revisión"); la BD lo hará con collation.
        String buscado = texto == null ? "" : texto.trim().toLowerCase();
        return activosMasRecientesPrimero().stream()
                .filter(o -> buscado.isEmpty()
                        || o.getNombre().toLowerCase().contains(buscado)
                        || o.getDescripcion().toLowerCase().contains(buscado))
                .filter(o -> tipo == null || o.getTipo() == tipo)
                .filter(o -> categoria == null || o.getCategoria() == categoria)
                .filter(o -> ubicacion == null || o.getUbicacion() == ubicacion)
                .filter(o -> estado == null || o.getEstado() == estado)
                .toList();
    }

    /** Últimos objetos registrados (HU-19). */
    public List<Objeto> recientes(int cantidad) {
        return activosMasRecientesPrimero().stream().limit(cantidad).toList();
    }

    /** Reportes del usuario (HU-18). */
    public List<Objeto> deUsuario(Usuario usuario) {
        return activosMasRecientesPrimero().stream().filter(o -> o.esDe(usuario)).toList();
    }

    public Objeto obtener(Long id) {
        return objetos.stream()
                .filter(o -> o.getIdObjeto().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /**
     * Registra un objeto (HU-04 a HU-11). La fecha y hora se ponen solas (HU-07).
     * Si lo registra seguridad es ENCONTRADO; si no, PERDIDO.
     */
    public Objeto registrar(String nombre, CategoriaObjeto categoria, Pabellon ubicacion,
                            String descripcion, String imagenUrl, Usuario autor) {
        TipoObjeto tipo = autor.esSeguridad() ? TipoObjeto.ENCONTRADO : TipoObjeto.PERDIDO;
        Objeto objeto = new Objeto(siguienteIdObjeto++, nombre, descripcion, tipo, categoria,
                ubicacion, imagenUrl, LocalDateTime.now(), autor);
        objetos.add(objeto);
        return objeto;
    }

    /** HU-21. */
    public void editar(Long id, String descripcion, Pabellon ubicacion, String imagenUrl) {
        obtener(id).editar(descripcion, ubicacion, imagenUrl);
    }

    /** HU-20. */
    public void eliminar(Long id) {
        obtener(id).desactivar();
    }

    /** HU-14: cambia el estado y deja el cambio en el historial (HU-15). */
    public void cambiarEstado(Long id, EstadoObjeto nuevoEstado, String observacion, Usuario realizadoPor) {
        Objeto objeto = obtener(id);
        historial.add(new HistorialEstado(siguienteIdRegistro++, objeto, objeto.getEstado(), nuevoEstado,
                LocalDateTime.now(), observacion, realizadoPor));
        objeto.cambiarEstado(nuevoEstado);
    }

    /** HU-15, del cambio más reciente al más antiguo. */
    public List<HistorialEstado> historialDe(Objeto objeto) {
        return historial.stream()
                .filter(h -> h.getObjeto() == objeto)
                .sorted(Comparator.comparing(HistorialEstado::getFechaHora).reversed())
                .toList();
    }

    /** HU-22, de la entrega más reciente a la más antigua. */
    public List<Entrega> entregas() {
        return entregas.stream()
                .sorted(Comparator.comparing(Entrega::getFechaHoraEntrega).reversed())
                .toList();
    }

    private List<Objeto> activosMasRecientesPrimero() {
        return objetos.stream()
                .filter(Objeto::isActivo)
                .sorted(Comparator.comparing(Objeto::getFechaHoraReporte).reversed())
                .toList();
    }

    // ---------------- Datos de ejemplo ----------------

    private void cargarDatosDeEjemplo(UsuarioService usuarios) {
        Usuario ana = usuarios.buscarPorRol(RolUsuario.ESTUDIANTE);
        Usuario jorge = usuarios.buscarPorRol(RolUsuario.SEGURIDAD);
        Usuario lucia = usuarios.buscarPorRol(RolUsuario.OFICINA_OBJETOS_PERDIDOS);
        LocalDateTime ahora = LocalDateTime.now();

        Objeto laptop = ejemplo("Laptop Lenovo ThinkPad gris", "Con sticker de la NASA en la tapa y cargador negro.",
                TipoObjeto.PERDIDO, CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_C, ahora.minusHours(3), ana);
        ejemplo("Botella Stanley verde", "Botella de 1 litro, abollada en la base, con mi nombre grabado.",
                TipoObjeto.PERDIDO, CategoriaObjeto.BOTELLAS, Pabellon.PABELLON_A, ahora.minusDays(1), ana);
        Objeto dni = ejemplo("DNI", "Encontrado en la cafetería, a nombre de un estudiante de Ingeniería.",
                TipoObjeto.ENCONTRADO, CategoriaObjeto.DOCUMENTOS, Pabellon.OTRO, ahora.minusDays(2), jorge);
        Objeto casaca = ejemplo("Casaca azul talla M", "Casaca impermeable azul marino con capucha.",
                TipoObjeto.ENCONTRADO, CategoriaObjeto.ROPA, Pabellon.PABELLON_B, ahora.minusDays(4), jorge);
        Objeto audifonos = ejemplo("Audífonos inalámbricos blancos", "Estuche blanco, le falta el audífono izquierdo.",
                TipoObjeto.ENCONTRADO, CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_D, ahora.minusDays(6), jorge);
        ejemplo("Calculadora Casio fx-991", "Tiene iniciales R.M. escritas con plumón en la parte de atrás.",
                TipoObjeto.ENCONTRADO, CategoriaObjeto.UTILES, Pabellon.PABELLON_E, ahora.minusDays(9), jorge);
        Objeto llaves = ejemplo("Llaves con llavero de Pikachu", "Tres llaves y un llavero amarillo.",
                TipoObjeto.ENCONTRADO, CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_A, ahora.minusDays(20), jorge);
        Objeto cuaderno = ejemplo("Cuaderno de Cálculo II", "Cuaderno A4 anillado, tapa roja.",
                TipoObjeto.PERDIDO, CategoriaObjeto.UTILES, Pabellon.PABELLON_B, ahora.minusDays(100), ana);

        cambiarEstado(laptop.getIdObjeto(), EstadoObjeto.EN_REVISION, "Revisando objetos encontrados en el Pabellón C.", lucia);
        cambiarEstado(dni.getIdObjeto(), EstadoObjeto.DISPONIBLE_EN_OFICINA, "Recibido de seguridad.", lucia);
        cambiarEstado(casaca.getIdObjeto(), EstadoObjeto.COINCIDENCIA_DETECTADA, "Coincide con un reporte de ayer.", lucia);
        cambiarEstado(audifonos.getIdObjeto(), EstadoObjeto.DISPONIBLE_EN_OFICINA, "Recibido de seguridad.", lucia);
        cambiarEstado(llaves.getIdObjeto(), EstadoObjeto.DISPONIBLE_EN_OFICINA, "Recibido de seguridad.", lucia);
        cambiarEstado(llaves.getIdObjeto(), EstadoObjeto.ENTREGADO, "Entregado a su dueña.", lucia);
        cambiarEstado(cuaderno.getIdObjeto(), EstadoObjeto.ARCHIVADO, "Más de 3 meses sin novedades.", lucia);

        entregas.add(new Entrega(siguienteIdRegistro++, llaves, "María Fernández López", "DNI 71234567",
                ahora.minusDays(15), "Describió correctamente el llavero.", lucia));
    }

    private Objeto ejemplo(String nombre, String descripcion, TipoObjeto tipo, CategoriaObjeto categoria,
                           Pabellon ubicacion, LocalDateTime fecha, Usuario autor) {
        Objeto objeto = new Objeto(siguienteIdObjeto++, nombre, descripcion, tipo, categoria, ubicacion, null, fecha, autor);
        objetos.add(objeto);
        return objeto;
    }
}
