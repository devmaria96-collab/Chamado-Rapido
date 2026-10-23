package br.com.chamadorapido;
import br.com.chamadorapido.Domain.Ticket;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
public interface TicketRepository extends JpaRepository<Ticket,Long>, JpaSpecificationExecutor<Ticket> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Ticket> findLockedById(Long id);
}
