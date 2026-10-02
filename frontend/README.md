# U-FOUND

Sistema de objetos perdidos y encontrados del campus. Spring Boot 4 + Thymeleaf, Java 17.

## Cómo correrlo

```
./mvnw spring-boot:run        (Windows: mvnw.cmd spring-boot:run)
```

Abrir http://localhost:8080/login. En el Nivel 1 no hay login real: usa los enlaces de "Vista previa"
para entrar como Estudiante, Seguridad u Oficina.

## Estructura

| Carpeta | Qué hay |
|---|---|
| `model/` | Clases y enums del diagrama UML (Objeto, Usuario, EstadoObjeto...) |
| `servicio/` | **MOCK**: datos en memoria. El backend cambia las listas por repositorios JPA |
| `controlador/PantallaController` | Abre cada pantalla. No tiene reglas de negocio |
| `templates/` | Vistas Thymeleaf. `fragmentos.html` tiene las piezas reutilizables |
| `static/css/` | `tokens.css` (colores, tamaños) y `app.css` (estilos) |

## Para el equipo de backend

Todo lo que dice `MOCK` en el código es temporal:

1. `ObjetoService` y `UsuarioService`: mantener los métodos públicos, cambiar las listas por JPA.
2. Login/logout: Spring Security con form login en `/login` (campos `username` y `password`) y `POST /logout`.
3. Borrar `/vista-previa/{rol}` del controlador y el bloque "Vista previa" de `login.html`.
4. Fotos: hoy se guardan como data URL en memoria (`aImagenUrl` del controlador).
5. Permisos por rol (quién puede editar, cambiar estado, ver entregas): hoy la vista solo oculta botones.

## Niveles de la interfaz

- **Nivel 1 (Sprint 1):** funcional, limpio, mobile-first y accesible. ← actual
- **Nivel 2:** pulido de UX, estados vacíos y de error.
- **Nivel 3:** animaciones y detalles finos.
