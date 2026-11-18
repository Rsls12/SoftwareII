# U-FOUND

Sistema web de objetos perdidos y encontrados del campus de la Universidad de Lima. Los estudiantes reportan lo que perdieron, el personal de Seguridad registra lo que encuentra y la Oficina de Objetos Perdidos gestiona cada caso hasta entregar el objeto a su dueño. En el Release 2 se agregaron la detección automática de coincidencias, las notificaciones, el análisis de fotos con IA, un chatbot de ayuda y las estadísticas para la Oficina.

- Aplicación publicada: https://ufound.onrender.com
- Tecnologías: Java 17, Spring Boot 4, Spring Security, Thymeleaf, JPA, Supabase (PostgreSQL y Storage), Google Gemini y Render con Docker.
- Proyecto del curso Ingeniería de Software II, Grupo 1 (sección 851), desarrollado con Scrum en dos Releases.

## Equipo

| Integrante | Rol |
|---|---|
| Rodrigo Lucen Samaniego | Product Owner |
| Mateo Ortega Escurra | Scrum Master |
| Rodrigo Arcos Mondragón | Desarrollador |
| César Porras Peña | Desarrollador |
| Jose Luis Misagel Huashuayo | Desarrollador |
| Luis Rudy Reguera Garcia | Desarrollador |
| Mathias Vegas Carbajal | Desarrollador |

## Cómo usarlo

### Roles

| Rol | Qué puede hacer |
|---|---|
| Estudiante | Reportar objetos perdidos con foto, buscar y filtrar objetos, ver sus publicaciones, editar o eliminar sus reportes, recibir notificaciones de coincidencias y consultar al chatbot |
| Seguridad | Registrar objetos encontrados con foto, buscar objetos y ver su detalle |
| Oficina de Objetos Perdidos | Cambiar el estado de los objetos, registrar entregas, revisar coincidencias y ver el dashboard, las estadísticas y el historial de entregas |

### Entrar al sistema

1. Abrir https://ufound.onrender.com/login. En el plan gratuito de Render el servidor se apaga tras unos minutos sin visitas, así que la primera carga puede tardar alrededor de un minuto.
2. Elegir el acceso: **Usuarios** (estudiantes y Seguridad) u **Oficina de objetos perdidos**.
3. Ingresar el correo y la contraseña. Un estudiante nuevo puede crear su cuenta desde el enlace de registro (`/registro`).

Cada rol ve un inicio distinto, con las opciones que le corresponden.

### Pantallas principales

| Ruta | Pantalla | Rol |
|---|---|---|
| `/` | Inicio según el rol | Todos |
| `/objetos/nuevo` | Reportar un objeto perdido o encontrado, con foto y autocompletado con IA | Estudiante y Seguridad |
| `/objetos/{id}` | Detalle del objeto, estado, historial y coincidencias | Todos |
| `/buscar` | Búsqueda y filtros por texto, tipo, categoría, ubicación y estado | Todos |
| `/recientes` | Objetos reportados recientemente | Todos |
| `/mis-publicaciones` | Reportes propios, con edición y eliminación | Estudiante |
| `/notificaciones` | Avisos de posibles coincidencias | Estudiante |
| `/coincidencias` | Coincidencias ordenadas por nivel de similitud | Oficina |
| `/dashboard` | Resumen de casos y casos sin movimiento | Oficina |
| `/estadisticas` | Estadísticas por estado, categoría, mes y ubicación | Oficina |
| `/entregas` | Historial de entregas | Oficina |

### Correrlo en local

Se necesita el archivo `backend/.env` con las credenciales de Supabase. Los nombres de las variables están en `backend/.env.example`.

```
cd backend
./mvnw spring-boot:run        (Windows: mvnw.cmd spring-boot:run)
```

Después abrir http://localhost:8080/login. Es una sola aplicación: `backend/` la compila y la levanta, y al hacerlo empaqueta las vistas y los estilos de `frontend/`.

Los usuarios y los objetos están en la base de datos de Supabase. Cada usuario tiene su contraseña guardada como hash BCrypt en `usuario.contrasena`.

El análisis de fotos y el chatbot usan Google Gemini y se activan con `GEMINI_API_KEY` y `CHATBOT_API_KEY`. Sin esas claves la aplicación funciona igual, sin esas dos funciones.

### Pruebas

