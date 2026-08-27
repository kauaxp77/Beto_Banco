# 🚀 Deploy em produção — Vercel (frontend) + Render (backend)

A plataforma tem duas partes e a Vercel hospeda **apenas uma** delas:

| Parte | O que é | Onde hospedar |
|---|---|---|
| Frontend | React/Vite (estático) | **Vercel** ✅ |
| Backend | Java Spring + PostgreSQL | **Render** (ou Railway/Fly) — a Vercel **não roda** Java |

> **Por que o login falha só com a Vercel?** O site chama a API em `/api/v1/...`.
> Sem um backend hospedado, essas chamadas não têm para onde ir → o login
> (e tudo que depende de dados) falha. Deploy do frontend sozinho = vitrine sem sistema.

---

## Passo 1 — Banco de dados: Supabase (recomendado)

Use o projeto Supabase **`Banco`** que já existe na sua conta — é o banco da
plataforma antiga, e as migrações do backend **migram os usuários legados
automaticamente** no primeiro boot (V3), sem apagar nada.

1. No [dashboard do Supabase](https://supabase.com/dashboard), abra o projeto
   **Banco** → **Connect** (topo) → aba **Session pooler** (porta 5432).
   ⚠️ Use o *pooler* (IPv4), não a conexão direta — o Render não tem IPv6.
2. Os dados de conexão são:
   - Host: `aws-0-us-west-2.pooler.supabase.com` · Porta: `5432` · Banco: `postgres`
   - Usuário: `postgres.bjnplubfqoltaxfboodl`
   - Senha: a senha do banco (se não lembrar: **Settings → Database → Reset
     database password**)
3. Para o backend, isso vira:
   - `DATABASE_URL` = `jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:5432/postgres`
   - `DATABASE_USER` = `postgres.bjnplubfqoltaxfboodl`
   - `DATABASE_PASSWORD` = a senha acima

> Alternativa: um Postgres do próprio Render (New → PostgreSQL, plano Free)
> também funciona — mas o do Render Free expira em 30 dias; o Supabase não.

## Passo 2 — Backend (Render, grátis)

1. **New → Web Service** → conecte o repositório `kauaxp77/Beto_Banco`.
2. Configure:
   - **Root Directory**: `backend`
   - **Runtime**: `Docker` (o `backend/Dockerfile` já está pronto)
   - **Region**: `Oregon (US West)` — mesma região do Supabase `Banco`
   - **Instance Type**: Free
3. **Environment Variables** (com os valores do Supabase do Passo 1):

   | Variável | Valor |
   |---|---|
   | `DATABASE_URL` | `jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:5432/postgres` |
   | `DATABASE_USER` | `postgres.bjnplubfqoltaxfboodl` |
   | `DATABASE_PASSWORD` | senha do banco no Supabase |
   | `JWT_SECRET` | um segredo longo e aleatório (32+ caracteres) |
   | `CORS_ALLOWED_ORIGINS` | `https://frontend-w77xp.vercel.app` (sua URL da Vercel) |
   | `APP_BASE_URL` | `https://frontend-w77xp.vercel.app` (links dos e-mails) |
   | `SMTP_HOST` / `SMTP_PORT` / `SMTP_USER` / `SMTP_PASSWORD` | seu provedor de e-mail (opcional no início — sem isso os e-mails ficam na fila) |

4. **Create Web Service** → aguarde o build (~5 min). As migrações Flyway
   rodam sozinhas no primeiro boot.
5. Anote a URL pública, ex.: `https://betobanco-api.onrender.com`.
6. Teste: `https://betobanco-api.onrender.com/api/v1/actuator/health`
   deve responder `{"status":"UP"}`.

> 💡 No plano Free o serviço "dorme" após 15 min sem uso e o primeiro acesso
> demora ~50s para acordar. Para produção de verdade, use o plano Starter.

## Passo 3 — Frontend (Vercel)

1. Edite **`frontend/vercel.json`** e troque `https://SEU-BACKEND.onrender.com`
   pela URL real do Passo 2. Commit + push (a Vercel redeploya sozinha).

   *Esse arquivo faz a Vercel **proxyar** `/api/*` para o backend: para o
   navegador tudo é a mesma origem, então o cookie de login funciona em
   qualquer navegador, sem CORS complicado.*

2. No projeto da Vercel, confira em **Settings**:
   - **Root Directory**: `frontend`
   - **Framework Preset**: Vite (build `npm run build`, output `dist` — automático)

3. **Settings → Deployment Protection** → desative **Vercel Authentication**
   para Production. ⚠️ *Sem isso, qualquer visitante cai na tela de login DA
   VERCEL em vez do seu site — é exatamente o que acontece hoje.*

## Passo 4 — Primeiro admin

Com o backend no ar, crie sua conta pelo site (`/login` → registrar não existe
público; use o registro via API) e promova a admin direto no banco
(Render → seu Postgres → **Shell**):

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'seu-email@aqui.com' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;
```

## Checklist final

- [ ] `/api/v1/actuator/health` do Render responde UP
- [ ] `frontend/vercel.json` aponta para a URL real do backend
- [ ] Deployment Protection desativada na Vercel
- [ ] Login funciona no site público
- [ ] Sua conta tem ROLE_ADMIN (menu Admin aparece)

---

### Alternativa sem proxy (não recomendada)

Se preferir o navegador falando direto com a API em outro domínio:
na Vercel defina `VITE_API_URL=https://sua-api/api/v1` e no Render
`COOKIE_SAME_SITE=None`. Funciona no Chrome, mas o cookie de sessão vira
"third-party" e o Safari bloqueia — por isso o proxy é o caminho padrão.
