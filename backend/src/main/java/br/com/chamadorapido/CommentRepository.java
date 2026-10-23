package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import org.springframework.data.jpa.repository.*;
public interface CommentRepository extends JpaRepository<Comment,Long>, JpaSpecificationExecutor<Comment> { java.util.List<Comment> findByTicketIdOrderByCreatedAtAsc(Long id); }
