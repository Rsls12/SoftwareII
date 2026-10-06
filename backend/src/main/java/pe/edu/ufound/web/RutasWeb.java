package pe.edu.ufound.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.util.WebUtils;
import pe.edu.ufound.almacenamiento.AlmacenamientoImagenes;
import pe.edu.ufound.excepcion.DatosInvalidosException;
import pe.edu.ufound.model.CategoriaObjeto;
import pe.edu.ufound.model.EstadoObjeto;
import pe.edu.ufound.model.Objeto;
import pe.edu.ufound.model.Pabellon;
import pe.edu.ufound.model.TipoObjeto;
import pe.edu.ufound.model.Usuario;
import pe.edu.ufound.repositorio.ObjetoRepository;
import pe.edu.ufound.repositorio.UsuarioRepository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Entrada HTTP de U-FOUND: une cada ruta con el objeto del modelo que la atiende y con su vista.
 * No tiene reglas de negocio: están en model/. Los permisos por rol están en SeguridadConfig.
 */
@Configuration
public class RutasWeb {

    private final ObjetoRepository objetos;
    private final UsuarioRepository usuarios;
    private final AlmacenamientoImagenes imagenes;

    public RutasWeb(ObjetoRepository objetos, UsuarioRepository usuarios, AlmacenamientoImagenes imagenes) {
        this.objetos = objetos;
        this.usuarios = usuarios;
        this.imagenes = imagenes;
    }

    @Bean
    public RouterFunction<ServerResponse> rutas() {
        return route()
                .GET("/login", this::login)
                .GET("/", this::inicio)
                .GET("/objetos/nuevo", this::nuevo)
                .POST("/objetos", this::registrar)
                .GET("/objetos/{id}", this::detalle)
                .POST("/objetos/{id}/estado", this::cambiarEstado)
                .GET("/objetos/{id}/editar", this::editar)
                .POST("/objetos/{id}", this::guardarEdicion)
                .POST("/objetos/{id}/eliminar", this::eliminar)
                .GET("/buscar", this::buscar)
                .GET("/mis-publicaciones", this::misPublicaciones)
                .GET("/entregas", this::historialDeEntregas)
                .GET("/api/categorias", this::categorias)
                .build();
    }

    // ---------- Inicio de sesión (HU-01); el POST /login y el POST /logout los procesa Spring Security ----------

    private ServerResponse login(ServerRequest peticion) {
        // Quien ya inició sesión no necesita ver el formulario otra vez.
        return peticion.principal().isPresent() ? redirigir(peticion, "/", null) : ServerResponse.ok().render("login");
    }

    // ---------- Inicio según rol (HU-03) y objetos recientes (HU-19) ----------

    private ServerResponse inicio(ServerRequest peticion) {
        Usuario yo = yo(peticion);
        Map<String, Object> modelo = modelo(peticion, yo);
        modelo.put("recientes", objetos.recientes(6));
        modelo.put("misReportes", objetos.findByReportadoPorAndActivoTrueOrderByFechaHoraReporteDesc(yo)
                .stream().limit(3).toList());
        return ServerResponse.ok().render("inicio", modelo);
    }

    // ---------- Registrar objeto perdido o encontrado (HU-04 a HU-11) ----------

    private ServerResponse nuevo(ServerRequest peticion) {
        return ServerResponse.ok().render("formulario", modelo(peticion, yo(peticion)));
    }

    private ServerResponse registrar(ServerRequest peticion) {
        // Primero se validan los datos; la foto solo se sube si el reporte es válido.
        Objeto objeto = Objeto.reportar(texto(peticion, "nombre"), texto(peticion, "descripcion"),
                opcion(peticion, "categoria", CategoriaObjeto.class), opcion(peticion, "ubicacion", Pabellon.class),
                null, yo(peticion));
        String imagenUrl = imagenes.guardar(foto(peticion));
        try {
            objeto.adjuntarImagen(imagenUrl);
            objeto = objetos.save(objeto);
        } catch (RuntimeException e) {
            imagenes.eliminar(imagenUrl);
            throw e;
        }
        return redirigir(peticion, "/objetos/" + objeto.getIdObjeto(),
                "Listo, quedó registrado con el código " + objeto.getCodigo() + ".");
    }

    // ---------- Detalle, estado e historial (HU-12 a HU-15) ----------

    private ServerResponse detalle(ServerRequest peticion) {
        Objeto objeto = objetoDe(peticion);
        Map<String, Object> modelo = modelo(peticion, yo(peticion));
        modelo.put("objeto", objeto);
        modelo.put("historial", objeto.getHistorial());
        modelo.put("entrega", objeto.getEntrega());
        return ServerResponse.ok().render("detalle", modelo);
    }

    private ServerResponse cambiarEstado(ServerRequest peticion) {
        Objeto objeto = objetoDe(peticion);
        EstadoObjeto estado = opcion(peticion, "estado", EstadoObjeto.class);
        String aviso;
        if (estado == EstadoObjeto.ENTREGADO) {
            // HU-22: al entregar se piden los datos de quien reclama; el encargado es quien inició sesión.
            objeto.entregar(texto(peticion, "nombreReclamante"), texto(peticion, "documentoReclamante"),
                    texto(peticion, "observacion"), yo(peticion));
            aviso = "Entrega registrada. El objeto ya figura en el historial de entregas.";
        } else {
            objeto.cambiarEstado(estado, texto(peticion, "observacion"), yo(peticion));
            aviso = "Estado actualizado a \"" + objeto.getEstado().getEtiqueta() + "\".";
        }
        objetos.save(objeto);
        return redirigir(peticion, "/objetos/" + objeto.getIdObjeto(), aviso);
    }

