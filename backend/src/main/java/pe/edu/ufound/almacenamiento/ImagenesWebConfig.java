package pe.edu.ufound.almacenamiento;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ImagenesWebConfig implements WebMvcConfigurer {

    private final AlmacenamientoLocal almacenamiento;

    public ImagenesWebConfig(AlmacenamientoLocal almacenamiento) {
        this.almacenamiento = almacenamiento;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        String ubicacion = almacenamiento.getDirectorio().toUri().toString();
        if (!ubicacion.endsWith("/")) {
            ubicacion = ubicacion + "/";
        }
        registro.addResourceHandler(almacenamiento.getRutaPublica() + "/**")
                .addResourceLocations(ubicacion);
    }
}
