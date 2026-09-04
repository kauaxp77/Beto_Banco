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

## 2. §05 — Paleta: resolvida, e não é nenhuma das duas do documento

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

**Resolvido em 04/09/2026.** O Product Owner definiu uma terceira paleta, que
não é nenhuma das duas: azul-marinho profundo com âmbar — `#000814`, `#001D3D`,
`#003566`, `#FFC300`, `#FFD60A`. Ela está aplicada e é a que vale.

A troca não ficou só nos tokens: havia valores fixos em landing, auth, cursos,
certificado, admin, componentes e gráficos. Duas consequências que a paleta nova
obrigou:

- **Campo de texto** passou a usar `--bb-surface` como preenchimento. Antes usava
  `--bb-surface-2`, que na paleta nova é a **mesma cor da borda** (`#003566`): o
  contorno sumia e o campo virava um bloco sólido.
- **`--bb-text-dim`** virou opacidade do próprio `--bb-text`. O cinza quente
  anterior (`#9a9789`) puxa para cáqui sobre azul-marinho.

`--bb-danger` e `--bb-success` ficaram de fora: vermelho e verde são convenção de
leitura, não identidade. Em âmbar, "erro" e "chamada para ação" teriam a mesma
cor.

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

## 7. Documento Mestre Premium V3.0 — o que ele pede além do V4.0

O V3.0 é a versão anterior deste mesmo documento, não um documento novo. A
maior parte do que ele descreve já está no V4.0 com outro número de seção. O que
segue é só o que **não** aparecia no V4.0 e por isso não tinha sido construído.

### Entregue

| V3.0 | O que entrou |
|---|---|
| §11 Leads + CRM | `lead_magnets`, `leads`, `lead_events`. Captação pública (`POST /leads/capture`), CRM em `/admin/leads` com funil, histórico e atribuição. |
| §8 Recuperação de vendas | Pagamento não concluído vira lead automaticamente, com curso, valor e motivo. |
| §5 Continue assistindo / Histórico / Favoritos | `lesson_playback` e `lesson_favorites`, com `/courses/me/continue`, `/courses/me/history` e `/courses/me/favorites`. |

### §9, Cupons — estava bloqueado, agora não está

**O documento pede:** "Cupons. CRUD." no dashboard administrativo.

**O código faz:** nada.

**Por quê:** o checkout é da InfinityPay, não nosso. O desconto é aplicado lá, e
o webhook nos entrega apenas o `amount_cents` final — sem dizer se houve cupom
nem qual. Um CRUD de cupons aqui geraria códigos que a plataforma não consegue
aplicar nem validar no momento da compra: o admin criaria "BLACKFRIDAY30", o
aluno digitaria no checkout e nada aconteceria.

**Destravado em 04/09/2026, mas ainda não construído.** Com a criação do link de
pagamento agora sob nosso controle (seção 8 abaixo), o desconto passa a ser
aplicável: quem monta o `items` enviado à InfinitePay somos nós, então um cupom
válido simplesmente reduz o `price` que vai no pedido. O que falta é o CRUD e as
regras de validade, uso e acumulação — não há mais impedimento técnico.

### Divergência de contrato — §8, "Recusado" e "Cancelado"

O V3.0 separa os dois estados: recusado cria lead, cancelado encerra acesso. O
`PaymentNotification.Tipo` atual tem só `CANCELADO` — o gateway manda um único
evento de não-conclusão. O lead é criado com motivo `PAGAMENTO_CANCELADO`.

A distinção já existe no banco (`lead_events.source` aceita
`PAGAMENTO_RECUSADO`) e na porta `LeadCapture.Motivo`. Ela passa a valer sozinha
no dia em que o gateway disser qual dos dois foi — e a diferença importa para
quem liga: cartão recusado costuma converter numa segunda tentativa,
cancelamento foi uma decisão e exige outro argumento.

### Reforça uma pendência que já existia — §2 e §18, a paleta

O V3.0 fixa a Crepúsculo Dourado com os valores exatos (`#030712`, `#0F172A`,
`#111827`, `#1E293B`, `#1E3A8A`, `#2563EB`, `#D4AF37`, `#F5C542`) e a repete em
"Decisões Permanentes". São agora **dois** documentos mandando a mesma coisa, e
o que está no ar continua sendo a spec 9.6.

Isso não muda a natureza da decisão, que segue sendo do PO (item 2 acima): a
troca é uma repintura completa do frontend em produção, não um ajuste de tokens.
Nada foi alterado.

### Não iniciado

| V3.0 | Observação |
|---|---|
| §12 Simulados | O schema de quiz é **por aula** (`quiz_questions.lesson_id`), não um simulado avulso. Faltam cronômetro, peso por questão e ranking — a estrutura é construível; o conteúdo esbarra na **pendência 03**. |
| §10 Blog + SEO | Mesmo item do §15 do V4.0. |
| §13 IA | Mesmo item do §17 do V4.0. |
| §14 SaaS White Label — cobrança | A base multi-tenant existe (item 5 acima). Falta a cobrança por mensalidade e por aluno ativo, que depende da **pendência 02**. |
| §9 Professores CRUD | `ROLE_INSTRUCTOR` existe desde a V2; não há tela nem rota de gestão. |
| §6 Vimeo como backup | Só faz sentido depois de o Panda Vídeo estar contratado (item 4 acima). |

---

## 8. §12 e V3.0 §8 — Checkout da InfinitePay: endereços novos e um defeito grave

**O aviso do provedor.** A InfinitePay migrou o Checkout Integrado. Os endereços
antigos param de responder:

