package br.com.chamadorapido;
import java.util.*;
/** Arvore deterministica. Nao envia dados a provedores de IA. */
public final class TriageEngine {
  public record Option(String id,String label,String next) {}
  public record Step(String id,String message,List<Option> options,String result) {}
  private static Option o(String id,String label,String next){return new Option(id,label,next);}
  private static Step step(String id,String msg,Option...options){return new Step(id,msg,List.of(options),"EM_CURSO");}
  public static final Map<String,Step> STEPS=Map.ofEntries(
    Map.entry("rede",step("rede","Vamos verificar a conexão. Outros computadores do setor também estão sem internet?",o("sim","Sim, mais pessoas estão sem internet","escalar"),o("nao","Não, apenas o meu","rede_cabo"))),
    Map.entry("rede_cabo",step("rede_cabo","Verifique apenas o encaixe do cabo de rede externo ou se o Wi-Fi está conectado. Não altere instalações elétricas nem reinicie equipamentos compartilhados. A conexão voltou?",o("sim","Sim, voltou a funcionar","resolvido"),o("nao","Ainda estou sem conexão","escalar"))),
    Map.entry("impressora",step("impressora","A impressora está ligada e mostra alguma mensagem no visor?",o("erro","Existe um erro no visor","escalar"),o("normal","Está ligada e sem mensagens","impressora_fila"),o("nao","Não está ligando","escalar"))),
    Map.entry("impressora_fila",step("impressora_fila","Confira se você selecionou a impressora correta e se há papel na bandeja. Não desmonte o equipamento. Conseguiu imprimir?",o("sim","Sim, consegui imprimir","resolvido"),o("nao","Ainda não consegui","escalar"))),
    Map.entry("acesso",step("acesso","Confira o usuário, Caps Lock e o endereço do sistema. Nunca compartilhe sua senha neste atendimento. Após verificar, o acesso funcionou?",o("sim","Sim, consegui acessar","resolvido"),o("nao","Preciso de ajuda com o acesso","escalar"))),
    Map.entry("computador",step("computador","Há cheiro de queimado, fumaça, faíscas ou aquecimento anormal?",o("sim","Sim, há um sinal de risco","risco"),o("nao","Não, mas apresenta falhas","computador_lento"))),
    Map.entry("computador_lento",step("computador_lento","Salve seus documentos e feche programas que não está usando. Não desligue servidores ou equipamentos compartilhados. Isso melhorou o funcionamento?",o("sim","Sim, melhorou","resolvido"),o("nao","O problema continua","escalar"))),
    Map.entry("risco",new Step("risco","Interrompa o uso e afaste-se do equipamento. Não abra o gabinete nem toque em fios. Avise imediatamente a TI ou a equipe de emergência responsável. O chamado não substitui o atendimento emergencial.",List.of(),"ENCAMINHADO")),
    Map.entry("escalar",new Step("escalar","Vamos encaminhar para a equipe de TI. O resumo desta conversa acompanhará seu chamado, para você não precisar repetir tudo.",List.of(),"ENCAMINHADO")),
    Map.entry("resolvido",new Step("resolvido","Que bom! Registrei que a orientação resolveu seu problema. Se precisar novamente, estamos por aqui.",List.of(),"RESOLVIDO"))
  );
  public static Step start(String category){if(!Set.of("rede","impressora","acesso","computador").contains(category))throw new IllegalArgumentException("Categoria de triagem inválida");return STEPS.get(category);}
  public static Step answer(String node,String choice){Step current=STEPS.get(node);if(current==null)throw new IllegalArgumentException("Etapa inválida");return STEPS.get(current.options.stream().filter(o->o.id.equals(choice)).findFirst().orElseThrow(()->new IllegalArgumentException("Resposta inválida")).next);}
}
