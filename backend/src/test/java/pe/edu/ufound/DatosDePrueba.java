package pe.edu.ufound;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.edu.ufound.model.CategoriaObjeto;
import pe.edu.ufound.model.EstadoObjeto;
import pe.edu.ufound.model.Objeto;
import pe.edu.ufound.model.Pabellon;
import pe.edu.ufound.model.RolUsuario;
import pe.edu.ufound.model.TipoObjeto;
import pe.edu.ufound.model.Usuario;
import pe.edu.ufound.repositorio.ObjetoRepository;
import pe.edu.ufound.repositorio.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;

/** Usuarios y objetos con los que corren las pruebas, en H2. No forma parte de la aplicación. */
@Component
public class DatosDePrueba implements CommandLineRunner {

    public static final String CLAVE_ESTUDIANTE = "prueba-ana-2026";
    public static final String CLAVE_SEGURIDAD = "prueba-jorge-2026";
    public static final String CLAVE_OFICINA = "prueba-lucia-2026";
    public static final String SIN_CONTRASENA = "sin.clave@universidad.edu.pe";

    private final UsuarioRepository usuarios;
    private final ObjetoRepository objetos;
    private final PasswordEncoder cifrador;

    public DatosDePrueba(UsuarioRepository usuarios, ObjetoRepository objetos, PasswordEncoder cifrador) {
        this.usuarios = usuarios;
        this.objetos = objetos;
        this.cifrador = cifrador;
    }

    @Override
    public void run(String... args) {
        Usuario ana = usuarios.save(new Usuario("Ana", "Torres Ríos", "ana.torres@universidad.edu.pe",
                cifrador.encode(CLAVE_ESTUDIANTE), RolUsuario.ESTUDIANTE));
        Usuario jorge = usuarios.save(new Usuario("Jorge", "Quispe Mamani", "jorge.quispe@universidad.edu.pe",
                cifrador.encode(CLAVE_SEGURIDAD), RolUsuario.SEGURIDAD));
        Usuario lucia = usuarios.save(new Usuario("Lucía", "Paredes Vega", "lucia.paredes@universidad.edu.pe",
                cifrador.encode(CLAVE_OFICINA), RolUsuario.OFICINA_OBJETOS_PERDIDOS));
        usuarios.save(new Usuario("Sin", "Contraseña", SIN_CONTRASENA, null, RolUsuario.ESTUDIANTE));

        LocalDateTime ahora = LocalDateTime.now();

        Objeto laptop = objeto("Laptop Lenovo ThinkPad gris", "Con sticker de la NASA en la tapa y cargador negro.",
                TipoObjeto.PERDIDO, CategoriaObjeto.TECNOLOGIA, Pabellon.PABELLON_C, ahora.minusHours(3), ana);
        laptop.cambiarEstado(EstadoObjeto.EN_REVISION, "Revisando objetos encontrados en el Pabellón C.", lucia);

        Objeto botella = objeto("Botella Stanley verde", "Botella de 1 litro, abollada en la base, con mi nombre grabado.",
                TipoObjeto.PERDIDO, CategoriaObjeto.BOTELLAS, Pabellon.PABELLON_A, ahora.minusDays(1), ana);

        Objeto dni = objeto("DNI", "Encontrado en la cafetería, a nombre de un estudiante de Ingeniería.",
                TipoObjeto.ENCONTRADO, CategoriaObjeto.DOCUMENTOS, Pabellon.OTRO, ahora.minusDays(2), jorge);
        dni.cambiarEstado(EstadoObjeto.DISPONIBLE_EN_OFICINA, "Recibido de seguridad.", lucia);

        Objeto llaves = objeto("Llaves con llavero de Pikachu", "Tres llaves y un llavero amarillo.",
                TipoObjeto.ENCONTRADO, CategoriaObjeto.ACCESORIOS, Pabellon.PABELLON_A, ahora.minusDays(20), jorge);
        llaves.cambiarEstado(EstadoObjeto.DISPONIBLE_EN_OFICINA, "Recibido de seguridad.", lucia);
        llaves.entregar("María Fernández López", "DNI 71234567", "Describió correctamente el llavero.", lucia);

        objetos.saveAll(List.of(laptop, botella, dni, llaves));
    }

    private static Objeto objeto(String nombre, String descripcion, TipoObjeto tipo, CategoriaObjeto categoria,
                                 Pabellon ubicacion, LocalDateTime fecha, Usuario autor) {
        return new Objeto(nombre, descripcion, tipo, categoria, ubicacion, null, fecha, autor);
    }
}
