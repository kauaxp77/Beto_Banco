<div align="center">

# 🏦 Beto Banco

### Plataforma completa de cursos para concursos bancários

*Área de membros premium no padrão das grandes plataformas do mercado — cursos, simulados,
certificados, comunicação com a turma e gestão financeira, tudo em um só lugar.*

![Java](https://img.shields.io/badge/Java_21-Spring_Boot-6DB33F?logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React_19-TypeScript-61DAFB?logo=react&logoColor=black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_17-Flyway-4169E1?logo=postgresql&logoColor=white)
![PWA](https://img.shields.io/badge/PWA-instalável-1a1c20?logo=pwa)
![Testes](https://img.shields.io/badge/testes-backend_%2B_frontend-46a758)

</div>

---

## ✨ Visão geral

O **Beto Banco** é a plataforma do Prof. Beto Fernandes para preparação de concursos
bancários (BB, Caixa, BNB, Bacen, BNDES e outros). O sistema cobre a jornada inteira:

```
Landing page → Compra (webhook de pagamento) → Acesso automático → Aulas com vídeo,
questões e materiais → Progresso e sequência de estudo → Certificado com validação pública
```

São três ambientes integrados sobre a mesma API:

| Ambiente | Para quem | O que entrega |
|---|---|---|
| 🌐 **Landing page** | Visitantes | Vitrine dos cursos, prova social e funil para o login |
| 🎓 **Área do aluno** | Quem comprou | Aulas, simulados, progresso, certificados, avisos |
| 🛠️ **Painel admin** | Professor/equipe | Conteúdo, alunos, financeiro, relatórios, moderação |

---

## 🎓 Área do aluno

- **Meus cursos** — grade com capa, barra de progresso e "continuar de onde parei"
- **Sala de aula** — player de vídeo (YouTube, Vimeo ou MP4), lista de módulos com aulas
  concluídas ✓, busca dentro do curso (ignora acentos) e atalhos ←/→ no teclado
- **Questões nas aulas** — simulado integrado no estilo das bancas: alternativas A–E,
  correção instantânea no servidor, gabarito comentado, histórico de tentativas e meta de 70%
- **Materiais complementares** — apostilas e PDFs anexados a cada aula
- **Comentários por aula** — dúvidas respondidas pelo professor (selo dourado *Professor*),
  com respostas encadeadas
- **Avaliação de aula** — 👍/👎 com contagem, um voto por aluno
- **Trilhas (combos)** — produtos que liberam 2+ cursos viram trilhas com progresso agregado
- **🔥 Sequência de estudo** — dias seguidos, recorde e dias ativos no mês
- **Certificados** — emitidos automaticamente a 100% do curso, com carga horária real,
  **código de validação pública** (`/certificado/BB-XXXXX-XXXXX`) e versão para impressão/PDF
- **Avisos do professor** — comunicados gerais ou por turma, direto no painel
- **Depoimentos** — o aluno conta sua experiência; aprovado, vira prova social do site
- **📱 PWA** — instalável no celular como aplicativo, com shell offline

## 🛠️ Painel do professor (admin)

- **Dashboard** — alunos, acessos, receita aprovada e **gráficos** (receita/dia,
  vendas/dia, pagamentos por status) desenhados em SVG próprio
- **Cursos** — criador completo: cursos → módulos → aulas, capa, publicar/despublicar,
  vínculo curso↔produto (combos com N cursos)
- **Banco de questões** — editor de questões por aula com alternativas, gabarito e comentário
- **Materiais** — anexos por aula
- **Alunos** — busca, detalhe, bloqueio, concessão e revogação manual de acessos
- **Convites** — acesso de cortesia por e-mail (bolsista, parceiro), com validade opcional;
  cria a conta e envia o link de primeiro acesso automaticamente
- **Relatórios** — engajamento por curso e **por aula** (conclusão, votos, comentários) —
  quedas bruscas mostram onde a turma abandona
- **Anúncios** — aviso no painel do aluno + **e-mail em massa** por turma (via outbox)
- **Comentários** — fila de moderação: responder como professor, ocultar/reexibir
- **Depoimentos** — aprovar/ocultar a prova social
- **Pagamentos** — livro-razão vindo dos webhooks, com filtros por status
- **Webhooks** — fila de eventos com reprocessamento e resolução manual
- **Auditoria** — trilha completa de ações sensíveis

## 🏗️ Plataforma (por baixo do capô)

- **Pagamento → acesso automático**: webhook idempotente (unique no banco decide), fila com
  retry, e-mail transacional via **outbox** (mensagem enviada não tem rollback)
- **Entitlements**: quem comprou o quê — a resposta única para "este aluno pode ver isto?";
  acesso a curso, aula, questão e material sempre validado no servidor
- **Auth**: JWT com refresh token **rotativo** em cookie HttpOnly; detecção de reuso
  derruba todas as sessões; primeiro acesso por link (aluno criado sem senha)
- **Segurança**: rate limiting, RBAC (aluno/admin/instrutor), identidade sempre do token
  (regra ArchUnit impede `userId` vindo do cliente), 404 idêntico para recurso alheio
  e inexistente (não vaza catálogo)
- **Arquitetura modular**: módulos se comunicam apenas pelos pacotes `api/` — regra
  verificada por teste de arquitetura (ArchUnit) a cada build

---

## 🧰 Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21 · Spring Boot (Web, Security, Data JPA, Validation) · Flyway |
| Banco | PostgreSQL 17 (migrações V1–V10) |
| Frontend | React 19 · TypeScript · Vite · TanStack Query · React Router 7 |
| Estilo | CSS próprio com design tokens (tema grafite + dourado) — sem framework |
| E-mail | SMTP via outbox (MailHog no dev) |
| Testes | JUnit + MockMvc + **Testcontainers** · Vitest + Testing Library · ArchUnit |
| Infra dev | Docker Compose (Postgres + MailHog) |

## 🚀 Rodando localmente

**Pré-requisitos:** Java 21, Node 20+, Docker.

```bash
# 1. Infra (Postgres + MailHog)
docker compose up -d

# 2. Backend — http://localhost:8080/api/v1 (Swagger em /swagger-ui.html)
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Frontend — http://localhost:5174
cd frontend
npm install
npm run dev
```

**Dados de demonstração** (opcional — nunca em produção): popula 15+ registros por tabela
(alunos, cursos com aulas e questões, pagamentos, comentários, certificados…):

```bash
docker exec -i betobanco-postgres psql -U betobanco -d betobanco \
  -v ON_ERROR_STOP=1 < docs/seed/seed-dev.sql
```

> Usuários seed: `aluno1@demo.local` … `aluno15@demo.local` (senha `DemoTeste123!`).

## ✅ Testes

```bash
# Backend: endpoints com banco real (Testcontainers) + regras de arquitetura
cd backend && ./mvnw test

# Frontend: componentes e fluxos
cd frontend && npx vitest run && npm run lint && npm run build
```

## 📁 Estrutura

```
Beto_Banco/
├── backend/
│   └── src/main/java/com/betobanco/
│       ├── auth/           # login, refresh rotativo, primeiro acesso
│       ├── users/          # identidade, alunos, papéis
│       ├── catalog/        # produtos à venda
│       ├── entitlements/   # quem tem acesso a quê
│       ├── payments/       # livro-razão financeiro
│       ├── webhooks/       # ingestão + processamento de pagamento
│       ├── courses/        # cursos, aulas, questões, progresso,
│       │                   # comentários, certificados, anúncios…
│       ├── invites/        # convites de cortesia
│       ├── email/          # outbox + envio SMTP
│       ├── audit/          # trilha de auditoria
│       └── dashboard/      # agregados do admin
├── frontend/
│   └── src/
│       ├── pages/          # aluno, admin, landing, auth, certificado
│       ├── ui/             # design system, gráficos SVG, player
│       └── api/            # cliente HTTP com refresh automático
├── docs/
│   ├── projeto/            # especificações
│   └── seed/               # dados de desenvolvimento
└── docker-compose.yml      # Postgres + MailHog
```

---

<div align="center">

**Beto Banco** · *Sua aprovação começa com um passo de cada vez.* 🥇

</div>
