package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import org.springframework.data.jpa.repository.*;
public interface HistoryRepository extends JpaRepository<History,Long>, JpaSpecificationExecutor<History> { java.util.List<History> findByTicketIdOrderByCreatedAtAsc(Long id); }
