package pe.edu.ufound.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueba unitaria de FrecuenciaCategoria.de() (HU-39): ranking de categorías más frecuentes. */
class CategoriasFrecuentesTest {

    private final Usuario estudiante = new Usuario("Ana", "Torres", "ana@u.pe", null, RolUsuario.ESTUDIANTE);
    private final Usuario seguridad = new Usuario("Jorge", "Quispe", "jorge@u.pe", null, RolUsuario.SEGURIDAD);

    @Test
    @DisplayName("PU-07: sin objetos -> ranking vacío")
    void sinObjetos() {
        assertTrue(FrecuenciaCategoria.de(List.of()).isEmpty());
    }

    @Test
    @DisplayName("PU-08: una sola categoría -> separa perdidos y encontrados, 100 %")
    void unaSolaCategoria() {
        assertEquals(List.of(new FrecuenciaCategoria(CategoriaObjeto.TECNOLOGIA, 2, 1, 100)),
                FrecuenciaCategoria.de(List.of(perdido(CategoriaObjeto.TECNOLOGIA),
                        perdido(CategoriaObjeto.TECNOLOGIA), encontrado(CategoriaObjeto.TECNOLOGIA))));
    }

    @Test
    @DisplayName("PU-09: varias categorías -> de la más frecuente a la menos")
    void ordenaDeMayorAMenor() {
        assertEquals(List.of(new FrecuenciaCategoria(CategoriaObjeto.TECNOLOGIA, 2, 1, 75),
                        new FrecuenciaCategoria(CategoriaObjeto.BOTELLAS, 1, 0, 25)),
                FrecuenciaCategoria.de(List.of(perdido(CategoriaObjeto.BOTELLAS), perdido(CategoriaObjeto.TECNOLOGIA),
                        encontrado(CategoriaObjeto.TECNOLOGIA), perdido(CategoriaObjeto.TECNOLOGIA))));
    }

    @Test
    @DisplayName("PU-10: empate -> primero la que va antes en CategoriaObjeto")
    void empate() {
        assertEquals(List.of(new FrecuenciaCategoria(CategoriaObjeto.DOCUMENTOS, 0, 1, 50),
                        new FrecuenciaCategoria(CategoriaObjeto.ROPA, 1, 0, 50)),
                FrecuenciaCategoria.de(List.of(perdido(CategoriaObjeto.ROPA), encontrado(CategoriaObjeto.DOCUMENTOS))));
    }

    @Test
    @DisplayName("PU-11: porcentajes con decimales -> se redondean (67 % y 33 %)")
    void redondeaPorcentajes() {
        assertEquals(List.of(new FrecuenciaCategoria(CategoriaObjeto.UTILES, 2, 0, 67),
                        new FrecuenciaCategoria(CategoriaObjeto.ROPA, 0, 1, 33)),
                FrecuenciaCategoria.de(List.of(perdido(CategoriaObjeto.UTILES), perdido(CategoriaObjeto.UTILES),
                        encontrado(CategoriaObjeto.ROPA))));
    }

    @Test
    @DisplayName("PU-12: solo objetos encontrados -> 0 perdidos")
    void soloEncontrados() {
        assertEquals(List.of(new FrecuenciaCategoria(CategoriaObjeto.ACCESORIOS, 0, 3, 100)),
                FrecuenciaCategoria.de(List.of(encontrado(CategoriaObjeto.ACCESORIOS),
                        encontrado(CategoriaObjeto.ACCESORIOS), encontrado(CategoriaObjeto.ACCESORIOS))));
    }

    private Objeto perdido(CategoriaObjeto categoria) {
        return new Objeto("Objeto de prueba", "Descripción de prueba suficiente", TipoObjeto.PERDIDO, categoria,
                Pabellon.PABELLON_A, null, LocalDateTime.now(), estudiante);
    }

    private Objeto encontrado(CategoriaObjeto categoria) {
        return new Objeto("Objeto de prueba", "Descripción de prueba suficiente", TipoObjeto.ENCONTRADO, categoria,
                Pabellon.PABELLON_A, null, LocalDateTime.now(), seguridad);
    }
}
