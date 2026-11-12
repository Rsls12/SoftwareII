package pe.edu.ufound.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.ufound.model.CategoriaObjeto;
import pe.edu.ufound.model.Coincidencia;
import pe.edu.ufound.model.NivelCoincidencia;
import pe.edu.ufound.model.Objeto;
import pe.edu.ufound.model.Pabellon;
import pe.edu.ufound.model.Usuario;
import pe.edu.ufound.repositorio.CoincidenciaRepository;
import pe.edu.ufound.repositorio.ObjetoRepository;
import pe.edu.ufound.repositorio.UsuarioRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-36: dado que un objeto perdido tiene varias coincidencias detectadas, el estudiante las ve ordenadas por
 * nivel de similitud, de la más relevante a la menos.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CoincidenciasPriorizadasTest {

    private static final String ESTUDIANTE = "ana.torres@universidad.edu.pe";
    private static final String SEGURIDAD = "jorge.quispe@universidad.edu.pe";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjetoRepository objetos;

    @Autowired
    private CoincidenciaRepository coincidencias;

    @Autowired
    private UsuarioRepository usuarios;

    @Test
    @WithUserDetails(ESTUDIANTE)
    void elEstudianteVeLasCoincidenciasDeSuObjetoDeLaMasParecidaALaMenos() throws Exception {
        Usuario ana = usuarios.findByCorreoIgnoreCase(ESTUDIANTE).orElseThrow();
        Usuario jorge = usuarios.findByCorreoIgnoreCase(SEGURIDAD).orElseThrow();
        Objeto perdida = objetos.save(Objeto.reportar("Audífonos Sony negros",
                "Audífonos Sony inalámbricos con estuche negro", CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_D,
                null, ana));
        // Mismo lugar y casi la misma descripción: nivel ALTO.
        Objeto alta = encontrado("Audífonos Sony negros", "Audífonos Sony inalámbricos con estuche negro",
                Pabellon.PABELLON_D, jorge);
        // Otro pabellón, varias palabras en común: nivel MEDIO.
        Objeto media = encontrado("Audífonos Sony", "Sony inalámbricos con estuche negro", Pabellon.PABELLON_B, jorge);
        // Otro pabellón y una sola palabra en común: nivel BAJO.
        Objeto baja = encontrado("Audífonos", "Encontrados en la cafetería del campus", Pabellon.PABELLON_A, jorge);
        // Se detectan en un orden distinto al de su relevancia.
        coincidencias.saveAll(baja.detectarCoincidencias(List.of(perdida)));
        coincidencias.saveAll(alta.detectarCoincidencias(List.of(perdida)));
        coincidencias.saveAll(media.detectarCoincidencias(List.of(perdida)));

        List<Coincidencia> priorizadas = coincidencias.de(perdida);
        assertEquals(3, priorizadas.size());
        assertEquals(NivelCoincidencia.ALTO, priorizadas.get(0).getNivelCoincidencia());
        assertEquals(NivelCoincidencia.MEDIO, priorizadas.get(1).getNivelCoincidencia());
        assertEquals(NivelCoincidencia.BAJO, priorizadas.get(2).getNivelCoincidencia());

        String pagina = mvc.perform(get("/objetos/" + perdida.getIdObjeto()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String seccion = pagina.substring(pagina.indexOf("titulo-coincidencias"), pagina.indexOf("titulo-historial"));
        assertTrue(seccion.contains("Ordenadas de la más parecida a la menos"));
        int posicionAlta = seccion.indexOf(alta.getCodigo());
        int posicionMedia = seccion.indexOf(media.getCodigo());
        int posicionBaja = seccion.indexOf(baja.getCodigo());
        assertTrue(posicionAlta >= 0 && posicionMedia >= 0 && posicionBaja >= 0);
        assertTrue(posicionAlta < posicionMedia && posicionMedia < posicionBaja);
        assertTrue(seccion.indexOf(">Alta<") < seccion.indexOf(">Media<"));
        assertTrue(seccion.indexOf(">Media<") < seccion.indexOf(">Baja<"));
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void conUnaSolaCoincidenciaNoSeMuestraElAvisoDeOrden() throws Exception {
        Usuario ana = usuarios.findByCorreoIgnoreCase(ESTUDIANTE).orElseThrow();
        Usuario jorge = usuarios.findByCorreoIgnoreCase(SEGURIDAD).orElseThrow();
        Objeto perdida = objetos.save(Objeto.reportar("Casaca impermeable azul",
                "Casaca impermeable azul talla M con capucha", CategoriaObjeto.ROPA, Pabellon.PABELLON_C, null, ana));
        Objeto encontrada = objetos.save(Objeto.reportar("Casaca impermeable azul",
                "Casaca impermeable azul talla M con capucha", CategoriaObjeto.ROPA, Pabellon.PABELLON_C, null, jorge));
        coincidencias.saveAll(encontrada.detectarCoincidencias(List.of(perdida)));

        String pagina = mvc.perform(get("/objetos/" + perdida.getIdObjeto()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(pagina.contains(encontrada.getCodigo()));
        assertFalse(pagina.contains("Ordenadas de la más parecida a la menos"));
    }

    private Objeto encontrado(String nombre, String descripcion, Pabellon ubicacion, Usuario agente) {
        return objetos.save(Objeto.reportar(nombre, descripcion, CategoriaObjeto.TECNOLOGIA, ubicacion, null, agente));
    }
}
