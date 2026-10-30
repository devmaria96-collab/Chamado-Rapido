package br.com.chamadorapido;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
/** Comparação da assinatura X-Hub-Signature-256. Não registra o corpo nem o segredo. */
public final class WhatsAppSignature {
  private WhatsAppSignature() {}
  public static boolean equal(String expected,String given) {
    if (expected==null||given==null) return false;
    return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),given.getBytes(StandardCharsets.UTF_8));
  }
  public static boolean signed(String secret,byte[] body,String header) {
    if (secret==null||secret.isBlank()||body==null||header==null||!header.startsWith("sha256=")) return false;
    try {
      Mac mac=Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
      String expected="sha256="+HexFormat.of().formatHex(mac.doFinal(body));
      return equal(expected,header.trim());
    } catch (Exception e) { return false; }
  }
}
