package br.com.chamadorapido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:chamado_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.demo=true","app.storage=./target/test-uploads"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class ApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  private MockHttpSession login(String email)throws Exception{return (MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).param("email",email).param("password","Demo@Rapido2026!")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
  private JsonNode read(String path,MockHttpSession session)throws Exception{return json.readTree(mvc.perform(get(path).session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
  private long version(long id,MockHttpSession s)throws Exception{return read("/api/tickets/"+id,s).path("ticket").path("version").asLong();}
  @Test void workflowAndPermissions()throws Exception{
    mvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
    MockHttpSession collaborator=login("recepcao@chamadorapido.local"),other=login("financeiro@chamadorapido.local"),admin=login("admin@chamadorapido.local");
    mvc.perform(get("/api/admin/users").session(collaborator)).andExpect(status().isForbidden());
    long category=read("/api/meta",collaborator).path("categories").get(0).path("id").asLong();
    String create="{\"title\":\"Computador nao inicia\",\"description\":\"A tela permanece preta ao iniciar o equipamento.\",\"categoryId\":"+category+",\"priority\":\"MEDIA\"}";
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
    mvc.perform(patch("/api/tickets/"+id).session(collaborator).with(csrf()).contentType("application/json").content("{\"version\":"+version(id,collaborator)+",\"status\":\"FECHADO\"}")).andExpect(status().isOk());
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
    String create="{\"title\":\"Problema coletivo na rede\",\"description\":\"Todos os computadores estao sem acesso a internet.\",\"priority\":\"ALTA\",\"categoryId\":"+category+",\"triageId\":"+tid+"}";
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
}
