package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import org.springframework.data.jpa.repository.*;
public interface ArticleRepository extends JpaRepository<Article,Long>, JpaSpecificationExecutor<Article> {  }
