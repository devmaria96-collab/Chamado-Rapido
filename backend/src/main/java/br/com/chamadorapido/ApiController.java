package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import br.com.chamadorapido.Dtos.*;
import static br.com.chamadorapido.UserService.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController
@RequestMapping("/api")
@Transactional
public class ApiController {
  private final UserService auth; private final WorkService work; private final AttachmentService storage;
  private final UserRepository users; private final DepartmentRepository departments; private final CategoryRepository categories;
  private final NotificationRepository notifications; private final ArticleRepository articles; private final TriageRepository triages;
  private final PasswordEncoder encoder; private final boolean demo;
  public ApiController(UserService auth,WorkService work,AttachmentService storage,UserRepository users,DepartmentRepository departments,CategoryRepository categories,NotificationRepository notifications,ArticleRepository articles,TriageRepository triages,PasswordEncoder encoder,@Value("${app.demo}")boolean demo){this.auth=auth;this.work=work;this.storage=storage;this.users=users;this.departments=departments;this.categories=categories;this.notifications=notifications;this.articles=articles;this.triages=triages;this.encoder=encoder;this.demo=demo;}
  @GetMapping("/public/info") public Object info(){return Map.of("name","Chamado Rápido","version","1.0.0","demo",demo);}
  @GetMapping("/public/sectors") public Object sectors(){return auth.loginSectors();}
  @GetMapping("/auth/csrf") public Object csrf(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
  @GetMapping("/auth/me") public Person me(){return Dtos.person(auth.current());}
  @PostMapping("/auth/password") public Object password(@Valid @RequestBody PasswordInput p,HttpServletRequest req){User u=auth.current();if(!encoder.matches(p.currentPassword(),u.passwordHash))throw error(400,"A senha atual está incorreta.");checkPassword(p.newPassword());if(encoder.matches(p.newPassword(),u.passwordHash))throw error(400,"Use uma senha diferente da atual.");u.passwordHash=encoder.encode(p.newPassword());u.authVersion++;u.mustChangePassword=false;users.saveAndFlush(u);if(req.getSession(false)!=null)req.getSession(false).invalidate();return Map.of("message","Senha atualizada. Entre novamente.");}
  @GetMapping("/meta") public Object meta(){User u=auth.operational();return Map.of("departments",departments.findAll(Sort.by("name")).stream().filter(d->d.active).map(d->Map.of("id",d.id,"name",d.name)).toList(),"categories",categories.findAll(Sort.by("name")).stream().filter(c->c.active).map(c->Map.of("id",c.id,"name",c.name)).toList(),"technicians",staff(u)?users.findByActiveTrueAndRoleNot(Role.COLABORADOR).stream().map(x->Map.of("id",x.id,"name",x.name)).toList():List.of(),"slaHours",Map.of("CRITICA",4,"ALTA",8,"MEDIA",24,"BAIXA",72));}
  @GetMapping("/tickets") public Object list(@RequestParam Map<String,String> filters,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="15")int size){return work.list(filters,page,size);}
  @PostMapping("/tickets") @ResponseStatus(HttpStatus.CREATED) public Object create(@Valid @RequestBody TicketInput p){return work.create(p);}
  @GetMapping("/tickets/{id}") public Detail detail(@PathVariable Long id){return work.detail(id);}
  @PatchMapping("/tickets/{id}") public Object patch(@PathVariable Long id,@Valid @RequestBody PatchInput p){return work.patch(id,p);}
  @PostMapping("/tickets/{id}/comments") public Object comment(@PathVariable Long id,@Valid @RequestBody CommentInput p){work.comment(id,p);return Map.of("ok",true);}
  @PostMapping("/tickets/{id}/rating") public Object rating(@PathVariable Long id,@Valid @RequestBody RatingInput p){work.rate(id,p);return Map.of("ok",true);}
  @PostMapping(value="/tickets/{id}/attachments",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Object upload(@PathVariable Long id,@RequestParam("file")MultipartFile f)throws IOException{storage.upload(id,f);return Map.of("ok",true);}
  @GetMapping("/attachments/{id}") public ResponseEntity<FileSystemResource> download(@PathVariable Long id){var d=storage.download(id);String mime=d.metadata().mime;boolean inline=mime.startsWith("image/")||mime.equals("application/pdf");ContentDisposition disposition=(inline?ContentDisposition.inline():ContentDisposition.attachment()).filename(d.metadata().originalName,StandardCharsets.UTF_8).build();return ResponseEntity.ok().contentType(MediaType.parseMediaType(mime)).header(HttpHeaders.CONTENT_DISPOSITION,disposition.toString()).header("X-Content-Type-Options","nosniff").contentLength(d.metadata().size).body(new FileSystemResource(d.path()));}
  @GetMapping("/dashboard") public Object summary(@RequestParam Map<String,String> filters){return work.summary(filters);}
  @GetMapping("/departments/overview") public Object departmentsOverview(){return work.departmentSummary();}
  @GetMapping("/reports/export") public ResponseEntity<byte[]> report(@RequestParam Map<String,String> f){return ResponseEntity.ok().contentType(new MediaType("text","csv",StandardCharsets.UTF_8)).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=chamados.csv").body(work.csv(f).getBytes(StandardCharsets.UTF_8));}
  @GetMapping("/notifications") public Object notifications(){User u=auth.operational();return notifications.findTop50ByRecipientIdOrderByCreatedAtDesc(u.id).stream().map(n->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",n.id);m.put("title",n.title);m.put("read",n.read);m.put("ticketId",n.ticket==null?null:n.ticket.id);m.put("createdAt",n.createdAt);return m;}).toList();}
  @PostMapping("/notifications/read-all") public Object readAll(){User u=auth.operational();notifications.findByRecipientIdAndReadFalse(u.id).forEach(n->n.read=true);return Map.of("ok",true);}
  @PostMapping("/notifications/{id}/read") public Object read(@PathVariable Long id){User u=auth.operational();Notification n=notifications.findById(id).filter(x->x.recipient.id.equals(u.id)).orElseThrow(()->error(404,"Notificação indisponível."));n.read=true;return Map.of("ok",true);}
  @GetMapping("/articles") public Object articles(){User u=auth.staff();return articles.findAll(Sort.by(Sort.Direction.DESC,"updatedAt")).stream().filter(a->a.active||u.role==Role.ADMIN).toList();}
  @PostMapping("/admin/articles") public Object addArticle(@Valid @RequestBody ArticleInput p){auth.admin();return saveArticle(new Article(),p);}
  @PutMapping("/admin/articles/{id}") public Object editArticle(@PathVariable Long id,@Valid @RequestBody ArticleInput p){auth.admin();return saveArticle(articles.findById(id).orElseThrow(()->error(404,"Artigo indisponível.")),p);}
  private Article saveArticle(Article a,ArticleInput p){a.title=p.title().trim();a.excerpt=p.excerpt().trim();a.body=p.body().trim();a.category=p.category().trim();a.active=p.active();a.updatedAt=Instant.now();return articles.save(a);}
  @PostMapping("/triages") @ResponseStatus(HttpStatus.CREATED) public Object startTriage(@Valid @RequestBody TriageStart p){User u=auth.operational();var step=TriageEngine.start(p.category());Triage t=new Triage();t.owner=u;t.category=p.category();t.node=step.id();t.transcript="Assistente: "+step.message()+"\n";triages.saveAndFlush(t);return triageView(t);}
  @GetMapping("/triages/{id}") public Object getTriage(@PathVariable Long id){User u=auth.operational();Triage t=triages.findById(id).filter(x->x.owner.id.equals(u.id)).orElseThrow(()->error(404,"Triagem não encontrada."));return triageView(t);}
  @PostMapping("/triages/{id}/answers") public Object answerTriage(@PathVariable Long id,@Valid @RequestBody TriageAnswer p){User u=auth.operational();Triage t=triages.findById(id).filter(x->x.owner.id.equals(u.id)).orElseThrow(()->error(404,"Triagem não encontrada."));if(!Objects.equals(t.version,p.version()))throw error(409,"Triagem atualizada. Recarregue-a.");if(!t.result.equals("EM_CURSO"))throw error(400,"Triagem já finalizada.");var old=TriageEngine.STEPS.get(t.node);String answer=old.options().stream().filter(o->o.id().equals(p.choice())).findFirst().orElseThrow(()->error(400,"Resposta inválida.")).label();var next=TriageEngine.answer(t.node,p.choice());t.transcript+="Usuário: "+answer+"\nAssistente: "+next.message()+"\n";t.node=next.id();t.result=next.result();if(!t.result.equals("EM_CURSO"))t.finishedAt=Instant.now();triages.saveAndFlush(t);return triageView(t);}
  private Object triageView(Triage t){var step=TriageEngine.STEPS.get(t.node);Map<String,Object> m=new LinkedHashMap<>();m.put("id",t.id);m.put("version",t.version);m.put("category",t.category);m.put("result",t.result);m.put("message",step.message());m.put("options",step.options().stream().map(o->Map.of("id",o.id(),"label",o.label())).toList());m.put("transcript",t.transcript);m.put("ticketId",t.ticket==null?null:t.ticket.id);return m;}
  @GetMapping("/admin/users") public Object users(){auth.admin();return users.findAll(Sort.by("name")).stream().map(Dtos::person).toList();}
  @PostMapping("/admin/users") @ResponseStatus(HttpStatus.CREATED) public Object createUser(@Valid @RequestBody UserInput p){auth.admin();checkPassword(p.password());User u=new User();u.passwordHash=encoder.encode(p.password());u.mustChangePassword=true;return updateUser(u,p,false);}
  @PutMapping("/admin/users/{id}") public Object editUser(@PathVariable Long id,@Valid @RequestBody UserInput p){User actor=auth.admin();User u=users.findById(id).orElseThrow(()->error(404,"Usuário não encontrado."));if(actor.id.equals(id)&&(!p.active()||p.role()!=Role.ADMIN))throw error(400,"Você não pode desativar ou rebaixar sua própria conta.");if(u.role==Role.ADMIN&&u.active&&(!p.active()||p.role()!=Role.ADMIN)&&users.countByActiveTrueAndRole(Role.ADMIN)<=1)throw error(400,"Mantenha pelo menos um administrador ativo.");if(p.password()!=null&&!p.password().isBlank()){checkPassword(p.password());u.passwordHash=encoder.encode(p.password());u.mustChangePassword=true;u.authVersion++;}return updateUser(u,p,true);}
  private Person updateUser(User u,UserInput p,boolean editing){Department d=departments.findById(p.departmentId()).filter(x->x.active).orElseThrow(()->error(400,"Departamento inativo ou inexistente."));String email=p.email().trim().toLowerCase(Locale.ROOT);users.findByEmailIgnoreCase(email).ifPresent(existing->{if(!Objects.equals(existing.id,u.id))throw error(409,"Este e-mail já está cadastrado.");});if(editing&&(u.role!=p.role()||u.active!=p.active()||!u.email.equals(email)||!u.department.id.equals(d.id)))u.authVersion++;u.name=p.name().trim();u.email=email;u.role=p.role();u.department=d;u.active=p.active();if(u.active&&UserService.staff(u)){String first=UserService.firstName(u.name);boolean duplicated=users.findByActiveTrue().stream().anyMatch(other->!Objects.equals(other.id,u.id)&&UserService.staff(other)&&UserService.firstName(other.name).equals(first));if(duplicated)throw error(409,"Já existe um integrante da TI com este primeiro nome.");}users.saveAndFlush(u);return Dtos.person(u);}
  @GetMapping("/admin/catalogs") public Object catalogs(){auth.admin();return Map.of("departments",departments.findAll(Sort.by("name")),"categories",categories.findAll(Sort.by("name")));}
  @PostMapping("/admin/departments") public Object newDept(@Valid @RequestBody CatalogInput p){auth.admin();Department d=new Department();d.name=p.name().trim();d.active=p.active();return departments.save(d);}
  @PutMapping("/admin/departments/{id}") public Object editDept(@PathVariable Long id,@Valid @RequestBody CatalogInput p){auth.admin();Department d=departments.findById(id).orElseThrow(()->error(404,"Departamento não encontrado."));d.name=p.name().trim();d.active=p.active();return departments.save(d);}
  @PostMapping("/admin/categories") public Object newCategory(@Valid @RequestBody CatalogInput p){auth.admin();Category c=new Category();c.name=p.name().trim();c.description=p.description();c.active=p.active();return categories.save(c);}
  @PutMapping("/admin/categories/{id}") public Object editCategory(@PathVariable Long id,@Valid @RequestBody CatalogInput p){auth.admin();Category c=categories.findById(id).orElseThrow(()->error(404,"Categoria não encontrada."));c.name=p.name().trim();c.description=p.description();c.active=p.active();return categories.save(c);}
}
