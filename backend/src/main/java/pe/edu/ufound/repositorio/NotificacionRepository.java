package pe.edu.ufound.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.ufound.model.Notificacion;
import pe.edu.ufound.model.Usuario;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    // HU-25, HU-26: las notificaciones de un usuario, de la más reciente a la más antigua.
    List<Notificacion> findByDestinatarioOrderByFechaHoraDescIdNotificacionDesc(Usuario destinatario);
}
