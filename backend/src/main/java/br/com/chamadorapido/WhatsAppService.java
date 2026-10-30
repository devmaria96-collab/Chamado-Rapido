package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class WhatsAppService {
  static final String CHANNEL_EMAIL="whatsapp-canal@chamadorapido.local";
  private static final Set<Status> OPEN=EnumSet.of(Status.ABERTO,Status.EM_ANDAMENTO,Status.AGUARDANDO_USUARIO,Status.AGUARDANDO_TERCEIROS);
  private final WhatsAppProperties props;
  private final WhatsAppClient client;
  private final ObjectMapper json;
  private final TicketRepository tickets;
  private final CommentRepository comments;
  private final UserRepository users;
  private final DepartmentRepository departments;
  private final CategoryRepository categories;
  private final HistoryRepository histories;
  private final NotificationRepository notifications;
  private final PasswordEncoder encoder;
  private final ApplicationEventPublisher events;
  private final ProtocolService protocols;
  public WhatsAppService(WhatsAppProperties props,WhatsAppClient client,ObjectMapper json,TicketRepository tickets,CommentRepository comments,UserRepository users,DepartmentRepository departments,CategoryRepository categories,HistoryRepository histories,NotificationRepository notifications,PasswordEncoder encoder,ApplicationEventPublisher events,ProtocolService protocols){
    this.props=props;this.client=client;this.json=json;this.tickets=tickets;this.comments=comments;this.users=users;this.departments=departments;this.categories=categories;this.histories=histories;this.notifications=notifications;this.encoder=encoder;this.events=events;this.protocols=protocols;
  }
  public Map<String,Object> status() {
    return Map.of("configured",props.configured(),"connected",false,"message",props.configured()
      ?"As credenciais estão no servidor. Ainda não houve um teste real de recebimento e envio, então o WhatsApp não está tratado como conectado."
      :"O WhatsApp não está configurado. As mensagens do painel ficam no chamado e não são enviadas.");
  }
  @Transactional
  public void accept(byte[] body) {
    JsonNode root;try{root=json.readTree(body);}catch(Exception e){return;}
    Set<Long> touched=new LinkedHashSet<>();
    for (JsonNode entry:root.path("entry")) for (JsonNode change:entry.path("changes")) {
      JsonNode value=change.path("value");
      Map<String,String> names=new HashMap<>();
      for (JsonNode contact:value.path("contacts")) names.put(digits(contact.path("wa_id").asText("")),clean(contact.path("profile").path("name").asText(""),120));
      for (JsonNode message:value.path("messages")) touched.addAll(message(message,names));
      for (JsonNode status:value.path("statuses")) status(status);
    }
    touched.forEach(id->events.publishEvent(new DeskEvent("whatsapp",id)));
  }
  @Transactional
  public void deliver(Comment comment) {
    Ticket ticket=comment.ticket;
    if (ticket.channel!=Channel.WHATSAPP||comment.internal) return;
    comment.channel=Channel.WHATSAPP;
    comment.direction=Direction.OUT;
    String phone=ticket.whatsappPhone;
    if (phone==null||phone.isBlank()) {fail(comment,"Chamado sem número de WhatsApp.");return;}
    if (ticket.whatsappWindowUntil==null||!ticket.whatsappWindowUntil.isAfter(Instant.now())) {fail(comment,"Fora da janela de 24 horas. A Meta só aceita texto livre dentro dessa janela, e não há modelo aprovado configurado para esta resposta.");return;}
    WhatsAppClient.Result result=client.sendText(phone,comment.body);
    if (!result.accepted()||result.externalId()==null||result.externalId().isBlank()) {fail(comment,result.error()==null?"Falha no envio.":result.error());return;}
    if (comments.findByExternalId(result.externalId()).filter(found->!found.id.equals(comment.id)).isPresent()) {fail(comment,"A API devolveu um identificador já usado.");return;}
    comment.externalId=result.externalId();
    comment.deliveryStatus=Delivery.SENT;
    comment.failureReason=null;
    comments.save(comment);
  }
  private Set<Long> message(JsonNode message,Map<String,String> names) {
    String external=clean(message.path("id").asText(""),128);
    String phone=digits(message.path("from").asText(""));
    if (external.isBlank()||phone.length()<8||phone.length()>20) return Set.of();
    if (comments.findByExternalId(external).isPresent()) return Set.of();
    String type=message.path("type").asText("");
    String text="text".equals(type)?message.path("text").path("body").asText(""):"Mensagem de "+clean(type,24)+" recebida. O arquivo ainda não é baixado nesta versão.";
    text=clean(text,6000);
    if (text.isBlank()) return Set.of();
    User channel=channelUser();
    String name=names.getOrDefault(phone,"");
    if (name.isBlank()) name="Colaborador";
    Ticket ticket=conversation(phone,name,text,channel);
    if (ticket==null) return Set.of();
    Comment comment=new Comment();
    comment.ticket=ticket;comment.author=channel;comment.body=text;comment.internal=false;comment.channel=Channel.WHATSAPP;comment.direction=Direction.IN;comment.externalId=external;
    String stamp=message.path("timestamp").asText("");
    if (stamp.matches("\\d{10,13}")) {long seconds=Long.parseLong(stamp);if (stamp.length()>10) seconds=seconds/1000;comment.createdAt=Instant.ofEpochSecond(seconds);}
    comments.save(comment);
    ticket.whatsappWindowUntil=Instant.now().plus(Duration.ofHours(24));
    ticket.updatedAt=Instant.now();
    tickets.save(ticket);
    return Set.of(ticket.id);
  }
  private Ticket conversation(String phone,String name,String text,User channel) {
    List<Ticket> same=tickets.findByWhatsappPhoneOrderByIdDesc(phone);
    Ticket open=same.stream().filter(t->OPEN.contains(t.status)).findFirst().orElse(null);
    if (open!=null) return open;
    Ticket last=same.isEmpty()?null:same.get(0);
    if (last!=null&&(last.status==Status.RESOLVIDO||last.status==Status.FECHADO)) {
      Instant when=last.resolvedAt!=null?last.resolvedAt:last.closedAt;
      if (when!=null&&when.isAfter(Instant.now().minus(Duration.ofHours(24)))) {
        last.status=Status.EM_ANDAMENTO;last.resolvedAt=null;last.closedAt=null;last.rating=null;last.updatedAt=Instant.now();
        history(last,channel,"Chamado reaberto","Nova mensagem do WhatsApp em até 24 horas após a resolução.");
        return tickets.save(last);
      }
    }
    java.util.List<Category> allCategories=categories.findAll();
    if (allCategories.isEmpty()||departments.findAll().isEmpty()) return null;
    Category category=allCategories.stream().filter(c->"Outros".equalsIgnoreCase(c.name)).findFirst().orElse(allCategories.get(0));
    Ticket ticket=new Ticket();
    ticket.channel=Channel.WHATSAPP;ticket.whatsappPhone=phone;ticket.requester=channel;ticket.requesterName=name;ticket.department=channel.department;ticket.category=category;
    ticket.priority=Priority.MEDIA;ticket.title=titleOf(text);ticket.description=text.length()<15?text+" Mensagem recebida pelo WhatsApp.":text;ticket.contact=phone;
    ticket.slaStartedAt=ticket.createdAt;ticket.dueAt=TicketRules.deadline(ticket.slaStartedAt,ticket.priority.name());
    protocols.assign(ticket);
    tickets.save(ticket);
    history(ticket,channel,"Chamado aberto","Solicitação recebida pelo WhatsApp.");
    for (User technician:users.findByActiveTrueAndRoleNot(Role.COLABORADOR)) notify(technician,ticket,"Novo chamado pelo WhatsApp");
    return ticket;
  }
  private void status(JsonNode status) {
    String external=clean(status.path("id").asText(""),128);
    if (external.isBlank()) return;
    Comment comment=comments.findByExternalId(external).orElse(null);
    if (comment==null||comment.direction!=Direction.OUT) return;
    String value=status.path("status").asText("");
    if ("read".equals(value)) comment.deliveryStatus=Delivery.READ;
    else if ("delivered".equals(value)) { if (comment.deliveryStatus!=Delivery.READ) comment.deliveryStatus=Delivery.DELIVERED; }
    else if ("sent".equals(value)) { if (comment.deliveryStatus==null||comment.deliveryStatus==Delivery.FAILED) comment.deliveryStatus=Delivery.SENT; }
    else if ("failed".equals(value)) { comment.deliveryStatus=Delivery.FAILED; comment.failureReason=clean(status.path("errors").path(0).path("title").asText("Falha confirmada pela API."),300); }
    else return;
    if (comment.deliveryStatus!=Delivery.FAILED) comment.failureReason=null;
    comments.save(comment);
    events.publishEvent(new DeskEvent("status",comment.ticket.id));
  }
  private void fail(Comment comment,String reason){comment.deliveryStatus=Delivery.FAILED;comment.failureReason=clean(reason,300);comments.save(comment);}
  private User channelUser() {
    return users.findByEmailIgnoreCase(CHANNEL_EMAIL).orElseGet(()->{
      Department department=departments.findByName("Tecnologia da Informação").orElseGet(()->departments.findAll().get(0));
      User user=new User();user.name="Canal WhatsApp";user.email=CHANNEL_EMAIL;user.role=Role.COLABORADOR;user.department=department;user.active=false;user.mustChangePassword=true;user.passwordHash=encoder.encode(UUID.randomUUID().toString());
      return users.save(user);
    });
  }
  private void history(Ticket ticket,User actor,String action,String details){History history=new History();history.ticket=ticket;history.actor=actor;history.action=action;history.details=details;histories.save(history);}
  private void notify(User to,Ticket ticket,String title){Notification notification=new Notification();notification.recipient=to;notification.ticket=ticket;notification.title=title;notifications.save(notification);}
  static String digits(String value){return value==null?"":value.replaceAll("\\D","");}
  private static String titleOf(String text){String flat=text.replaceAll("\\s+"," ").trim();if (flat.length()<5) flat="WhatsApp: "+flat;return flat.length()>180?flat.substring(0,180):flat;}
  private static String clean(String value,int max){if (value==null) return "";String flat=value.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]","").trim();return flat.length()>max?flat.substring(0,max):flat;}
}
