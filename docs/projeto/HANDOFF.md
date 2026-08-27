# Beto Banco 2.0 — Relatório de Estado Técnico

**Data:** 2026-08-26 · **Branch atual:** `feat/fase-3-comercio` · **HEAD:** `91c8083`
**Repositório:** `C:\Users\wende\OneDrive\Documentos\Beto_project\Beto_Banco` · remoto `github.com/kauaxp77/Beto_Banco`

---

## 1. Visão Geral

Plataforma SaaS de preparação para concursos bancários. O objetivo do programa
**Beto Banco 2.0** é substituir o Supabase como backend por uma aplicação Java
proprietária, na qual **toda a regra de negócio e toda a autorização residem no
servidor**.

O sistema legado é um protótipo React 19 + Vite (~2.300 linhas) que usa Supabase
como BaaS (Auth, RLS, PostgreSQL) e possui três falhas de segurança conhecidas,
ainda ativas, descritas na seção 6.

O programa foi decomposto em **cinco sub-projetos**, cada um com spec e ciclo próprio:

| # | Sub-projeto | Conteúdo | Estado |
|---|---|---|---|
| 1 | **Núcleo** | fundação, auth, alunos, produtos, entitlements, pagamento, webhook, e-mail | **em andamento** |
| 2 | Conteúdo | cursos, módulos, aulas, progresso, CMS | não iniciado |
| 3 | Simulados | motor servidor-autoritativo, timer, analytics | não iniciado |
| 4 | Inteligência | engine de cronograma, métricas de questões | não iniciado |
| 5 | Engajamento | redações, gamificação, ranking, notificações | não iniciado |

O sub-projeto 1 (Núcleo) está dividido em 4 fases: **Fase 1 (Fundação) e Fase 2
(Autenticação) concluídas**; **Fase 3 (Comércio) em andamento**; Fase 4 (Frontend)
não iniciada.

**Documentos de referência no repositório:**
- `docs/superpowers/specs/2026-08-26-nucleo-pagamento-acesso-design.md` — **spec autoritativa**
- `docs/superpowers/plans/2026-08-26-nucleo-fase-1-fundacao.md`
- `docs/superpowers/plans/2026-08-26-nucleo-fase-2-autenticacao.md`

---

## 2. Stack Tecnológica

**Backend** (`backend/`)
- Java 21 (Temurin, em `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`)
- Spring Boot **3.5.6** (fixado — o `start.spring.io` já não serve esta linha)
- Spring Web, Spring Security, Spring Data JPA, Bean Validation, Spring Mail
- PostgreSQL **17** · Flyway · Maven Wrapper (`./mvnw`, Maven **não** instalado)
- JJWT 0.12.6 · Bouncy Castle 1.78.1 (Argon2) · Bucket4j `bucket4j_jdk17-core` 8.14.0
- springdoc-openapi 2.8.6 · Logstash Logback Encoder 8.0
- JUnit 5 · Testcontainers **1.21.4** · ArchUnit 1.3.0

**Banco:** PostgreSQL do Supabase (projeto `bjnplubfqoltaxfboodl`, versão 17.6.1.155).
Em desenvolvimento e teste, container `postgres:17-alpine`.

**Frontend legado** (`frontend-react/`): React 19 + Vite 8 + Supabase JS. Será
substituído na Fase 4.

**IMPORTANTE — variável de ambiente:** `JAVA_HOME` não está definido. Todo comando Maven
precisa do prefixo:
```bash
cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw test
```

---

## 3. Arquitetura e Dados

### Monólito modular com fronteiras verificadas pelo build

Cada módulo publica um pacote `api/` com interfaces e DTOs. **Nenhum módulo importa
`entity` ou `repository` de outro** — três regras ArchUnit reprovam o build quando isso
acontece (e já pegaram cinco violações reais).

```
com.betobanco
├── config/        PasswordEncoderConfig, OpenApiConfig
├── shared/        exception · response · pagination · trace
├── security/      SecurityConfig · JwtService · JwtAuthFilter · RateLimitFilter
│                  EnvelopeAuthenticationEntryPoint · EnvelopeAccessDeniedHandler
├── users/         api/{UserDirectory,UserAccount} · entity · repository · service
├── auth/          entity/{RefreshToken,PasswordResetToken} · service · controller
├── catalog/       api/ProductCatalog · entity/Product
├── entitlements/  api/EntitlementService · entity/Entitlement
├── payments/      api/{PaymentGateway,PaymentNotification} · gateway/FakePaymentGateway
├── webhooks/      controller · WebhookIngestService · WebhookProcessor · WebhookQueue
├── email/         api/EmailService · EmailOutboxService · EmailDispatcher · SmtpEmailSender
└── audit/         api/AuditLogger · entity/AuditLog
```

