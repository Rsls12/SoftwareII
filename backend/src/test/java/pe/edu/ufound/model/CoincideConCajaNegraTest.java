package pe.edu.ufound.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sección 10.3.7 - Prueba de caja negra (partición equivalente y valores límite) de Mathias Vegas
 * sobre Objeto.coincideCon() (HU-16 y HU-17).
 */
class CoincideConCajaNegraTest {

    private Usuario ana;
    private Usuario oficina;
    private Objeto laptop;

    @BeforeEach
    void prepararObjetoBase() {
        ana = new Usuario("Ana", "Torres", "ana.torres@universidad.edu.pe", "hash", RolUsuario.ESTUDIANTE);
        oficina = new Usuario("Lucía", "Paredes", "lucia.paredes@universidad.edu.pe", "hash",
                RolUsuario.OFICINA_OBJETOS_PERDIDOS);
        laptop = new Objeto("Laptop Lenovo ThinkPad gris",
                "Con sticker de la NASA en la tapa y cargador negro.",
                TipoObjeto.PERDIDO, CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_C,
                null, LocalDateTime.now(), ana);
        laptop.cambiarEstado(EstadoObjeto.EN_REVISION, "En revisión por la oficina", oficina);
    }

    @Test
    @DisplayName("CN-01: todos los filtros nulos (V1, V4, V6, V8, V10) -> devuelve true")
    void cn01SinFiltrosCoincide() {
        assertTrue(laptop.coincideCon(null, null, null, null, null));
    }

    @Test
    @DisplayName("CN-02: palabra en nombre en mayúsculas y todos los filtros exactos (V2, V5, V7, V9, V11) -> true")
    void cn02CoincidenciaCompletaEnNombreYFiltros() {
        assertTrue(laptop.coincideCon("LENOVO", TipoObjeto.PERDIDO, CategoriaObjeto.TECNOLOGIA,
                Pabellon.PABELLON_C, EstadoObjeto.EN_REVISION));
    }

    @Test
    @DisplayName("CN-03: frase contenida en la descripción con espacios laterales (V3, V7) -> true")
    void cn03CoincidenciaEnDescripcion() {
        assertTrue(laptop.coincideCon("  sticker de la nasa  ", null, CategoriaObjeto.TECNOLOGIA, null, null));
    }

    @Test
    @DisplayName("CN-04: valor límite inferior de texto vacío '' (0 caracteres, V1) -> true")
    void cn04LimiteTextoVacio() {
        assertTrue(laptop.coincideCon("", TipoObjeto.PERDIDO, null, null, null));
    }

    @Test
    @DisplayName("CN-05: valor límite de 1 carácter presente en el nombre ('L', V2) -> true")
    void cn05LimiteUnCaracter() {
        assertTrue(laptop.coincideCon("L", null, null, null, null));
    }

    @Test
    @DisplayName("CN-06: valor límite con el largo exacto del nombre completo (27 caracteres, V2) -> true")
    void cn06LimiteNombreCompletoExacto() {
        assertTrue(laptop.coincideCon("Laptop Lenovo ThinkPad gris", null, null, null, null));
    }

    @Test
    @DisplayName("CN-07: valor límite con 1 carácter adicional sobre el nombre completo (NV1) -> false")
    void cn07LimiteUnCaracterSobreElNombre() {
        assertFalse(laptop.coincideCon("Laptop Lenovo ThinkPad grisX", null, null, null, null));
    }

    @Test
    @DisplayName("CN-08: tipo distinto al del objeto (NV2) -> false")
    void cn08TipoDistintoNoCoincide() {
        assertFalse(laptop.coincideCon("Lenovo", TipoObjeto.ENCONTRADO, CategoriaObjeto.TECNOLOGIA,
                Pabellon.PABELLON_C, EstadoObjeto.EN_REVISION));
    }

    @Test
    @DisplayName("CN-09: categoría distinta a la del objeto (NV3) -> false")
    void cn09CategoriaDistintaNoCoincide() {
        assertFalse(laptop.coincideCon("Lenovo", TipoObjeto.PERDIDO, CategoriaObjeto.BOTELLAS,
                Pabellon.PABELLON_C, EstadoObjeto.EN_REVISION));
    }

    @Test
    @DisplayName("CN-10: ubicación y estado distintos a los del objeto (NV4, NV5) -> false")
    void cn10UbicacionYEstadoDistintosNoCoinciden() {
        assertFalse(laptop.coincideCon("Lenovo", TipoObjeto.PERDIDO, CategoriaObjeto.TECNOLOGIA,
                Pabellon.PABELLON_A, EstadoObjeto.REPORTADO));
    }
}
