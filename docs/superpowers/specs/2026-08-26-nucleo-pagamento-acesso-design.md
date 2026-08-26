# Beto Banco 2.0 — Núcleo: pagamento → acesso

**Data:** 2026-08-26
**Sub-projeto:** 1 de 5
**Status:** design aprovado, aguardando plano de implementação

---

## 1. Contexto

A plataforma Beto Banco existe hoje como uma aplicação React 19 + Vite servida na Vercel,
com Supabase atuando como backend completo (Auth, JWT, RLS e PostgreSQL). São
aproximadamente 2.276 linhas de JSX/JS, sem TypeScript e sem testes.

O objetivo do programa Beto Banco 2.0 é substituir o Supabase como backend por uma
aplicação Java 21 + Spring Boot proprietária, onde toda a regra de negócio e toda a
autorização passam a residir no servidor.

O programa completo abrange doze domínios de negócio independentes. Ele foi decomposto em
cinco sub-projetos, cada um com sua própria spec, plano e ciclo de implementação:

| # | Sub-projeto | Conteúdo |
|---|---|---|
| 1 | **Núcleo** | fundação, autenticação, alunos, produtos, entitlements, pagamento, webhook, e-mail, admin mínimo |
| 2 | Conteúdo | cursos, módulos, aulas, progresso, edital verticalizado, storage, CMS da landing |
| 3 | Simulados | motor servidor-autoritativo, timer, submissão, analytics, heurísticas |
| 4 | Inteligência | engine de cronograma, métricas de questões, desempenho |
| 5 | Engajamento | redações, gamificação, ranking, notificações |

Este documento especifica **apenas o sub-projeto 1**.

### 1.1 Estado real do sistema atual

O levantamento do código revelou divergências relevantes entre a documentação existente e
o que está implementado. A documentação descreve intenção, não comportamento.

| Documentação afirma | Código faz |
|---|---|
| Timer de 4 horas persistente | Cronômetro crescente, sem limite, reiniciado a cada refresh |
| Heurística anti-cola "Right/Wrong" impede desfazer escolha | Não existe; resposta pode ser trocada livremente |
| Tabela `simulado_attempts` | Tabela chama-se `attempts`; `simulado_id` nulo, `level` fixo em `GERAL` |
| Split payment via InfinitePay | Edge function trata payloads de Hotmart/Kiwify; `checkout_link` aponta para Kiwify |
| Sistema processa o gabarito | Nota calculada no navegador e inserida diretamente na tabela pelo cliente |

Sempre que os documentos e o código divergirem, esta spec segue o **comportamento decidido
com o responsável pelo produto**, registrado na seção 3, e não a letra de nenhum dos dois.

### 1.2 Falhas de segurança existentes

Três problemas estão em produção hoje. O desenho deste sub-projeto os elimina por
construção, mas eles ficam registrados porque, enquanto o Núcleo não estiver no ar, seguem
ativos.

1. **Autorização inexistente no frontend.** `PrivateRoute` verifica apenas se há sessão. O
   comentário no próprio arquivo admite: *"since we don't have roles fetched easily yet,
   just check if logged in"*. Qualquer aluno autenticado abre `/admin`.
2. **Gabarito exposto.** `SimuladoEngine` executa `select('*, question_options(*)')`, o que
   envia o campo `is_correct` de todas as alternativas ao navegador antes de o aluno
   responder.
3. **Nota manipulável.** O escore é calculado no cliente e gravado via `insert` direto. A
   política RLS valida apenas `student_id = auth.uid()`, permitindo ao aluno gravar
   qualquer nota.

Nenhum dos três é corrigido por este sub-projeto — todos pertencem ao sub-projeto 3
(Simulados). Estão aqui para que a decisão de priorização seja consciente.

---

## 2. Escopo

### 2.1 Dentro do escopo

- Fundação Spring Boot: Maven, Java 21, Docker Compose, Flyway, perfis de ambiente,
  tratamento global de exceções, padrão de resposta, OpenAPI, Actuator, CI
- Autenticação própria com JWT e refresh token rotativo
- Autorização por role e por entitlement
- Cadastro e perfil de aluno
- Catálogo de produtos
- Entitlements (quem tem direito a qual produto)
- Abstração `PaymentGateway` e implementação InfinitePay com split
- Recepção, idempotência, retry e auditoria de webhooks de pagamento
- Liberação automática de acesso e revogação em estorno
- Outbox e envio de e-mail transacional
- Auditoria
- Frontend: landing, login, primeiro acesso, recuperação de senha, dashboard mínimo,
  perfil e painel administrativo do Núcleo

### 2.2 Fora do escopo

