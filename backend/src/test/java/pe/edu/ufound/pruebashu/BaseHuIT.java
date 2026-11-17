package pe.edu.ufound.pruebashu;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pe.edu.ufound.repositorio.CoincidenciaRepository;
import pe.edu.ufound.repositorio.NotificacionRepository;
import pe.edu.ufound.repositorio.ObjetoRepository;
import pe.edu.ufound.repositorio.UsuarioRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Base de las pruebas de integración. */
@AutoConfigureMockMvc
@ActiveProfiles("it")
public abstract class BaseHuIT {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjetoRepository objetos;

    @Autowired
    protected UsuarioRepository usuarios;

    @Autowired
    private CoincidenciaRepository coincidencias;

    @Autowired
    private NotificacionRepository notificaciones;

    @Autowired
    private PasswordEncoder cifrador;

    @Autowired
    private JdbcTemplate jdbc;

    protected DatosHu datos;

    @BeforeEach
    void prepararDatos() {
        datos = new DatosHu(objetos, usuarios, coincidencias, notificaciones, cifrador, jdbc);
        datos.limpiar();
    }

    protected static RequestPostProcessor estudiante(String correo) {
        return user(correo).roles("ESTUDIANTE");
    }

    protected static RequestPostProcessor seguridad() {
        return user(DatosHu.JORGE).roles("SEGURIDAD");
    }

    protected static RequestPostProcessor oficina() {
        return user(DatosHu.LUCIA).roles("OFICINA_OBJETOS_PERDIDOS");
    }

    protected String html(String url, RequestPostProcessor quien) throws Exception {
        return mvc.perform(get(url).with(quien)).andReturn().getResponse().getContentAsString();
    }
}
