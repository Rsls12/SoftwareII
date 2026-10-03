# Pendientes para el Nivel 2

- Contador de caracteres en vivo para la descripción (10–500).
- Errores junto a cada campo en vez de los globos del navegador (requiere validación en el servidor).
- `mis-publicaciones.html`: el título salta de h1 a h3.
- Historial de estados: cambiar la flecha "→" por "cambió a" para lectores de pantalla.
- "Ver todos" y "Limpiar filtros" miden menos de 44px de alto en móvil; falta un estado `:active` al tocar.
- Búsqueda vacía: decir "con esos filtros" solo cuando se filtró algo.
- Navegación de staff por debajo de 1024px: definir si se pasa a `<details>` o se mantiene la barra inferior.

## Para backend (no es frontend)
- ~~Restringir rutas por rol~~: hecho en `SeguridadConfig`. Falta una página propia para el 403 (hoy sale la de error por defecto).
- ¿Seguridad puede editar o eliminar los objetos encontrados que registró? Hoy ve esos botones.
