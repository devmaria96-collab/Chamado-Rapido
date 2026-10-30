package br.com.chamadorapido;
import br.com.chamadorapido.Domain.Ticket;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
public interface TicketRepository extends JpaRepository<Ticket,Long>, JpaSpecificationExecutor<Ticket> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Ticket> findLockedById(Long id);
  java.util.List<Ticket> findByWhatsappPhoneOrderByIdDesc(String phone);
  long countByStatus(br.com.chamadorapido.Domain.Status status);
  long countByStatusIn(java.util.Collection<br.com.chamadorapido.Domain.Status> statuses);
  long countByStatusInAndResolvedAtGreaterThanEqualAndResolvedAtLessThan(java.util.Collection<br.com.chamadorapido.Domain.Status> statuses, java.time.Instant from, java.time.Instant to);
}
