# U-FOUND

Sistema de objetos perdidos y encontrados del campus. Spring Boot 4 + Thymeleaf, Java 17.

## Cómo correrlo

```
cd backend
./mvnw spring-boot:run        (Windows: mvnw.cmd spring-boot:run)
```

Es una sola aplicación: `backend/` la compila y la levanta, y al hacerlo empaqueta las vistas y los estilos de `frontend/`.

Abrir http://localhost:8080/login e iniciar sesión con el correo y la contraseña de un usuario de la tabla `usuario` de Supabase. Cada rol (Estudiante, Seguridad, Oficina) ve un inicio distinto.

El código no trae usuarios ni objetos de ejemplo: todo sale de la base de datos. Cada usuario tiene su propia contraseña, guardada como hash BCrypt en `usuario.contrasena`. Los usuarios se dan de alta en Supabase, generando el hash con `extensions.crypt('clave', extensions.gen_salt('bf', 10))`.

## Estructura

**`backend/`**: proyecto Spring Boot (Java). Los paquetes están en `backend/src/main/java/pe/edu/ufound/`.

| Carpeta | Qué hay |
|---|---|
| `model/` | Los objetos del sistema y sus reglas: `Usuario`, `Objeto`, `HistorialEstado`, `Entrega` y los enums. Aquí está toda la lógica de negocio |
| `repositorio/` | Interfaces Spring Data JPA que guardan y buscan los objetos en Supabase (PostgreSQL) |
| `almacenamiento/` | `AlmacenamientoImagenes` (interfaz) y `AlmacenamientoSupabase`, que sube las fotos a Supabase Storage |
| `seguridad/SeguridadConfig` | Spring Security: login con el hash BCrypt de cada usuario, logout y rutas por rol |
| `web/RutasWeb` | Entrada HTTP: une cada ruta con el objeto que la atiende y con su vista. No tiene reglas de negocio |
| `src/main/resources/application.properties` | Configuración de la aplicación |
| `src/test/` | Pruebas |
| `pom.xml` | Dependencias. También indica que las vistas y estilos se toman de `../frontend` |

**`frontend/`**: lo que ve el usuario. No tiene Java.

| Carpeta | Qué hay |
|---|---|
| `templates/` | Vistas Thymeleaf. `fragmentos.html` tiene las piezas reutilizables |
| `static/css/` | `tokens.css` (colores, tamaños) y `app.css` (estilos) |
| `docs/` | Dirección visual y pendientes de la interfaz |

## Para el equipo de backend

Pendiente: solo el dueño edita su reporte (HU-21); hoy la vista solo oculta el botón. Eliminar ya lo valida `Objeto.eliminar` (HU-20).

Sin services ni controllers: la lógica vive en los objetos de `model/`.

- No hay clases `@Service` de negocio ni `@Controller`. Lo que hacía `ObjetoService` ahora lo hace `Objeto` (`reportar`, `cambiarEstado`, `coincideCon`, `editar`, `desactivar`) y las consultas están en `ObjetoRepository` (`buscar`, `recientes`).
- `Objeto.cambiarEstado(estado, observacion, usuario)` crea su `HistorialEstado`; el historial es parte del objeto y se guarda con él.
- `HistorialEstado` y `Entrega` ya son entidades JPA (tablas `historial_estado` y `entrega`).
- `Objeto.entregar(nombre, documento, observacion, encargado)` crea su `Entrega` y cierra el caso: un objeto entregado ya no cambia de estado.
- Las pruebas usan H2 en memoria, un Supabase Storage falso y sus propios datos (`src/test/java/pe/edu/ufound/DatosDePrueba`): no tocan Supabase.

Sprint 1, HU-04, HU-05 y HU-09 (Rodrigo Arcos):

- `Objeto` y `Usuario` son entidades JPA (tablas `objeto` y `usuario`) en Supabase (PostgreSQL).
- `categoria`, `ubicacion`, `tipo` y `estado` se guardan como varchar, sin tabla propia (nota del diagrama). `GET /api/categorias` las lista.
- `POST /objetos` guarda el objeto en BD; el estudiante registra PERDIDO y seguridad ENCONTRADO.
- Fotos: `AlmacenamientoImagenes` (interfaz) con `AlmacenamientoSupabase`, que las sube al bucket de Supabase Storage. Se valida el tipo real del archivo (JPG, PNG, GIF, WEBP).

Ya está hecho (Sprint 1, HU-01 a HU-03): login y logout con Spring Security, sesión por usuario y rutas por rol
(`/entregas` y cambiar estado solo Oficina; registrar objeto solo Estudiante y Seguridad). Los formularios con
`th:action` ya envían el token CSRF solos; un formulario nuevo con `action` a secas daría 403.

## Niveles de la interfaz

- **Nivel 1 (Sprint 1):** funcional, limpio, mobile-first y accesible. ← actual
- **Nivel 2:** pulido de UX, estados vacíos y de error.
- **Nivel 3:** animaciones y detalles finos.
