package pe.edu.ufound.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Criterios de aceptación del Sprint 1: iniciar sesión (HU-01), cerrar sesión (HU-02) e inicio según rol (HU-03). */
@SpringBootTest
@AutoConfigureMockMvc
class SesionTest {

    private static final String ESTUDIANTE = "ana.torres@universidad.edu.pe";
    private static final String SEGURIDAD = "jorge.quispe@universidad.edu.pe";
    private static final String OFICINA = "lucia.paredes@universidad.edu.pe";

    @Autowired
    private MockMvc mvc;

    @Value("${ufound.mock.clave}")
    private String clave;

    // ---------- HU-01 ----------

    @Test
    void sinSesionCualquierPantallaMandaAlLogin() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/buscar")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void laPantallaDeLoginSeVeSinSesionYYaNoOfreceVistaPrevia() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(not(containsString("vista-previa"))));
    }

    @Test
    void credencialesValidasDanAccesoYLlevanAlInicio() throws Exception {
        mvc.perform(formLogin().user(ESTUDIANTE).password(clave))
                .andExpect(authenticated().withUsername(ESTUDIANTE).withRoles("ESTUDIANTE"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void cadaUsuarioEntraConSuRol() throws Exception {
        mvc.perform(formLogin().user(SEGURIDAD).password(clave))
                .andExpect(authenticated().withRoles("SEGURIDAD"));
        mvc.perform(formLogin().user(OFICINA).password(clave))
                .andExpect(authenticated().withRoles("OFICINA_OBJETOS_PERDIDOS"));
    }

    @Test
    void contrasenaIncorrectaVuelveAlLoginConError() throws Exception {
        mvc.perform(formLogin().user(ESTUDIANTE).password("otra-clave"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void correoDesconocidoDaElMismoErrorQueUnaContrasenaIncorrecta() throws Exception {
        mvc.perform(formLogin().user("nadie@universidad.edu.pe").password(clave))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void camposVaciosNoDanAcceso() throws Exception {
        mvc.perform(formLogin().user("").password(""))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void laVistaPreviaPorRolYaNoExiste() throws Exception {
        mvc.perform(get("/vista-previa/OFICINA_OBJETOS_PERDIDOS"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void laVistaPreviaTampocoCambiaElRolDeQuienYaEntro() throws Exception {
        mvc.perform(get("/vista-previa/OFICINA_OBJETOS_PERDIDOS")).andExpect(status().isNotFound());
    }

    // ---------- HU-02 ----------

    @Test
    void cerrarSesionTerminaLaSesionYVuelveAlLogin() throws Exception {
        MockHttpSession sesion = (MockHttpSession) mvc.perform(formLogin().user(ESTUDIANTE).password(clave))
                .andReturn().getRequest().getSession();
        mvc.perform(get("/").session(sesion)).andExpect(status().isOk());

        mvc.perform(post("/logout").session(sesion).with(csrf()))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?logout"));

        assertTrue(sesion.isInvalid());
    }

    @Test
    void elAvisoDeSesionCerradaSeMuestraEnElLogin() throws Exception {
        mvc.perform(get("/login").param("logout", ""))
                .andExpect(content().string(containsString("Cerraste sesión.")));
    }

    // ---------- HU-03 ----------

    @Test
    @WithUserDetails(ESTUDIANTE)
    void inicioDelEstudiante() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hola, Ana")))
                .andExpect(content().string(containsString("Reportar objeto perdido")))
                .andExpect(content().string(containsString("Mis reportes")))
                .andExpect(content().string(not(containsString("Registrar objeto encontrado"))))
                .andExpect(content().string(not(containsString("Historial de entregas"))));
    }

    @Test
    @WithUserDetails(SEGURIDAD)
    void inicioDeSeguridad() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hola, Jorge")))
                .andExpect(content().string(containsString("Registrar objeto encontrado")))
                .andExpect(content().string(not(containsString("Reportar objeto perdido"))))
                .andExpect(content().string(not(containsString("Historial de entregas"))));
    }

    @Test
    @WithUserDetails(OFICINA)
    void inicioDeOficina() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hola, Lucía")))
                .andExpect(content().string(containsString("Gestionar objetos")))
                .andExpect(content().string(containsString("Historial de entregas")))
                .andExpect(content().string(not(containsString("Reportar objeto perdido"))))
                .andExpect(content().string(not(containsString("Registrar objeto encontrado"))));
    }

    @Test
    @WithUserDetails(SEGURIDAD)
    void seguridadNoAbreLoQueEsDeOficina() throws Exception {
        mvc.perform(get("/entregas")).andExpect(status().isForbidden());
        mvc.perform(post("/objetos/1/estado").param("estado", "EN_REVISION").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(OFICINA)
    void oficinaVeEntregasPeroNoRegistraObjetos() throws Exception {
        mvc.perform(get("/entregas")).andExpect(status().isOk());
        mvc.perform(get("/objetos/nuevo")).andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(ESTUDIANTE)
    void estudianteReportaPeroNoVeEntregas() throws Exception {
        mvc.perform(get("/objetos/nuevo")).andExpect(status().isOk());
        mvc.perform(get("/entregas")).andExpect(status().isForbidden());
    }
}
