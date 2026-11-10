package pe.edu.ufound.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** HU-23: reglas para decidir si un objeto perdido y uno encontrado podrían ser el mismo. */
class CoincidenciaTest {

    private static final LocalDateTime HOY = LocalDateTime.of(2026, 11, 10, 10, 0);

    private final Usuario estudiante = new Usuario("Ana", "Torres", "ana@u.pe", null, RolUsuario.ESTUDIANTE);
    private final Usuario seguridad = new Usuario("Jorge", "Quispe", "jorge@u.pe", null, RolUsuario.SEGURIDAD);
    private final Usuario oficina = new Usuario("Lucía", "Paredes", "lucia@u.pe", null,
            RolUsuario.OFICINA_OBJETOS_PERDIDOS);

    @Test
    void mismaLaptopPerdidaYEncontradaCoincideConPuntajeAlto() {
        Objeto perdida = perdido("Laptop Lenovo gris", "Laptop Lenovo con sticker de la NASA",
                CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_C, HOY);
        Objeto encontrada = encontrado("Laptop gris", "Lenovo gris con sticker NASA en la tapa",
                CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_C, HOY.plusHours(5));

        List<Coincidencia> detectadas = encontrada.detectarCoincidencias(List.of(perdida));

        assertEquals(1, detectadas.size());
        assertSame(perdida, detectadas.get(0).getObjetoPerdido());
        assertSame(encontrada, detectadas.get(0).getObjetoEncontrado());
        assertEquals(100, detectadas.get(0).getPuntaje());
        assertEquals(100, perdida.puntajeDeCoincidenciaCon(encontrada));
    }

    @Test
    void soloLaCategoriaYLaFechaNoAlcanzan() {
        Objeto perdida = perdido("Laptop HP", "Laptop plateada con funda azul",
                CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_A, HOY);
        Objeto celular = encontrado("Celular Samsung", "Samsung negro con la pantalla rota",
                CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_E, HOY.plusDays(1));

        assertEquals(55, perdida.puntajeDeCoincidenciaCon(celular));
        assertTrue(perdida.detectarCoincidencias(List.of(celular)).isEmpty());
    }

    @Test
    void distintaCategoriaMismoTipoOMasDeUnMesNoCoinciden() {
        Objeto perdida = perdido("Botella Stanley", "Botella Stanley verde abollada",
                CategoriaObjeto.BOTELLAS, Pabellon.PABELLON_A, HOY);

        assertEquals(0, perdida.puntajeDeCoincidenciaCon(encontrado("Botella Stanley", "Botella Stanley verde",
                CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_A, HOY)));
        assertEquals(0, perdida.puntajeDeCoincidenciaCon(perdido("Botella Stanley", "Botella Stanley verde",
                CategoriaObjeto.BOTELLAS, Pabellon.PABELLON_A, HOY)));
        assertEquals(0, perdida.puntajeDeCoincidenciaCon(encontrado("Botella Stanley", "Botella Stanley verde",
                CategoriaObjeto.BOTELLAS, Pabellon.PABELLON_A, HOY.plusDays(31))));
        assertEquals(0, perdida.puntajeDeCoincidenciaCon(perdida));
    }

    @Test
    void unObjetoEntregadoOArchivadoYaNoCoincide() {
        Objeto perdida = perdido("Llaves", "Tres llaves con llavero de Pikachu",
                CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_A, HOY);
        Objeto entregadas = encontrado("Llaves", "Llavero de Pikachu con tres llaves",
                CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_A, HOY);
        Objeto archivadas = encontrado("Llaves", "Llavero de Pikachu con tres llaves",
                CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_A, HOY);
        entregadas.entregar("María López", "DNI 71234567", null, oficina);
        archivadas.cambiarEstado(EstadoObjeto.ARCHIVADO, null, oficina);

        assertTrue(perdida.detectarCoincidencias(List.of(entregadas, archivadas)).isEmpty());
    }

    @Test
    void lasTildesYMayusculasNoImpidenReconocerPalabrasEnComun() {
        Objeto perdida = perdido("Cuaderno de Cálculo", "Cuaderno ANILLADO de cálculo",
                CategoriaObjeto.UTILES, Pabellon.PABELLON_A, HOY);
        Objeto encontrado = encontrado("cuaderno calculo", "cuaderno anillado",
                CategoriaObjeto.UTILES, Pabellon.PABELLON_E, HOY.plusDays(10));

        // 40 (categoría) + 5 (fecha) + 15 (cuaderno, calculo, anillado)
        assertEquals(60, perdida.puntajeDeCoincidenciaCon(encontrado));
    }

    private Objeto perdido(String nombre, String descripcion, CategoriaObjeto categoria, Pabellon lugar,
                           LocalDateTime fecha) {
        return new Objeto(nombre, descripcion, TipoObjeto.PERDIDO, categoria, lugar, null, fecha, estudiante);
    }

    private Objeto encontrado(String nombre, String descripcion, CategoriaObjeto categoria, Pabellon lugar,
                              LocalDateTime fecha) {
        return new Objeto(nombre, descripcion, TipoObjeto.ENCONTRADO, categoria, lugar, null, fecha, seguridad);
    }
}