Cursos, módulos, aulas, progresso, CMS, cronograma, métricas de questões, simulados,
analytics, redações, gamificação, ranking e notificações in-app. Todos pertencem aos
sub-projetos 2 a 5.

O redesenho visual completo do dashboard do aluno também fica fora. Desenhá-lo agora
significaria desenhá-lo sem cronograma, sem desempenho e sem simulados — ou seja,
desenhá-lo duas vezes.

---

## 3. Decisões

| # | Decisão | Justificativa |
|---|---|---|
| D1 | Gateway real: **InfinitePay, com split** | Definição do responsável pelo produto. Os demais gateways permanecem como interface sem implementação |
| D2 | **Manter o PostgreSQL do Supabase**, abandonando Auth, RLS e edge functions | Zero migração de dados, backups já configurados, permitido pelo requisito original |
| D3 | Modelo de acesso: **catálogo** — cada produto libera um conjunto | Definição do responsável pelo produto. Permite vender turmas e módulos avulsos |
| D4 | Primeiro acesso por **link de definição de senha**, não senha temporária | Nenhuma senha trafega ou permanece arquivada em caixa de entrada. Reutiliza o mecanismo de recuperação de senha, custando menos código |
| D5 | Webhook: **recebe-e-registra síncrono, processa em background** | Única alternativa que entrega simultaneamente segurança, idempotência, auditabilidade e resiliência sem infraestrutura adicional |
| D6 | **Estorno e chargeback revogam o acesso automaticamente** | Sem isso, reembolso devolve o dinheiro e mantém o acesso indefinidamente |
| D7 | Monorepo: `backend/` ao lado de `frontend/` no repositório existente | O `vercel.json` na raiz continua válido |
| D8 | Frontend novo em **TypeScript**, em diretório paralelo | O antigo permanece no ar até o corte; rollback é uma linha no `vercel.json` |
| D9 | Hashing com `DelegatingPasswordEncoder`: Argon2id para senhas novas, bcrypt aceito para as legadas, re-hash no login | Preserva o acesso dos alunos existentes e migra a base gradualmente |
| D10 | Módulo `admins` **não é criado**; `catalog` e `entitlements` **são** | Admin é uma role sobre `User`, não uma entidade. Catálogo e entitlement precisam de dono explícito, sob pena de a regra de acesso vazar para dentro de `payments` |
| D11 | Tabela `payment_events` **não é criada** | `webhook_events`, `payments.status` e `audit_logs` já cobrem o fato sem duplicação |
| D12 | Identificador do aluno é **o mesmo UUID de `auth.users`/`profiles`** | Mantém válidas as chaves estrangeiras legadas de `attempts` e `questions` |

### 3.1 Divergências deliberadas do prompt mestre

Três pontos do documento de requisitos original foram contrariados de forma consciente,
com aprovação:

- **Item 11 e CSU-01** pedem geração e envio de credenciais temporárias por e-mail.
  Substituído por link de definição de senha (D4). O objetivo do requisito — o aluno recebe
  acesso automaticamente após o pagamento — é integralmente atendido.
- **Item 5** lista um módulo `admins`. Removido (D10); endpoints administrativos residem no
  módulo dono do dado.
- **Item 8** lista a tabela `payment_events`. Removida (D11).

---

## 4. Arquitetura e fronteiras de módulos

Monólito modular. O que distingue essa arquitetura de um monólito desorganizado não é a
estrutura de pastas — é o fato de as fronteiras serem **verificáveis pelo build**.

Cada módulo publica um pacote `api/` contendo interfaces e DTOs destinados aos demais
módulos. `entity`, `repository` e `service` são consumo interno. **Nenhum módulo importa
`entity` ou `repository` de outro.** Um teste ArchUnit executa no CI e falha quando essa
regra é violada.

Exemplo concreto da fronteira: `payments` não conhece `Student`. Ao confirmar um pagamento,
ele invoca `EntitlementService.grant(userId, productId, origem)` — interface publicada por
`entitlements`. Alterar como o entitlement é persistido não toca em `payments`.

```
com.betobanco
├── config/        OpenAPI, Jackson, CORS, scheduling
├── shared/        exception · response · pagination · audit · util
├── security/      JwtService · JwtAuthFilter · SecurityConfig · @CurrentUser
├── users/         identidade, credenciais, roles
├── auth/          login · refresh · logout · forgot/reset password
├── students/      perfil do aluno, status de acesso
├── catalog/       produtos à venda
├── entitlements/  quem tem direito a qual produto
├── payments/      Payment · PaymentGateway · InfinitePayGateway · splits
├── webhooks/      recepção · webhook_events · worker de processamento
├── email/         EmailService · outbox · worker de entrega
├── audit/         audit_logs
└── dashboard/     agregação somente-leitura para o admin
```

