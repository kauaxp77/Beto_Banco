# Desvios do Documento Mestre V4.0

A §00 do Documento Mestre estabelece que ele tem precedência sobre tickets e
conversas, e que "divergência = erro de implementação". Este arquivo registra os
pontos em que o código **conscientemente não segue** o documento, com o motivo —
para que cada um seja uma decisão rastreável do Product Owner, e não um erro
silencioso.

Branch: `feat/documento-mestre-v4`, a partir de `main` (`27ead81`).

---

## 1. §19 — Formato de erro: envelope próprio, não RFC 7807

**O documento pede:** `Erro no formato RFC 7807: { type, title, status, detail, errors[] }`.

**O código faz:** envelope `{ success, data, error }`, com `ErrorCode` (código
estável), `ErrorPayload` e `FieldErrorItem` (erros por campo).

**Por quê:** a substância do que a §19 pede — identidade de erro estável e
legível por máquina, mais a lista de erros por campo — já existe e é usada de
ponta a ponta. Converter para RFC 7807 quebraria **todas** as chamadas do
frontend TypeScript que já está em produção, sem ganho funcional.

**Custo de reverter:** alto e crescente. Se a decisão for adotar RFC 7807, o
momento é antes de o frontend crescer mais, e o caminho não-destrutivo é
negociação de conteúdo (`Accept: application/problem+json`) em vez de troca.

**Status:** aguardando decisão do PO.

---

## 2. §05 — Paleta: a de produção, não a "Crepúsculo Dourado"

**O documento pede:** paleta Crepúsculo Dourado — fundo `#030712`, dourado
`#D4AF37`, tipografia Spectral + IBM Plex. A §31 registra isso como **decisão
permanente**.

**O código faz:** o design system de `frontend/src/styles/tokens.css`,
documentado como "spec 9.6" — grafite `#1A1C20`, dourado `#C4A15A`, Inter +
Fraunces, com contraste já verificado sobre o fundo escuro.

**Por quê:** são dois documentos com decisões diferentes sobre a mesma coisa, e a
paleta da spec 9.6 é a que está no ar, aplicada em landing, auth, área de membros
e painel administrativo. Repintar um produto publicado é decisão de produto e de
marca, não de engenharia — e a §31 do próprio Documento Mestre diz que decisão
permanente só muda "por nova versão MAJOR, com justificativa registrada".

**O que precisa acontecer:** o PO decide qual das duas prevalece. Se for a do
Documento Mestre, é uma nova versão MAJOR dele *ou* uma revisão da spec 9.6, e o
trabalho é uma repintura completa do frontend — não um ajuste de tokens.

**Status:** aguardando decisão do PO. Nada foi alterado.

---

## 3. §10 — Marca d'água sem CPF

**O documento pede:** "Marca d'água dinâmica com nome e CPF parcial do aluno
sobre o vídeo."

**O código faz:** nome + e-mail mascarado (`a*****a@exemplo.com`).

**Por quê:** a plataforma não coleta CPF em nenhum ponto do schema atual. A §22
lista CPF entre os dados tratados, mas a coleta nunca foi implementada — não há
de onde tirar o dado. Nome + e-mail mascarado é igualmente pessoal e igualmente
rastreável até a conta, que é o efeito dissuasório pretendido.

**Custo de reverter:** uma linha em `PlayerController`, no dia em que a coleta de
CPF existir.

---

## 4. §10 — Link de vídeo expirável não implementado

**O documento pede:** "Player com domínio restrito e link expirável."

**O código faz:** nada — `lessons.video_url` continua sendo uma URL simples,
protegida por verificação de matrícula no servidor (`exigirAcesso`), mas não
expirável.

**Por quê:** link assinado depende da API do host de vídeo. A §31 fixa Panda
Vídeo como decisão permanente, mas ele não está contratado nem configurado.
Escrever a assinatura agora seria código especulativo contra uma API que ninguém
leu.

**Status:** bloqueado por contratação do Panda Vídeo.

