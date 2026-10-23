package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import org.springframework.data.jpa.repository.*;
public interface UserRepository extends JpaRepository<User,Long>, JpaSpecificationExecutor<User> { java.util.Optional<User> findByEmailIgnoreCase(String email); java.util.List<User> findByActiveTrueAndRoleNot(Role role); long countByActiveTrueAndRole(Role role); @EntityGraph(attributePaths="department") java.util.List<User> findByActiveTrue(); }