```
cd backend
./mvnw test        (Windows: mvnw.cmd test)
```

Las pruebas usan una base H2 en memoria, un Supabase Storage falso y sus propios datos (`src/test/java/pe/edu/ufound/DatosDePrueba`), así que no tocan Supabase ni llaman a Gemini.

### Despliegue

Se despliega en Render con Docker (`Dockerfile` y `render.yaml`). Cada push a `main` vuelve a desplegar la aplicación. Los pasos están en [DESPLIEGUE.md](DESPLIEGUE.md).

## Lo realizado en cada Sprint

### Release 1, Sprint 1 (16/09/2026 al 11/10/2026)

Objetivo: entregar el MVP con acceso por rol, registro y catálogo de objetos, y gestión de estados e historiales. Se completaron las 22 historias comprometidas (48 puntos).

| HU | Historia | Lo realizado |
|---|---|---|
| HU-01 | Iniciar sesión en el sistema | Login con Spring Security y contraseñas cifradas con BCrypt |
| HU-02 | Cerrar sesión | Cierre de sesión que vuelve al login |
| HU-03 | Visualizar home según rol | Inicio distinto para Estudiante, Seguridad y Oficina, con rutas protegidas por rol |
| HU-04 | Registrar objeto perdido | El estudiante reporta un objeto perdido y queda guardado en Supabase |
| HU-05 | Registrar categoría del objeto | Categoría obligatoria en el reporte |
| HU-06 | Registrar ubicación del objeto | Pabellón donde se perdió o se encontró el objeto |
| HU-07 | Registrar fecha y hora del reporte | Fecha y hora automáticas al guardar el reporte |
| HU-08 | Registrar descripción física | Descripción validada de 10 a 500 caracteres |
| HU-09 | Subir foto perdido | Foto del objeto perdido en Supabase Storage, con validación del tipo real del archivo |
| HU-10 | Registrar objeto encontrado | Seguridad registra los objetos que encuentra |
| HU-11 | Subir foto del objeto encontrado | Foto del objeto encontrado en Supabase Storage |
| HU-12 | Ver detalle de objeto | Pantalla de detalle con todos los datos del reporte |
| HU-13 | Mostrar estado del objeto | Estado visible en el detalle y en los listados |
| HU-14 | Cambiar estado del objeto | La Oficina cambia el estado y registra la entrega con los datos de quien reclama |
| HU-15 | Historial de estado del objeto | Línea de tiempo con cada cambio de estado, quién lo hizo y cuándo |
| HU-16 | Buscar objetos | Búsqueda por texto en el nombre y la descripción |
| HU-17 | Filtrar búsqueda | Filtros por tipo, categoría, ubicación y estado |
| HU-18 | Historial publicaciones | El estudiante ve todos sus reportes |
| HU-19 | Objetos recientes | Listado de los últimos objetos reportados |
| HU-20 | Eliminar reportes | Solo el estudiante dueño puede eliminar su reporte |
| HU-21 | Editar reportes | Solo el dueño puede editar su reporte mientras siga en estado Reportado |
| HU-22 | Historial de entregas | La Oficina consulta los objetos entregados y a quién se entregaron |

Además, el código se organizó en `backend/` y `frontend/`, y la lógica de negocio quedó en los objetos del modelo, sin services ni controllers.

### Release 2, Sprint 2 (14/10/2026 al 15/11/2026)

Objetivo: agregar inteligencia al sistema con coincidencias automáticas, notificaciones, IA para el registro, chatbot de ayuda y estadísticas para la Oficina. Se completaron las 18 historias comprometidas (54 puntos).

