package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import java.time.*;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
public class Bootstrap implements ApplicationRunner {
  private final UserRepository users; private final DepartmentRepository departments; private final CategoryRepository categories; private final TicketRepository tickets; private final ArticleRepository articles; private final HistoryRepository history; private final CommentRepository comments; private final NotificationRepository notifications; private final PasswordEncoder encoder;
  private final boolean demo; private final String adminEmail; private final String adminPassword;
  public Bootstrap(UserRepository users,DepartmentRepository departments,CategoryRepository categories,TicketRepository tickets,ArticleRepository articles,HistoryRepository history,CommentRepository comments,NotificationRepository notifications,PasswordEncoder encoder,@Value("${app.demo}")boolean demo,@Value("${app.admin-email}")String adminEmail,@Value("${app.admin-password}")String adminPassword){this.users=users;this.departments=departments;this.categories=categories;this.tickets=tickets;this.articles=articles;this.history=history;this.comments=comments;this.notifications=notifications;this.encoder=encoder;this.demo=demo;this.adminEmail=adminEmail;this.adminPassword=adminPassword;}
  @Override @Transactional public void run(ApplicationArguments args){
    if(users.count()>0)return;
    List<Department> ds=new ArrayList<>();for(String name:List.of("Recepção","Diretoria","Cobrança","Assistentes","RH","Telefonista","Jurídico","Financeiro","Compras","Contabilidade","Conferência","Tecnologia da Informação")){Department d=new Department();d.name=name;ds.add(departments.save(d));}
    List<Category> cs=new ArrayList<>();for(String name:List.of("Computadores","Rede e internet","Impressoras","Sistemas e acessos","E-mail e comunicação","Instalação de software","Telefonia","Outros")){Category c=new Category();c.name=name;c.description="Suporte para "+name.toLowerCase(Locale.ROOT);cs.add(categories.save(c));}
    String password=demo?"Demo@Rapido2026!":adminPassword;
    String email=demo?"admin@chamadorapido.local":adminEmail;
    if(email.isBlank()||!email.contains("@"))throw new IllegalStateException("Defina ADMIN_EMAIL para o primeiro acesso.");UserService.checkPassword(password);
    String hash=encoder.encode(password);User admin=user("Administrador",email,Role.ADMIN,ds.get(11),hash);admin.mustChangePassword=!demo;users.save(admin);
    addArticles();if(!demo)return;
    User tech=user("Equipe de TI","ti@chamadorapido.local",Role.TECNICO,ds.get(11),hash);
    String[] names={"Ana Martins","Daniel Costa","Juliana Santos","Rafael Lima","Camila Ferreira","Beatriz Oliveira","Marcos Ribeiro","Luiza Almeida","Bruno Souza","Patrícia Mendes","Felipe Rocha"};
    String[] emails={"recepcao","diretoria","cobranca","assistentes","rh","telefonista","juridico","financeiro","compras","contabilidade","conferencia"};
    List<User> people=new ArrayList<>();for(int i=0;i<11;i++)people.add(user(names[i],emails[i]+"@chamadorapido.local",Role.COLABORADOR,ds.get(i),hash));
    String[] titles={"Impressora não imprime os documentos","Sem acesso ao sistema financeiro","Computador reiniciando durante o uso","Configurar e-mail no novo computador","Internet instável no departamento","Instalação de leitor de PDF","Monitor sem imagem na estação","Ramal sem receber ligações","Acesso à pasta compartilhada","Planilha não abre no computador","Atualização do sistema de cobrança","Configuração de impressora de etiquetas"};
    int[] cat={2,3,0,4,1,5,0,6,3,0,3,2};
    Instant now=Instant.now();
    for(int i=0;i<36;i++){
      User requester=people.get(i%11);Ticket t=new Ticket();t.title=titles[i%12];t.description="Solicito apoio da equipe de TI. "+t.title+". O problema está dificultando as atividades do setor. Já conferimos os procedimentos básicos. Dados fictícios para demonstração.";t.requester=requester;t.department=requester.department;t.category=cs.get(cat[i%12]);t.priority=Priority.values()[i%4];t.location="Estação "+(i%8+1)+" - "+requester.department.name;t.equipment="PC-"+String.format("%03d",i+1);t.contact="Ramal "+(200+i);t.createdAt=now.minus(Duration.ofHours(i*5L+1));t.updatedAt=t.createdAt.plusSeconds(1800);t.slaStartedAt=t.createdAt;t.dueAt=TicketRules.deadline(t.slaStartedAt,t.priority.name());
      int state=i%9;t.status=state<2?Status.ABERTO:state<5?Status.EM_ANDAMENTO:state==5?Status.AGUARDANDO_USUARIO:state==6?Status.AGUARDANDO_TERCEIROS:state==7?Status.RESOLVIDO:Status.FECHADO;
      if(t.status!=Status.ABERTO){t.assignee=i%2==0?admin:tech;t.firstResponseAt=t.createdAt.plusSeconds(1800);}
      if(t.status==Status.RESOLVIDO||t.status==Status.FECHADO){t.solution="Configuração revisada e testes realizados com o solicitante. Funcionamento normalizado.";t.resolvedAt=t.createdAt.plus(Duration.ofHours(3));t.updatedAt=t.resolvedAt;t.rating=i%2==0?5:4;if(t.status==Status.FECHADO)t.closedAt=t.resolvedAt.plusSeconds(600);}
      tickets.save(t);History h=new History();h.ticket=t;h.actor=requester;h.action="Chamado aberto";h.details="Dados demonstrativos.";h.createdAt=t.createdAt;history.save(h);
      if(t.assignee!=null){Comment c=new Comment();c.ticket=t;c.author=t.assignee;c.body="Recebemos sua solicitação. Estamos verificando o equipamento e acompanharemos o caso por aqui.";c.createdAt=t.firstResponseAt;comments.save(c);}
      if(i<5){Notification n=new Notification();n.recipient=admin;n.ticket=t;n.title="Novo chamado: "+t.title;n.createdAt=t.createdAt;notifications.save(n);}
    }
  }
  private User user(String name,String email,Role role,Department dep,String hash){User u=new User();u.name=name;u.email=email;u.role=role;u.department=dep;u.passwordHash=hash;return users.save(u);}
  private void addArticles(){
    String[][] data={
      {"Primeiros passos no Chamado Rápido","Aprenda a pedir ajuda, acompanhar respostas e avaliar seu atendimento.","Primeiros passos","1. Escolha Novo chamado ou use o Assistente.\n2. Informe o problema com um título objetivo.\n3. Descreva quando começou, o que já tentou e o impacto no trabalho.\n4. Anexe uma captura sem dados pessoais ou senhas.\n5. Acompanhe a conversa e as atualizações em Meus chamados.\n6. Após a solução, confirme o encerramento e avalie o atendimento."},
      {"Minha internet parou. O que verificar?","Faça verificações simples e seguras antes de pedir ajuda.","Rede e internet","Verifique se outros colegas enfrentam o mesmo problema. Confira apenas o encaixe do cabo externo de rede ou a conexão Wi-Fi. Não reinicie roteadores corporativos, switches ou servidores. Se a internet estiver totalmente indisponível, use outro dispositivo conectado ou o canal alternativo de TI da empresa. Ao abrir o chamado, informe o setor e os equipamentos afetados."},
      {"Impressora não imprime","Confira a impressora selecionada e registre a mensagem do visor.","Impressoras","Confira se escolheu a impressora correta na janela de impressão e se há papel na bandeja. Anote a mensagem de erro do visor. Não abra partes internas, force bandejas ou cancele trabalhos de outros colaboradores. Se persistir, abra um chamado com o nome da impressora e uma foto do erro, sem expor documentos."},
      {"Como escrever um bom chamado","Um relato completo ajuda a equipe de TI a agir com mais agilidade.","Boas práticas","Use um título específico: 'Não consigo entrar no sistema de cobrança' é melhor do que 'Urgente'. Descreva o resultado esperado e o que aparece na tela. Informe a localização, equipamento e ramal. Não compartilhe senhas, dados bancários ou informações de clientes. Informe quantas pessoas estão afetadas."},
      {"Proteja suas credenciais","Saiba o que nunca enviar em um chamado ou na conversa com o assistente.","Segurança","A equipe de TI não precisa saber sua senha. Não envie códigos de autenticação, certificados digitais ou documentos de clientes. Desconfie de links inesperados e confirme solicitações suspeitas pelo canal oficial. Oculte informações pessoais em capturas de tela antes de anexá-las."},
      {"Entenda as prioridades e prazos","Veja como a prioridade ajuda a organizar o atendimento.","Atendimento","Crítica: atividade essencial interrompida para várias pessoas (meta de 4 horas). Alta: trabalho impedido sem alternativa (8 horas). Média: problema parcial ou com alternativa (24 horas). Baixa: melhoria ou solicitação sem interrupção (72 horas). São metas em horas corridas, não promessas contratuais. A equipe de TI pode reclassificar a prioridade. A espera pelo usuário ou terceiros não pausa o prazo nesta versão."}
    };for(String[] d:data){Article a=new Article();a.title=d[0];a.excerpt=d[1];a.category=d[2];a.body=d[3];articles.save(a);}
  }
}
