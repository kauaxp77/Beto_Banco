# Mapa do Documento Mestre V4.0 → código

Este arquivo liga cada seção do *Documento Mestre da Plataforma V4.0* ao que
existe no repositório e ao que ainda não existe. Ele serve para duas coisas:
achar rápido onde uma regra vive, e não perder de vista o que falta.

**Branch:** `feat/v4-plataforma`. `main` continua sendo a produção atual e não
foi tocada.

Legenda: **✅ implementado** · **◐ parcial** · **○ não iniciado** · **⛔ bloqueado
por decisão sua**

---

## Parte I — Negócio

| § | Assunto | Estado | Onde |
|---|---|---|---|
| 00 | Governança do documento | ✅ | Este arquivo + `docs/` |
| 01 | Visão e posicionamento | ⛔ | **Pendência 01**: nome de marca, domínio e INPI. Sem isso o checkout não pode ser configurado no gateway. |
| 02 | Mercado e concorrência | — | Documento estratégico, sem contrapartida em código |
| 03 | Modelo de negócio e preços | ◐ | `Cupom` (teto 30% + validade), `Curso.diasAcesso` (365), `Dinheiro`. Tabela de preços definitiva é a **Pendência 02** |
| 04 | Unit economics e metas | ○ | Depende do dashboard administrativo (Fase 2) |

## Parte II — Produto e design

| § | Assunto | Estado | Onde |
|---|---|---|---|
| 05 | Identidade visual (Crepúsculo Dourado) | ✅ | `frontend-react/src/estilos/tokens.css` — inclui as duas correções da V4.0: conflito `#0F172A`/`#111827` resolvido e nenhum azul escuro como cor de texto |
| 06 | Design system | ✅ | `estilos/base.css`, `estilos/componentes.css` — tipografia, escala de 4 px, grid de 12, e **todos** os estados obrigatórios de cada componente |
| 06 | Telas de exceção | ✅ | `pages/Excecoes/TelasDeExcecao.jsx` — as 8 telas + `public/offline.html` |
| 06 | Acessibilidade WCAG 2.1 AA | ◐ | Foco visível, `prefers-reduced-motion`, pular-para-conteúdo, contraste anotado por token. Falta a varredura semanal com axe (§24) |
| 07 | Arquitetura de conteúdo | ✅ | `V1__baseline` — `concurso_carreira` é a tabela de junção que a regra "um concurso em mais de uma carreira" exige |
| 07 | Busca | ✅ | `V2__busca_unificada.sql` + `busca/ControladorBusca.java` — tsvector + pg_trgm, tolerante a acento e erro de digitação |
| 08 | Homepage | ◐ | Landing existente segue no ar, agora com a paleta corrigida. Falta reorganizar as seções na ordem da §08 |
| 09 | Área do aluno | ◐ | `/me/matriculas`, player e progresso prontos. Faltam favoritos, certificado (Fase 3) e a régua de e-mail completa |
| 09 | Mobile / PWA | ✅ | `public/manifest.webmanifest`, `public/sw.js`, `public/offline.html` |
| 09 | Engajamento e retenção | ◐ | `dia_estudo`, `meta_estudo`, `conquista` no schema; o cálculo da sequência ainda não tem tela |
| 10 | Vídeo, materiais e proteção | ◐ | `ServicoPlayer` — matrícula viva, marca d'água com CPF parcial, limite de 2 dispositivos, alerta de 4+ IPs. Falta a assinatura de URL do R2 |
| 11 | Sistema de concursos | ✅ | `ControladorCatalogo` + `vw_concurso_revisao_pendente` — toda ficha sai com `verificado_em` e sinaliza defasagem acima de 60 dias |

## Parte III — Engenharia

| § | Assunto | Estado | Onde |
|---|---|---|---|
| 12 | Pagamentos, acesso e reembolso | ✅ | `pagamento/` — HMAC, idempotência, ordem por `ocorrido_em`, fila com backoff 1/5/30/120, fila morta com alerta, reconciliação às 03h. Os 7 estados em `StatusPedido` |
| 12 | Reembolso (CDC art. 49) | ◐ | Tabela `reembolso` com base legal; falta o botão de autoatendimento no perfil |
| 13 | Simulados e banco de questões | ◐ | Schema completo (peso, anulada, antifraude). ⛔ **Pendência 03**: origem das questões |
| 14 | Redações e correção | ◐ | Schema com rubrica por banca, prazo de 7 dias e cota; falta a fila do corretor |
| 15 | Blog e SEO | ◐ | `post` tem `CHECK` que impede publicar sem revisor humano; falta o pipeline de coleta em fonte oficial |
| 16 | Leads e CRM | ◐ | Tabela `lead` com estágios e consentimento; falta a automação WhatsApp → e-mail → CRM |
| 17 | Inteligência artificial | ◐ | `ia_cache` (cache por hash) e `ia_consumo` (teto por aluno/mês); as chamadas ainda não foram escritas |
| 18 | Modelo de dados | ✅ | 4 migrações Flyway, ~40 tabelas. Centavos em BIGINT, UTC, `tenant_id` desde a Fase 1, exclusão lógica |
| 19 | API | ✅ | Base `/api/v1`, snake_case, RFC 7807, cursor, `Idempotency-Key`, rate limit, OpenAPI em `/api/docs` |
| 20 | Perfis e permissões | ✅ | 6 perfis, access 15 min, refresh 30 d com rotação e invalidação de família, 2FA para Admin/Suporte, log de auditoria |
| 21 | Segurança | ✅ | BCrypt 12, bloqueio após 5 falhas, senhas vazadas, HSTS/CSP/frame-options, consulta parametrizada, segredos por variável de ambiente |

