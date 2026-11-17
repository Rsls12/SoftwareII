package pe.edu.ufound.model;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

/** Datos de prueba para las pruebas unitarias. */
public final class FabricaPruebas {

    private FabricaPruebas() {
    }

    public static Usuario usuario(long id, RolUsuario rol) {
        Usuario usuario = new Usuario("Nombre" + id, "Apellido" + id, "usuario" + id + "@universidad.edu.pe",
                "hash-ficticio", rol);
        ReflectionTestUtils.setField(usuario, "idUsuario", id);
        return usuario;
    }

    public static Objeto objeto(String nombre, TipoObjeto tipo, CategoriaObjeto categoria, LocalDateTime fecha,
                                Usuario autor) {
        return new Objeto(nombre, "Descripción de prueba de " + nombre, tipo, categoria, Pabellon.PABELLON_A,
                null, fecha, autor);
    }

    public static Objeto conEstado(Objeto objeto, EstadoObjeto estado) {
        objeto.cambiarEstado(estado);
        return objeto;
    }

    public static void fijarFechaDeEntrega(Entrega entrega, LocalDateTime fecha) {
        ReflectionTestUtils.setField(entrega, "fechaHoraEntrega", fecha);
    }
}
