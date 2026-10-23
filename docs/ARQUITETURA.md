# Arquitetura

```text
Navegador: Angular 20.3 / TypeScript
    | HTTPS, JSON, cookie HttpOnly e token CSRF
Spring Boot 3.5 / Java 17
    | SecurityConfig -> ApiController -> servicos -> repositorios JPA
PostgreSQL 16 + Flyway
    | Volume persistente do banco
Anexos: volume privado /app/storage, fora da pasta publica
```

Desenvolvimento: Angular em 4200 com proxy `/api` para Java em 8080. Build: a interface compilada fica nos recursos estáticos do Spring Boot, na mesma origem da API. Navegação por hash (`/#tickets`), sem exigir configuração adicional de rotas no servidor.

## Organização

`Domain.java`: entidades. `Dtos.java`: entradas validadas e saídas sem credenciais. Repositórios: persistência. `WorkService`: escopo, regras, chamados, histórico e indicadores. `AttachmentService`: validação e armazenamento de anexos. `UserService`: permissões e revogação. `TriageEngine`: grafo de perguntas. `Bootstrap`: catálogos, artigos e, no modo demo, dados fictícios.

No Angular, `ApiService` trata sessão, CSRF, JSON, multipart e downloads. `AppComponent` organiza os fluxos e a navegação; `IconComponent` possui ícones SVG locais; `models.ts` define contratos. Para crescimento em equipe, divida o componente e o controlador em módulos por funcionalidade.

## Entidades

| Tabela | Finalidade |
|---|---|
| departments | Setores ativos/inativos |
| app_users | Contas, perfil, departamento, hash e versão de autorização |
| categories | Categorias de chamado |
| tickets | Solicitação, responsável, status, prioridade, prazos e avaliação |
| comments | Conversa e notas internas |
| ticket_history | Histórico cronológico |
| attachments | Metadados; arquivos no volume privado |
| notifications | Avisos internos por pessoa |
| articles | Base de conhecimento |
| triages | Sessão, transcrição e chamado vinculado |

Departamento possui usuários e chamados; usuário abre vários chamados; técnico atende vários. Chamado possui comentários, histórico e anexos. Triagem pode originar no máximo um chamado. O setor do chamado é registrado na abertura: mudar o departamento do colaborador não transfere os chamados anteriores.

Chamados e triagens usam controle otimista de versão: alterações obsoletas retornam 409. Uploads bloqueiam o registro do chamado para serializar a verificação do limite de anexos. Chaves estrangeiras e unicidade protegem relacionamentos. Chamados não são excluídos pela interface; cancelamento preserva o histórico.

## Limites e evolução

Uma instância da API, sessões em memória, volume local de anexos, agregados de indicadores calculados em memória. Reiniciar a API encerra sessões, não os dados. Antes de alto volume/cluster: sessões compartilhadas, armazenamento de objetos, agregados SQL, rate limiting distribuído e testes de carga.

O histórico não é uma trilha externa imutável; quem administra diretamente o banco pode alterá-lo. A prévia `demonstracao/` é independente em JavaScript, não o build Angular nem prova de segurança do back-end.

## Compatibilidade de schema

Enums persistidos usam `@JdbcTypeCode(SqlTypes.VARCHAR)` para manter a mesma representacao textual no PostgreSQL e no H2. Isso evita a expectativa de ENUM nativo do Hibernate em H2 ao validar a migracao SQL. Referencia oficial: https://docs.hibernate.org/orm/6.5/migration-guide/#datatype-for-enums
