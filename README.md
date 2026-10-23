# Chamado Rápido
### Tecnologia que simplifica, suporte que resolve.

Sistema de chamados dos departamentos para a equipe de TI. **Angular + TypeScript**, **Java 17 + Spring Boot**, **PostgreSQL** e ambiente local H2. Identidade azul-escuro `#102A43` e amarelo `#FFC928`, utilizando a logo de guepardo fornecida.

**Primeiro passo: extraia todo o ZIP e abra `COMECE-AQUI.html`.**

> **Estado da entrega:** código-fonte da versão inicial 1.0.0, com demonstração navegável, configuração e testes. A demonstração e as regras Java puras foram verificadas. O build completo Angular/Spring Boot e os testes de integração **não foram executados** porque o ambiente de preparação não tinha Maven nem acesso aos repositórios npm/Maven. Devem ser executados em uma máquina com internet antes do uso real. O Dockerfile executa a compilação e os testes, sem ignorá-los. Consulte `docs/TESTES.md`.

## Funcionalidades implementadas

Login, sessão de servidor, senha BCrypt, perfis Colaborador/Técnico/Administrador; painel com indicadores reais; abertura de chamados; protocolos; prioridades e prazos; lista paginada e filtros; quadro de atendimento; atribuição; conversa; notas internas; histórico; anexos; resolução; reabertura; avaliação; notificações internas; relatórios e CSV; base de conhecimento; gestão de pessoas, setores e categorias; assistente de triagem; tema claro/escuro e layout responsivo.

**Setores iniciais:** Recepção, Diretoria, Cobrança, Assistentes, RH, Telefonista, Jurídico, Financeiro, Compras, Contabilidade e Conferência. Foi acrescentado Tecnologia da Informação para a equipe de atendimento. Os cadastros podem ser editados pelo administrador.

Por padrão, colaboradores consultam **somente seus próprios chamados**, não todos os chamados do setor. A TI e os administradores visualizam a fila geral.

**Fora desta versão:** integrações WhatsApp/e-mail/Teams, SSO, IA generativa, controle remoto, antivírus de anexos, SLA em horário comercial e alta disponibilidade. O assistente usa perguntas predefinidas em `TriageEngine.java`, sem serviços pagos nem envio de dados a uma IA. Não há editor visual de seus fluxos.

## 1. Explorar sem instalar nada

Abra **`demonstracao/index.html`** em Edge, Chrome ou Firefox. Dados fictícios e operações locais: criar chamado, conversar, alterar status, trocar o perfil simulado, cadastrar artigos/pessoas, explorar o assistente e exportar CSV.

Esta prévia é **independente**, em HTML/CSS/JavaScript. Não é o build Angular, não utiliza Java nem autenticação real e não compartilha dados entre computadores. **Não insira dados reais ou senhas.** O armazenamento local depende do navegador, das permissões e do espaço disponível. A demonstração não sincroniza informações com o banco da aplicação.

## 2. Executar Angular + Java 17 + PostgreSQL

### Windows com Docker

Instale e abra **Docker Desktop com Compose v2**. A primeira construção exige internet para baixar dependências e imagens.

1. Extraia o pacote inteiro em uma pasta de trabalho.
2. Execute `INICIAR-WINDOWS.bat`.
3. O script cria `.env` de demonstração, se ainda não existir, compila o projeto e inicia os serviços.
4. Acesse **http://localhost:8080** e use uma conta demonstrativa abaixo.

No Linux/macOS: `sh iniciar.sh`. Pelo terminal, na raiz:

```sh
# Primeiro: copie .env.example para .env.
# Windows CMD: copy .env.example .env
# Linux/macOS: cp .env.example .env

docker compose up --build -d --wait --wait-timeout 180
docker compose logs -f app
```

Para parar preservando banco e anexos:

```sh
docker compose down
```

**Não use `docker compose down -v` em uma instalação cujos dados queira manter.** Os volumes `database` e `uploads` persistem os dados. Alterar apenas `DB_PASSWORD` no `.env` não altera a senha de um PostgreSQL já inicializado; a rotação deve ser aplicada no banco também.

A porta fica restrita a `127.0.0.1` por padrão. Para outros computadores acessarem, a TI deve preparar HTTPS, proxy, DNS, firewall e credenciais reais. Não exponha o modo demonstrativo na rede.

### Credenciais exclusivamente demonstrativas