---

## 5. §27 — Multi-tenant estrutural, isolamento ainda inerte

**O documento pede:** "tenant_id em todas as tabelas com row-level security."

**O código faz:** `tenant_id` nas raízes de propriedade (`users`, `products`,
`courses`, `payments`, `legal_documents`) e RLS ligado **em modo permissivo** —
a política só restringe quando `app.tenant_id` está definido na sessão, e nenhum
caminho da aplicação define esse parâmetro ainda.

**Por quê:** duas razões distintas.

*Sobre "todas as tabelas":* `lesson_progress` pertence a uma `lesson`, que
pertence a um `course`, que tem dono. Repetir a coluna nas folhas seria
desnormalização que diverge com o tempo — a folha apontando para um tenant e a
raiz para outro, sem nada avisar.

*Sobre o modo permissivo:* ligar isolamento real hoje exigiria que toda consulta
da aplicação declarasse o tenant corrente, e qualquer caminho esquecido
devolveria zero linhas — a plataforma inteira vazia para os alunos que já pagaram.
A parte cara e irreversível (a coluna, a chave estrangeira, o índice único por
tenant) está feita; a Fase 5 passa a definir `app.tenant_id` e o isolamento entra
em vigor sem alterar uma tabela sequer.

**Verificado:** com papel de aplicação comum, sem `app.tenant_id` a consulta vê
tudo (comportamento de hoje); com o tenant definido vê só o dele; e gravar em
tenant alheio é recusado pelo `WITH CHECK`.

---

## 6. §07 — Busca sem posts, por enquanto

**O documento pede:** "busca única sobre concursos, cursos **e posts**".

**O código faz:** busca única sobre concursos e cursos.

**Por quê:** o domínio de blog não existe (§15, abaixo). A `vw_search` é um
`UNION ALL`, e posts entram como mais um ramo quando houver o que indexar — sem
tocar em nada do que já está lá.

---

## Seções não iniciadas, e o tamanho real delas

| Seção | Situação | Ordem de grandeza |
|---|---|---|
| §11 Sistema de concursos | **Feito.** Carreira/órgão/cargo, ficha indexável, junção com carreiras, fila de revisão de 60 dias. Falta a importação automática de edital. | — |
| §07 Busca unificada | **Feito** para concursos e cursos, com `tsvector` + `pg_trgm`. | — |
| §15 Blog e SEO | **Não iniciado.** Nenhum post, autor ou fluxo de revisão humana. Inclui a política de fontes reescrita e os sinais de E-E-A-T. | Comparável ao módulo `courses` |
| §13 Banco de questões | Schema de quiz existe (V10), mas ⛔ travado pela **pendência 03**: a origem das questões não foi decidida, e copiar comentário de terceiro é infração. | Bloqueado por decisão |
| §17 Inteligência artificial | Não iniciado. Cache por hash, teto de gasto por aluno, rótulo de "gerado por IA" e revisão humana obrigatória antes de publicar. | Médio |

---

## Pendências que só o Product Owner decide (§31)

Continuam abertas e travando trabalho real:

| # | Decisão | Trava |
|---|---|---|
| 01 | Nome da marca, domínio, INPI | Configuração do gateway e do e-mail transacional |
| 02 | Tabela de preços definitiva | Seed de produtos e projeção de receita |
| 03 | Origem do banco de questões | §13 — o schema existe, o conteúdo não pode ser copiado |
| 04 | Garantia de 8 a 30 dias | Texto dos termos de uso (hoje um rascunho no banco) |
| 05 | Quem desenvolve | Todas as datas do roadmap |
| 09 | Aceitar aluno menor de 18 | Regra de consentimento de responsável |

---

## Verificação

Os testes de integração (Testcontainers) não rodaram: o engine do Docker Desktop
estava fora do ar na máquina de desenvolvimento. Toda validação de banco foi
feita contra um PostgreSQL 17 local, com as 16 migrações aplicadas em base limpa.
53 testes unitários passam.
