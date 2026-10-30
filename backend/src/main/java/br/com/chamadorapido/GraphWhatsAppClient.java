package br.com.chamadorapido;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;
@Component
public class GraphWhatsAppClient implements WhatsAppClient {
  private final WhatsAppProperties props;
  private final ObjectMapper json;
  private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  public GraphWhatsAppClient(WhatsAppProperties props,ObjectMapper json){this.props=props;this.json=json;}
  @Override public Result sendText(String to,String body) {
    if (!props.sendingReady()) return Result.fail("WhatsApp não configurado no servidor.");
    try {
      String payload=json.writeValueAsString(Map.of("messaging_product","whatsapp","to",to,"type","text","text",Map.of("preview_url",false,"body",body)));
      HttpRequest request=HttpRequest.newBuilder().uri(URI.create("https://graph.facebook.com/"+props.graphVersionOrDefault()+"/"+props.getPhoneNumberId()+"/messages")).timeout(Duration.ofSeconds(10)).header("Authorization","Bearer "+props.getToken()).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(payload)).build();
      HttpResponse<String> response=http.send(request,HttpResponse.BodyHandlers.ofString());
      if (response.statusCode()>=200&&response.statusCode()<300) {
        String id=json.readTree(response.body()).path("messages").path(0).path("id").asText("");
        return id.isBlank()?Result.fail("A API não devolveu o identificador da mensagem."):Result.ok(id);
      }
      String error=json.readTree(response.body()).path("error").path("message").asText("");
      return Result.fail(error.isBlank()?"A API do WhatsApp recusou o envio.":trim(error));
    } catch (Exception e) { return Result.fail("Não foi possível falar com a API do WhatsApp."); }
  }
  private static String trim(String value){String clean=value.replaceAll("\\s+"," ").trim();return clean.length()>300?clean.substring(0,300):clean;}
}
