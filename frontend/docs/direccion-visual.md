# U-FOUND: dirección visual (Nivel 1)

Público: estudiantes (móvil) y personal de seguridad y oficina (escritorio). Tono: confiable, tipo servicio público. Sin animaciones en el Nivel 1.

## 1. Concepto

**"La ficha del mostrador, sellada en tinta."** Cada objeto es una ficha de consigna sobre papel piedra, con texto en tinta profunda y un sello de estado. Un único acento azul tinta (el color del sello) y estados en tonos apagados que armonizan entre sí, no un semáforo.
Encaja porque el estudiante llega con prisa y nervios: necesita leer rápido "dónde está y en qué estado". Seguridad y Oficina escanean listas largas, y unos tonos apagados cansan menos la vista que unos saturados.

## 2. Paleta

| Rol | Hex | Uso | Contraste |
|---|---|---|---|
| Fondo (piedra) | `#F4F3F1` | página | - |
| Superficie | `#FDFDFC` | tarjetas, formularios | - |
| Texto (tinta) | `#1C2230` | cuerpo y títulos | 14.3:1 |
| Texto secundario | `#625F5A` | metadatos, ayudas | 5.7:1 |
| Borde | `#E2DFDA` | divisores | decorativo |
| Borde de control | `#8E8A84` | inputs, selects | 3.4:1 (no texto, AA) |
| Acento (azul tinta) | `#2A3F7E` | botón primario, enlaces, foco | 9.8:1 con superficie |
| Acento hover | `#1F2F60` | hover/active | - |
| Acento suave | `#E6E9F2` | nav activa, filtro activo | 8.2:1 con acento |
| Error | `#A3321F` | mensajes de error | 6.8:1 |

### Estados (`EstadoObjeto`)

Insignia = fondo apagado + texto oscuro del mismo tono + **siempre la etiqueta en texto** (nunca solo color). Todos pasan AA (4.5:1) con texto de 13px en negrita.

| Estado | Texto visible | Tono | Texto | Fondo | Contraste |
|---|---|---|---|---|---|
| REPORTADO | Reportado | arcilla | `#8A3B2B` | `#F5E6E1` | 6.3:1 |
| EN_REVISION | En revisión | ocre | `#6B5212` | `#F3EBD6` | 6.2:1 |
| COINCIDENCIA_DETECTADA | Coincidencia | ciruela | `#5E3A6E` | `#EEE6F1` | 7.5:1 |
| DISPONIBLE_EN_OFICINA | En oficina | salvia | `#365A41` | `#E3EDE4` | 6.5:1 |
| ENTREGADO | Entregado | tinta (sólido: caso cerrado) | `#F4F3F1` | `#1C2230` | 14.3:1 |
| ARCHIVADO | Archivado | piedra (borde punteado) | `#5E5B57` | `#ECEAE7` | 5.6:1 |

Sin modo oscuro en Nivel 1 (tema claro fijo); los tokens ya están separados para añadirlo después.

## 3. Tipografía

- **Atkinson Hyperlegible Next** (Google Fonts, 400/700): diseñada para legibilidad, distingue bien `0/O`, `1/l/I`. Ideal para códigos y nombres de objetos.
- **Atkinson Hyperlegible Mono** solo para códigos de objeto y fechas en tablas.
- Respaldo: `system-ui, sans-serif`.

Escala (base 16px, razón ~1.2, en `rem`):

| Token | Tamaño | Uso |
|---|---|---|
| `--fs-xs` | 0.8125rem (13px) | insignias, metadatos |
| `--fs-sm` | 0.875rem (14px) | etiquetas de formulario, tablas |
| `--fs-base` | 1rem (16px) | cuerpo, inputs (evita zoom en iOS) |
| `--fs-lg` | 1.25rem (20px) | título de tarjeta, h3 |
| `--fs-xl` | 1.5rem (24px) | título de pantalla, h2 |
| `--fs-2xl` | 1.875rem (30px) | h1 (solo login/home) |

Interlineado 1.5 cuerpo, 1.2 títulos. Pesos: 400 y 700, nada más.

## 4. Espaciado, radios, sombras