    // ---------- Editar y eliminar reporte propio (HU-20, HU-21) ----------
    // Los permisos (solo el dueño) siguen pendientes; aquí la vista solo oculta los botones.

    private ServerResponse editar(ServerRequest peticion) {
        Map<String, Object> modelo = modelo(peticion, yo(peticion));
        modelo.put("objeto", objetoDe(peticion));
        return ServerResponse.ok().render("formulario", modelo);
    }

    private ServerResponse guardarEdicion(ServerRequest peticion) {
        Objeto objeto = objetoDe(peticion);
        objeto.editar(texto(peticion, "descripcion"), opcion(peticion, "ubicacion", Pabellon.class),
                imagenes.guardar(foto(peticion)));
        objetos.save(objeto);
        return redirigir(peticion, "/objetos/" + objeto.getIdObjeto(), "Cambios guardados.");
    }

    private ServerResponse eliminar(ServerRequest peticion) {
        Objeto objeto = objetoDe(peticion);
        objeto.eliminar(yo(peticion));
        objetos.save(objeto);
        return redirigir(peticion, "/mis-publicaciones", "Reporte eliminado.");
    }

    // ---------- Buscar y filtrar (HU-16, HU-17) ----------

    private ServerResponse buscar(ServerRequest peticion) {
        String q = texto(peticion, "q");
        TipoObjeto tipo = opcion(peticion, "tipo", TipoObjeto.class);
        CategoriaObjeto categoria = opcion(peticion, "categoria", CategoriaObjeto.class);
        Pabellon ubicacion = opcion(peticion, "ubicacion", Pabellon.class);
        EstadoObjeto estado = opcion(peticion, "estado", EstadoObjeto.class);

        Map<String, Object> modelo = modelo(peticion, yo(peticion));
        modelo.put("resultados", objetos.buscar(q, tipo, categoria, ubicacion, estado));
        // Se devuelven los filtros para que el formulario los mantenga seleccionados.
        modelo.put("q", q);
        modelo.put("tipoElegido", tipo);
        modelo.put("categoriaElegida", categoria);
        modelo.put("ubicacionElegida", ubicacion);
        modelo.put("estadoElegido", estado);
        return ServerResponse.ok().render("buscar", modelo);
    }

    // ---------- Mis publicaciones (HU-18) e historial de entregas (HU-22) ----------

    private ServerResponse misPublicaciones(ServerRequest peticion) {
        Usuario yo = yo(peticion);
        Map<String, Object> modelo = modelo(peticion, yo);
        modelo.put("misReportes", objetos.findByReportadoPorAndActivoTrueOrderByFechaHoraReporteDesc(yo));
        return ServerResponse.ok().render("mis-publicaciones", modelo);
    }

    private ServerResponse historialDeEntregas(ServerRequest peticion) {
        Map<String, Object> modelo = modelo(peticion, yo(peticion));
        modelo.put("entregas", objetos.entregas());
        return ServerResponse.ok().render("entregas", modelo);
    }

    // ---------- Categorías en JSON (HU-05) ----------

    private ServerResponse categorias(ServerRequest peticion) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(
                Arrays.stream(CategoriaObjeto.values())
                        .map(categoria -> Map.of("codigo", categoria.name(), "etiqueta", categoria.getEtiqueta()))
                        .toList());
    }

    // ---------- Lectura de la petición ----------

    private Usuario yo(ServerRequest peticion) {
        return peticion.principal()
                .flatMap(sesion -> usuarios.findByCorreoIgnoreCase(sesion.getName()))
                .orElseThrow(IllegalStateException::new);
    }

    private Objeto objetoDe(ServerRequest peticion) {
        try {
            return objetos.findById(Long.valueOf(peticion.pathVariable("id")))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private static String texto(ServerRequest peticion, String nombre) {
        return peticion.param(nombre).orElse(null);
    }

    private static <E extends Enum<E>> E opcion(ServerRequest peticion, String nombre, Class<E> tipo) {
        String valor = peticion.param(nombre).orElse("");
        if (valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new DatosInvalidosException(nombre);
        }
    }

    private static MultipartFile foto(ServerRequest peticion) {
        MultipartHttpServletRequest formulario =
                WebUtils.getNativeRequest(peticion.servletRequest(), MultipartHttpServletRequest.class);
        return formulario == null ? null : formulario.getFile("foto");
    }

    // ---------- Respuestas ----------

    /** Datos que usan todas las pantallas: el usuario actual, las opciones de los selects y el aviso pendiente. */
    private static Map<String, Object> modelo(ServerRequest peticion, Usuario yo) {
        Map<String, Object> modelo = new HashMap<>();
        Map<String, ?> avisos = RequestContextUtils.getInputFlashMap(peticion.servletRequest());
        if (avisos != null) {
            modelo.putAll(avisos);
        }
        modelo.put("yo", yo);
        modelo.put("tipos", TipoObjeto.values());
        modelo.put("categorias", CategoriaObjeto.values());
        modelo.put("pabellones", Pabellon.values());
        modelo.put("estados", EstadoObjeto.values());
        return modelo;
    }

    private static ServerResponse redirigir(ServerRequest peticion, String destino, String mensaje) {
        if (mensaje != null) {
            RequestContextUtils.getOutputFlashMap(peticion.servletRequest()).put("mensaje", mensaje);
        }
        return ServerResponse.status(HttpStatus.FOUND).render("redirect:" + destino);
    }
}
