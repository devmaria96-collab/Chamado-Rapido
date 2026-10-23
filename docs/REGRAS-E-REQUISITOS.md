# Regras e requisitos da versão 1.0

## Acesso

**Colaborador:** abre e acompanha chamados próprios, conversa, anexa, usa assistente, consulta artigos, cancela uma solicitação ainda aberta, encerra/reabre a própria solicitação resolvida e avalia o atendimento. Não vê todos os chamados do setor.

**Técnico:** visualiza a fila geral, assume/atribui, prioriza, registra solução, muda status, consulta notas internas, relatórios e CSV.

**Administrador:** possui as operações de TI e gerencia contas, setores, categorias e artigos. Apenas o solicitante pode avaliar seu atendimento, independentemente do perfil.

Permissões são verificadas pela API. Esconder botões não é a barreira principal. Não há cadastro público nem escolha livre de papel no login real.

## Abertura e estados

Título: 5 a 180 caracteres; descrição: 15 a 12.000; categoria ativa; prioridade válida; conta autenticada em departamento ativo. Local, equipamento e ramal são opcionais. Solicitante/departamento vêm da sessão. Protocolo `CR-000001` derivado da identidade do banco, sem reinício anual.

```text
ABERTO -> EM_ANDAMENTO ou CANCELADO
EM_ANDAMENTO -> AGUARDANDO_USUARIO, AGUARDANDO_TERCEIROS, RESOLVIDO ou CANCELADO
AGUARDANDO_USUARIO -> EM_ANDAMENTO, RESOLVIDO ou CANCELADO
AGUARDANDO_TERCEIROS -> EM_ANDAMENTO, RESOLVIDO ou CANCELADO
RESOLVIDO -> FECHADO ou EM_ANDAMENTO
FECHADO -> EM_ANDAMENTO, apenas pela TI
CANCELADO -> sem transicao
```

Resolução exige solução com ao menos 10 caracteres. Cancelamento e reabertura exigem justificativa de ao menos 10 caracteres. Resolvidos/encerrados precisam ser reabertos para alterar atribuição/prioridade ou anexar. Cancelados não podem ser reabertos. Conversa continua permitida em resolvidos, não em encerrados/cancelados. Atribuição admite somente contas ativas de TI/admin.

## Prazos e avaliação

Crítica 4h; Alta 8h; Média 24h; Baixa 72h. Horas corridas, sem pausas e sem calendário comercial. Reabrir reinicia o ciclo, limpa a avaliação anterior e preserva a solução anterior até nova resolução. Alterar prioridade recalcula o prazo desde o início do ciclo atual. Médias de resolução contam desde a abertura original. Cancelados não contam como resolvidos.

O solicitante avalia de 1 a 5 após resolução/encerramento e pode atualizar a nota. Indicadores de período são coortes de chamados abertos no período, não todos os eventos ocorridos nele. Totais de setores são históricos.

## Triagem e anexos

Triagem opcional: rede, impressoras, sistemas/acessos e computador. Resposta resolvida termina sem chamado. Encaminhamento permite criar um chamado com a transcrição; uma triagem gera no máximo um chamado do mesmo solicitante. Fluxos em código, sem IA generativa. O assistente não executa comandos nem solicita senha. Riscos físicos encaminham para a equipe, sem orientar desmontagem elétrica.

Até 5 anexos de 5 MB cada: PNG, JPEG, PDF e TXT UTF-8, conferindo extensão e assinatura inicial/conteúdo textual. Armazenamento UUID e download autenticado. Isso **não substitui antivírus**. Não enviar senhas, tokens ou documentos pessoais de terceiros.

## Aceite para homologação

Validar login, troca obrigatória de senha, acesso entre contas diferentes, abertura, upload/download, atribuição, isolamento das notas internas, resolução, fechamento, reabertura, avaliação, triagem, CSV, expiração de sessão, conta inativa, persistência após reinício e restauração de backup. A lista é um roteiro, não afirmação de que a instalação da empresa já foi homologada.