| | Antigo | Novo |
|---|---|---|
| Criar link | `POST https://api.infinitepay.io/invoices/public/checkout/links` | `POST https://api.checkout.infinitepay.io/links` |
| Conferir pagamento | `POST .../checkout/payment_check` | `POST https://api.checkout.infinitepay.io/payment_check` |

**Primeira constatação: os endereços antigos não existiam neste código.** Uma
busca por eles no repositório inteiro não retorna nada. A integração era só de
entrada (webhook); nunca houve chamada de saída. Ou seja, o alerta diário da
InfinitePay vem de outro lugar — não deste sistema, que não corria risco de
"parar de funcionar" porque nunca chamou aquelas URLs.

**Segunda constatação, essa séria: o webhook real era descartado em silêncio.**
O corpo que a InfinitePay documenta é este:

```json
{"invoice_slug":"abc123","amount":1000,"paid_amount":1010,"installments":1,
 "capture_method":"credit_card","transaction_nsu":"UUID",
 "order_nsu":"UUID-do-pedido","receipt_url":"...","items":[]}
```

Não tem `event`, não tem `event_id`, não tem e-mail do comprador e não tem SKU.
O parser exigia os dois primeiros e devolvia vazio sem eles. O sintoma em
produção seria o pior possível: **o aluno paga e não recebe acesso.**

Corrigido: a identidade do evento sai do `transaction_nsu` e o tipo é inferido
(a notificação existe porque a fatura foi paga). Também estava errada a leitura
do valor — `amount` já vem em centavos, e o código tratava como reais, o que
multiplicaria toda venda por cem.

**O que foi construído.** Sem e-mail nem SKU no webhook, ele sozinho não diz
quem comprou o quê. Por isso o pedido passa a existir do nosso lado
(`checkout_orders`): `POST /checkout` abre o pedido, manda o id como `order_nsu`
e devolve a URL de pagamento; o webhook volta com esse `order_nsu` e é por ele
que o pagamento reencontra o comprador e o produto.

**A regra de que só recebe o curso quem pagou** está em dois pontos, não em um:

1. O preço vem do catálogo, nunca do corpo da requisição. Aceitá-lo do cliente
   deixaria qualquer pessoa comprar a mentoria de R$ 3.564 por um real.
2. Antes de liberar, o pagamento é **confirmado na API do provedor**
   (`payment_check`), e o valor confirmado é comparado com o que o pedido
   cobrava. Se o provedor não confirma, ou confirma um valor menor, nada é
   liberado e o evento vai para a fila do administrador.

O item 2 existe porque o Checkout Integrado **não documenta assinatura** no
corpo que envia. Sem perguntar ao provedor "este pedido foi mesmo pago?",
bastaria descobrir a URL do webhook e mandar um JSON com um `order_nsu` válido
para receber um curso de graça.

**Desvio consciente:** essa confirmação é uma chamada de rede dentro da transação
de processamento, e o `WebhookProcessor` documenta que não haveria nenhuma. Fica
assim porque a alternativa é liberar acesso sem verificar. Os tempos limite são
curtos (5 s e 10 s) e a transação é uma por evento. Em produção o parâmetro
`betobanco.payments.infinitypay.confirmar-antes-de-liberar` fica `true`; em
desenvolvimento vem `false`, porque não há conta real para consultar.

**Ressalva que precisa de você.** O contrato acima foi escrito a partir da
documentação pública do Checkout Integrado, não de uma chamada real. Antes de
vender de verdade, uma requisição de homologação precisa confirmar os nomes dos
campos — em especial o que a criação de link devolve além de `url`. A base fica
em configuração (`betobanco.payments.infinitypay.base-url`) para que a próxima
migração de endereço seja variável de ambiente, não deploy.

**Falta configurar:** `INFINITYPAY_HANDLE` (a InfiniteTag da conta que recebe) e
`INFINITYPAY_WEBHOOK_URL`. Sem a primeira, o checkout recusa toda compra — e
avisa no boot em vez de falhar na primeira venda.

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

A suíte completa roda: **238 testes, 0 falhas**, incluindo os de integração com
Testcontainers, que até então nunca tinham sido executados por indisponibilidade
do engine do Docker na máquina de desenvolvimento.

A primeira execução com o Docker no ar encontrou três defeitos que a validação
por SQL não pegaria, todos corrigidos:

| Defeito | Por que só apareceu agora |
|---|---|
| `agencies.state CHAR(2)` contra a entidade mapeada como `varchar` | Falha na validação de schema do Hibernate, no boot — a migração aplicava sem erro. Corrigido pela V17, que também troca `CHAR` por `TEXT` com `CHECK`: `CHAR` completa com espaço à direita e faria `'S'` virar `'S '` em silêncio. |
| `GET /contests` respondia 500 sem filtro de banca | O parâmetro nulo ia sem tipo declarado e o Postgres resolvia `upper(?)` para `upper(bytea)`, função que não existe. Quebrava justamente o caso comum, o de não filtrar. |
| `PlayerController` alcançava `RefreshTokenRepository` direto | Violação da regra de fronteira entre módulos. O token de renovação é credencial: quem alcança o repositório dele alcança hash, revogação e rotação. Agora passa pela porta `auth.api.ActiveSessions`, que expõe só o que a tela mostra. |

A regra de fronteira do ArchUnit também passou a cobrir `contests`, `essays` e
`privacy` — módulo fora da lista nasce sem a regra, e a primeira violação dele só
apareceria quando já custasse caro desfazer.

Validação de banco: as 19 migrações aplicadas em PostgreSQL 17 em base limpa.
