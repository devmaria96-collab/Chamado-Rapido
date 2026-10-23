package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import org.springframework.data.jpa.repository.*;
public interface AttachmentRepository extends JpaRepository<Attachment,Long>, JpaSpecificationExecutor<Attachment> { java.util.List<Attachment> findByTicketIdOrderByCreatedAtAsc(Long id); long countByTicketId(Long id); }
