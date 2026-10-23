# API REST

Prefixo `/api`. JSON UTF-8, exceto anexos e CSV. Datas ISO-8601 em UTC, apresentadas pela interface no horário local.

## Sessão e CSRF

1. `GET /auth/csrf` recebe `{token, headerName}` e cookie de sessão.
2. `POST /auth/login`: `application/x-www-form-urlencoded`, campos `email` e `password`, cookie da mesma sessão e cabeçalho CSRF retornado.
3. Busque `/auth/csrf` novamente depois do login, pois a autenticação renova a proteção.
4. `GET /auth/me` retorna a conta sem senha/hash.
5. Escritas `POST`, `PUT`, `PATCH`, inclusive upload/logout, exigem sessão e token CSRF.
6. `POST /auth/logout` invalida a sessão; `POST /auth/password` recebe `currentPassword` e `newPassword`, troca a senha e exige novo login.

Senha temporária bloqueia demais funcionalidades até sua troca. Em ambiente real, usar HTTPS.

## Rotas principais

| Método | Rota | Finalidade |
|---|---|---|
| GET | `/public/info` | Nome, versão e modo demo |
| GET | `/meta` | Categorias/setores ativos e técnicos permitidos |
| GET / POST | `/tickets` | Listar autorizados / criar chamado |
| GET / PATCH | `/tickets/{id}` | Detalhar / atualizar status, atribuição e prioridade |
| POST | `/tickets/{id}/comments` | `{body, internal}` |
| POST | `/tickets/{id}/rating` | `{rating: 1..5}`, apenas solicitante |
| POST | `/tickets/{id}/attachments` | Multipart com campo `file` |
| GET | `/attachments/{id}` | Download autorizado |
| GET | `/dashboard` | Indicadores da fila autorizada |
| GET | `/departments/overview` | Totais históricos por setor, TI |
| GET | `/reports/export` | CSV, TI |
| GET | `/notifications` | Últimos 50 avisos da conta |
| POST | `/notifications/{id}/read` | Marcar aviso próprio como lido |
| POST | `/notifications/read-all` | Marcar todos os avisos próprios |
| GET | `/articles` | Artigos publicados; admin também vê inativos |
| POST | `/triages` | `{category}`: rede, impressora, acesso ou computador |
| GET | `/triages/{id}` | Consultar triagem própria |
| POST | `/triages/{id}/answers` | `{choice, version}` |
| GET / POST | `/admin/users` | Listar/criar contas |
| PUT | `/admin/users/{id}` | Editar/inativar/resetar senha |
| GET | `/admin/catalogs` | Catálogos incluindo inativos |
| POST / PUT | `/admin/departments[/{id}]` | Criar/editar setor |
| POST / PUT | `/admin/categories[/{id}]` | Criar/editar categoria |
| POST / PUT | `/admin/articles[/{id}]` | Criar/editar artigo |

Rotas `/admin` exigem administrador; demais operações seguem as permissões por chamado.

## Pesquisa

`GET /tickets?page=0&size=12&q=impressora&status=ABERTO&priority=ALTA&department=1&category=3&mine=true&overdue=true&days=30`

Parâmetros opcionais. Página começa em zero, tamanho máximo 200. `days=0` considera todo o histórico; `q=CR-000001` pesquisa protocolo. `mine` significa atribuídos à conta para a TI e chamados próprios para colaboradores. `/dashboard` e `/reports/export` aceitam filtros aplicáveis; a interface de relatórios envia apenas `days`.

## Exemplo de abertura

```json
{
  "title": "Impressora do Financeiro nao imprime",
  "description": "A impressora apresenta uma mensagem de erro ao enviar documentos. Outros usuarios tambem estao afetados.",
  "categoryId": 3,
  "priority": "ALTA",
  "location": "Financeiro - sala 2",
  "equipment": "IMP-005",
  "contact": "Ramal 210",
  "triageId": null
}
```

Use um `categoryId` retornado por `/meta`; o exemplo não garante ID fixo. Solicitante, departamento, status e prazo são determinados pelo servidor.

## Exemplo de resolução

```json
{
  "version": 2,
  "status": "RESOLVIDO",
  "solution": "Fila de impressao corrigida e teste concluido com o solicitante."
}
```

Use a versão atual dos detalhes. `assigneeId` atribui; `unassign: true` remove a atribuição; `priority` ajusta prioridade; `reason` justifica cancelamento/reabertura. Omitir campo opcional mantém seu valor. Transições inválidas são rejeitadas.

## Erros

400: dados/regras inválidos. 401: sessão ausente/expirada. 403: permissão, CSRF ou senha temporária. 404: recurso ausente ou chamado não autorizado. 409: versão obsoleta/duplicidade. 413: arquivo grande. 429: excesso de falhas de login. 500: erro genérico, sem stacktrace. Formato habitual: `{ "message": "..." }`.