### Migrations Flyway

| Migration | Conteúdo |
|---|---|
| `V1__baseline.sql` | no-op, só liga o Flyway |
| `V2__identity_schema.sql` | `users`, `roles`, `user_roles`, `students` |
| `V3__migrate_legacy_profiles.sql` | migra `public.profiles` + `auth.users` do Supabase |
| `V4__auth_tokens.sql` | `refresh_tokens`, `password_reset_tokens` |
| `V5__commerce.sql` | `products`, `entitlements`, `payments`, `payment_splits`, `webhook_events`, `email_outbox`, `audit_logs` |

### Entidades e relações principais

```
users (id UUID PK, email UNIQUE, password_hash, full_name, status)
  ├─1:1─ students (id = users.id)          ← MESMO UUID, não gera outro
  ├─N:N─ roles via user_roles              ← ROLE_STUDENT | ROLE_ADMIN | ROLE_INSTRUCTOR
  ├─1:N─ refresh_tokens (token_hash UNIQUE, replaced_by → self)
  ├─1:N─ password_reset_tokens (purpose: FIRST_ACCESS | RESET)
  └─1:N─ entitlements ─N:1─ products

payments (provider + provider_transaction_id UNIQUE)
  ├─1:N─ payment_splits
  └── referencia users e products

webhook_events (provider + event_id UNIQUE)   ← mecanismo de idempotência
email_outbox (dedup_key UNIQUE)
audit_logs
```

**Índices que carregam regra de negócio:**
- `entitlements_ativo_unico`: `UNIQUE (user_id, product_id) WHERE revoked_at IS NULL`
  — torna a concessão idempotente **e** permite recompra após estorno
- `webhook_events (provider, event_id) UNIQUE` — a idempotência do webhook
- `payments (provider, provider_transaction_id) UNIQUE`

---

## 4. Regras de Negócio

### Decisões tomadas e aprovadas (spec, seção 3)

| # | Decisão |
|---|---|
| D1 | Gateway real: **InfinitePay com split**. Demais gateways ficam como interface sem implementação |
| D2 | **Manter o PostgreSQL do Supabase**; abandonar Auth, RLS e edge functions |
| D3 | Modelo de acesso: **catálogo** — cada produto libera um conjunto |
| D4 | Primeiro acesso por **link de definição de senha**, NÃO senha temporária por e-mail |
| D5 | Webhook: **recebe-e-registra síncrono, processa em background** |
| D6 | **Estorno e chargeback revogam o acesso automaticamente** |
| D9 | `DelegatingPasswordEncoder`: Argon2id para novas, bcrypt aceito para legadas, re-hash no login |
| D12 | Identificador do aluno é **o mesmo UUID de `auth.users`/`profiles`** |
| D13 | `payments.status` inclui `CANCELLED`, distinto de `REFUNDED` |
| D14 | `ROLE_INSTRUCTOR` é obrigatório |

### Invariantes que NÃO podem ser quebradas

1. **Identidade nunca vem do cliente.** Nenhum controller aceita `userId` por path,
   query ou body. Regra ArchUnit verifica isso no build.
2. **Role ≠ entitlement.** Role diz o que a pessoa É; entitlement diz o que ela COMPROU.
   Um aluno adimplente e um inadimplente têm a mesma role.
3. **Idempotência vem do banco**, via unique constraint — nunca de "consulta se existe,
   senão insere", entre cujos passos cabe outra requisição concorrente.
4. **A assinatura do webhook é verificada sobre os BYTES CRUS**, antes de qualquer
   desserialização.
5. **Nenhuma chamada externa dentro de transação de domínio.** E-mail vai para a outbox.
6. **SKU desconhecido não é adivinhado** — vira `FAILED` e cai na fila do admin.
7. **Stack trace nunca sai na resposta.** `code` é contrato estável; `message` é mutável
   e não pode divergir da natureza do `status`.
8. Autorização: **negar por padrão**; liberar exige regra explícita.
9. `ddl-auto=validate` sempre. Nenhuma tabela criada pelo Hibernate.

### Envelope de resposta (obrigatório)

```json
// sucesso
{ "success": true, "data": {} }
// erro
{ "success": false, "error": { "code","message","status","path","traceId","timestamp","fieldErrors" } }
```

