package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import org.springframework.data.jpa.repository.*;
public interface TriageRepository extends JpaRepository<Triage,Long>, JpaSpecificationExecutor<Triage> {  }
