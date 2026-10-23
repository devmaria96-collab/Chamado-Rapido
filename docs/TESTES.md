# Verificações e roteiro de testes

## Executado na preparação

| Verificação | Resultado e alcance |
|---|---|
| Regras Java puras | 43 verificações aprovadas: transições, permissões, SLA, CSV e grafo de triagem |
| Sintaxe Java | 24 arquivos sem erro de parsing; não resolve dependências Spring |
| Sintaxe TypeScript | 5 arquivos sem erro de transpilação sintática; não compila templates Angular |
| Tipos do código TypeScript próprio | Sem erro com declarações simplificadas dos imports Angular; não equivale ao compilador Angular |
| Demonstração no Chromium | 24 cenários aprovados e nenhum erro JavaScript capturado |
| Inspeção visual | Painel, abertura, assistente, setores, tema escuro e celular; sem transbordamento horizontal da página nos tamanhos verificados |

As regras foram compiladas com `javac --release 17` e executadas em **OpenJDK 21** disponível no ambiente. Isso verifica o alvo de linguagem Java 17, não uma execução integrada em JDK 17.

Os 24 cenários da prévia incluíram criar chamado e anexo; conversa; atribuir; nota interna e ocultamento; resolver; encerrar e avaliar; filtrar; cadastrar artigo/pessoa/categoria; triagem resolvida/encaminhada; CSV; tema e celular. Resultado detalhado: `resultados-demo.json`.

O navegador do ambiente bloqueava navegação direta a arquivos/localhost. A prévia foi verificada com conteúdo injetado e armazenamento em memória no harness de testes. Foi testada a serialização. A persistência real de `localStorage` entre aberturas depende do navegador do usuário e precisa ser validada nele. Os testes da prévia **não validam o servidor Java**.

Capturas em `previas/` são da demonstração, com dados fictícios.

## Não executado neste ambiente

Instalação npm, build completo Angular, compilação Maven/Spring Boot, testes de integração da API, execução PostgreSQL, construção Docker e ensaio HTTPS real. O ambiente não tinha Maven nem conectividade aos repositórios de dependências. **Não há evidência de build integrado concluído nesta entrega.** Execute as etapas abaixo antes do uso real; erros eventualmente encontrados precisam ser corrigidos antes da implantação.

## Executar

Regras independentes de bibliotecas:

```sh
sh scripts/test-rules.sh
```

Windows: `scripts/test-rules-windows.bat`.

API com JDK 17 e Maven:

```sh
cd backend
mvn clean verify
```

Angular:

```sh
cd frontend
npm install
npm run build
```

Build integrado:

```sh
docker compose build --no-cache
```

`ApiIntegrationTest` usa H2/PostgreSQL mode e MockMvc: login, CSRF, isolamento por solicitante, nota interna, abertura, transições, solução obrigatória, versão obsoleta, fechamento, avaliação, relatórios, triagem única e anexos. H2 não substitui PostgreSQL real; repita os fluxos com Docker e duas contas em navegadores distintos.

## Antes de produção

Validar troca obrigatória de senha, revogação, conta inativa, acesso direto a ID alheio, edições concorrentes, uploads simultâneos, arquivos maliciosos, limite de login, expiração, HTTPS, backups e restauração, teclado/leitor de tela, celulares reais e carga representativa. Definir metas com a TI antes de declarar desempenho ou disponibilidade.
