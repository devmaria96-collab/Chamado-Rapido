package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import org.springframework.data.jpa.repository.*;
public interface DepartmentRepository extends JpaRepository<Department,Long>, JpaSpecificationExecutor<Department> { java.util.Optional<Department> findByName(String name); }
