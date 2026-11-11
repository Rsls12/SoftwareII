package pe.edu.ufound.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.ufound.model.CategoriaObjeto;
import pe.edu.ufound.model.Objeto;
import pe.edu.ufound.model.Pabellon;
import pe.edu.ufound.model.RolUsuario;
import pe.edu.ufound.model.Usuario;
import pe.edu.ufound.repositorio.CoincidenciaRepository;
import pe.edu.ufound.repositorio.ObjetoRepository;
import pe.edu.ufound.repositorio.UsuarioRepository;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-24: la Oficina ve las coincidencias detectadas con su nivel, de la más parecida a la menos. */
@SpringBootTest
@AutoConfigureMockMvc
class CoincidenciasTest {

    private static final String ESTUDIANTE = "ana.torres@universidad.edu.pe";
    private static final String OFICINA = "lucia.paredes@universidad.edu.pe";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjetoRepository objetos;

    @Autowired
    private CoincidenciaRepository coincidencias;

    @Autowired
    private UsuarioRepository usuarios;

    @Test
    @WithUserDetails(OFICINA)
    void oficinaVeLasCoincidenciasConSuNivelDeLaMasParecidaALaMenos() throws Exception {
        Usuario ana = usuarios.findFirstByRolOrderByIdUsuarioAsc(RolUsuario.ESTUDIANTE).orElseThrow();
        Usuario jorge = usuarios.findFirstByRolOrderByIdUsuarioAsc(RolUsuario.SEGURIDAD).orElseThrow();

        Objeto perdida = objetos.save(Objeto.reportar("Audífonos Sony negros",
                "Audífonos Sony inalámbricos con estuche negro", CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_D, null, ana));
        Objeto encontrada = objetos.save(Objeto.reportar("Audífonos Sony negros",
                "Sony inalámbricos con estuche negro", CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_D, null, jorge));
        coincidencias.saveAll(encontrada.detectarCoincidencias(List.of(perdida)));

        mvc.perform(get("/coincidencias"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Audífonos Sony negros")))
                .andExpect(content().string(containsString("Alta")));
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void soloLaOficinaEntraACoincidencias() throws Exception {
        mvc.perform(get("/coincidencias")).andExpect(status().isForbidden());
    }
}
