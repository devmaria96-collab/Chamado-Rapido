package br.com.chamadorapido;
import java.time.*;
/** Executavel com apenas o JDK. Nao substitui os testes de integracao Spring. */
public class PureRulesCheck {
  private static int assertions=0;
  private static void check(boolean ok,String msg){assertions++;if(!ok)throw new AssertionError(msg);}
  public static void main(String[] args){
    Instant start=Instant.parse("2026-10-23T12:00:00Z");
    check(TicketRules.deadline(start,"CRITICA").equals(start.plusSeconds(4*3600)),"SLA critico");
    check(TicketRules.deadline(start,"ALTA").equals(start.plusSeconds(8*3600)),"SLA alto");
    check(TicketRules.deadline(start,"MEDIA").equals(start.plusSeconds(24*3600)),"SLA medio");
    check(TicketRules.deadline(start,"BAIXA").equals(start.plusSeconds(72*3600)),"SLA baixo");
    check(TicketRules.canTransition("ABERTO","EM_ANDAMENTO",true),"Tecnico pode iniciar");
    check(!TicketRules.canTransition("ABERTO","RESOLVIDO",true),"Nao pular atendimento");
    check(!TicketRules.canTransition("ABERTO","EM_ANDAMENTO",false),"Usuario nao atende");
    check(TicketRules.canTransition("ABERTO","CANCELADO",false),"Usuario cancela aberto");
    check(TicketRules.canTransition("RESOLVIDO","FECHADO",false),"Usuario confirma solucao");
    check(TicketRules.canTransition("RESOLVIDO","EM_ANDAMENTO",false),"Usuario reabre resolvido");
    check(!TicketRules.canTransition("FECHADO","EM_ANDAMENTO",false),"Usuario nao reabre encerrado");
    check(TicketRules.canTransition("FECHADO","EM_ANDAMENTO",true),"TI reabre encerrado");
    check(!TicketRules.canTransition("CANCELADO","EM_ANDAMENTO",true),"Cancelado imutavel");
    check(TicketRules.terminal("RESOLVIDO")&&!TicketRules.terminal("AGUARDANDO_USUARIO"),"Status finais");
    check(TicketRules.csv("=SUM(A1)").startsWith("\"'="),"CSV formula");
    check(TicketRules.csv("  =1\n2").startsWith("\"'"),"CSV multiline e espacos");
    check(TicketRules.csv("\tmalicious").startsWith("\"'"),"CSV tab");
    check(TicketRules.csv("a\"b").equals("\"a\"\"b\""),"CSV aspas");
    check(TicketRules.csv(null).equals("\"\""),"CSV nulo");
    check(TriageEngine.start("rede").options().size()==2,"Opcoes de rede");
    check(TriageEngine.answer("rede","sim").result().equals("ENCAMINHADO"),"Falha coletiva escala");
    check(TriageEngine.answer("rede_cabo","sim").result().equals("RESOLVIDO"),"Triagem resolve");
    check(TriageEngine.answer("computador","sim").id().equals("risco"),"Seguranca fisica");
    boolean rejected=false;try{TriageEngine.start("inexistente");}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Categoria invalida");
    rejected=false;try{TriageEngine.answer("rede","inventado");}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Resposta invalida");
    for(var node:TriageEngine.STEPS.values()){for(var option:node.options())check(TriageEngine.STEPS.containsKey(option.next()),"Destino existente");if(node.options().isEmpty())check(!node.result().equals("EM_CURSO"),"Folha finalizada");}
    System.out.println("OK: "+assertions+" verificacoes de regras puras (Java 17).");
  }
}