- Espaciado en escala de 4px: 4, 8, 12, 16, 24, 32, 48. Padding de página 16px móvil / 32px escritorio. Ancho máximo de contenido 1200px.
- Objetivo táctil mínimo 44px de alto.
- Radios (regla única): controles y botones 8px, tarjetas 12px, insignias 999px (píldora).
- Sombras: solo una, sutil y teñida, para tarjetas sobre el fondo gris. Sin sombras en botones. Foco: anillo de 3px color acento con 2px de separación.

## 5. Bloque para `static/css/tokens.css`

```css
:root {
  /* Color base */
  --c-bg: #F4F3F1;
  --c-surface: #FDFDFC;
  --c-text: #1C2230;
  --c-text-muted: #625F5A;
  --c-border: #E2DFDA;
  --c-border-strong: #8E8A84;
  --c-accent: #2A3F7E;
  --c-accent-hover: #1F2F60;
  --c-accent-soft: #E6E9F2;
  --c-on-accent: #FDFDFC;
  --c-error: #A3321F;

  /* Estados de objeto: texto / fondo */
  --st-reportado-fg: #8A3B2B;    --st-reportado-bg: #F5E6E1;
  --st-revision-fg: #6B5212;     --st-revision-bg: #F3EBD6;
  --st-coincidencia-fg: #5E3A6E; --st-coincidencia-bg: #EEE6F1;
  --st-oficina-fg: #365A41;      --st-oficina-bg: #E3EDE4;
  --st-entregado-fg: #F4F3F1;    --st-entregado-bg: #1C2230;
  --st-archivado-fg: #5E5B57;    --st-archivado-bg: #ECEAE7;

  /* Tipografía */
  --font-sans: "Atkinson Hyperlegible Next", system-ui, sans-serif;
  --font-mono: "Atkinson Hyperlegible Mono", ui-monospace, monospace;
  --fs-xs: 0.8125rem;
  --fs-sm: 0.875rem;
  --fs-base: 1rem;
  --fs-lg: 1.25rem;
  --fs-xl: 1.5rem;
  --fs-2xl: 1.875rem;
  --lh-body: 1.5;
  --lh-tight: 1.2;
  --fw-regular: 400;
  --fw-bold: 700;

  /* Espaciado */
  --sp-1: 0.25rem;
  --sp-2: 0.5rem;
  --sp-3: 0.75rem;
  --sp-4: 1rem;
  --sp-6: 1.5rem;
  --sp-8: 2rem;
  --sp-12: 3rem;
  --tap-min: 44px;
  --content-max: 1200px;

  /* Forma */
  --radius-control: 8px;
  --radius-card: 12px;
  --radius-pill: 999px;
  --shadow-card: 0 1px 2px rgb(28 34 48 / 0.06), 0 2px 8px rgb(28 34 48 / 0.06);
  --focus-ring: 0 0 0 2px var(--c-surface), 0 0 0 5px var(--c-accent);
}
```

## 6. Tarjeta de objeto y navegación

**Tarjeta (fragmento `objeto-card`).** Superficie `#FDFDFC`, radio 12px, sombra sutil. Arriba a la izquierda la insignia de estado; a la derecha el código del objeto en mono (`UF-0142`). Título del objeto en 20px negrita. Debajo una línea de metadatos en texto secundario: categoría, ubicación (Pabellón C) y fecha. Si hay foto, miniatura cuadrada de 72px a la izquierda; si no, un bloque piedra (`--c-bg`) con el ícono de la categoría. Toda la tarjeta es un único enlace al detalle (sin botones internos). En móvil: lista de una columna; en escritorio: grilla de 2-3 columnas para estudiantes y **tabla** para Seguridad/Oficina (estado, código, objeto, ubicación, fecha, acción).

**Navegación móvil (estudiante).** Barra superior fina con "U-FOUND" y el usuario. Barra inferior fija con 4 destinos de icono + texto: Inicio, Buscar, Reportar (acento), Mis publicaciones. Ítem activo con fondo acento suave y `aria-current="page"`.

**Navegación escritorio (Seguridad / Oficina).** Barra lateral izquierda fija de 240px con los destinos del rol (Inicio, Objetos, Cambiar estado, Historial de entregas) y el rol visible arriba. Contenido a la derecha con título de pantalla y filtros en una fila sobre la tabla. Por debajo de 1024px la barra lateral pasa a la barra superior con menú `<details>` nativo, sin JS.
