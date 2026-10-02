package pe.edu.ufound.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.ufound.model.*;
import pe.edu.ufound.servicio.ObjetoService;
import pe.edu.ufound.servicio.UsuarioService;

import java.io.IOException;
import java.util.Base64;

/**
 * Abre cada pantalla del Sprint 1 y le pasa los datos que muestra.
 * No tiene reglas de negocio: todo eso está en los servicios.
 */
@Controller
public class PantallaController {

    private final ObjetoService objetos;
    private final UsuarioService usuarios;

    public PantallaController(ObjetoService objetos, UsuarioService usuarios) {
        this.objetos = objetos;
        this.usuarios = usuarios;
    }

    /** Datos que usan todas las pantallas: el usuario actual y las opciones de los selects. */
    @ModelAttribute
    public void datosComunes(Model model) {
        model.addAttribute("yo", usuarios.usuarioActual());
        model.addAttribute("tipos", TipoObjeto.values());
        model.addAttribute("categorias", CategoriaObjeto.values());
        model.addAttribute("pabellones", Pabellon.values());
        model.addAttribute("estados", EstadoObjeto.values());
    }

    // ---------- Sesión (HU-01, HU-02) ----------

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // MOCK: Spring Security procesará el POST /login y el POST /logout. Por ahora solo navegan.
    @PostMapping("/login")
    public String entrar() {
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String salir() {
        return "redirect:/login";
    }

    // MOCK: solo sirve para revisar la vista de cada rol en el Nivel 1. Se borra al integrar Spring Security.
    @GetMapping("/vista-previa/{rol}")
    public String vistaPrevia(@PathVariable RolUsuario rol) {
        usuarios.usarRol(rol);
        return "redirect:/";
    }

    // ---------- Inicio según rol (HU-03) y objetos recientes (HU-19) ----------

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("recientes", objetos.recientes(6));
        model.addAttribute("misReportes", objetos.deUsuario(usuarios.usuarioActual()).stream().limit(3).toList());
        return "inicio";
    }

    // ---------- Registrar objeto perdido o encontrado (HU-04 a HU-11) ----------

    @GetMapping("/objetos/nuevo")
    public String nuevo() {
        return "formulario";
    }

    @PostMapping("/objetos")
    public String registrar(@RequestParam String nombre, @RequestParam CategoriaObjeto categoria,
                            @RequestParam Pabellon ubicacion, @RequestParam String descripcion,
                            @RequestParam(required = false) MultipartFile foto,
                            RedirectAttributes redirect) throws IOException {
        Objeto objeto = objetos.registrar(nombre, categoria, ubicacion, descripcion,
                aImagenUrl(foto), usuarios.usuarioActual());
        redirect.addFlashAttribute("mensaje", "Listo, quedó registrado con el código " + objeto.getCodigo() + ".");
        return "redirect:/objetos/" + objeto.getIdObjeto();
    }

    // ---------- Detalle, estado e historial (HU-12 a HU-15) ----------

    @GetMapping("/objetos/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Objeto objeto = objetos.obtener(id);
        model.addAttribute("objeto", objeto);
        model.addAttribute("historial", objetos.historialDe(objeto));
        return "detalle";
    }

    @PostMapping("/objetos/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam EstadoObjeto estado,
                                @RequestParam(required = false) String observacion, RedirectAttributes redirect) {
        objetos.cambiarEstado(id, estado, observacion, usuarios.usuarioActual());
        redirect.addFlashAttribute("mensaje", "Estado actualizado a \"" + estado.getEtiqueta() + "\".");
        return "redirect:/objetos/" + id;
    }

    // ---------- Editar y eliminar reporte propio (HU-20, HU-21) ----------
    // Los permisos (solo el dueño) los valida el backend; aquí la vista solo oculta los botones.

    @GetMapping("/objetos/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("objeto", objetos.obtener(id));
        return "formulario";
    }

    @PostMapping("/objetos/{id}")
    public String guardarEdicion(@PathVariable Long id, @RequestParam String descripcion,
                                 @RequestParam Pabellon ubicacion,
                                 @RequestParam(required = false) MultipartFile foto,
                                 RedirectAttributes redirect) throws IOException {
        objetos.editar(id, descripcion, ubicacion, aImagenUrl(foto));
        redirect.addFlashAttribute("mensaje", "Cambios guardados.");
        return "redirect:/objetos/" + id;
    }

    @PostMapping("/objetos/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        objetos.eliminar(id);
        redirect.addFlashAttribute("mensaje", "Reporte eliminado.");
        return "redirect:/mis-publicaciones";
    }

    // ---------- Buscar y filtrar (HU-16, HU-17) ----------

    @GetMapping("/buscar")
    public String buscar(@RequestParam(required = false) String q,
                         @RequestParam(required = false) TipoObjeto tipo,
                         @RequestParam(required = false) CategoriaObjeto categoria,
                         @RequestParam(required = false) Pabellon ubicacion,
                         @RequestParam(required = false) EstadoObjeto estado,
                         Model model) {
        model.addAttribute("resultados", objetos.buscar(q, tipo, categoria, ubicacion, estado));
        // Se devuelven los filtros para que el formulario los mantenga seleccionados.
        model.addAttribute("q", q);
        model.addAttribute("tipoElegido", tipo);
        model.addAttribute("categoriaElegida", categoria);
        model.addAttribute("ubicacionElegida", ubicacion);
        model.addAttribute("estadoElegido", estado);
        return "buscar";
    }

    // ---------- Mis publicaciones (HU-18) e historial de entregas (HU-22) ----------

    @GetMapping("/mis-publicaciones")
    public String misPublicaciones(Model model) {
        model.addAttribute("misReportes", objetos.deUsuario(usuarios.usuarioActual()));
        return "mis-publicaciones";
    }

    @GetMapping("/entregas")
    public String entregas(Model model) {
        model.addAttribute("entregas", objetos.entregas());
        return "entregas";
    }

    /** MOCK: guarda la foto dentro del texto (data URL). El backend la subirá a un almacenamiento real. */
    private String aImagenUrl(MultipartFile foto) throws IOException {
        if (foto == null || foto.isEmpty() || foto.getContentType() == null
                || !foto.getContentType().startsWith("image/")) {
            return null;
        }
        return "data:" + foto.getContentType() + ";base64," + Base64.getEncoder().encodeToString(foto.getBytes());
    }
}
