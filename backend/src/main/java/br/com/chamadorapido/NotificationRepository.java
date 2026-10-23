package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import org.springframework.data.jpa.repository.*;
public interface NotificationRepository extends JpaRepository<Notification,Long>, JpaSpecificationExecutor<Notification> { java.util.List<Notification> findTop50ByRecipientIdOrderByCreatedAtDesc(Long id); java.util.List<Notification> findByRecipientIdAndReadFalse(Long id); }