### Fluxo pagamento → acesso (3 estágios)

1. **Recepção** (`WebhookIngestService`): valida assinatura sobre bytes crus → `INSERT`
   em `webhook_events` → responde `200` em milissegundos. Duplicado colide no índice e
   também devolve `200`. Assinatura inválida → `401` **sem persistir**.
2. **Processamento** (`WebhookProcessor`, `@Scheduled`): uma transação por evento —
   registra `payments` + `payment_splits`, cria aluno **sem senha**, concede entitlement,
   grava auditoria, enfileira e-mail. Falha → backoff 1m/5m/15m/1h/6h → depois `MANUAL`.
3. **Entrega do e-mail** (`EmailDispatcher`, `@Scheduled`): fora da transação.
   Garantia **at-least-once** declarada.

---

## 5. Estado Atual

### Fase 1 — Fundação ✅ CONCLUÍDA (28 commits, publicada)
Branch `docs/spec-nucleo-pagamento-acesso` no remoto. Projeto Maven, perfis de ambiente,
Docker Compose (Postgres 17 + MailHog), Flyway, Testcontainers, `traceId`, envelope de
resposta, tratamento global de exceções, paginação com teto 100, OpenAPI, 3 regras
ArchUnit, CI no GitHub Actions.

### Fase 2 — Autenticação ✅ CONCLUÍDA (11 commits, publicada)
Branch `feat/fase-2-autenticacao` no remoto. **92 testes verdes, `BUILD SUCCESS`.**

Endpoints funcionando (verificados com a aplicação rodando de verdade):
```
POST /api/v1/auth/register          POST /api/v1/auth/login
GET  /api/v1/auth/me                POST /api/v1/auth/refresh
POST /api/v1/auth/logout            POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password    GET/PUT /api/v1/students/me
```
Argon2id + bcrypt legado com re-hash automático · JWT HS256 15min · refresh token opaco
rotativo com detecção de reuso · rate limit configurável · 401/403 no envelope padrão.

**Migração do Supabase testada:** os 2 usuários existentes (`teste@gmail.com` ALUNO,
`admin@gmail.com` ADMIN) têm hash `$2a$10$` (bcrypt puro). A V3 preserva os UUIDs e
prefixa `{bcrypt}`, então **entram com a senha atual** e o hash é promovido a Argon2id
no primeiro login.

### Fase 3 — Comércio 🔄 EM ANDAMENTO (2 commits, local)
Branch `feat/fase-3-comercio`. **109 testes, 107 verdes, 2 falhas.**

Já implementado e com testes passando:
- ✅ `V5__commerce.sql` — 7 tabelas
- ✅ Catálogo (`Product`, `ProductCatalog`) — 6 testes verdes
- ✅ Entitlements com concessão idempotente e recompra pós-estorno — 6 testes verdes
- ✅ `PaymentGateway` / `PaymentNotification` (abstração)
- ✅ `FakePaymentGateway` (SHA-256 de segredo+corpo no header `X-Signature`)
- ✅ Recepção do webhook com idempotência por constraint
- ✅ `WebhookProcessor` (aprovado / pendente / cancelado / estorno / ignorado)
- ✅ Outbox de e-mail + `EmailDispatcher` + `SmtpEmailSender` (4 templates)
- ✅ `AuditLogger`

Testes do fluxo que **já passam** (9 de 11 em `WebhookFluxoTest`): pagamento aprovado cria
aluno + libera acesso + enfileira e-mail; duplicado concorrente com 8 threads cria um só;
assinatura inválida → 401 sem persistir; SKU desconhecido → FAILED; pendente não libera;
estorno revoga; evento sem efeito → IGNORED; outbox não duplica.

---

## 6. Pendências e Bugs

### 🔴 BUG ATIVO — as 2 falhas da última interação

**Ambas têm a MESMA causa raiz:** o `WebhookProcessor` (módulo `webhooks`) manipula
`PaymentRepository`, `PaymentSplitRepository` e a entidade `Payment` **diretamente**, em
vez de falar com uma interface publicada por `payments`. Isso viola a fronteira de módulo
que a spec exige.

**Falha 1 — `ModuleBoundariesTest.nenhumModuloAcessaEntityOuRepositoryDeOutro`**
```
Architecture Violation ... 'no classes that reside outside of package
'com.betobanco.payments..' should depend on classes that reside in any package
['com.betobanco.payments.entity..', 'com.betobanco.payments.repository..']'
was violated (38 times)
```

