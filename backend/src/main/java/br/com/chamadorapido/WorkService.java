package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import br.com.chamadorapido.Dtos.*;
import static br.com.chamadorapido.UserService.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
@Service
@Transactional
public class WorkService {
  final UserService auth; final TicketRepository tickets; final UserRepository users; final DepartmentRepository departments;
  final CategoryRepository categories; final CommentRepository comments; final HistoryRepository histories;
  final NotificationRepository notifications; final AttachmentRepository attachments; final TriageRepository triages; final WhatsAppService whatsapp; final ApplicationEventPublisher events; final ProtocolService protocols;
  public WorkService(UserService auth,TicketRepository tickets,UserRepository users,DepartmentRepository departments,CategoryRepository categories,CommentRepository comments,HistoryRepository histories,NotificationRepository notifications,AttachmentRepository attachments,TriageRepository triages,WhatsAppService whatsapp,ApplicationEventPublisher events,ProtocolService protocols){this.auth=auth;this.tickets=tickets;this.users=users;this.departments=departments;this.categories=categories;this.comments=comments;this.histories=histories;this.notifications=notifications;this.attachments=attachments;this.triages=triages;this.whatsapp=whatsapp;this.events=events;this.protocols=protocols;}
  public Ticket visible(Long id,User u){Ticket t=tickets.findById(id).orElseThrow(()->error(404,"Chamado não encontrado."));if(!staff(u)&&!t.requester.id.equals(u.id))throw error(404,"Chamado não encontrado.");return t;}
  public Ticket locked(Long id,User u){Ticket t=tickets.findLockedById(id).orElseThrow(()->error(404,"Chamado nao encontrado."));if(!staff(u)&&!t.requester.id.equals(u.id))throw error(404,"Chamado não encontrado.");return t;}
  public void history(Ticket t,User actor,String action,String details){History h=new History();h.ticket=t;h.actor=actor;h.action=action;h.details=details;histories.save(h);}
  public void notify(User to,Ticket t,String title){Notification n=new Notification();n.recipient=to;n.ticket=t;n.title=title;notifications.save(n);}
  private void notifyParticipants(Ticket t,User actor,String title){if(!t.requester.id.equals(actor.id))notify(t.requester,t,title);if(t.assignee!=null&&!t.assignee.id.equals(actor.id)&&!t.assignee.id.equals(t.requester.id))notify(t.assignee,t,title);}
  public Specification<Ticket> scope(User u,Map<String,String> f){return (root,query,cb)->{
    List<Predicate> p=new ArrayList<>();
    if(!staff(u))p.add(cb.equal(root.get("requester").get("id"),u.id));
    if("true".equals(f.get("mine")))p.add(cb.equal(root.get(staff(u)?"assignee":"requester").get("id"),u.id));
    for(String key:List.of("status","priority")){String v=f.get(key);if(v!=null&&!v.isBlank())p.add(cb.equal(root.get(key),key.equals("status")?Status.valueOf(v):Priority.valueOf(v)));}
    for(String key:List.of("department","category","assignee")){String v=f.get(key);if(v!=null&&!v.isBlank())p.add(cb.equal(root.get(key).get("id"),Long.valueOf(v)));}
    if(f.get("days")!=null&&!f.get("days").equals("0")){int days=Integer.parseInt(f.get("days"));if(days<1||days>3660)throw error(400,"Período inválido.");p.add(cb.greaterThanOrEqualTo(root.get("createdAt"),Instant.now().minus(Duration.ofDays(days))));}
    String q=f.getOrDefault("q","").trim();
    if(q.length()>200)throw error(400,"Pesquisa muito longa.");
    if(!q.isBlank()){
      if(q.matches("(?i)CR-\\d+")){p.add(cb.equal(root.get("id"),Long.parseLong(q.substring(3))));}
      else {String like="%"+q.toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";Predicate text=cb.or(cb.like(cb.lower(root.get("title")),like,'\\'),cb.like(cb.lower(root.get("requester").get("name")),like,'\\'),cb.like(cb.lower(cb.coalesce(root.get("requesterName"),"")),like,'\\'),cb.like(cb.lower(root.get("department").get("name")),like,'\\'),cb.like(cb.lower(cb.coalesce(root.get("whatsappPhone"),"")),like,'\\'),cb.like(cb.lower(cb.coalesce(root.get("contact"),"")),like,'\\'));if(q.matches("\\d{1,6}"))p.add(cb.or(text,cb.equal(root.get("protocolNo"),Integer.parseInt(q))));else p.add(text);}
    }
    if("true".equals(f.get("board")))p.add(root.get("status").in(Status.ABERTO,Status.EM_ANDAMENTO,Status.AGUARDANDO_USUARIO,Status.AGUARDANDO_TERCEIROS,Status.RESOLVIDO,Status.FECHADO));
    if("true".equals(f.get("overdue")))p.add(cb.and(cb.lessThan(root.get("dueAt"),Instant.now()),root.get("status").in(Status.ABERTO,Status.EM_ANDAMENTO,Status.AGUARDANDO_USUARIO,Status.AGUARDANDO_TERCEIROS)));
    return cb.and(p.toArray(Predicate[]::new));
  };}
  @Transactional(readOnly=true)
  public Map<String,Object> list(Map<String,String> filters,int page,int size){User u=auth.operational();boolean board="true".equals(filters.get("board"));int limit=board?500:200;Sort sort=Sort.by(Sort.Direction.DESC,board?"updatedAt":"createdAt","id");Page<Ticket> found=tickets.findAll(scope(u,filters),PageRequest.of(Math.max(0,page),Math.max(1,Math.min(limit,size)),sort));java.util.List<Long> ids=found.getContent().stream().map(t->t.id).toList();Map<Long,Direction> last=new HashMap<>();if(!ids.isEmpty())for(Comment comment:comments.findByInternalFalseAndTicketIdInOrderByIdDesc(ids))last.putIfAbsent(comment.ticket.id,comment.direction);Map<String,Object> body=new LinkedHashMap<>();body.put("items",found.getContent().stream().map(t->Dtos.ticket(t,last.get(t.id)==Direction.IN)).toList());body.put("total",found.getTotalElements());body.put("pages",found.getTotalPages());body.put("page",found.getNumber());if(board&&staff(u)){ZoneId zone=ZoneId.of("America/Sao_Paulo");ZonedDateTime start=LocalDate.now(zone).atStartOfDay(zone);long today=tickets.countByStatusInAndResolvedAtGreaterThanEqualAndResolvedAtLessThan(List.of(Status.RESOLVIDO,Status.FECHADO),start.toInstant(),start.plusDays(1).toInstant());body.put("counts",Map.of("novos",tickets.countByStatus(Status.ABERTO),"atendimento",tickets.countByStatus(Status.EM_ANDAMENTO),"aguardando",tickets.countByStatusIn(List.of(Status.AGUARDANDO_USUARIO,Status.AGUARDANDO_TERCEIROS)),"resolvidos",tickets.countByStatusIn(List.of(Status.RESOLVIDO,Status.FECHADO)),"resolvidosHoje",today));}return body;}
  @Transactional(readOnly=true)
  public Detail detail(Long id){User u=auth.operational();Ticket t=visible(id,u);return new Detail(Dtos.ticket(t),comments.findByTicketIdOrderByCreatedAtAsc(id).stream().filter(c->staff(u)||!c.internal).map(this::commentView).toList(),histories.findByTicketIdOrderByCreatedAtAsc(id).stream().filter(h->staff(u)||!h.action.equals("Nota interna")).map(h->new HistoryView(h.id,h.actor.name,h.action,h.details,h.createdAt)).toList(),attachments.findByTicketIdOrderByCreatedAtAsc(id).stream().map(a->new AttachmentView(a.id,a.originalName,a.mime,a.size,a.author.name,a.createdAt)).toList(),TicketRules.TRANSITIONS.get(t.status.name()).stream().filter(s->TicketRules.canTransition(t.status.name(),s,staff(u))).sorted().toList());}
  private CommentView commentView(Comment c){String author=c.direction==Direction.IN&&c.ticket.requesterName!=null&&!c.ticket.requesterName.isBlank()?c.ticket.requesterName:c.author.name;Role role=c.direction==Direction.IN?Role.COLABORADOR:c.author.role;return new CommentView(c.id,author,role,c.body,c.internal,c.createdAt,c.channel.name(),c.direction.name(),c.deliveryStatus==null?null:c.deliveryStatus.name(),c.failureReason);}
  public TicketView create(TicketInput input){
    User u=auth.operational();if(input.title().trim().length()<5||input.description().trim().length()<15)throw error(400,"Informe um titulo com 5 caracteres e uma descricao com 15 caracteres ou mais.");if(!u.department.active)throw error(400,"Seu departamento está inativo. Consulte o administrador.");
    Category c=categories.findById(input.categoryId()).filter(x->x.active).orElseThrow(()->error(400,"Selecione uma categoria ativa."));
    String solicitante=input.requesterName().trim();if(solicitante.length()<3)throw error(400,"Informe o nome completo de quem está abrindo o chamado.");
    Ticket t=new Ticket();t.title=input.title().trim();t.description=input.description().trim();t.requester=u;t.requesterName=solicitante;t.department=u.department;t.category=c;t.priority=input.priority();t.equipment=input.equipment();t.location=input.location();t.contact=input.contact();t.slaStartedAt=t.createdAt;t.dueAt=TicketRules.deadline(t.slaStartedAt,t.priority.name());
    Triage triage=null;if(input.triageId()!=null){triage=triages.findById(input.triageId()).orElseThrow(()->error(404,"Triagem indisponível."));if(!triage.owner.id.equals(u.id)||!triage.result.equals("ENCAMINHADO")||triage.ticket!=null)throw error(400,"Esta triagem não pode originar outro chamado.");t.description+="\n\n--- Resumo do assistente ---\n"+triage.transcript;}
    protocols.assign(t);tickets.saveAndFlush(t);if(triage!=null){triage.ticket=t;triages.saveAndFlush(triage);}
    history(t,u,"Chamado aberto","Solicitação de "+t.requesterName+" para "+t.department.name);
    for(User technician:users.findByActiveTrueAndRoleNot(Role.COLABORADOR))if(!technician.id.equals(u.id))notify(technician,t,"Novo chamado: "+t.title);
    events.publishEvent(new DeskEvent("ticket",t.id));
    return Dtos.ticket(t);
  }
  public TicketView patch(Long id,PatchInput p){
    User u=auth.operational();Ticket t=visible(id,u);if(!Objects.equals(t.version,p.version()))throw error(409,"Este chamado foi alterado. Reabra-o antes de salvar.");
    if(!staff(u)&&(p.priority()!=null||p.assigneeId()!=null||p.unassign()))throw error(403,"Somente a TI pode atribuir e priorizar chamados.");
    if(TicketRules.terminal(t.status.name())&&(p.priority()!=null||p.assigneeId()!=null||p.unassign()))throw error(400,"Reabra o chamado antes de alterar a atribuição ou prioridade.");
    boolean changed=false;
    if(p.priority()!=null&&p.priority()!=t.priority){String old=t.priority.name();t.priority=p.priority();t.dueAt=TicketRules.deadline(t.slaStartedAt,t.priority.name());history(t,u,"Prioridade alterada",old+" -> "+t.priority);changed=true;}
    if(p.unassign()&&t.assignee!=null){t.assignee=null;history(t,u,"Responsável removido","Chamado devolvido à fila.");changed=true;}
    if(p.assigneeId()!=null&&(t.assignee==null||!t.assignee.id.equals(p.assigneeId()))){User a=users.findById(p.assigneeId()).filter(x->x.active&&staff(x)).orElseThrow(()->error(400,"Técnico indisponível."));t.assignee=a;history(t,u,"Chamado atribuído",a.name);if(!a.id.equals(u.id))notify(a,t,"Você recebeu o chamado "+Dtos.protocol(t));changed=true;}
    if(p.status()!=null&&p.status()!=t.status){
      if(!staff(u))throw error(403,"Somente a equipe de TI pode alterar o status do chamado.");
      Status old=t.status;if(!TicketRules.canTransition(old.name(),p.status().name(),true))throw error(400,"Transição de status não permitida.");
      boolean reopen=(old==Status.RESOLVIDO||old==Status.FECHADO)&&p.status()==Status.EM_ANDAMENTO;
      if((p.status()==Status.CANCELADO||reopen)&&(p.reason()==null||p.reason().trim().length()<10))throw error(400,"Informe uma justificativa com pelo menos 10 caracteres.");
      if(p.status()==Status.RESOLVIDO){if(p.solution()==null||p.solution().trim().length()<10)throw error(400,"Descreva a solução em pelo menos 10 caracteres.");t.solution=p.solution().trim();t.resolvedAt=Instant.now();}
      if(p.status()==Status.EM_ANDAMENTO){if(t.assignee==null&&staff(u))t.assignee=u;if(reopen){t.resolvedAt=null;t.closedAt=null;t.rating=null;t.slaStartedAt=Instant.now();t.dueAt=TicketRules.deadline(t.slaStartedAt,t.priority.name());}}
      if(p.status()==Status.FECHADO||p.status()==Status.CANCELADO)t.closedAt=Instant.now();
      t.status=p.status();history(t,u,reopen?"Chamado reaberto":"Status atualizado",old+" -> "+t.status+(p.reason()==null?"":". "+p.reason()));changed=true;
    }
    if(changed){t.updatedAt=Instant.now();if(staff(u)&&t.firstResponseAt==null)t.firstResponseAt=Instant.now();tickets.saveAndFlush(t);notifyParticipants(t,u,"Chamado atualizado: "+t.title);events.publishEvent(new DeskEvent("ticket",t.id));}
    return Dtos.ticket(t);
  }
  public void comment(Long id,CommentInput p){User u=auth.operational();if(!staff(u))throw error(403,"Somente a equipe de TI registra mensagens no chamado.");Ticket t=visible(id,u);if(p.internal()&&!staff(u))throw error(403,"Notas internas são exclusivas da TI.");if(t.status==Status.CANCELADO||t.status==Status.FECHADO)throw error(400,"Chamado encerrado. Reabra-o para continuar a conversa.");Comment c=new Comment();c.ticket=t;c.author=u;c.body=p.body().trim();c.internal=p.internal();if(t.channel==Channel.WHATSAPP&&!c.internal){c.channel=Channel.WHATSAPP;c.direction=Direction.OUT;}comments.saveAndFlush(c);if(c.channel==Channel.WHATSAPP&&!c.internal)whatsapp.deliver(c);t.updatedAt=Instant.now();if(staff(u)&&t.firstResponseAt==null)t.firstResponseAt=Instant.now();history(t,u,p.internal()?"Nota interna":"Mensagem enviada",p.internal()?"Registro exclusivo da equipe de TI.":"Nova mensagem na conversa.");if(!p.internal())notifyParticipants(t,u,"Nova mensagem em "+Dtos.protocol(t));events.publishEvent(new DeskEvent("message",t.id));}
  public void rate(Long id,RatingInput input){User u=auth.operational();Ticket t=visible(id,u);if(!t.requester.id.equals(u.id))throw error(403,"Apenas o solicitante pode avaliar.");if(t.status!=Status.RESOLVIDO&&t.status!=Status.FECHADO)throw error(400,"Avalie após a resolução.");t.rating=input.rating();history(t,u,"Atendimento avaliado",input.rating()+" de 5 estrelas");}
  @Transactional(readOnly=true)
  public Map<String,Object> summary(Map<String,String> filters){User u=auth.staff();List<Ticket> all=tickets.findAll(scope(u,filters));List<Ticket> solved=all.stream().filter(t->t.resolvedAt!=null&&t.status!=Status.CANCELADO).toList();long active=all.stream().filter(t->!TicketRules.terminal(t.status.name())).count();long late=all.stream().filter(t->!TicketRules.terminal(t.status.name())&&t.dueAt.isBefore(Instant.now())).count();long onTime=solved.stream().filter(t->!t.resolvedAt.isAfter(t.dueAt)).count();double hours=solved.stream().mapToLong(t->Duration.between(t.createdAt,t.resolvedAt).toMinutes()).average().orElse(0)/60;
    List<Map<String,Object>> daily=new ArrayList<>();ZoneId zone=ZoneId.of("America/Sao_Paulo");for(int i=6;i>=0;i--){LocalDate d=LocalDate.now(zone).minusDays(i);daily.add(Map.of("date",d.toString(),"created",all.stream().filter(t->t.createdAt.atZone(zone).toLocalDate().equals(d)).count(),"resolved",solved.stream().filter(t->t.resolvedAt.atZone(zone).toLocalDate().equals(d)).count()));}
    List<Map<String,Object>> byStatus=Arrays.stream(Status.values()).map(s->Map.<String,Object>of("status",s.name(),"count",all.stream().filter(t->t.status==s).count())).toList();
    return Map.of("total",all.size(),"active",active,"overdue",late,"resolved",solved.size(),"slaPercent",solved.isEmpty()?100:Math.round(100.0*onTime/solved.size()),"averageResolutionHours",Math.round(hours*10)/10.0,"daily",daily,"byStatus",byStatus,"averageRating",Math.round(all.stream().filter(t->t.rating!=null).mapToInt(t->t.rating).average().orElse(0)*10)/10.0);
  }
  @Transactional(readOnly=true)
  public List<Map<String,Object>> departmentSummary(){auth.staff();List<Ticket> all=tickets.findAll();return departments.findAll(Sort.by("name")).stream().map(d->Map.<String,Object>of("id",d.id,"name",d.name,"active",d.active,"total",all.stream().filter(t->t.department.id.equals(d.id)).count(),"open",all.stream().filter(t->t.department.id.equals(d.id)&&!TicketRules.terminal(t.status.name())).count(),"overdue",all.stream().filter(t->t.department.id.equals(d.id)&&!TicketRules.terminal(t.status.name())&&t.dueAt.isBefore(Instant.now())).count())).toList();}
  @Transactional(readOnly=true)
  public String csv(Map<String,String> filters){User u=auth.staff();List<Ticket> data=tickets.findAll(scope(u,filters),Sort.by(Sort.Direction.DESC,"createdAt"));StringBuilder b=new StringBuilder("﻿Protocolo;Titulo;Departamento;Solicitante;Responsavel;Categoria;Prioridade;Status;Abertura;Prazo;Resolucao;Nota\r\n");for(Ticket t:data){b.append(Stream.of(Dtos.protocol(t),t.title,t.department.name,Dtos.solicitante(t),t.assignee==null?"":t.assignee.name,t.category.name,t.priority.name(),t.status.name(),t.createdAt.toString(),t.dueAt.toString(),t.resolvedAt==null?"":t.resolvedAt.toString(),t.rating==null?"":t.rating.toString()).map(TicketRules::csv).collect(Collectors.joining(";"))).append("\r\n");}return b.toString();}
}
