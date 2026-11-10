package pe.edu.ufound.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** HU-39: ranking de las categorías más frecuentes. */
class FrecuenciaCategoriaTest {

    private final Usuario estudiante = new Usuario("Ana", "Torres", "ana@u.pe", null, RolUsuario.ESTUDIANTE);
    private final Usuario seguridad = new Usuario("Jorge", "Quispe", "jorge@u.pe", null, RolUsuario.SEGURIDAD);

    @Test
    void ordenaDeLaMasFrecuenteALaMenosYSeparaPerdidosDeEncontrados() {
        List<FrecuenciaCategoria> ranking = FrecuenciaCategoria.de(List.of(
                objeto(TipoObjeto.PERDIDO, CategoriaObjeto.BOTELLAS),
                objeto(TipoObjeto.PERDIDO, CategoriaObjeto.TECNOLOGIA),
                objeto(TipoObjeto.ENCONTRADO, CategoriaObjeto.TECNOLOGIA),
                objeto(TipoObjeto.PERDIDO, CategoriaObjeto.TECNOLOGIA)));

        assertEquals(List.of(CategoriaObjeto.TECNOLOGIA, CategoriaObjeto.BOTELLAS),
                ranking.stream().map(FrecuenciaCategoria::categoria).toList());
        FrecuenciaCategoria primera = ranking.get(0);
        assertEquals(3, primera.total());
        assertEquals(2, primera.perdidos());
        assertEquals(1, primera.encontrados());
        assertEquals(75, primera.porcentaje());
        assertEquals(25, ranking.get(1).porcentaje());
    }

    @Test
    void anteUnEmpateRespetaElOrdenDeLasCategoriasYSinObjetosNoHayRanking() {
        List<FrecuenciaCategoria> ranking = FrecuenciaCategoria.de(List.of(
                objeto(TipoObjeto.PERDIDO, CategoriaObjeto.ROPA),
                objeto(TipoObjeto.ENCONTRADO, CategoriaObjeto.DOCUMENTOS)));

        assertEquals(List.of(CategoriaObjeto.DOCUMENTOS, CategoriaObjeto.ROPA),
                ranking.stream().map(FrecuenciaCategoria::categoria).toList());
        assertTrue(FrecuenciaCategoria.de(List.of()).isEmpty());
    }

    private Objeto objeto(TipoObjeto tipo, CategoriaObjeto categoria) {
        return new Objeto("Objeto de prueba", "Descripción de prueba suficiente", tipo, categoria,
                Pabellon.PABELLON_A, null, LocalDateTime.now(), tipo == TipoObjeto.PERDIDO ? estudiante : seguridad);
    }
}
