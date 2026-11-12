# Despliegue de UFound en Render

UFound es una app Spring Boot (Java) que se queda corriendo como servidor, así que no se puede subir a Vercel.
Usamos **Render** (plan gratis) con Docker. La base de datos y las fotos siguen en Supabase.

## Qué hay en el repo

| Archivo | Para qué |
|---|---|
| `Dockerfile` | Compila el `.jar` con Maven y lo corre con Java 17. Se construye desde la raíz porque el `pom.xml` empaqueta `../frontend`. |
| `.dockerignore` | Deja fuera `.env`, `target/` y `.git` para que ningún secreto entre en la imagen. |
| `render.yaml` | Blueprint de Render: servicio web Docker, plan free, health check en `/login`, variables de entorno. |
| `application.properties` | `server.port=${PORT:8080}` (Render asigna el puerto) y `server.forward-headers-strategy=framework` (para que los redirects usen https detrás del proxy). |

## Pasos (una sola vez)

1. **Subir los cambios a GitHub**: hacer push de `main` (por ejemplo desde GitHub Desktop).
2. **Crear cuenta en Render**: https://render.com → *Get Started* → entrar con GitHub.
3. **Crear el servicio desde el Blueprint**: *New* → *Blueprint* → elegir el repo `Rsls12/SoftwareII` → Render lee `render.yaml`.
4. **Llenar las variables secretas** que Render pide (los mismos valores que `backend/.env`):
   - `DB_URL`: `jdbc:postgresql://<host-del-pooler>:5432/postgres?sslmode=require`
   - `DB_USERNAME`
   - `DB_PASSWORD`
   - `SUPABASE_URL`
   - `SUPABASE_KEY`
   - `GEMINI_API_KEY` (opcional, HU-33): clave de Google AI Studio (https://aistudio.google.com, gratis y sin tarjeta) para que Gemini reconozca las características de las fotos. El plan gratuito tiene un límite de peticiones por minuto y por día, y Google puede usar lo enviado para mejorar sus productos. Si se deja vacía, la app funciona igual, sin ese análisis.
5. **Aplicar** (*Apply*). El primer build tarda entre 5 y 10 minutos. Al terminar, Render muestra la URL (`https://ufound-xxxx.onrender.com`).
6. Abrir `https://<tu-url>/login` y entrar con un usuario de la tabla `usuario` de Supabase.

Después de esto, cada push a `main` vuelve a desplegar solo.

> **Importante (DB_URL):** usar la cadena del **pooler** de Supabase (*Session pooler*, puerto 5432), no la conexión directa `db.<id>.supabase.co`, porque esa solo funciona con IPv6 y Render usa IPv4.

## Antes de presentar

- **Abrir la URL 1–2 minutos antes.** En el plan gratis, Render apaga el servidor tras unos 15 minutos sin visitas y tarda alrededor de 1 minuto en despertar.
- **Supabase gratis pausa el proyecto tras 7 días sin uso.** Si no se ha usado en una semana, entrar a https://supabase.com/dashboard y darle *Restore*.
- Tener a mano un usuario de cada rol (Estudiante, Seguridad, Oficina) y algunos objetos cargados.

## Si algo falla

- *Logs* del servicio en Render: buscar `APPLICATION FAILED TO START`.
- `Connection refused` o `UnknownHost` con la base: revisar `DB_URL` (usar el pooler) y que el proyecto de Supabase no esté pausado.
- Se queda sin memoria (`OutOfMemoryError` o reinicios): bajar `-Xmx` en `JAVA_OPTS` del `Dockerfile`, o pasar al plan Starter.
