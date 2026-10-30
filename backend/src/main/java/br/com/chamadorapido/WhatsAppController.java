package br.com.chamadorapido;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
public class WhatsAppController {
  private final WhatsAppProperties props;
  private final WhatsAppService whatsapp;
  private final UserService auth;
  public WhatsAppController(WhatsAppProperties props,WhatsAppService whatsapp,UserService auth){this.props=props;this.whatsapp=whatsapp;this.auth=auth;}
  @GetMapping("/api/whatsapp/status") public Map<String,Object> status(){auth.staff();return whatsapp.status();}
  @GetMapping("/api/public/whatsapp/webhook")
  public ResponseEntity<String> verify(@RequestParam(name="hub.mode",required=false) String mode,@RequestParam(name="hub.verify_token",required=false) String token,@RequestParam(name="hub.challenge",required=false) String challenge) {
    if (!props.receivingReady()) return ResponseEntity.status(503).contentType(MediaType.TEXT_PLAIN).body("Webhook não configurado.");
    if (!"subscribe".equals(mode)||challenge==null||challenge.length()>256||challenge.chars().anyMatch(c->c<' '||c>126)||!WhatsAppSignature.equal(props.getVerifyToken(),token==null?"":token))
      return ResponseEntity.status(403).contentType(MediaType.TEXT_PLAIN).body("Verificação recusada.");
    return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(challenge);
  }
  @PostMapping("/api/public/whatsapp/webhook")
  public ResponseEntity<Void> receive(HttpServletRequest request) throws java.io.IOException {
    if (!props.receivingReady()) return ResponseEntity.status(503).build();
    byte[] body=request.getInputStream().readNBytes(1_000_001);
    if (body.length>1_000_000) return ResponseEntity.badRequest().build();
    if (!WhatsAppSignature.signed(props.getAppSecret(),body,request.getHeader("X-Hub-Signature-256"))) return ResponseEntity.status(403).build();
    whatsapp.accept(body);
    return ResponseEntity.ok().build();
  }
}