| HU | Historia | Lo realizado |
|---|---|---|
| HU-23 | Detectar coincidencias | Puntaje de 0 a 100 entre objetos perdidos y encontrados según categoría, lugar, fechas y palabras en común |
| HU-24 | Evaluar coincidencia | Nivel ALTO, MEDIO o BAJO según el puntaje |
| HU-25 | Generar notificación | Aviso al estudiante cuando aparece una coincidencia relevante con su objeto |
| HU-26 | Ver notificaciones | Pantalla de notificaciones del estudiante |
| HU-27 | Marcar notificación como leída | El estudiante marca sus avisos como leídos |
| HU-28 | Dashboard administrativo de Oficina | Resumen de casos para la Oficina, con los casos sin movimiento por más de 7 días |
| HU-29 | Interactuar con chatbot de ayuda | Chatbot con Google Gemini para los estudiantes |
| HU-30 | Consultar dudas sobre uso del sistema | El chatbot responde dudas sobre cómo usar U-FOUND |
| HU-31 | Recibir guía automática en el registro de objetos | Cubierta por el registro asistido con IA de las HU-33 y HU-35 |
| HU-32 | Subir imagen para análisis automático del objeto | Vista previa de la foto antes de enviarla al análisis |
| HU-33 | Detectar características del objeto mediante IA | Gemini reconoce en la foto el tipo de objeto, los colores, la marca y otros rasgos |
| HU-34 | Sugerir categoría automática del objeto | Categoría sugerida a partir del análisis de la foto |
| HU-35 | Autocompletar formulario de registro de objeto | El formulario se llena con lo que reconoce la IA |
| HU-36 | Priorizar coincidencias por nivel de similitud | Coincidencias ordenadas de mayor a menor similitud |
| HU-37 | Sugerir posibles coincidencias automáticamente al usuario | Al registrar un objeto se muestran sus posibles coincidencias |
| HU-38 | Visualizar estadísticas inteligentes de objetos perdidos | Estadísticas por estado, categoría y mes, con filtro de fechas |
| HU-39 | Identificar categorías más frecuentes mediante análisis | Ranking de las categorías con más reportes en el inicio de la Oficina |
| HU-40 | Mostrar tendencias de pérdida por ubicación | Ranking de pabellones con más objetos perdidos |

Además, la aplicación se publicó en Render, el login pasó a tener un acceso para usuarios y otro para la Oficina, los estudiantes pueden crear su propia cuenta y las claves salieron del código a un archivo `.env`.

## Estructura del proyecto

**`backend/`**: proyecto Spring Boot (Java). Los paquetes están en `backend/src/main/java/pe/edu/ufound/`.

| Carpeta | Qué hay |
|---|---|
| `model/` | Los objetos del sistema y sus reglas de negocio: `Usuario`, `Objeto`, `HistorialEstado`, `Entrega`, `Coincidencia`, `Notificacion`, `CaracteristicasObjeto`, `Estadistica`, `FrecuenciaCategoria`, `FrecuenciaUbicacion` y los enums |
| `repositorio/` | Interfaces Spring Data JPA que guardan y buscan los objetos en Supabase (PostgreSQL) |
| `almacenamiento/` | `AlmacenamientoImagenes` (interfaz) y `AlmacenamientoSupabase`, que sube las fotos a Supabase Storage |
| `reconocimiento/` | `ReconocimientoImagenes` (interfaz) y `ReconocimientoGemini`, que analiza la foto con Gemini; `RespuestaGemini` convierte su respuesta en `CaracteristicasObjeto` |
| `chatbot/` | `ChatbotGemini`, que responde las preguntas de los estudiantes |
| `seguridad/` | `SeguridadConfig`: login con BCrypt, dos accesos, logout y rutas por rol |
| `web/` | `RutasWeb`: une cada ruta con el objeto que la atiende y con su vista, sin reglas de negocio |
| `src/main/resources/` | `application.properties` con la configuración de la aplicación |
| `src/test/` | Pruebas automatizadas con JUnit 5 |
| `pom.xml` | Dependencias del proyecto. También indica que las vistas y los estilos se toman de `../frontend` |

**`frontend/`**: lo que ve el usuario. No tiene Java.

| Carpeta | Qué hay |
|---|---|
| `templates/` | Vistas Thymeleaf. `fragmentos.html` tiene las piezas reutilizables |
| `static/css/` | `tokens.css` (colores y tamaños) y `app.css` (estilos) |
| `docs/` | Dirección visual de la interfaz |

## Decisiones de diseño

- Sin services ni controllers: la lógica vive en los objetos de `model/`. Por ejemplo, `Objeto` sabe reportarse, cambiar de estado, entregarse, editarse y calcular su puntaje de coincidencia con otro objeto.
- `Objeto.cambiarEstado(estado, observacion, usuario)` crea su `HistorialEstado`, y `Objeto.entregar(...)` crea su `Entrega` y cierra el caso: un objeto entregado ya no cambia de estado.
- Las categorías, ubicaciones, tipos y estados se guardan como texto, sin tabla propia.
- Los formularios usan `th:action`, que agrega el token CSRF que pide Spring Security.
