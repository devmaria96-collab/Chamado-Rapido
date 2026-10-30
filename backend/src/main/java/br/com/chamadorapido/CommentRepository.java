package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import org.springframework.data.jpa.repository.*;
public interface CommentRepository extends JpaRepository<Comment,Long>, JpaSpecificationExecutor<Comment> {
  java.util.List<Comment> findByTicketIdOrderByCreatedAtAsc(Long id);
  java.util.Optional<Comment> findByExternalId(String externalId);
  java.util.List<Comment> findByInternalFalseAndTicketIdInOrderByIdDesc(java.util.Collection<Long> ids);
}