Estrutura interna de cada módulo: `api/`, `controller/`, `dto/`, `service/`, `repository/`,
`entity/`.

Endpoints administrativos residem no módulo dono do dado, sob o prefixo `/api/v1/admin/`.
Exemplos: `students/controller/AdminStudentController`,
`webhooks/controller/AdminWebhookController`. O módulo `dashboard` é a única exceção: ele
agrega dados de vários módulos, não possui tabela própria e consome exclusivamente as
interfaces `api/` dos demais.

---

## 5. Modelo de dados

Treze tabelas novas em PostgreSQL, criadas por migrations Flyway versionadas. Nenhuma tabela
existente é alterada ou removida, exceto pela remoção de políticas RLS descrita em 5.4.

Todos os identificadores são UUID gerados por `gen_random_uuid()`. Todos os timestamps são
`timestamptz` em UTC. `spring.jpa.hibernate.ddl-auto=validate` em todos os ambientes.

### 5.1 Identidade e sessão

**`users`** — `id`, `email` (unique, normalizado em minúsculas), `password_hash`,
`full_name`, `status` (`ACTIVE`, `BLOCKED`), `created_at`, `updated_at`

**`roles`** — `id`, `name` (unique): `ROLE_STUDENT`, `ROLE_ADMIN`, `ROLE_INSTRUCTOR`

**`user_roles`** — `user_id`, `role_id`, PK composta

**`students`** — `user_id` (PK e FK para `users`), `phone`, `created_at`, `updated_at`

O bloqueio de um aluno é registrado em `users.status`, não em `students`. Manter dois campos
de status para a mesma pessoa produziria estados contraditórios sem nenhum ganho: quem
governa o acesso ao conteúdo é o entitlement, não o perfil.

**`refresh_tokens`** — `id`, `user_id`, `token_hash` (unique), `issued_at`, `expires_at`,
`revoked_at`, `replaced_by`, `user_agent`, `ip`

**`password_reset_tokens`** — `id`, `user_id`, `token_hash` (unique), `purpose`
(`FIRST_ACCESS`, `RESET`), `expires_at`, `used_at`, `created_at`

Primeiro acesso e recuperação de senha compartilham tabela e endpoint, distinguidos pelo
`purpose` e pelo prazo: 72 horas para `FIRST_ACCESS`, 1 hora para `RESET`.

Não existe denylist de access token: ele é curto e stateless; a revogação ocorre pelo
refresh.

### 5.2 Comércio

**`products`** — `id`, `sku` (unique), `name`, `description`, `price_cents`, `currency`,
`active`, `created_at`, `updated_at`

**`entitlements`** — `id`, `user_id`, `product_id`, `source` (`PAYMENT`, `MANUAL`,
`MIGRATION`), `source_ref`, `granted_at`, `expires_at` (nulo = vitalício), `revoked_at`,
`granted_by`

Índice único parcial em `(user_id, product_id) WHERE revoked_at IS NULL`. É ele que torna
`grant` idempotente.

**`payments`** — `id`, `provider`, `provider_transaction_id`, `product_id`, `user_id`
(nulo até o aluno ser resolvido), `buyer_email`, `buyer_name`, `amount_cents`, `currency`,
`status` (`PENDING`, `APPROVED`, `REFUNDED`, `CHARGEBACK`, `FAILED`), `approved_at`,
`created_at`, `updated_at`

Unique em `(provider, provider_transaction_id)`.

**`payment_splits`** — `id`, `payment_id`, `recipient`, `amount_cents`, `percentage`

### 5.3 Processamento

**`webhook_events`** — `id`, `provider`, `event_id`, `event_type`, `payload` (`jsonb`),
`signature_valid`, `received_at`, `processed_at`, `status` (`RECEIVED`, `PROCESSING`,
`PROCESSED`, `FAILED`, `IGNORED`, `MANUAL`), `attempts`, `next_attempt_at`, `error_message`

**Unique em `(provider, event_id)`.** Esta constraint é o mecanismo de idempotência do
sistema.

**`email_outbox`** — `id`, `to_address`, `template`, `payload` (`jsonb`), `status`
(`PENDING`, `SENT`, `FAILED`), `attempts`, `next_attempt_at`, `sent_at`, `error_message`,
`dedup_key` (unique), `created_at`

**`audit_logs`** — `id`, `actor_user_id` (nulo para ações do sistema), `action`,
`entity_type`, `entity_id`, `ip`, `user_agent`, `result`, `metadata` (`jsonb`), `created_at`

