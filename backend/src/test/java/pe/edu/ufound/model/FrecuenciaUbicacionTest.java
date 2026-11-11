package pe.edu.ufound.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** HU-40: ranking de pabellones donde más se pierden objetos. */
class FrecuenciaUbicacionTest {

    private final Usuario estudiante = new Usuario("Ana", "Torres", "ana@u.pe", null, RolUsuario.ESTUDIANTE);
    private final Usuario seguridad = new Usuario("Jorge", "Quispe", "jorge@u.pe", null, RolUsuario.SEGURIDAD);

    @Test
    void ordenaLosPabellonesDeMasPerdidasAMenosYSoloCuentaPerdidos() {
        List<FrecuenciaUbicacion> ranking = FrecuenciaUbicacion.de(List.of(
                objeto(TipoObjeto.PERDIDO, Pabellon.PABELLON_A),
                objeto(TipoObjeto.PERDIDO, Pabellon.PABELLON_C),
                objeto(TipoObjeto.PERDIDO, Pabellon.PABELLON_C),
                objeto(TipoObjeto.PERDIDO, Pabellon.PABELLON_C),
                // Un objeto ENCONTRADO en el Pabellón A no debe sumar a su conteo de pérdidas.
                objeto(TipoObjeto.ENCONTRADO, Pabellon.PABELLON_A)));

        assertEquals(List.of(Pabellon.PABELLON_C, Pabellon.PABELLON_A),
                ranking.stream().map(FrecuenciaUbicacion::ubicacion).toList());
        assertEquals(3, ranking.get(0).cantidad());
        assertEquals(75, ranking.get(0).porcentaje());
        assertEquals(1, ranking.get(1).cantidad());
        assertEquals(25, ranking.get(1).porcentaje());
    }

    @Test
    void anteUnEmpateRespetaElOrdenDeLosPabellonesYSinPerdidosNoHayRanking() {
        List<FrecuenciaUbicacion> ranking = FrecuenciaUbicacion.de(List.of(
                objeto(TipoObjeto.PERDIDO, Pabellon.PABELLON_E),
                objeto(TipoObjeto.PERDIDO, Pabellon.PABELLON_B)));

        assertEquals(List.of(Pabellon.PABELLON_B, Pabellon.PABELLON_E),
                ranking.stream().map(FrecuenciaUbicacion::ubicacion).toList());

        assertTrue(FrecuenciaUbicacion.de(List.of()).isEmpty());
        assertTrue(FrecuenciaUbicacion.de(List.of(objeto(TipoObjeto.ENCONTRADO, Pabellon.PABELLON_A))).isEmpty());
    }

    private Objeto objeto(TipoObjeto tipo, Pabellon ubicacion) {
        Usuario autor = tipo == TipoObjeto.PERDIDO ? estudiante : seguridad;
        return new Objeto("Objeto de prueba", "Descripción de prueba suficiente", tipo, CategoriaObjeto.OTROS,
                ubicacion, null, LocalDateTime.now(), autor);
    }
}
