package br.com.chamadorapido;
import br.com.chamadorapido.Domain.AttendanceCycle;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
public interface AttendanceCycleRepository extends JpaRepository<AttendanceCycle,Short> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from AttendanceCycle c where c.id = 1")
  Optional<AttendanceCycle> lock();
}
