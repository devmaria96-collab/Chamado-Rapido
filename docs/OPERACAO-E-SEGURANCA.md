# Operação e segurança

## Preparar ambiente real

Esta entrega é uma base de projeto, não um serviço gerenciado nem uma auditoria concluída. Homologue antes de inserir dados reais.

Use banco/volumes novos; `DEMO_ENABLED=false`; perfil `container`; senhas exclusivas; HTTPS em proxy reverso; `COOKIE_SECURE=true`; DNS e firewall adequados. Não publique a porta do PostgreSQL nem execute o perfil `local` em produção.

Cadastre o administrador inicial via variáveis de ambiente e troque a senha ao entrar. Após provisionar, remova o segredo inicial da configuração e mantenha um procedimento seguro de recuperação. A recuperação comum é feita pelo administrador; não existe e-mail automático de reset nem acesso alternativo secreto.

Desativar o modo demo **não remove contas de exemplo** já cadastradas. Não reutilize o banco da demonstração.

## Controles presentes e limites

BCrypt custo 12; sessão de servidor; cookie HttpOnly/SameSite=Lax; CSRF; autorização por chamado; revogação por versão da conta; limite de falhas de login por IP; validação de entrada; ORM; CSP; proteção contra enquadramento; downloads autenticados; nomes UUID; proteção contra fórmulas em CSV.

Senhas novas: 12 a 72 **bytes UTF-8**, maiúscula, minúscula e número. Contas novas e senhas redefinidas exigem troca. Mudanças de senha, perfil, setor, e-mail ou estado ativo revogam a autorização das sessões anteriores.

O rate limiting é em memória por IP. Atrás de proxy pode agrupar vários usuários; configure controle de taxa e política de proxies confiáveis na infraestrutura, sem aceitar indiscriminadamente cabeçalhos do cliente.

**Ainda necessários conforme o uso:** antivírus/quarentena; monitoramento; trilha externa de eventos administrativos/logins; plano de incidente; política de retenção; revisão de privacidade; teste de restauração; varredura de dependências; testes de carga e segurança. A assinatura de um PDF/imagem não prova que ele é seguro. O sistema não deve ser descrito como certificado ou automaticamente conforme à LGPD.

## Backup

Banco e anexos devem ter backups **coordenados**. Um dump PostgreSQL não inclui os arquivos do volume `uploads`. Mantenha cópias criptografadas fora do servidor com acesso restrito e retenção aprovada.

Exemplo, na raiz do projeto, ajustando o usuário se necessário:

```sh
docker compose exec -T db pg_dump -U chamado -d chamado_rapido -Fc > backup-chamado.dump
```

No Windows, use CMD ou redirecionamento binário compatível; não regrave o dump como texto. Copie também `uploads` usando ferramenta de backup de volumes. Suspenda escritas para obter snapshots consistentes. Teste a restauração em **ambiente separado** antes de depender do backup.

No H2 de estudo, pare a API antes de copiar `backend/data`. H2 é para desenvolvimento local, não a instalação multiusuário recomendada.

## Atualizar e diagnosticar

Faça backup e homologue as mudanças. Depois de inicializar um banco, não edite `V1__schema.sql`: adicione migrações `V2__...sql`. Gere/versione o lockfile npm. Para reprodutibilidade mais estrita, fixe digests de imagens Docker após validação. Revise correções de segurança das dependências.

```sh
docker compose ps
docker compose logs --tail=200 app
docker compose logs --tail=100 db
```

Porta ocupada: altere `APP_PORT`. Falha de banco: confira credenciais e saúde do serviço. CSRF: recarregue e entre novamente, não desative a proteção. Build sem dependências: confira acesso HTTPS a npm/Maven e proxy. Não ignore testes para contornar erros de compilação.

## Marca e materiais

Logo original preservada em `frontend/public/assets/logo-original.png`. Dependências mantêm suas próprias licenças. Não foi feita pesquisa de registro de marca, exclusividade da imagem ou disponibilidade comercial do nome.