Ações registradas no Núcleo: `LOGIN`, `LOGIN_FAILED`, `PASSWORD_RESET`, `PAYMENT_APPROVED`,
`PAYMENT_REFUNDED`, `ACCESS_GRANTED`, `ACCESS_REVOKED`, `STUDENT_BLOCKED`,
`STUDENT_UNBLOCKED`, `WEBHOOK_REPROCESSED`, `WEBHOOK_RESOLVED_MANUALLY`, `ADMIN_ACTION`.

### 5.4 Índices

`users(email)` unique · `payments(provider, provider_transaction_id)` unique ·
`webhook_events(provider, event_id)` unique · `webhook_events(status, next_attempt_at)` ·
`email_outbox(status, next_attempt_at)` · `email_outbox(dedup_key)` unique ·
`entitlements(user_id)` · `entitlements(user_id, product_id) WHERE revoked_at IS NULL`
unique · `payments(status, created_at)` · `refresh_tokens(token_hash)` unique ·
`password_reset_tokens(token_hash)` unique · `audit_logs(actor_user_id, created_at)` ·
`audit_logs(entity_type, entity_id)`

### 5.5 Convivência com o esquema legado

O backend conecta com um **role dedicado**, não com `postgres`. Isso importa: o proprietário
da tabela ignora RLS por padrão, mas um role comum não. Se as políticas permanecerem ativas
e nenhuma corresponder ao role novo, o acesso é negado e a aplicação falha de forma confusa.

Portanto, uma migration Flyway **remove explicitamente as políticas RLS e desabilita RLS**
nas tabelas que o backend passa a governar. Depender do bypass implícito de proprietário
seria funcionar por acidente.

As tabelas legadas `questions`, `question_options`, `simulados`, `attempts`,
`attempt_answers` e `site_settings` permanecem intocadas neste sub-projeto e serão tratadas
pelos sub-projetos 2 e 3.

### 5.6 Migração dos usuários existentes

Uma migration popula `users` e `students` a partir de `public.profiles` e `auth.users`,
preservando os UUIDs (D12), e mapeia as roles: `ALUNO → ROLE_STUDENT`,
`PROFESSOR → ROLE_INSTRUCTOR`, `ADMIN` e `SUPER_ADMIN → ROLE_ADMIN`.

O GoTrue do Supabase armazena `encrypted_password` em bcrypt, o mesmo algoritmo suportado
pelo Spring Security. Se a verificação no momento da migration confirmar o formato, os
hashes são copiados e os alunos existentes continuam entrando com a senha atual. Caso
contrário, o fallback é `password_hash` nulo e redefinição obrigatória pelo fluxo de
primeiro acesso — ninguém perde a conta em nenhum dos cenários.

---

## 6. Autenticação e autorização

### 6.1 Tokens

**Access token** — JWT com `sub` (userId), `roles`, `iat`, `exp`, `jti`, assinado em HS256,
validade de 15 minutos. HS256 é suficiente: RS256 só agrega quando um serviço separado
precisa verificar sem conhecer o segredo, o que não ocorre em um monólito modular.

**Refresh token** — valor aleatório opaco de 256 bits, nunca um JWT, persistido apenas como
hash, validade de 30 dias. **Rotaciona a cada uso**, e o registro anterior guarda
`replaced_by`. Se um token já rotacionado reaparecer, isso é tratado como indício de roubo e
toda a cadeia daquele usuário é revogada. Um refresh token em formato JWT não poderia ser
revogado, o que anularia sua função.

### 6.2 Armazenamento no navegador

Access token exclusivamente em memória, nunca em `localStorage`. Refresh token em cookie
`HttpOnly; Secure; SameSite`.

Isso impõe uma restrição de deploy: com frontend em `app.betobanco.com` e API em
`api.betobanco.com`, `SameSite=Lax` funciona e o cookie fica protegido. Com a API em domínio
de terceiro, `SameSite=None` passa a ser necessário, o que reabre a superfície de CSRF e
obriga a um token anti-CSRF no endpoint de refresh. **Recomendação: servir a API sob
subdomínio do mesmo domínio.**

### 6.3 Senhas

`DelegatingPasswordEncoder` com Argon2id como padrão para senhas novas, bcrypt aceito para
as legadas do Supabase, e **re-hash automático no primeiro login bem-sucedido**. Nenhum
aluno existente percebe mudança, e a base migra sozinha conforme cada um acessa.

### 6.4 Autorização

Duas camadas.

No `SecurityFilterChain`: `/api/v1/auth/**`, `/api/v1/products` e `/api/v1/webhooks/**` são
públicos; `/api/v1/admin/**` exige `ROLE_ADMIN`; **todo o restante exige autenticação por
padrão**. A regra é negar; liberar é que precisa ser explícito.

