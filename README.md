# U-FOUND

Sistema de objetos perdidos y encontrados del campus. Spring Boot 4 + Thymeleaf, Java 17.

## Cómo correrlo

```
cd backend
./mvnw spring-boot:run        (Windows: mvnw.cmd spring-boot:run)
```

Es una sola aplicación: `backend/` la compila y la levanta, y al hacerlo empaqueta las vistas y los estilos de `frontend/`.

Abrir http://localhost:8080/login e iniciar sesión con uno de los usuarios de ejemplo. Cada uno ve un inicio distinto:

| Rol | Correo |
|---|---|
| Estudiante | `ana.torres@universidad.edu.pe` |
| Seguridad | `jorge.quispe@universidad.edu.pe` |
| Oficina | `lucia.paredes@universidad.edu.pe` |

Los tres usan la clave de `ufound.mock.clave` (en `application.properties`) mientras no haya base de datos.

## Estructura

**`backend/`**: proyecto Spring Boot (Java). Los paquetes están en `backend/src/main/java/pe/edu/ufound/`.

| Carpeta | Qué hay |
|---|---|
| `model/` | Clases y enums del diagrama UML (Objeto, Usuario, EstadoObjeto...) |
| `servicio/` | **MOCK**: datos en memoria. El backend cambia las listas por repositorios JPA |
| `seguridad/` | Spring Security: login, logout y rutas por rol (`SeguridadConfig`), y el puente con los usuarios (`UsuarioDetailsService`) |
| `controlador/PantallaController` | Abre cada pantalla. No tiene reglas de negocio |
| `controlador/SesionController` | Muestra el login. El POST `/login` y el POST `/logout` los procesa Spring Security |
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

Todo lo que dice `MOCK` en el código es temporal:

1. `ObjetoService` y `UsuarioService`: mantener los métodos públicos, cambiar las listas por JPA.
2. Contraseñas: hoy los usuarios de ejemplo comparten `ufound.mock.clave`. Con la BD, `UsuarioDetailsService` lee el hash BCrypt de cada usuario.
3. Fotos: hoy se guardan como data URL en memoria (`aImagenUrl` del controlador).
4. Permisos sobre un objeto concreto (solo el dueño edita o elimina su reporte): hoy la vista solo oculta botones.

Sprint 1, HU-04, HU-05 y HU-09 (Rodrigo Arcos):

- `Objeto` y `Usuario` ya son entidades JPA (tablas `objeto` y `usuario`). Base H2 embebida mientras no haya servidor de BD.
- `categoria`, `ubicacion`, `tipo` y `estado` se guardan como varchar, sin tabla propia (nota del diagrama). `GET /api/categorias` las lista.
- `POST /objetos` guarda el objeto en BD; el estudiante registra PERDIDO y seguridad ENCONTRADO.
- Fotos: `AlmacenamientoImagenes` (interfaz) con `AlmacenamientoLocal` en `backend/uploads/imagenes`, servidas en `/imagenes/**`. Se valida el tipo real del archivo (JPG, PNG, GIF, WEBP).
- `HistorialEstado` y `Entrega` siguen en memoria (HU de otros integrantes).

Ya está hecho (Sprint 1, HU-01 a HU-03): login y logout con Spring Security, sesión por usuario y rutas por rol
(`/entregas` y cambiar estado solo Oficina; registrar objeto solo Estudiante y Seguridad). Los formularios con
`th:action` ya envían el token CSRF solos; un formulario nuevo con `action` a secas daría 403.

## Niveles de la interfaz

- **Nivel 1 (Sprint 1):** funcional, limpio, mobile-first y accesible. ← actual
- **Nivel 2:** pulido de UX, estados vacíos y de error.
- **Nivel 3:** animaciones y detalles finos.
