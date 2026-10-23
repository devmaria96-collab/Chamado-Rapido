package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
public final class Dtos {
  private Dtos(){}
  public record Person(Long id,String name,String email,Role role,Long departmentId,String department,boolean active,boolean mustChangePassword){}
  public static Person person(User u){return new Person(u.id,u.name,u.email,u.role,u.department.id,u.department.name,u.active,u.mustChangePassword);}
  public record TicketInput(@NotBlank @Size(min=5,max=180) String title,@NotBlank @Size(min=15,max=12000) String description,@NotNull Long categoryId,@NotNull Priority priority,@Size(max=120) String location,@Size(max=120) String equipment,@Size(max=80) String contact,Long triageId){}
  public record PatchInput(@NotNull Long version,Status status,Priority priority,Long assigneeId,boolean unassign,@Size(max=12000) String solution,@Size(max=1000) String reason){}
  public record CommentInput(@NotBlank @Size(min=2,max=6000) String body,boolean internal){}
  public record UserInput(@NotBlank @Size(min=2,max=100) String name,@NotBlank @Email @Size(max=180) String email,@NotNull Role role,@NotNull Long departmentId,@Size(max=72) String password,boolean active){}
  public record PasswordInput(@NotBlank String currentPassword,@NotBlank @Size(min=12,max=72) String newPassword){}
  public record CatalogInput(@NotBlank @Size(min=2,max=80) String name,@Size(max=200) String description,boolean active){}
  public record ArticleInput(@NotBlank @Size(min=5,max=180) String title,@NotBlank @Size(max=300) String excerpt,@NotBlank @Size(min=20,max=12000) String body,@NotBlank @Size(max=80) String category,boolean active){}
  public record RatingInput(@NotNull @Min(1) @Max(5) Integer rating){}
  public record TriageStart(@NotBlank String category){}
  public record TriageAnswer(@NotBlank String choice,@NotNull Long version){}
  public record TicketView(Long id,Long version,String protocol,String title,String description,Long requesterId,String requester,Long departmentId,String department,Long categoryId,String category,Long assigneeId,String assignee,Status status,Priority priority,String location,String equipment,String contact,String solution,Integer rating,Instant createdAt,Instant updatedAt,Instant dueAt,Instant firstResponseAt,Instant resolvedAt,Instant closedAt,boolean overdue){}
  public static TicketView ticket(Ticket t){return new TicketView(t.id,t.version,String.format("CR-%06d",t.id),t.title,t.description,t.requester.id,t.requester.name,t.department.id,t.department.name,t.category.id,t.category.name,t.assignee==null?null:t.assignee.id,t.assignee==null?null:t.assignee.name,t.status,t.priority,t.location,t.equipment,t.contact,t.solution,t.rating,t.createdAt,t.updatedAt,t.dueAt,t.firstResponseAt,t.resolvedAt,t.closedAt,!TicketRules.terminal(t.status.name())&&t.dueAt.isBefore(Instant.now()));}
  public record CommentView(Long id,String author,Role role,String body,boolean internal,Instant createdAt){}
  public record HistoryView(Long id,String actor,String action,String details,Instant createdAt){}
  public record AttachmentView(Long id,String name,String mime,long size,String author,Instant createdAt){}
  public record Detail(TicketView ticket,List<CommentView> comments,List<HistoryView> history,List<AttachmentView> attachments,List<String> transitions){}
}
