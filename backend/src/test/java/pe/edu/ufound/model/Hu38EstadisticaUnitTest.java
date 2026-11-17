package pe.edu.ufound.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static pe.edu.ufound.model.FabricaPruebas.*;

/** Pruebas unitarias de HU-38. */
class Hu38EstadisticaUnitTest {

    private final Usuario ana = usuario(1, RolUsuario.ESTUDIANTE);

    private Objeto perdido(CategoriaObjeto categoria, LocalDateTime fecha) {
        return objeto("Perdido " + fecha, TipoObjeto.PERDIDO, categoria, fecha, ana);
    }

    private Objeto encontrado(CategoriaObjeto categoria, LocalDateTime fecha) {
        return objeto("Encontrado " + fecha, TipoObjeto.ENCONTRADO, categoria, fecha, ana);
    }

    private static LocalDateTime dia(int anio, int mes, int dia) {
        return LocalDateTime.of(anio, mes, dia, 12, 0);
    }

    @Test
    @DisplayName("porEstado sin objetos: todo en cero")
    void porEstadoVacio() {
        List<Estadistica> barras = Estadistica.porEstado(List.of());

        assertEquals(EstadoObjeto.values().length, barras.size());
        assertTrue(barras.stream().allMatch(b -> b.cantidad() == 0 && b.porcentaje() == 0));
    }

    @Test
    @DisplayName("porEstado con uno y con varios objetos")
    void porEstadoConDatos() {
        Objeto a = perdido(CategoriaObjeto.OTROS, dia(2026, 11, 1));
        Objeto b = conEstado(perdido(CategoriaObjeto.OTROS, dia(2026, 11, 2)), EstadoObjeto.EN_REVISION);
        Objeto c = conEstado(perdido(CategoriaObjeto.OTROS, dia(2026, 11, 3)), EstadoObjeto.EN_REVISION);
        Objeto d = conEstado(perdido(CategoriaObjeto.OTROS, dia(2026, 11, 4)), EstadoObjeto.ENTREGADO);

        List<Estadistica> uno = Estadistica.porEstado(List.of(a));
        assertEquals(new Estadistica("Reportado", 1, 100), uno.get(0));
        assertTrue(uno.subList(1, uno.size()).stream().allMatch(e -> e.cantidad() == 0));

        List<Estadistica> varios = Estadistica.porEstado(List.of(a, b, c, d));
        assertEquals(List.of(
                new Estadistica("Reportado", 1, 25),
                new Estadistica("En revisión", 2, 50),
                new Estadistica("Coincidencia", 0, 0),
                new Estadistica("En oficina", 0, 0),
                new Estadistica("Entregado", 1, 25),
                new Estadistica("Archivado", 0, 0)), varios);
    }

    @Test
    @DisplayName("PU-01: porMes sin objetos")
    void porMesVacio() {
        assertTrue(Estadistica.porMes(List.of()).isEmpty());
    }

    @Test
    @DisplayName("PU-02: porMes agrupa el mismo mes")
    void porMesMismoMes() {
        assertEquals(List.of(new Estadistica("nov 2026", 3, 100)), Estadistica.porMes(List.of(
                perdido(CategoriaObjeto.OTROS, dia(2026, 11, 1)),
                perdido(CategoriaObjeto.OTROS, dia(2026, 11, 15)),
                perdido(CategoriaObjeto.OTROS, dia(2026, 11, 30)))));
    }

    @Test
    @DisplayName("PU-03: porMes ordena los meses")
    void porMesDesordenados() {
        assertEquals(List.of(
                new Estadistica("oct 2026", 1, 33),
                new Estadistica("nov 2026", 1, 33),
                new Estadistica("dic 2026", 1, 33)), Estadistica.porMes(List.of(
                perdido(CategoriaObjeto.OTROS, dia(2026, 12, 1)),
                perdido(CategoriaObjeto.OTROS, dia(2026, 10, 5)),
                perdido(CategoriaObjeto.OTROS, dia(2026, 11, 15)))));
    }

    @Test
    @DisplayName("PU-04: porMes con cambio de año")
    void porMesCambioDeAnio() {
        assertEquals(List.of(
                new Estadistica("dic 2025", 1, 25),
                new Estadistica("ene 2026", 2, 50),
                new Estadistica("oct 2026", 1, 25)), Estadistica.porMes(List.of(
                perdido(CategoriaObjeto.OTROS, dia(2026, 1, 15)),
                perdido(CategoriaObjeto.OTROS, dia(2025, 12, 31)),
                perdido(CategoriaObjeto.OTROS, dia(2026, 1, 2)),
                perdido(CategoriaObjeto.OTROS, dia(2026, 10, 5)))));
    }

    @Test
    @DisplayName("Categorías sin objetos")
    void categoriasVacio() {
        assertTrue(FrecuenciaCategoria.de(List.of()).isEmpty());
    }

    @Test
    @DisplayName("Categorías: perdidos, encontrados y porcentaje")
    void categoriasMezcla() {
        List<Objeto> objetos = List.of(
                perdido(CategoriaObjeto.TECNOLOGIA, dia(2026, 11, 1)),
                encontrado(CategoriaObjeto.TECNOLOGIA, dia(2026, 11, 2)),
                perdido(CategoriaObjeto.TECNOLOGIA, dia(2026, 11, 3)),
                perdido(CategoriaObjeto.ROPA, dia(2026, 11, 4)));

        List<FrecuenciaCategoria> filas = FrecuenciaCategoria.de(objetos);

        assertEquals(new FrecuenciaCategoria(CategoriaObjeto.TECNOLOGIA, 2, 1, 75), filas.get(0));
        assertEquals(3, filas.get(0).total());
        assertEquals(new FrecuenciaCategoria(CategoriaObjeto.ROPA, 1, 0, 25), filas.get(1));
    }

    @Test
    @DisplayName("Categorías empatadas")
    void categoriasEmpate() {
        List<FrecuenciaCategoria> filas = FrecuenciaCategoria.de(List.of(
                perdido(CategoriaObjeto.BOTELLAS, dia(2026, 11, 1)),
                perdido(CategoriaObjeto.TECNOLOGIA, dia(2026, 11, 2)),
                encontrado(CategoriaObjeto.ACCESORIOS, dia(2026, 11, 3))));

        assertEquals(3, filas.size());
        assertTrue(filas.stream().allMatch(f -> f.total() == 1 && f.porcentaje() == 33));
    }

    @Test
    @DisplayName("Categorías: redondeo del porcentaje")
    void categoriasRedondeo() {
        List<Objeto> objetos = new ArrayList<>();
        objetos.add(perdido(CategoriaObjeto.ROPA, dia(2026, 11, 1)));
        for (int i = 0; i < 7; i++) {
            objetos.add(perdido(CategoriaObjeto.TECNOLOGIA, dia(2026, 11, 2 + i)));
        }

        FrecuenciaCategoria ropa = FrecuenciaCategoria.de(objetos).stream()
                .filter(f -> f.categoria() == CategoriaObjeto.ROPA).findFirst().orElseThrow();

        assertEquals(13, ropa.porcentaje());
    }
}
