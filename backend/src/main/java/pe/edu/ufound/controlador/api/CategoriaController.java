package pe.edu.ufound.controlador.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.ufound.model.CategoriaObjeto;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    @GetMapping
    public List<Opcion> listar() {
        return Arrays.stream(CategoriaObjeto.values()).map(Opcion::de).toList();
    }
}
