package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import org.springframework.data.jpa.repository.*;
public interface CategoryRepository extends JpaRepository<Category,Long>, JpaSpecificationExecutor<Category> {  }