## Parte IV — Operação

| § | Assunto | Estado | Onde |
|---|---|---|---|
| 22 | LGPD e jurídico | ✅ | Documentos versionados, aceite com data/hora/IP/versão, consentimento por finalidade, portal do titular (exportar, revogar, excluir com anonimização) e banner de cookies com recusa em um clique |
| 22 | DPO e plano de incidente | ○ | Decisão organizacional, não de código |
| 23 | Infraestrutura e observabilidade | ◐ | `docker-compose.yml`, CI no GitHub Actions, `trace_id` em toda requisição. Falta plugar o Sentry e o uptime externo |
| 24 | QA e critérios de aceite | ◐ | 46 testes unitários sobre webhook, cupom, sessão e anonimização. Faltam integração (Testcontainers), E2E (Playwright), carga (k6) e axe |
| 25 | Analytics | ✅ | `lib/analytics.js` — os 8 eventos da §25, com fila que só é enviada após o consentimento |
| 26 | Suporte e professores | ○ | Fase 2 |
| 27 | SaaS White Label | ✅ *(fundação)* | `tenant_id` em todas as tabelas de catálogo e usuário + RLS por `app.tenant_id`. A Fase 5 adiciona linhas, não reescreve o backend |

## Parte V — Execução

| § | Assunto | Estado |
|---|---|---|
| 28 | Custos operacionais | — | Planilha, não código |
| 29 | Roadmap | — | Fase 1 é o alvo desta branch |
| 30 | Riscos | ✅ *(os mitigáveis em código)* | Webhook (§12), pirataria (§10), incidente com dado pessoal (§22) |
| 31 | Decisões permanentes | ✅ | Todas as 11 respeitadas — ver abaixo |

---

## As 11 decisões permanentes (§31)

| Decisão | Como está garantida |
|---|---|
| Dark mode permanente | `color-scheme: dark` em `:root`; não existe tema claro no CSS |
| Paleta com as correções da §05 | `estilos/tokens.css` é a fonte única; `index.css` só traduz nome antigo para token novo |
| InfinityPay como gateway | `ClienteInfinityPay` + `ControladorWebhookInfinityPay` |
| Panda Vídeo, sem YouTube | `aula.panda_video_id`; o service worker recusa cachear qualquer host de vídeo |
| React + Vite · Java 21 + Spring Boot · PostgreSQL | `frontend-react/` e `backend/` |
| Área do aluno proprietária | `/me/*` com matrícula viva verificada no servidor |
| SEO e IA integrados | Schema de `post` com fonte e revisor; `ia_cache` com teto de gasto |
| `tenant_id` desde a Fase 1 | Em todas as tabelas de catálogo e usuário, com RLS |
| Revisão humana antes de publicar IA | `CHECK (publicado_em IS NULL OR revisado_por IS NOT NULL)` no banco — não é uma regra que dá para esquecer no código |
| Acesso por prazo, nunca vitalício | `matricula.expira_em` + job diário de expiração |
| WCAG 2.1 AA | Foco visível, contraste anotado por token, `prefers-reduced-motion`, semântica nas telas de exceção |

---

## Pendências que só você pode decidir (§31)

Estas seis bloqueiam trabalho real. As três primeiras travam a Fase 1 inteira.

| # | Decisão | O que trava agora |
|---|---|---|
| 01 | Nome da marca, domínio, INPI | Configuração do gateway, e-mail transacional, manifest do PWA |
| 02 | Tabela de preços definitiva | Seed de cursos e a projeção de receita |
| 03 | Origem do banco de questões | Fase 3 inteira — o schema está pronto, o conteúdo não pode ser copiado de terceiros |
| 04 | Política de garantia de 8 a 30 dias | Texto dos termos de uso e a regra de `< 20% consumido` |
| 05 | Quem desenvolve | Todas as datas do roadmap |
| 09 | Aceitar aluno menor de 18 anos | Campo `data_nascimento` existe; falta a regra de consentimento de responsável |

---

## Como rodar

```bash
docker compose up -d
cd backend && mvn spring-boot:run      # http://localhost:8080/api/docs/ui
cd frontend-react && npm run dev       # http://localhost:5173
```

Variáveis obrigatórias em produção, sem default utilizável (§21 — segredo nunca
no repositório): `JWT_SECRET`, `INFINITYPAY_WEBHOOK_SECRET`, `INFINITYPAY_API_TOKEN`,
`DATABASE_URL`, `R2_*`.