Nos métodos: `@PreAuthorize` para o que depende de dado, por exemplo
`@PreAuthorize("@access.hasProduct(#productId)")`.

**Role e entitlement são coisas distintas.** Role diz o que o usuário é; entitlement diz o
que ele comprou. Um aluno adimplente e um inadimplente têm a mesma role — o que os separa é
o entitlement. Confundir os dois é o mecanismo mais comum de vazamento de acesso.

### 6.5 Identidade nunca vem do cliente

Existe um resolver `@CurrentUser` que extrai o `userId` **do token**, e um teste ArchUnit
que proíbe qualquer controller de aceitar `userId` por path, query ou body.

### 6.6 Proteções adicionais

Rate limiting com Bucket4j em `/auth/login`, `/auth/forgot-password` e
`/auth/reset-password`, por IP e por e-mail. Limitação conhecida: o contador é por instância;
uma implantação replicada exigirá Redis.

`forgot-password` responde sempre de forma idêntica, exista o e-mail ou não — caso
contrário o endpoint funciona como enumerador de clientes.

Nenhuma resposta da API contém `password_hash`, hash de token, payload interno de webhook
para não-admins, nem qualquer campo administrativo desnecessário.

---

## 7. Fluxo pagamento → acesso

Três estágios, cada um com uma responsabilidade e um modo de falha próprios.

### 7.1 Estágio 1 — recepção

`POST /api/v1/webhooks/payment/{provider}`

1. O endpoint recebe o corpo **em bytes crus**, não desserializado. A assinatura HMAC é
   calculada sobre os bytes exatos enviados pelo provedor; qualquer round-trip por Jackson
   reordena campos ou altera espaçamento e quebra a verificação de um modo que se parece com
   erro de chave.
2. `PaymentGateway.verifySignature(rawBody, headers)`. Assinatura inválida → `401`, log
   estruturado e métrica, **sem persistir o payload**. Persistir corpos não autenticados
   transformaria a tabela em alvo de enchimento por qualquer um que descobrisse a URL.
3. O gateway extrai o `event_id`. `INSERT` em `webhook_events` com status `RECEIVED`.
4. **Violação da unique constraint `(provider, event_id)` significa evento duplicado** →
   responde `200` sem qualquer efeito. É o banco que decide, não a aplicação: entre um
   "consulta se existe" e um "insere" cabe outra requisição concorrente, e é exatamente
   assim que se cria aluno duplicado sob retry do gateway.
5. Responde `200` em milissegundos. Nenhuma regra de negócio executa aqui.

Se o banco estiver indisponível, o endpoint responde **`5xx` deliberadamente**, para que o
gateway reenvie. Responder `200` sem ter persistido nada perde a venda silenciosamente.

### 7.2 Estágio 2 — processamento

Worker `@Scheduled` que busca eventos pendentes com `SELECT ... FOR UPDATE SKIP LOCKED`,
garantindo que instâncias múltiplas não disputem nem dupliquem o mesmo evento.

O gateway traduz o payload para um `PaymentNotification` canônico. **Daqui para dentro,
nenhuma parte do domínio conhece o formato da InfinitePay.**

Em uma única transação, sem nenhuma chamada externa:

1. Insere ou atualiza `payments`, com os `payment_splits` correspondentes
2. Resolve o produto pelo SKU. **SKU desconhecido não é inferido** — o evento vai para
   `FAILED` com mensagem clara e entra na fila do administrador. Adivinhar qual produto
   liberar é pior do que não liberar
3. Localiza o usuário pelo e-mail normalizado, ou cria `users` + `students`
4. `EntitlementService.grant(...)`, idempotente pelo índice único parcial
5. Grava `audit_logs`: `PAYMENT_APPROVED` e `ACCESS_GRANTED`
6. Enfileira o e-mail em `email_outbox` com `dedup_key`
7. Marca o evento como `PROCESSED`

Falha em qualquer passo reverte a transação inteira. O evento passa a `FAILED`, com
`attempts` incrementado, a mensagem de erro registrada e `next_attempt_at` em backoff
exponencial — 1min, 5min, 15min, 1h, 6h. Esgotadas as tentativas, o status passa a `MANUAL`
e o administrador é notificado.

Esse comportamento é a implementação do fluxo de exceção 2a do CSU-01, que hoje não existe:
a edge function atual apenas retorna `400` e o evento se perde.

**Eventos de estorno e chargeback** (D6) revogam o entitlement preenchendo `revoked_at`,
gravam `ACCESS_REVOKED` na auditoria e notificam o administrador.

### 7.3 Estágio 3 — entrega do e-mail

