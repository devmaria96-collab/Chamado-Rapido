package br.com.chamadorapido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:chamado_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.demo=true","app.storage=./target/test-uploads","app.whatsapp.app-secret=teste-webhook-secreto-32","app.whatsapp.verify-token=teste-verify"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class ApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired TicketRepository tickets;
  @Autowired UserService accounts;
  private MockHttpSession login(String email)throws Exception{return login(email,email.startsWith("admin@")||email.startsWith("ti@")?"coop2026":"Demo@Rapido2026!");}
  private MockHttpSession login(String email,String password)throws Exception{return (MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).param("email",email).param("password",password)).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
  private JsonNode read(String path,MockHttpSession session)throws Exception{return json.readTree(mvc.perform(get(path).session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
  private long version(long id,MockHttpSession s)throws Exception{return read("/api/tickets/"+id,s).path("ticket").path("version").asLong();}
  @Test void workflowAndPermissions()throws Exception{
    mvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
    MockHttpSession collaborator=login("recepcao@chamadorapido.local"),other=login("financeiro@chamadorapido.local"),admin=login("admin@chamadorapido.local");
    mvc.perform(get("/api/admin/users").session(collaborator)).andExpect(status().isForbidden());
    mvc.perform(get("/api/dashboard").session(collaborator)).andExpect(status().isForbidden());
    mvc.perform(get("/api/departments/overview").session(collaborator)).andExpect(status().isForbidden());
    mvc.perform(get("/api/articles").session(collaborator)).andExpect(status().isForbidden());
    long category=read("/api/meta",collaborator).path("categories").get(0).path("id").asLong();
    String create="{\"title\":\"Computador nao inicia\",\"description\":\"A tela permanece preta ao iniciar o equipamento.\",\"requesterName\":\"Ana Martins\",\"categoryId\":"+category+",\"priority\":\"MEDIA\"}";
    mvc.perform(post("/api/tickets").session(collaborator).contentType("application/json").content(create)).andExpect(status().isForbidden());
    long id=json.readTree(mvc.perform(post("/api/tickets").session(collaborator).with(csrf()).contentType("application/json").content(create)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();
    mvc.perform(get("/api/tickets/"+id).session(other)).andExpect(status().isNotFound());
    mvc.perform(post("/api/tickets/"+id+"/comments").session(collaborator).with(csrf()).contentType("application/json").content("{\"body\":\"Nota secreta\",\"internal\":true}")).andExpect(status().isForbidden());
    mvc.perform(post("/api/tickets/"+id+"/comments").session(admin).with(csrf()).contentType("application/json").content("{\"body\":\"NOTA_INTERNA_TESTE\",\"internal\":true}")).andExpect(status().isOk());
    assertFalse(read("/api/tickets/"+id,collaborator).toString().contains("NOTA_INTERNA_TESTE"));
    mvc.perform(patch("/api/tickets/"+id).session(admin).with(csrf()).contentType("application/json").content("{\"version\":"+version(id,admin)+",\"status\":\"EM_ANDAMENTO\"}")).andExpect(status().isOk());
    mvc.perform(patch("/api/tickets/"+id).session(admin).with(csrf()).contentType("application/json").content("{\"version\":"+version(id,admin)+",\"status\":\"RESOLVIDO\"}")).andExpect(status().isBadRequest());
    long oldVersion=version(id,admin);
    mvc.perform(patch("/api/tickets/"+id).session(admin).with(csrf()).contentType("application/json").content("{\"version\":"+oldVersion+",\"status\":\"RESOLVIDO\",\"solution\":\"Configuracao revisada e funcionamento normalizado.\"}")).andExpect(status().isOk());
    mvc.perform(patch("/api/tickets/"+id).session(admin).with(csrf()).contentType("application/json").content("{\"version\":"+oldVersion+",\"status\":\"FECHADO\"}")).andExpect(status().isConflict());
    mvc.perform(patch("/api/tickets/"+id).session(collaborator).with(csrf()).contentType("application/json").content("{\"version\":"+version(id,collaborator)+",\"status\":\"FECHADO\"}")).andExpect(status().isForbidden());
    mvc.perform(post("/api/tickets/"+id+"/comments").session(collaborator).with(csrf()).contentType("application/json").content("{\"body\":\"Mensagem do colaborador\",\"internal\":false}")).andExpect(status().isForbidden());
    mvc.perform(post("/api/tickets/"+id+"/rating").session(collaborator).with(csrf()).contentType("application/json").content("{\"rating\":5}")).andExpect(status().isOk());
    mvc.perform(post("/api/tickets/"+id+"/rating").session(other).with(csrf()).contentType("application/json").content("{\"rating\":5}")).andExpect(status().isNotFound());
    mvc.perform(get("/api/reports/export").session(admin)).andExpect(status().isOk()).andExpect(header().string("Content-Disposition","attachment; filename=chamados.csv"));
    mvc.perform(get("/api/reports/export").session(collaborator)).andExpect(status().isForbidden());
  }
  @Test void triageAndUploadValidation()throws Exception{
    MockHttpSession s=login("recepcao@chamadorapido.local");
    JsonNode triage=json.readTree(mvc.perform(post("/api/triages").session(s).with(csrf()).contentType("application/json").content("{\"category\":\"rede\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    long tid=triage.path("id").asLong();
    mvc.perform(post("/api/triages/"+tid+"/answers").session(s).with(csrf()).contentType("application/json").content("{\"choice\":\"sim\",\"version\":"+triage.path("version").asLong()+"}")).andExpect(status().isOk()).andExpect(jsonPath("$.result").value("ENCAMINHADO"));
    long category=read("/api/meta",s).path("categories").get(0).path("id").asLong();
    String create="{\"title\":\"Problema coletivo na rede\",\"description\":\"Todos os computadores estao sem acesso a internet.\",\"requesterName\":\"Ana Martins\",\"priority\":\"ALTA\",\"categoryId\":"+category+",\"triageId\":"+tid+"}";
    long id=json.readTree(mvc.perform(post("/api/tickets").session(s).with(csrf()).contentType("application/json").content(create)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();
    mvc.perform(post("/api/tickets").session(s).with(csrf()).contentType("application/json").content(create)).andExpect(status().isBadRequest());
    MockMultipartFile invalid=new MockMultipartFile("file","unsafe.svg","image/svg+xml","<svg onload='alert(1)'/>".getBytes());
    mvc.perform(multipart("/api/tickets/"+id+"/attachments").file(invalid).session(s).with(csrf())).andExpect(status().isBadRequest());
    MockMultipartFile valid=new MockMultipartFile("file","relato.txt","text/plain","Detalhes da falha no computador".getBytes());
    mvc.perform(multipart("/api/tickets/"+id+"/attachments").file(valid).session(s).with(csrf())).andExpect(status().isOk());
    long attachment=read("/api/tickets/"+id,s).path("attachments").get(0).path("id").asLong();
    mvc.perform(get("/api/attachments/"+attachment).session(s)).andExpect(status().isOk());
    mvc.perform(get("/api/attachments/"+attachment).session(login("rh@chamadorapido.local"))).andExpect(status().isNotFound());
  }
  @Test void nameLoginAcceptsOnlyTiStaff()throws Exception{
    assertEquals("ti@chamadorapido.local",accounts.emailForName("  Equipe "));
    assertEquals("",accounts.emailForName("Ana"));
    mvc.perform(post("/api/auth/login").with(csrf()).param("nome","Ana").param("password","Demo@Rapido2026!")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/auth/login").with(csrf()).param("nome","  equipe ").param("password","errada")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/auth/login").with(csrf()).param("nome","Equipe").param("password","coop2026")).andExpect(status().isOk());
    mvc.perform(post("/api/auth/login").with(csrf()).param("nome","Administrador").param("password","coop2026")).andExpect(status().isOk());
  }
  @Test void whatsappWebhookKeepsOneOpenTicket()throws Exception{
    mvc.perform(get("/api/public/whatsapp/webhook").param("hub.mode","subscribe").param("hub.verify_token","errado").param("hub.challenge","12345")).andExpect(status().isForbidden());
    mvc.perform(get("/api/public/whatsapp/webhook").param("hub.mode","subscribe").param("hub.verify_token","teste-verify").param("hub.challenge","12345")).andExpect(status().isOk()).andExpect(content().string("12345"));
    String body="{\"entry\":[{\"changes\":[{\"value\":{\"contacts\":[{\"profile\":{\"name\":\"Marina Costa\"},\"wa_id\":\"5511987654321\"}],\"messages\":[{\"from\":\"5511987654321\",\"id\":\"wamid.TESTE1\",\"timestamp\":\"1710000000\",\"type\":\"text\",\"text\":{\"body\":\"Bom dia! Estou com problema para acessar o sistema.\"}}]}}]}]}";
    mvc.perform(post("/api/public/whatsapp/webhook").contentType("application/json").content(body)).andExpect(status().isForbidden());
    mvc.perform(post("/api/public/whatsapp/webhook").contentType("application/json").header("X-Hub-Signature-256",sign(body)).content(body)).andExpect(status().isOk());
    mvc.perform(post("/api/public/whatsapp/webhook").contentType("application/json").header("X-Hub-Signature-256",sign(body)).content(body)).andExpect(status().isOk());
    String second=body.replace("wamid.TESTE1","wamid.TESTE2").replace("Bom dia! Estou com problema para acessar o sistema.","A tela continua pedindo a senha.");
    mvc.perform(post("/api/public/whatsapp/webhook").contentType("application/json").header("X-Hub-Signature-256",sign(second)).content(second)).andExpect(status().isOk());
    MockHttpSession admin=login("admin@chamadorapido.local");
    mvc.perform(get("/api/whatsapp/status").session(login("recepcao@chamadorapido.local"))).andExpect(status().isForbidden());
    mvc.perform(get("/api/whatsapp/status").session(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.connected").value(false));
    var same=tickets.findByWhatsappPhoneOrderByIdDesc("5511987654321");
    assertEquals(1,same.size());
    long id=same.get(0).id;
    assertEquals(2,read("/api/tickets/"+id,admin).path("comments").size());
    var old=same.get(0);old.status=Domain.Status.RESOLVIDO;old.resolvedAt=Instant.now().minus(Duration.ofHours(48));old.solution="Resolvido no teste da regra de novo chamado.";tickets.saveAndFlush(old);
    String third=body.replace("wamid.TESTE1","wamid.TESTE3").replace("Bom dia! Estou com problema para acessar o sistema.","O problema voltou no dia seguinte e preciso de outro atendimento.");
    mvc.perform(post("/api/public/whatsapp/webhook").contentType("application/json").header("X-Hub-Signature-256",sign(third)).content(third)).andExpect(status().isOk());
    var after=tickets.findByWhatsappPhoneOrderByIdDesc("5511987654321");
    assertEquals(2,after.size());
    long fresh=after.get(0).id;
    mvc.perform(post("/api/tickets/"+fresh+"/comments").session(admin).with(csrf()).contentType("application/json").content("{\"body\":\"Vamos verificar o seu acesso.\",\"internal\":false}")).andExpect(status().isOk());
    JsonNode reply=read("/api/tickets/"+fresh,admin).path("comments");
    assertEquals("FAILED",reply.get(reply.size()-1).path("deliveryStatus").asText());
    assertFalse(reply.toString().contains("Entregue"));
  }
  @Test void protocolContinuesWhileAnActiveTicketRemains()throws Exception{
    MockHttpSession admin=login("admin@chamadorapido.local");
    int max=0;for(JsonNode item:read("/api/tickets?size=200&board=true",admin).path("items"))max=Math.max(max,Integer.parseInt(item.path("protocol").asText()));
    long category=read("/api/meta",admin).path("categories").get(0).path("id").asLong();
    String body="{\"title\":\"Protocolo sequencial\",\"description\":\"Preciso de apoio para validar a numeracao do protocolo.\",\"requesterName\":\"Maria Souza\",\"categoryId\":"+category+",\"priority\":\"MEDIA\"}";
    String protocol=json.readTree(mvc.perform(post("/api/tickets").session(admin).with(csrf()).contentType("application/json").content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("protocol").asText();
    assertEquals(max+1<100?String.format("%02d",max+1):Integer.toString(max+1),protocol);
    assertTrue(read("/api/tickets?size=200&board=true",admin).path("counts").path("novos").asInt()>=1);
  }
  @Test void resolvedTodayCountsOnlySaoPauloResolutionDate()throws Exception{
    MockHttpSession admin=login("admin@chamadorapido.local");
    int before=read("/api/tickets?size=1&board=true",admin).path("counts").path("resolvidosHoje").asInt();
    long category=read("/api/meta",admin).path("categories").get(0).path("id").asLong();
    String body="{\"title\":\"Resolucao de hoje\",\"description\":\"Chamado criado para conferir o indicador do dia.\",\"requesterName\":\"Carla Mendes\",\"categoryId\":"+category+",\"priority\":\"MEDIA\"}";
    long id=json.readTree(mvc.perform(post("/api/tickets").session(admin).with(csrf()).contentType("application/json").content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asLong();
    mvc.perform(patch("/api/tickets/"+id).session(admin).with(csrf()).contentType("application/json").content("{\"version\":"+version(id,admin)+",\"status\":\"EM_ANDAMENTO\"}")).andExpect(status().isOk());
    mvc.perform(patch("/api/tickets/"+id).session(admin).with(csrf()).contentType("application/json").content("{\"version\":"+version(id,admin)+",\"status\":\"RESOLVIDO\",\"solution\":\"Configuracao revisada e funcionamento normalizado.\"}")).andExpect(status().isOk());
    assertEquals(before+1,read("/api/tickets?size=1&board=true",admin).path("counts").path("resolvidosHoje").asInt());
    mvc.perform(patch("/api/tickets/"+id).session(admin).with(csrf()).contentType("application/json").content("{\"version\":"+version(id,admin)+",\"status\":\"EM_ANDAMENTO\",\"reason\":\"Voltando o chamado para atendimento.\"}")).andExpect(status().isOk());
    assertEquals(before,read("/api/tickets?size=1&board=true",admin).path("counts").path("resolvidosHoje").asInt());
    mvc.perform(patch("/api/tickets/"+id).session(admin).with(csrf()).contentType("application/json").content("{\"version\":"+version(id,admin)+",\"status\":\"RESOLVIDO\",\"solution\":\"Configuracao revisada e funcionamento normalizado.\"}")).andExpect(status().isOk());
    Domain.Ticket saved=tickets.findById(id).orElseThrow();
    saved.resolvedAt=LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1).atTime(15,0).atZone(ZoneId.of("America/Sao_Paulo")).toInstant();
    tickets.saveAndFlush(saved);
    JsonNode counts=read("/api/tickets?size=1&board=true",admin).path("counts");
    assertEquals(before,counts.path("resolvidosHoje").asInt());
    assertTrue(counts.path("resolvidos").asInt()>counts.path("resolvidosHoje").asInt());
  }
  private String sign(String body)throws Exception{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec("teste-webhook-secreto-32".getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return "sha256="+HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));}
}