**Falha 2 — `WebhookFluxoTest.webhookDuplicadoNaoDuplicaNadaEDevolve200`**
```
expected: 200 but was: 500   (WebhookFluxoTest.java:142)
```
O segundo envio do mesmo webhook devolve 500 em vez de 200. Provável causa: a
`DataIntegrityViolationException` capturada em `WebhookIngestService` marca a transação
como *rollback-only*, e o commit falha depois — o `catch` não basta quando o `saveAndFlush`
já sujou a transação.

**Correção proposta (não aplicada):**
1. Criar `com.betobanco.payments.api.PaymentLedger` expondo o que `webhooks` precisa
   (`registrar(PaymentNotification, String provider) → PaymentRef`, `marcarAprovado`,
   `marcarStatus`, `registrarSplits`), e mover para `payments/service/` a lógica que hoje
   está em `WebhookProcessor.registrarPagamento`.
2. Para a falha 2: isolar a inserção do `webhook_events` em transação própria
   (`REQUIRES_NEW`) ou consultar antes com tratamento de corrida, de modo que a colisão
   não invalide a transação externa.

### ⚠️ Bloqueio externo — InfinitePay
**Não temos a documentação do webhook da InfinitePay** (formato do payload, esquema de
assinatura, eventos de split). Toda a máquina está pronta e testada com o
`FakePaymentGateway`; falta **uma classe** (`InfinitePayGateway implements PaymentGateway`).

### ⚠️ Interferência de ferramenta externa
O **GitHub Copilot App Modernization** (extensão do VS Code) rodou sozinho neste
repositório duas vezes: criou a branch `appmod/java-upgrade-20260826191003`, um
`stash@{0}`, instalou JDK 25 em `C:\Users\wende\.jdk`, e **reverteu alterações
aprovadas** (`ENV SPRING_PROFILES_ACTIVE=prod` do Dockerfile). **Recomendação: desativar
a extensão.** Antes de cada commit, confirmar `git branch --show-current` e que
`<java.version>` continua **21**.

### Pendências menores registradas
- `frontend-react/dist` foi removido do rastreamento; **confirmar como o Vercel publica
  antes de mergear** — é a única mudança capaz de derrubar o site no ar.
- Refresh token viaja no corpo JSON, não em cookie `HttpOnly` (spec §6.2). É
  **pré-requisito da Fase 4**.
- Rate limit em memória: o limite multiplica pelo número de instâncias. Migrar para Redis
  quando houver replicação.
- Chave publicável do Supabase permanece no histórico do Git — impede tornar o repositório
  público sem limpeza.
- **3 falhas de segurança ainda ativas no sistema legado** (serão corrigidas no
  sub-projeto 3): `/admin` acessível a qualquer aluno logado; gabarito (`is_correct`) enviado
  ao navegador antes de responder; nota calculada no cliente e gravada via insert direto.

### Faltando para concluir a Fase 3
- Corrigir as 2 falhas acima
- Endpoints admin: `/admin/products` (CRUD), `/admin/payments`, `/admin/webhooks`
  (listar + reprocessar + resolver manualmente), `/admin/students/{id}/entitlements`,
  `/admin/audit-logs`, `/admin/dashboard`
- `GET /api/v1/products` (público) e `GET /api/v1/students/me/entitlements`
- Ligar o `forgot-password` da Fase 2 à outbox (hoje cria o token e o descarta)

---

## Como retomar

```bash
cd "C:\Users\wende\OneDrive\Documentos\Beto_project\Beto_Banco"
git checkout feat/fase-3-comercio
docker compose up -d                       # Postgres 17 + MailHog

cd backend && JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" ./mvnw test
# esperado hoje: 109 testes, 2 falhas

# subir a aplicação
JAVA_HOME="..." ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# health:  http://localhost:8080/api/v1/actuator/health
# swagger: http://localhost:8080/api/v1/swagger-ui.html
# MailHog: http://localhost:8025
```

**Branches:** `main` · `docs/spec-nucleo-pagamento-acesso` (Fase 1, publicada) ·
`chore/organizar-estrutura` (publicada) · `feat/fase-2-autenticacao` (Fase 2, publicada) ·
`feat/fase-3-comercio` (atual, **local**). PR sugerido:
`https://github.com/kauaxp77/Beto_Banco/pull/new/feat/fase-2-autenticacao`

**Identidade Git** (o repositório não tem uma configurada):
```bash
git -c user.name="kauaxp77" -c user.email="77xpferramentas@gmail.com" commit -m "..."
```