Worker separado lê a outbox, envia via `EmailService`, marca `SENT`, com retry em backoff.

Fica fora da transação porque e-mail enviado não tem rollback: se estivesse dentro, uma
falha posterior desfaria a criação do aluno mas não desfaria a mensagem já entregue.

A garantia é **at-least-once**. Se o processo morrer entre o envio e a marcação, o aluno
recebe a mensagem duas vezes. É o trade-off correto — nunca receber é muito pior — mas é uma
propriedade declarada do desenho, não um defeito.

Usuário recém-criado recebe e-mail com token de primeiro acesso (72h). Usuário já existente
que comprou um segundo produto recebe e-mail de conteúdo liberado, sem token.

### 7.4 Liberação manual

A tela `/admin/webhooks` lista eventos `FAILED` e `MANUAL` com payload, erro e número de
tentativas, oferecendo duas ações:

- **Reprocessar** — devolve o evento ao status `RECEIVED`
- **Resolver manualmente** — formulário com e-mail e produto, concedendo entitlement com
  `source = MANUAL`, registrado em auditoria com o administrador responsável

Existe também `POST /api/v1/admin/students/{id}/entitlements` e o `DELETE` correspondente,
para conceder e revogar diretamente.

---

## 8. Contrato da API

### 8.1 Formato de resposta

O documento de requisitos define dois formatos incompatíveis para erro (itens 35 e 69).
A resolução mantém o envelope do item 69 como forma externa e acomoda os campos do item 35
dentro de `error`.

Sucesso:

```json
{ "success": true, "data": { } }
```

Lista paginada:

```json
{ "success": true, "data": [],
  "pagination": { "page": 0, "size": 20, "totalElements": 100, "totalPages": 5 } }
```

Erro:

```json
{ "success": false,
  "error": { "code": "PRODUCT_NOT_FOUND", "message": "Produto não encontrado",
             "status": 404, "path": "/api/v1/products/xyz",
             "traceId": "a1b2c3", "timestamp": "2026-08-26T14:03:11Z",
             "fieldErrors": [] } }
```

**`code` é contrato; `message` não é.** O frontend decide comportamento pelo `code`, que é
um enum estável versionado junto com a API. A `message` é texto em português destinado a
humanos e pode mudar a qualquer momento.

### 8.2 Códigos HTTP

`400` corpo malformado ou ilegível · `401` não autenticado ou token inválido · `403`
autenticado sem permissão · `404` recurso inexistente · `409` conflito de estado ·
`422` corpo bem-formado mas semanticamente inválido, incluindo Bean Validation, com
`fieldErrors` preenchido · `429` rate limit excedido · `500` erro interno

Um único `@RestControllerAdvice`. `traceId` vem do MDC e retorna no header `X-Trace-Id`,
propagado quando o cliente o envia. Stack trace nunca é exposto no perfil `prod`.

### 8.3 Paginação

`?page=0&size=20&sort=createdAt,desc`, com **teto rígido de `size`** em 100. Sem o teto,
`size=100000` transforma qualquer listagem em negação de serviço.

### 8.4 Endpoints

Todos sob `/api/v1`.

**Autenticação** — `POST /auth/login` · `POST /auth/refresh` · `POST /auth/logout` ·
`POST /auth/register` · `POST /auth/forgot-password` · `POST /auth/reset-password` ·
`GET /auth/me`

`reset-password` atende primeiro acesso e recuperação, distinguidos pelo `purpose` do token.

`register` cria um `user` **sem nenhum entitlement**. Isso não é inútil: corresponde ao caso
de uso 5.3 dos documentos originais, a captura de lead do visitante que clica no CTA. É
importante que isso esteja explícito, porque é fácil presumir depois que `/auth/register`
libera a plataforma.

**Aluno** — `GET /students/me` · `PUT /students/me` · `GET /students/me/entitlements`

**Catálogo público** — `GET /products` (apenas ativos)

**Webhook** — `POST /webhooks/payment/{provider}`

**Administração** — `GET /admin/students` (paginado, com filtros) ·
`GET /admin/students/{id}` · `PATCH /admin/students/{id}/status` ·
`POST /admin/students/{id}/entitlements` · `DELETE /admin/students/{id}/entitlements/{eid}` ·
`GET /admin/payments` (paginado, com filtros) · `GET /admin/webhooks` ·
`POST /admin/webhooks/{id}/reprocess` · `POST /admin/webhooks/{id}/resolve-manually` ·
`GET /admin/products` · `POST /admin/products` · `PUT /admin/products/{id}` ·
`GET /admin/audit-logs` · `GET /admin/dashboard`

