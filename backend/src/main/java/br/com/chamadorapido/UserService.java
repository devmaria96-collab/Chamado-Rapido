package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
@Transactional(readOnly=true)
public class UserService {
  private final UserRepository users;
  public UserService(UserRepository users){this.users=users;}
  public User current(){
    var auth=SecurityContextHolder.getContext().getAuthentication();
    if(auth==null||!(auth.getPrincipal() instanceof SecurityConfig.Principal p))throw error(401,"Entre novamente.");
    User u=users.findById(p.id()).orElseThrow(()->error(401,"Usuário indisponível."));
    if(!u.active||u.authVersion!=p.version())throw error(401,"Sua permissão ou senha mudou. Entre novamente.");
    return u;
  }
  public User operational(){User u=current();if(u.mustChangePassword)throw error(403,"Altere sua senha temporária em Minha conta antes de continuar.");return u;}
  public User staff(){User u=operational();if(!staff(u))throw error(403,"Acesso exclusivo da equipe de TI.");return u;}
  public User admin(){User u=operational();if(u.role!=Role.ADMIN)throw error(403,"Acesso exclusivo do administrador.");return u;}
  public static boolean staff(User u){return u.role!=Role.COLABORADOR;}
  public List<Map<String,String>> loginSectors(){
    List<User> active=users.findByActiveTrue();
    Map<Long,User> pick=new LinkedHashMap<>();
    active.stream().filter(u->u.role!=Role.ADMIN&&u.department!=null&&u.department.active).sorted(Comparator.comparing((User u)->u.department.name,String.CASE_INSENSITIVE_ORDER).thenComparingInt(u->u.role==Role.COLABORADOR?0:1)).forEach(u->pick.putIfAbsent(u.department.id,u));
    List<Map<String,String>> out=new ArrayList<>();
    pick.values().forEach(u->out.add(Map.of("id",String.valueOf(u.department.id),"name",u.department.name)));
    if(active.stream().anyMatch(u->u.role==Role.ADMIN))out.add(Map.of("id","admin","name","Administração"));
    return out;
  }
  public String emailForSector(String sector){
    if(sector==null||sector.isBlank())return "";
    List<User> active=users.findByActiveTrue();
    if("admin".equals(sector))return active.stream().filter(u->u.role==Role.ADMIN).map(u->u.email).findFirst().orElse("");
    long id;try{id=Long.parseLong(sector);}catch(NumberFormatException e){return "";}
    return active.stream().filter(u->u.role!=Role.ADMIN&&u.department!=null&&u.department.id.equals(id)).sorted(Comparator.comparingInt(u->u.role==Role.COLABORADOR?0:1)).map(u->u.email).findFirst().orElse("");
  }
  public static ResponseStatusException error(int status,String message){return new ResponseStatusException(HttpStatus.valueOf(status),message);}
  public static void checkPassword(String value){if(value==null||value.length()<12||value.getBytes(StandardCharsets.UTF_8).length>72||!value.matches("(?s).*[a-z].*")||!value.matches("(?s).*[A-Z].*")||!value.matches("(?s).*\\d.*"))throw error(400,"A senha deve ter 12 a 72 bytes, letras maiúsculas, minúsculas e números.");}
}
