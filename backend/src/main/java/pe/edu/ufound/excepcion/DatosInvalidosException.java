package pe.edu.ufound.excepcion;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DatosInvalidosException extends RuntimeException {

    private final String campo;

    public DatosInvalidosException(String campo) {
        super(campo);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