Total: 26 endpoints — 12 públicos ou de aluno, 14 administrativos. Todos documentados em
OpenAPI, agrupados por domínio, com exemplos de
requisição, resposta e erro.

---

## 9. Frontend

### 9.1 Estratégia

Diretório `frontend/` novo, com Vite + React 19 + TypeScript, enquanto `frontend-react/`
permanece intocado e em produção. Os componentes da landing atual (`Hero`,
`BenefitsSection`, `TestimonialsSection`, `ProfessorSection`, `CoursesSection`,
`CTASection`, `Footer`, `Header`, `TrustIndicators`) são portados praticamente como estão —
o que muda neles é a origem dos dados. O corte final é uma linha no `vercel.json`, e o
rollback é a mesma linha de volta.

### 9.2 Estado

`@tanstack/react-query` para tudo que vem da API: cache, revalidação, loading e erro deixam
de ser reimplementados a cada tela. Um store mínimo apenas para a sessão. Respostas da API
não vão para store global — é essa prática que produz dados obsoletos em tela e o "fetch em
loop" que o requisito 54 proíbe.

### 9.3 Cliente HTTP

Access token em memória, em um módulo. Um interceptor trata `401` renovando o token via
cookie e refazendo a requisição.

**Detalhe crítico:** se várias requisições falharem com `401` simultaneamente, uma
implementação ingênua dispara vários refreshes concorrentes. Como o refresh token rotaciona
(6.1), todos menos o primeiro chegam com token já substituído, o sistema interpreta como
roubo e desloga o usuário. Portanto o refresh é uma **promise única compartilhada**, e as
demais requisições aguardam nela.

### 9.4 Guardas de rota

`<RequireAuth>` e `<RequireRole role="ADMIN">` substituem o `PrivateRoute` atual.

O guard de frontend evita que o usuário veja uma tela quebrada. **Quem impede o acesso é o
backend.** Se o guard for removido por engano, `/admin` responde `403` e o pior resultado é
uma tela vazia — não um vazamento.

### 9.5 Telas

Landing pública · `/login` · `/esqueci-senha` · `/definir-senha/:token` (primeiro acesso e
recuperação) · `/dashboard` mínimo, exibindo os produtos liberados · `/perfil` ·
`/admin/dashboard` · `/admin/alunos` · `/admin/alunos/:id` · `/admin/pagamentos` ·
`/admin/webhooks` · `/admin/produtos` · `/admin/auditoria`

### 9.6 Design system

Apenas os componentes que o Núcleo consome: tokens de cor, tipografia e espaçamento
extraídos da identidade atual, mais `Button`, `Input`, `Card`, `Table`, `Modal`, `Badge`,
`Skeleton`, `EmptyState`, `ErrorState`, `Toast`. Construir a biblioteca completa agora
produziria componentes sem consumidor real, e componente sem uso nasce errado.

### 9.7 Estados de interface

Loading, skeleton, vazio, erro, sucesso, não autorizado e não encontrado não ficam a cargo
da disciplina de cada tela: um wrapper deriva esses estados a partir do estado da query.
"Esqueci o empty state" deixa de ser possível.

### 9.8 Responsividade e acessibilidade

Mobile-first, funcional de 320px a monitores grandes. No mobile, sidebar vira drawer e
tabelas do admin viram cards. Navegação por teclado, foco visível, labels associados,
`aria-live` para erros de formulário e contraste conforme WCAG 2.1 AA.

---

## 10. Testes

Testcontainers configurado com **a mesma versão do PostgreSQL da instância Supabase**.
Índice único parcial, `FOR UPDATE SKIP LOCKED` e `jsonb` comportam-se de forma diferente
entre versões; testar contra outra versão produz confiança falsa. As migrations Flyway
executam no container a cada suíte, de modo que migration quebrada falha no CI.

### 10.1 Testes unitários

`JwtService` · delegação e upgrade de `PasswordEncoder` · `EntitlementService` (idempotência
de `grant`) · cálculo de backoff · parsing de payload do gateway · verificação de assinatura

### 10.2 Testes de fluxo

Dos doze cenários do documento de requisitos, três pertencem a este sub-projeto:

- **T01** — pagamento aprovado → webhook → aluno criado → acesso liberado → e-mail enviado
- **T02** — webhook duplicado não duplica aluno
- **T03** — login devolve JWT válido

Sete cenários adicionais, com o motivo de cada um:

- **Webhook duplicado concorrente** — duas threads, mesmo `event_id`, simultâneas. T02 envia
  o duplicado em sequência, e um `if (existe) return` ingênuo **passa** nesse teste e ainda
  assim cria dois alunos sob retry real. Só a versão concorrente prova que a constraint está
  sustentando a idempotência
