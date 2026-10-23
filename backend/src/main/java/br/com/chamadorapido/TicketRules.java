package br.com.chamadorapido;
import java.time.*;
import java.util.*;
/** Regras puras: status e SLA em horas corridas, sem dependencias externas. */
public final class TicketRules {
  private TicketRules() {}
  public static final Map<String,Set<String>> TRANSITIONS=Map.of(
    "ABERTO",Set.of("EM_ANDAMENTO","CANCELADO"),
    "EM_ANDAMENTO",Set.of("AGUARDANDO_USUARIO","AGUARDANDO_TERCEIROS","RESOLVIDO","CANCELADO"),
    "AGUARDANDO_USUARIO",Set.of("EM_ANDAMENTO","RESOLVIDO","CANCELADO"),
    "AGUARDANDO_TERCEIROS",Set.of("EM_ANDAMENTO","RESOLVIDO","CANCELADO"),
    "RESOLVIDO",Set.of("FECHADO","EM_ANDAMENTO"),
    "FECHADO",Set.of("EM_ANDAMENTO"),"CANCELADO",Set.of());
  public static boolean canTransition(String from,String to,boolean staff) {
    if (!TRANSITIONS.getOrDefault(from,Set.of()).contains(to)) return false;
    return staff || (from.equals("ABERTO")&&to.equals("CANCELADO")) ||
      (from.equals("RESOLVIDO")&&(to.equals("FECHADO")||to.equals("EM_ANDAMENTO")));
  }
  public static boolean terminal(String status) {return Set.of("RESOLVIDO","FECHADO","CANCELADO").contains(status);}
  public static Instant deadline(Instant start,String priority) {
    int hours=switch(priority){case "CRITICA"->4;case "ALTA"->8;case "MEDIA"->24;case "BAIXA"->72;default->throw new IllegalArgumentException("Prioridade inválida");};
    return start.plus(Duration.ofHours(hours));
  }
  public static String csv(String value) {
    if (value==null) value="";
    String clean=value.stripLeading();
    if ((!clean.isEmpty()&&"=+-@".indexOf(clean.charAt(0))>=0)||value.startsWith("\t")||value.startsWith("\r")||value.startsWith("\n")) value="'"+value;
    return "\""+value.replace("\"","\"\"")+"\"";
  }
}
