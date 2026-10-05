package pe.edu.ufound.controlador.api;

import pe.edu.ufound.model.CategoriaObjeto;

public record Opcion(String codigo, String etiqueta) {

    public static Opcion de(CategoriaObjeto categoria) {
        return new Opcion(categoria.name(), categoria.getEtiqueta());
    }
}