- **Assinatura inválida** → `401` e `webhook_events` permanece vazia
- **SKU desconhecido** → evento em `FAILED`, aluno não criado, visível na fila do admin
- **Estorno** → entitlement revogado e auditoria gravada
- **Refresh token reutilizado** → cadeia inteira do usuário revogada
- **Aluno acessando `/api/v1/admin/**`** → `403`. É a regressão da falha existente hoje;
  sem teste, ela retorna
- **Senha legada em bcrypt** → login bem-sucedido e hash promovido a Argon2id no mesmo
  request

### 10.3 Testes de arquitetura

Três regras ArchUnit executadas no build:

1. Nenhum módulo importa `entity` ou `repository` de outro
2. Nenhum controller aceita `userId` por path, query ou body
3. Nenhum controller retorna `@Entity`

São as três regras cuja violação custa mais caro e que passam despercebidas em revisão de
código depois de alguns meses.

### 10.4 Dublês

`FakePaymentGateway` para os testes de fluxo. GreenMail para os testes de e-mail. A
implementação da InfinitePay é testada contra payloads reais capturados.

---

## 11. Observabilidade e operação

Actuator expondo `health`, `readiness`, `liveness` e `prometheus`. Logs em JSON com
`traceId` correlacionando requisição, processamento de webhook e envio de e-mail.

Métricas de negócio, não apenas técnicas:

- webhooks recebidos, processados e falhos, por provedor
- **latência entre pagamento aprovado e acesso liberado** — a métrica que o documento de
  arquitetura define como devendo ser próxima de zero
- itens pendentes na outbox de e-mail
- logins falhos por minuto

Duas condições exigem alerta desde o primeiro dia, porque significam dinheiro recebido sem
acesso entregue:

1. Qualquer evento em status `MANUAL`
2. Item na outbox pendente há mais de 15 minutos

### 11.1 Ambientes e segredos

`application.yml` mais os perfis `dev`, `test` e `prod`. Todos os segredos vêm de variáveis
de ambiente: `JWT_SECRET`, `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`,
`SMTP_HOST`, `SMTP_USER`, `SMTP_PASSWORD`, `INFINITEPAY_API_KEY`,
`INFINITEPAY_WEBHOOK_SECRET`. Um `.env.example` documenta todas sem valores.

`docker-compose.yml` com `backend`, `postgres` e `mailhog` para desenvolvimento. Nenhum
serviço opcional é obrigatório em produção.

CI no GitHub Actions: build, testes, ArchUnit e build da imagem Docker a cada push.

---

## 12. Riscos e pendências

| Risco | Impacto | Mitigação |
|---|---|---|
| **Documentação do webhook da InfinitePay indisponível** | Bloqueia a implementação concreta do gateway | O design não depende dela: a abstração isola o contrato. É necessário obter a documentação ou um payload real antes da fase de pagamentos |
| **Java 21 e Maven não instalados** na máquina de desenvolvimento | Bloqueia a Fase 1 | Instalação prévia; o `docker-compose` reduz o restante da configuração |
| Formato dos hashes de senha do Supabase não confirmado | Alunos existentes podem precisar redefinir senha | Verificação na escrita da migration; fallback definido em 5.6 |
| Rate limiting em memória não sobrevive a réplicas | Limite efetivo multiplica pelo número de instâncias | Documentado; migração para Redis quando houver replicação |
| Entrega de e-mail é at-least-once | Aluno pode receber a mesma mensagem duas vezes | Propriedade aceita e declarada; `dedup_key` reduz a janela |
| `node_modules` versionado no Git (11.379 arquivos) e ausência de `.gitignore` | Repositório pesado, diffs poluídos, risco de conflito | Adicionar `.gitignore` e remover do índice como tarefa da Fase 1 |
| Falhas de segurança da seção 1.2 permanecem ativas | Aluno pode acessar `/admin` e manipular notas | Corrigidas apenas pelo sub-projeto 3; priorização consciente |

---

## 13. Critério de pronto

Uma funcionalidade deste sub-projeto está concluída quando possuir: migration Flyway,
entidade, repositório, serviço com a regra de negócio, DTOs de requisição e resposta,
mapper, controller, autorização, tratamento de erro, testes unitários e de integração,
documentação OpenAPI, integração com o frontend, e os estados de loading, erro e vazio na
interface, funcionando de 320px a desktop.

O sub-projeto está concluído quando os dez cenários da seção 10.2 passarem no CI, os três
testes ArchUnit da seção 10.3 estiverem verdes, e um pagamento real de teste na InfinitePay
resultar em aluno criado, acesso liberado e e-mail recebido, sem intervenção manual.
