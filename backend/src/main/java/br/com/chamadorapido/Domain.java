package br.com.chamadorapido;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
/** Entidades internas. Controllers retornam DTOs, nunca entidades com credenciais. */
public final class Domain {
  private Domain() {}
  public enum Role { ADMIN, TECNICO, COLABORADOR }
  public enum Status { ABERTO, EM_ANDAMENTO, AGUARDANDO_USUARIO, AGUARDANDO_TERCEIROS, RESOLVIDO, FECHADO, CANCELADO }
  public enum Priority { BAIXA, MEDIA, ALTA, CRITICA }
  @Entity @Table(name="departments")
  public static class Department {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,unique=true,length=80) public String name;
    @Column(nullable=false) public boolean active=true;
  }
  @Entity @Table(name="app_users")
  public static class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,length=100) public String name;
    @Column(nullable=false,unique=true,length=180) public String email;
    @Column(nullable=false,length=100) public String passwordHash;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable=false,length=24) public Role role;
    @ManyToOne(optional=false) public Department department;
    @Column(nullable=false) public boolean active=true;
    @Column(nullable=false) public boolean mustChangePassword=false;
    @Column(nullable=false) public int authVersion=0;
    @Column(nullable=false) public Instant createdAt=Instant.now();
  }
  @Entity @Table(name="categories")
  public static class Category {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,unique=true,length=80) public String name;
    @Column(length=200) public String description;
    @Column(nullable=false) public boolean active=true;
  }
  @Entity @Table(name="tickets")
  public static class Ticket {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Version public Long version;
    @Column(nullable=false,length=180) public String title;
    @Column(nullable=false,columnDefinition="text") public String description;
    @ManyToOne(optional=false) public User requester;
    @ManyToOne(optional=false) public Department department;
    @ManyToOne(optional=false) public Category category;
    @ManyToOne public User assignee;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable=false,length=30) public Status status=Status.ABERTO;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) @Column(nullable=false,length=16) public Priority priority=Priority.MEDIA;
    @Column(length=120) public String location;
    @Column(length=120) public String equipment;
    @Column(length=80) public String contact;
    @Column(columnDefinition="text") public String solution;
    public Integer rating;
    @Column(nullable=false) public Instant createdAt=Instant.now();
    @Column(nullable=false) public Instant updatedAt=Instant.now();
    @Column(nullable=false) public Instant slaStartedAt=Instant.now();
    @Column(nullable=false) public Instant dueAt;
    public Instant firstResponseAt;
    public Instant resolvedAt;
    public Instant closedAt;
  }
  @Entity @Table(name="comments")
  public static class Comment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional=false) public Ticket ticket;
    @ManyToOne(optional=false) public User author;
    @Column(nullable=false,columnDefinition="text") public String body;
    @Column(nullable=false) public boolean internal=false;
    @Column(nullable=false) public Instant createdAt=Instant.now();
  }
  @Entity @Table(name="ticket_history")
  public static class History {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional=false) public Ticket ticket;
    @ManyToOne(optional=false) public User actor;
    @Column(nullable=false,length=100) public String action;
    @Column(length=1000) public String details;
    @Column(nullable=false) public Instant createdAt=Instant.now();
  }
  @Entity @Table(name="attachments")
  public static class Attachment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional=false) public Ticket ticket;
    @ManyToOne(optional=false) public User author;
    @Column(nullable=false,length=180) public String originalName;
    @Column(nullable=false,unique=true,length=80) public String storageKey;
    @Column(nullable=false,length=80) public String mime;
    @Column(nullable=false) public long size;
    @Column(nullable=false) public Instant createdAt=Instant.now();
  }
  @Entity @Table(name="notifications")
  public static class Notification {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional=false) public User recipient;
    @ManyToOne public Ticket ticket;
    @Column(nullable=false,length=220) public String title;
    @Column(name="is_read",nullable=false) public boolean read=false;
    @Column(nullable=false) public Instant createdAt=Instant.now();
  }
  @Entity @Table(name="articles")
  public static class Article {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,length=180) public String title;
    @Column(nullable=false,length=300) public String excerpt;
    @Column(nullable=false,columnDefinition="text") public String body;
    @Column(nullable=false,length=80) public String category;
    @Column(nullable=false) public boolean active=true;
    @Column(nullable=false) public Instant updatedAt=Instant.now();
  }
  @Entity @Table(name="triages")
  public static class Triage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Version public Long version;
    @ManyToOne(optional=false) public User owner;
    @Column(nullable=false,length=40) public String category;
    @Column(nullable=false,length=40) public String node;
    @Column(nullable=false,length=30) public String result="EM_CURSO";
    @Column(nullable=false,columnDefinition="text") public String transcript="";
    @OneToOne public Ticket ticket;
    @Column(nullable=false) public Instant createdAt=Instant.now();
    public Instant finishedAt;
  }
}