| Perfil | E-mail | Senha |
|---|---|---|
| Administrador | `admin@chamadorapido.local` | `Demo@Rapido2026!` |
| Técnico | `ti@chamadorapido.local` | `Demo@Rapido2026!` |
| Recepção | `recepcao@chamadorapido.local` | `Demo@Rapido2026!` |

Os demais logins são `diretoria`, `cobranca`, `assistentes`, `rh`, `telefonista`, `juridico`, `financeiro`, `compras`, `contabilidade` e `conferencia`, com sufixo `@chamadorapido.local` e a mesma senha demonstrativa. São criados apenas no primeiro provisionamento de um banco vazio com demo ativa.

## 3. Desenvolver sem Docker

Requisitos: **JDK 17**, **Maven 3.9** e **Node.js 22.12 ou superior da linha 22**. O `package.json` também admite Node 24. Confira `java -version`, `mvn -version`, `node -v` e `npm -v`.

Abra dois terminais:

```sh
# Terminal 1
cd backend
mvn spring-boot:run
```

```sh
# Terminal 2
cd frontend
npm install
npm start
```

Abra **http://localhost:4200**. O proxy encaminha `/api` para Java em 8080. O perfil local utiliza **H2 persistente** em `backend/data`, migra o schema e cria dados fictícios no banco vazio. Não precisa instalar PostgreSQL nessa modalidade. No Windows, `scripts/dev-windows.bat` abre os terminais.

O ZIP não inclui `node_modules`, imagens Docker, `target` nem JAR pré-compilado. O primeiro `npm install` gera `package-lock.json`: revise e versione esse arquivo depois da homologação. Nas instalações seguintes, utilize `npm ci`. O Dockerfile usa `npm ci` quando o lockfile está presente.

## 4. Gerar um JAR com a interface incorporada

Execute `scripts/build-windows.bat` ou `sh scripts/build.sh`. O script compila Angular, copia seus arquivos para o Spring Boot e executa `mvn clean verify`. Ao concluir, gera `executavel/chamado-rapido.jar`.

```sh
java -jar executavel/chamado-rapido.jar
```

Sem outras configurações, esse comando inicia o perfil local demonstrativo em **http://localhost:8080**. Não use o perfil local para produção.

## 5. Instalação real

Leia `docs/OPERACAO-E-SEGURANCA.md`. Use **banco e volumes novos**, não os da demonstração. Copie `.env.production.example` para `.env` e preencha `DB_PASSWORD`, `ADMIN_EMAIL` e `ADMIN_PASSWORD`. Configure HTTPS e mantenha `DEMO_ENABLED=false` e `COOKIE_SECURE=true`.

O primeiro administrador deverá trocar a senha ao entrar e cadastrar as pessoas autorizadas. **Desativar demo não remove contas demonstrativas já existentes**: não reutilize esse banco.

## 6. Estrutura

```text
Chamado-Rapido/
  COMECE-AQUI.html       Guia inicial
  INICIAR-WINDOWS.bat    Inicializacao Docker
  compose.yaml          Aplicacao + PostgreSQL
  Dockerfile            Build e runtime Java 17
  frontend/             Angular / TypeScript
  backend/              Java 17 / Spring Boot
  demonstracao/         Previa offline independente
  scripts/              Desenvolvimento, build e testes
  docs/                 API, arquitetura, regras e operacao
```

## 7. Regras de atendimento

Prioridades propostas: **Crítica 4h; Alta 8h; Média 24h; Baixa 72h**, em **horas corridas**, incluindo finais de semana. A espera não pausa o prazo. Reabrir reinicia o ciclo; alterar a prioridade recalcula a partir do início do ciclo atual. Esses valores são metas iniciais, não um compromisso contratual aprovado pela empresa.

Indicadores de período consideram chamados abertos no intervalo selecionado. O gráfico mostra os últimos sete dias dentro desse conjunto. As médias contam da abertura original; os totais por departamento são históricos. Sem chamados resolvidos, o percentual de cumprimento usa 100% convencional, não evidência de desempenho medido.

Se um computador estiver totalmente sem rede, o colaborador precisará de outro dispositivo conectado ou de um canal alternativo de TI.

## Referências oficiais

- Angular, compatibilidade: https://angular.dev/reference/versions
- Spring Boot 3.5, requisitos: https://docs.spring.io/spring-boot/3.5/system-requirements.html
- Spring Security, CSRF: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
- PostgreSQL, backups: https://www.postgresql.org/docs/16/backup.html

Material de projeto, **não certificação** de segurança, conformidade legal ou disponibilidade. Homologue antes de usar dados reais.
