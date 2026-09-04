-- ==========================================================================
-- DADOS DE DEMONSTRACAO — ambiente local apenas.
--
-- Nao faz parte das migracoes e nao vai para producao. Serve para a plataforma
-- ter conteudo em todas as telas e rotas: cursos com aulas, alunos com
-- progresso, pagamentos, leads, redacoes e pedidos de checkout.
--
-- Todos os numeros sao ilustrativos. Nenhuma ficha de concurso aqui foi
-- conferida contra edital.
--
-- Idempotente: pode rodar de novo sem duplicar (ON CONFLICT / NOT EXISTS).
-- ==========================================================================

BEGIN;

-- --------------------------------------------------------------------------
-- Pessoas. Todas com a mesma senha do admin (BetoBanco2026!), reaproveitando
-- o hash existente em vez de embutir um hash literal no arquivo.
-- --------------------------------------------------------------------------
INSERT INTO users (email, password_hash, full_name, status)
SELECT v.email, (SELECT password_hash FROM users WHERE email = 'admin@betobanco.local'),
       v.nome, 'ACTIVE'
  FROM (VALUES
        ('prof@betobanco.local',       'Beto Fernandes'),
        ('corretora@betobanco.local',  'Marina Corretora'),
        ('ana.souza@exemplo.com',      'Ana Souza'),
        ('bruno.lima@exemplo.com',     'Bruno Lima'),
        ('carla.dias@exemplo.com',     'Carla Dias'),
        ('diego.rocha@exemplo.com',    'Diego Rocha'),
        ('elisa.martins@exemplo.com',  'Elisa Martins')
       ) AS v(email, nome)
 WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = v.email);

INSERT INTO students (id, phone)
SELECT u.id, v.fone
  FROM (VALUES
        ('ana.souza@exemplo.com',     '(61) 99911-0001'),
        ('bruno.lima@exemplo.com',    '(11) 98822-0002'),
        ('carla.dias@exemplo.com',    '(85) 99733-0003'),
        ('diego.rocha@exemplo.com',   '(51) 99644-0004'),
        ('elisa.martins@exemplo.com', '(21) 98555-0005'),
        ('aluno@betobanco.local',     '(61) 99400-0006')
       ) AS v(email, fone)
  JOIN users u ON u.email = v.email
    ON CONFLICT (id) DO NOTHING;

-- Papeis: professor e corretora. O corretor tem perfil proprio porque corrigir
-- redacao da acesso ao texto de um aluno identificado (secao 14).
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
 WHERE (u.email = 'prof@betobanco.local'      AND r.name = 'ROLE_INSTRUCTOR')
    OR (u.email = 'corretora@betobanco.local' AND r.name = 'ROLE_CORRECTOR')
    ON CONFLICT DO NOTHING;

-- Todo mundo tambem e aluno: e ROLE_STUDENT que faz a pessoa aparecer em
-- /admin/alunos e entrar na contagem do dashboard. Sem ele, o cadastro existe
-- e some da tela.
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
 WHERE r.name = 'ROLE_STUDENT'
   AND u.email IN ('prof@betobanco.local', 'corretora@betobanco.local',
                   'ana.souza@exemplo.com', 'bruno.lima@exemplo.com',
                   'carla.dias@exemplo.com', 'diego.rocha@exemplo.com',
                   'elisa.martins@exemplo.com')
    ON CONFLICT DO NOTHING;

-- --------------------------------------------------------------------------
-- Catalogo
-- --------------------------------------------------------------------------
INSERT INTO products (sku, name, description, price_cents, active) VALUES
('CURSO-CONHECIMENTOS-BANCARIOS', 'Conhecimentos Bancários do Zero',
 'Curso completo de conhecimentos bancários para concursos.', 49700, true),
('CURSO-MATEMATICA-FINANCEIRA',   'Matemática Financeira para Concursos',
 'Juros, descontos e equivalência de capitais com foco em prova.', 39700, true),
('COMBO-BANCARIO-2027',           'Combo Bancário 2027',
 'Todos os cursos da carreira bancária, com atualizações do ano.', 89900, true)
    ON CONFLICT (sku) DO NOTHING;

-- --------------------------------------------------------------------------
-- Cursos, modulos e aulas
-- --------------------------------------------------------------------------
INSERT INTO courses (title, slug, description, published) VALUES
('Conhecimentos Bancários do Zero', 'conhecimentos-bancarios',
 'Do sistema financeiro nacional aos produtos de crédito, na ordem em que as bancas cobram.', true),
('Matemática Financeira para Concursos', 'matematica-financeira',
 'Juros simples e compostos, descontos e equivalência — resolvendo questões desde a primeira aula.', true),
('Redação para Concursos Bancários', 'redacao-bancaria',
 'Estrutura, argumentação e os erros que mais custam pontos na correção.', true)
    ON CONFLICT (slug) DO NOTHING;

INSERT INTO course_modules (course_id, title, position)
SELECT c.id, v.titulo, v.pos
  FROM (VALUES
        ('conhecimentos-bancarios', 'Sistema Financeiro Nacional', 1),
        ('conhecimentos-bancarios', 'Produtos e Serviços Bancários', 2),
        ('conhecimentos-bancarios', 'Compliance e Prevenção à Lavagem', 3),
        ('matematica-financeira',   'Juros Simples', 1),
        ('matematica-financeira',   'Juros Compostos', 2),
        ('redacao-bancaria',        'Estrutura do Texto Dissertativo', 1),
        ('redacao-bancaria',        'Argumentação e Repertório', 2)
       ) AS v(slug, titulo, pos)
  JOIN courses c ON c.slug = v.slug
 WHERE NOT EXISTS (
        SELECT 1 FROM course_modules m WHERE m.course_id = c.id AND m.title = v.titulo);

INSERT INTO lessons (module_id, title, description, video_url, duration_seconds, position, published)
SELECT m.id, v.titulo, v.descricao,
       -- MP4 publico de amostra. NAO e Panda Video de proposito: o parser do
       -- frontend (ui/video.ts) so reconhece YouTube, Vimeo e MP4 direto, e uma
       -- URL do Panda cairia em "aula sem video publicado" — o player ficaria
       -- vazio e o teste nao mostraria nada.
       'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/'
           || v.arquivo_video,
       v.duracao, v.pos, true
  FROM (VALUES
        ('Sistema Financeiro Nacional',      'O que é o SFN e quem manda em quem', 'Órgãos normativos, supervisores e operadores — e por que a banca adora essa divisão.', 'BigBuckBunny.mp4', 1140, 1),
        ('Sistema Financeiro Nacional',      'Conselho Monetário Nacional',        'Composição, competências e as pegadinhas clássicas.',                                   'ElephantsDream.mp4',  980, 2),
        ('Sistema Financeiro Nacional',      'Banco Central: papel e instrumentos','Política monetária na prática, com questões resolvidas.',                                'ForBiggerBlazes.mp4', 1320, 3),
        ('Produtos e Serviços Bancários',    'Depósito à vista e a prazo',         'CDB, RDB e poupança: o que muda de verdade.',                                            'ForBiggerEscapes.mp4', 1050, 1),
        ('Produtos e Serviços Bancários',    'Crédito: modalidades e garantias',   'Do cheque especial ao crédito consignado.',                                              'ForBiggerFun.mp4', 1230, 2),
        ('Compliance e Prevenção à Lavagem', 'As três fases da lavagem',           'Colocação, ocultação e integração, com exemplos reais.',                                 'ForBiggerJoyrides.mp4',  890, 1),
        ('Compliance e Prevenção à Lavagem', 'Comunicação ao COAF',                'Quando comunicar, e o que acontece se não comunicar.',                                   'ForBiggerMeltdowns.mp4',  760, 2),
        ('Juros Simples',                    'Conceito e fórmula',                 'Montante, capital e taxa sem decorar fórmula.',                                          'Sintel.mp4',   820, 1),
        ('Juros Simples',                    'Taxas proporcionais',                'O erro de unidade que derruba metade dos candidatos.',                                   'TearsOfSteel.mp4',   700, 2),
        ('Juros Compostos',                  'Capitalização composta',             'Por que o tempo pesa mais que a taxa.',                                                  'VolkswagenGTIReview.mp4',   960, 1),
        ('Juros Compostos',                  'Taxas equivalentes',                 'Convertendo ao mês, ao ano e ao dia.',                                                   'WeAreGoingOnBullrun.mp4',  1010, 2),
        ('Estrutura do Texto Dissertativo',  'Introdução que não enrola',          'Tese clara no primeiro parágrafo.',                                                      'demo-red-1',  640, 1),
        ('Estrutura do Texto Dissertativo',  'Desenvolvimento em dois eixos',      'Como não repetir a mesma ideia com outras palavras.',                                    'demo-red-2',  720, 2),
        ('Argumentação e Repertório',        'Repertório que a banca aceita',      'Dado, lei e fato histórico — e o que evitar.',                                           'demo-red-3',  810, 1)
       ) AS v(modulo, titulo, descricao, arquivo_video, duracao, pos)
  JOIN course_modules m ON m.title = v.modulo
 WHERE NOT EXISTS (
        SELECT 1 FROM lessons l WHERE l.module_id = m.id AND l.title = v.titulo);

INSERT INTO lesson_materials (lesson_id, title, url, position)
SELECT l.id, v.titulo, v.url, 1
  FROM (VALUES
        ('O que é o SFN e quem manda em quem', 'Mapa mental do SFN (PDF)',  'https://betobanco.example/materiais/mapa-sfn.pdf'),
        ('Conselho Monetário Nacional',        'Resumo do CMN (PDF)',       'https://betobanco.example/materiais/cmn.pdf'),
        ('Conceito e fórmula',                 'Lista de exercícios (PDF)', 'https://betobanco.example/materiais/juros-simples.pdf')
       ) AS v(aula, titulo, url)
  JOIN lessons l ON l.title = v.aula
 WHERE NOT EXISTS (
        SELECT 1 FROM lesson_materials mt WHERE mt.lesson_id = l.id AND mt.title = v.titulo);

-- Quiz da secao 13. O schema e por aula; o simulado avulso ainda nao existe.
INSERT INTO quiz_questions (lesson_id, statement, options, correct_index, explanation, position)
SELECT l.id, v.enunciado, v.opcoes::jsonb, v.correta, v.explicacao, v.pos
  FROM (VALUES
        ('O que é o SFN e quem manda em quem',
         'Qual órgão é normativo no Sistema Financeiro Nacional?',
         '["Banco do Brasil","Conselho Monetário Nacional","Caixa Econômica Federal","B3"]',
         1, 'O CMN é o órgão normativo máximo; o Bacen é supervisor, e os demais são operadores.', 1),
        ('O que é o SFN e quem manda em quem',
         'O Banco Central atua como:',
         '["Órgão normativo","Entidade supervisora","Operador de crédito","Câmara de compensação"]',
         1, 'O Bacen supervisiona e fiscaliza; quem normatiza é o CMN.', 2),
        ('Conceito e fórmula',
         'Capital de R$ 1.000 a 2% ao mês, por 3 meses, em juros simples, rende:',
         '["R$ 20","R$ 60","R$ 61,21","R$ 600"]',
         1, 'J = C x i x t = 1000 x 0,02 x 3 = 60. Em juros simples não há capitalização.', 1)
       ) AS v(aula, enunciado, opcoes, correta, explicacao, pos)
  JOIN lessons l ON l.title = v.aula
 WHERE NOT EXISTS (
        SELECT 1 FROM quiz_questions q WHERE q.lesson_id = l.id AND q.statement = v.enunciado);

-- Que produto libera que curso.
INSERT INTO course_products (course_id, product_id)
SELECT c.id, p.id
  FROM (VALUES
        ('conhecimentos-bancarios', 'CURSO-CONHECIMENTOS-BANCARIOS'),
        ('conhecimentos-bancarios', 'COMBO-BANCARIO-2027'),
        ('conhecimentos-bancarios', 'MENTORIA-BB-2027'),
        ('matematica-financeira',   'CURSO-MATEMATICA-FINANCEIRA'),
        ('matematica-financeira',   'COMBO-BANCARIO-2027'),
        ('matematica-financeira',   'MENTORIA-BB-2027'),
        ('redacao-bancaria',        'COMBO-BANCARIO-2027'),
        ('redacao-bancaria',        'MENTORIA-BB-2027')
       ) AS v(slug, sku)
  JOIN courses c ON c.slug = v.slug
  JOIN products p ON p.sku = v.sku
 WHERE NOT EXISTS (
        SELECT 1 FROM course_products cp WHERE cp.course_id = c.id AND cp.product_id = p.id);

-- --------------------------------------------------------------------------
-- Quem comprou o que
-- --------------------------------------------------------------------------
INSERT INTO entitlements (user_id, product_id, source, source_ref)
SELECT u.id, p.id, 'PAYMENT', 'demo'
  FROM (VALUES
        ('aluno@betobanco.local',     'MENTORIA-BB-2027'),
        ('admin@betobanco.local',     'COMBO-BANCARIO-2027'),
        ('ana.souza@exemplo.com',     'MENTORIA-BB-2027'),
        ('bruno.lima@exemplo.com',    'CURSO-CONHECIMENTOS-BANCARIOS'),
        ('carla.dias@exemplo.com',    'COMBO-BANCARIO-2027'),
        ('diego.rocha@exemplo.com',   'CURSO-MATEMATICA-FINANCEIRA')
       ) AS v(email, sku)
  JOIN users u ON u.email = v.email
  JOIN products p ON p.sku = v.sku
 WHERE NOT EXISTS (
        SELECT 1 FROM entitlements e WHERE e.user_id = u.id AND e.product_id = p.id);

-- --------------------------------------------------------------------------
-- Progresso, "continue assistindo" e favoritos (V3.0 secao 5)
-- --------------------------------------------------------------------------

-- Aulas concluidas: o aluno terminou as duas primeiras do SFN.
INSERT INTO lesson_progress (user_id, lesson_id, completed_at)
SELECT u.id, l.id, now() - (v.dias || ' days')::interval
  FROM (VALUES
        ('aluno@betobanco.local', 'O que é o SFN e quem manda em quem', 4),
        ('aluno@betobanco.local', 'Conselho Monetário Nacional',        2),
        ('ana.souza@exemplo.com', 'O que é o SFN e quem manda em quem', 6)
       ) AS v(email, aula, dias)
  JOIN users u ON u.email = v.email
  JOIN lessons l ON l.title = v.aula
    ON CONFLICT (user_id, lesson_id) DO NOTHING;

-- Comecadas e NAO concluidas: e isso que aparece em "continue assistindo".
INSERT INTO lesson_playback (user_id, lesson_id, position_seconds, updated_at)
SELECT u.id, l.id, v.segundos, now() - (v.horas || ' hours')::interval
  FROM (VALUES
        ('aluno@betobanco.local', 'Banco Central: papel e instrumentos', 640, 3),
        ('aluno@betobanco.local', 'Depósito à vista e a prazo',          210, 26),
        ('aluno@betobanco.local', 'Conceito e fórmula',                  480, 50),
        ('ana.souza@exemplo.com', 'Conselho Monetário Nacional',         300, 8)
       ) AS v(email, aula, segundos, horas)
  JOIN users u ON u.email = v.email
  JOIN lessons l ON l.title = v.aula
    ON CONFLICT (user_id, lesson_id) DO NOTHING;

INSERT INTO lesson_favorites (user_id, lesson_id)
SELECT u.id, l.id
  FROM (VALUES
        ('aluno@betobanco.local', 'Taxas equivalentes'),
        ('aluno@betobanco.local', 'Comunicação ao COAF'),
        ('ana.souza@exemplo.com', 'As três fases da lavagem')
       ) AS v(email, aula)
  JOIN users u ON u.email = v.email
  JOIN lessons l ON l.title = v.aula
    ON CONFLICT (user_id, lesson_id) DO NOTHING;

-- Engajamento: comentario, avaliacao e tentativa de quiz.
INSERT INTO lesson_comments (lesson_id, user_id, body, status)
SELECT l.id, u.id, v.texto, v.situacao
  FROM (VALUES
        ('O que é o SFN e quem manda em quem', 'ana.souza@exemplo.com',
         'Professor, a CVM entra como supervisora também? Fiquei em dúvida na questão 2.', 'VISIBLE'),
        ('Conceito e fórmula', 'bruno.lima@exemplo.com',
         'Aula excelente. A parte de unidade de tempo resolveu um erro que eu repetia há meses.', 'VISIBLE'),
        ('Conselho Monetário Nacional', 'diego.rocha@exemplo.com',
         'Alguém tem o resumo em PDF atualizado de 2026?', 'HIDDEN')
       ) AS v(aula, email, texto, situacao)
  JOIN lessons l ON l.title = v.aula
  JOIN users u ON u.email = v.email
 WHERE NOT EXISTS (
        SELECT 1 FROM lesson_comments c WHERE c.lesson_id = l.id AND c.user_id = u.id);

INSERT INTO lesson_ratings (lesson_id, user_id, helpful)
SELECT l.id, u.id, v.util
  FROM (VALUES
        ('O que é o SFN e quem manda em quem', 'aluno@betobanco.local', true),
        ('Conceito e fórmula',                 'aluno@betobanco.local', true),
        ('Taxas proporcionais',                'bruno.lima@exemplo.com', false)
       ) AS v(aula, email, util)
  JOIN lessons l ON l.title = v.aula
  JOIN users u ON u.email = v.email
 WHERE NOT EXISTS (
        SELECT 1 FROM lesson_ratings r WHERE r.lesson_id = l.id AND r.user_id = u.id);

INSERT INTO quiz_attempts (user_id, lesson_id, correct_count, total_count, answers, created_at)
SELECT u.id, l.id, 1, 2, '[{"questionIndex":0,"selected":1},{"questionIndex":1,"selected":0}]'::jsonb,
       now() - interval '3 days'
  FROM users u, lessons l
 WHERE u.email = 'aluno@betobanco.local'
   AND l.title = 'O que é o SFN e quem manda em quem'
   AND NOT EXISTS (SELECT 1 FROM quiz_attempts q WHERE q.user_id = u.id AND q.lesson_id = l.id);

-- --------------------------------------------------------------------------
-- Avisos, depoimentos e certificado
-- --------------------------------------------------------------------------
INSERT INTO announcements (course_id, title, body, created_by)
SELECT c.id, v.titulo, v.corpo, (SELECT id FROM users WHERE email = 'prof@betobanco.local')
  FROM (VALUES
        ('conhecimentos-bancarios', 'Aulas novas de PLD no ar',
         'Subi as duas aulas de prevenção à lavagem de dinheiro. Assistam antes do simulado de sexta.'),
        ('matematica-financeira',   'Lista de exercícios atualizada',
         'A lista de juros simples ganhou 12 questões novas de bancas de 2026.'),
        (NULL,                      'Edital do Banco do Brasil publicado',
         'Saiu o edital. Ficha completa na plataforma, com salário e prazo de inscrição conferidos na fonte oficial.')
       ) AS v(slug, titulo, corpo)
  LEFT JOIN courses c ON c.slug = v.slug
 WHERE NOT EXISTS (SELECT 1 FROM announcements a WHERE a.title = v.titulo);

INSERT INTO testimonials (user_id, course_id, body, status, created_at)
SELECT u.id, c.id, v.texto, v.situacao, now() - (v.dias || ' days')::interval
  FROM (VALUES
        ('ana.souza@exemplo.com',   'conhecimentos-bancarios',
         'Passei na primeira fase do BB estudando só por aqui. O mapa do SFN vale o curso inteiro.', 'APPROVED', 20),
        ('bruno.lima@exemplo.com',  'matematica-financeira',
         'Eu travava em taxas equivalentes. Duas aulas e destravou.', 'APPROVED', 12),
        ('carla.dias@exemplo.com',  'conhecimentos-bancarios',
         'Conteúdo direto ao ponto, sem enrolação. Recomendo.', 'PENDING', 2)
       ) AS v(email, slug, texto, situacao, dias)
  JOIN users u ON u.email = v.email
  JOIN courses c ON c.slug = v.slug
 WHERE NOT EXISTS (SELECT 1 FROM testimonials t WHERE t.user_id = u.id AND t.course_id = c.id);

INSERT INTO certificates (user_id, course_id, code, hours, issued_at)
SELECT u.id, c.id, 'BB-DEMO-2026-0001', 40, now() - interval '15 days'
  FROM users u, courses c
 WHERE u.email = 'ana.souza@exemplo.com' AND c.slug = 'conhecimentos-bancarios'
   AND NOT EXISTS (SELECT 1 FROM certificates ce WHERE ce.code = 'BB-DEMO-2026-0001');

-- --------------------------------------------------------------------------
-- Financeiro: pagamentos espalhados no tempo, com status variados
-- --------------------------------------------------------------------------
INSERT INTO payments (provider, provider_transaction_id, product_id, user_id, buyer_email,
                      buyer_name, amount_cents, status, approved_at, created_at)
SELECT 'infinitypay', v.tx, p.id, u.id, v.email, v.nome, p.price_cents, v.situacao,
       CASE WHEN v.situacao = 'APPROVED' THEN now() - (v.dias || ' days')::interval END,
       now() - (v.dias || ' days')::interval
  FROM (VALUES
        ('tx-demo-001', 'MENTORIA-BB-2027',              'ana.souza@exemplo.com',    'Ana Souza',      'APPROVED',   62),
        ('tx-demo-002', 'CURSO-CONHECIMENTOS-BANCARIOS', 'bruno.lima@exemplo.com',   'Bruno Lima',     'APPROVED',   48),
        ('tx-demo-003', 'COMBO-BANCARIO-2027',           'carla.dias@exemplo.com',   'Carla Dias',     'APPROVED',   33),
        ('tx-demo-004', 'CURSO-MATEMATICA-FINANCEIRA',   'diego.rocha@exemplo.com',  'Diego Rocha',    'APPROVED',   19),
        ('tx-demo-005', 'MENTORIA-BB-2027',              'aluno@betobanco.local',    'Aluno de Teste', 'APPROVED',    9),
        ('tx-demo-006', 'COMBO-BANCARIO-2027',           'elisa.martins@exemplo.com','Elisa Martins',  'CANCELLED',   6),
        ('tx-demo-007', 'MENTORIA-BB-2027',              'fabio.alves@exemplo.com',  'Fábio Alves',    'CANCELLED',   3),
        ('tx-demo-008', 'CURSO-CONHECIMENTOS-BANCARIOS', 'gisele.nunes@exemplo.com', 'Gisele Nunes',   'PENDING',     1),
        ('tx-demo-009', 'COMBO-BANCARIO-2027',           'ana.souza@exemplo.com',    'Ana Souza',      'REFUNDED',   27)
       ) AS v(tx, sku, email, nome, situacao, dias)
  JOIN products p ON p.sku = v.sku
  LEFT JOIN users u ON u.email = v.email
 WHERE NOT EXISTS (
        SELECT 1 FROM payments pg WHERE pg.provider_transaction_id = v.tx);

-- Historico de webhooks, para a tela de monitoramento do admin.
INSERT INTO webhook_events (provider, event_id, event_type, payload, signature_valid,
                            status, attempts, received_at, processed_at, occurred_at)
SELECT 'infinitypay', v.tx, v.tipo,
       json_build_object('transaction_nsu', v.tx, 'order_nsu', gen_random_uuid(),
                         'invoice_slug', 'fatura-' || v.tx, 'amount', 35640)::jsonb,
       true, v.situacao, 1,
       now() - (v.dias || ' days')::interval,
       CASE WHEN v.situacao = 'PROCESSED' THEN now() - (v.dias || ' days')::interval END,
       now() - (v.dias || ' days')::interval
  FROM (VALUES
        ('tx-demo-001', 'payment.approved',  'PROCESSED', 62),
        ('tx-demo-006', 'payment.cancelled', 'PROCESSED',  6),
        ('tx-demo-008', 'payment.pending',   'PROCESSED',  1),
        ('tx-demo-666', 'payment.approved',  'FAILED',     2)
       ) AS v(tx, tipo, situacao, dias)
 WHERE NOT EXISTS (
        SELECT 1 FROM webhook_events w WHERE w.provider = 'infinitypay' AND w.event_id = v.tx);

-- --------------------------------------------------------------------------
-- Pedidos de checkout (V3.0 secao 8)
-- --------------------------------------------------------------------------
INSERT INTO checkout_orders (product_id, buyer_email, buyer_name, buyer_phone, amount_cents,
                             status, checkout_url, invoice_slug, transaction_nsu, paid_at, created_at)
SELECT p.id, v.email, v.nome, v.fone, p.price_cents, v.situacao,
       'https://checkout.infinitepay.com.br/betobanco?lenc=' || v.slug,
       'fatura-' || v.slug,
       CASE WHEN v.situacao = 'PAID' THEN 'txn-' || v.slug END,
       CASE WHEN v.situacao = 'PAID' THEN now() - (v.dias || ' days')::interval END,
       now() - (v.dias || ' days')::interval
  FROM (VALUES
        ('demo-pedido-1', 'MENTORIA-BB-2027',              'ana.souza@exemplo.com',     'Ana Souza',       '(61) 99911-0001', 'PAID',       62),
        ('demo-pedido-2', 'CURSO-CONHECIMENTOS-BANCARIOS', 'bruno.lima@exemplo.com',    'Bruno Lima',      '(11) 98822-0002', 'PAID',       48),
        ('demo-pedido-3', 'COMBO-BANCARIO-2027',           'helena.prado@exemplo.com',  'Helena Prado',    '(31) 99400-0007', 'CREATED',     1),
        ('demo-pedido-4', 'MENTORIA-BB-2027',              'igor.santana@exemplo.com',  'Igor Santana',    '(71) 99300-0008', 'CANCELLED',   4)
       ) AS v(slug, sku, email, nome, fone, situacao, dias)
  JOIN products p ON p.sku = v.sku
 WHERE NOT EXISTS (
        SELECT 1 FROM checkout_orders o WHERE o.invoice_slug = 'fatura-' || v.slug);

-- --------------------------------------------------------------------------
-- Leads e CRM (V3.0 secao 11)
-- --------------------------------------------------------------------------

-- Materiais de captacao no ar. A V18 os semeia inativos de proposito.
UPDATE lead_magnets
   SET active = true,
       file_url = 'https://betobanco.example/materiais/' || slug || '.pdf'
 WHERE active = false;

INSERT INTO leads (name, email, whatsapp, status, notes, first_seen_at, last_seen_at)
SELECT v.nome, v.email, v.fone, v.situacao, v.nota,
       now() - (v.dias_primeiro || ' days')::interval,
       now() - (v.dias_ultimo || ' days')::interval
  FROM (VALUES
        ('Helena Prado',   'helena.prado@exemplo.com',  '(31) 99400-0007', 'NEGOTIATING', 'Pediu desconto para pagamento à vista. Retornar quinta.', 12, 1),
        ('Igor Santana',   'igor.santana@exemplo.com',  '(71) 99300-0008', 'CONTACTED',   'Cartão recusado duas vezes. Sugerido Pix.',                 9, 4),
        ('Júlia Ferreira', 'julia.ferreira@exemplo.com','(19) 99200-0009', 'NEW',         NULL,                                                        2, 2),
        ('Fábio Alves',    'fabio.alves@exemplo.com',   NULL,              'LOST',        'Optou por outro cursinho.',                                30, 3),
        ('Gisele Nunes',   'gisele.nunes@exemplo.com',  '(41) 99100-0010', 'NEW',         NULL,                                                        1, 1),
        ('Ana Souza',      'ana.souza@exemplo.com',     '(61) 99911-0001', 'WON',         'Comprou a mentoria.',                                      70, 62)
       ) AS v(nome, email, fone, situacao, nota, dias_primeiro, dias_ultimo)
 WHERE NOT EXISTS (SELECT 1 FROM leads l WHERE lower(l.email) = lower(v.email));

-- Historico: cada aparicao vira um evento, e e o motivo que ordena quem ligar
-- primeiro — cartao recusado hoje vale mais que PDF baixado no mes passado.
INSERT INTO lead_events (lead_id, source, magnet_id, occurred_at)
SELECT l.id, 'MATERIAL', m.id, now() - (v.dias || ' days')::interval
  FROM (VALUES
        ('helena.prado@exemplo.com',  'cronograma-90-dias',                   12),
        ('helena.prado@exemplo.com',  'caderno-de-questoes-bb',                5),
        ('julia.ferreira@exemplo.com','guia-carreira-bancaria',                2),
        ('gisele.nunes@exemplo.com',  'mapa-mental-conhecimentos-bancarios',   1),
        ('fabio.alves@exemplo.com',   'guia-carreira-bancaria',               30)
       ) AS v(email, slug, dias)
  JOIN leads l ON lower(l.email) = v.email
  JOIN lead_magnets m ON m.slug = v.slug
 WHERE NOT EXISTS (
        SELECT 1 FROM lead_events e WHERE e.lead_id = l.id AND e.magnet_id = m.id);

INSERT INTO lead_events (lead_id, source, product_id, amount_cents, reason, occurred_at)
SELECT l.id, 'PAGAMENTO_CANCELADO', p.id, p.price_cents, v.motivo,
       now() - (v.dias || ' days')::interval
  FROM (VALUES
        ('igor.santana@exemplo.com', 'MENTORIA-BB-2027',   'Pagamento não concluído no gateway infinitypay', 4),
        ('fabio.alves@exemplo.com',  'MENTORIA-BB-2027',   'Pagamento não concluído no gateway infinitypay', 3)
       ) AS v(email, sku, motivo, dias)
  JOIN leads l ON lower(l.email) = v.email
  JOIN products p ON p.sku = v.sku
 WHERE NOT EXISTS (
        SELECT 1 FROM lead_events e
         WHERE e.lead_id = l.id AND e.source = 'PAGAMENTO_CANCELADO');

-- --------------------------------------------------------------------------
-- Redacoes (secao 14). A cota nasce em zero; aqui ela e concedida.
-- --------------------------------------------------------------------------
INSERT INTO essay_quotas (user_id, period, available, used)
SELECT u.id, date_trunc('month', now())::date, v.disponivel, v.usadas
  FROM (VALUES
        ('aluno@betobanco.local', 4, 1),
        ('ana.souza@exemplo.com', 4, 2),
        ('carla.dias@exemplo.com', 4, 0)
       ) AS v(email, disponivel, usadas)
  JOIN users u ON u.email = v.email
    ON CONFLICT (user_id, period) DO NOTHING;

INSERT INTO essays (user_id, prompt, board, file_url, status, submitted_at, due_at)
SELECT u.id, v.tema, v.banca,
       'https://betobanco.example/redacoes/' || v.arquivo || '.pdf',
       v.situacao, now() - (v.dias || ' days')::interval,
       now() - (v.dias || ' days')::interval + interval '7 days'
  FROM (VALUES
        ('aluno@betobanco.local', 'Os desafios da inclusão financeira no Brasil',
         'CESGRANRIO', 'demo-red-1', 'SUBMITTED', 2),
        ('ana.souza@exemplo.com', 'Educação financeira como política pública',
         'CESGRANRIO', 'demo-red-2', 'CORRECTED', 12),
        ('ana.souza@exemplo.com', 'O papel dos bancos públicos no desenvolvimento regional',
         'FGV', 'demo-red-3', 'IN_REVIEW', 5)
       ) AS v(email, tema, banca, arquivo, situacao, dias)
  JOIN users u ON u.email = v.email
 WHERE NOT EXISTS (SELECT 1 FROM essays e WHERE e.prompt = v.tema);

-- Uma devolutiva publicada. A IA nao publica nota (secao 14): ai_draft e
-- rascunho para o corretor, e as notas sao dele.
INSERT INTO essay_corrections (essay_id, corrector_id, rubric_id, scores, total_score,
                               comment, annotations, ai_draft, assigned_at, completed_at)
SELECT e.id,
       (SELECT id FROM users WHERE email = 'corretora@betobanco.local'),
       r.id,
       '{"Apresentacao":180,"Estrutura":170,"Argumentacao":160,"Norma culta":175}'::jsonb,
       685,
       'Boa tese e repertório pertinente. Perde pontos na conclusão, que só repete a introdução em vez de propor intervenção. Atenção também à regência em "implicar em".',
       '[]'::jsonb,
       'Rascunho gerado por IA e revisado pela corretora antes de publicar.',
       now() - interval '11 days', now() - interval '9 days'
  FROM essays e
  JOIN essay_rubrics r ON r.board = 'CESGRANRIO' AND r.active = true
 WHERE e.prompt = 'Educação financeira como política pública'
   AND NOT EXISTS (SELECT 1 FROM essay_corrections c WHERE c.essay_id = e.id);

-- Uma em correcao, ainda sem nota.
INSERT INTO essay_corrections (essay_id, corrector_id, rubric_id, scores, annotations, assigned_at)
SELECT e.id,
       (SELECT id FROM users WHERE email = 'corretora@betobanco.local'),
       r.id, '{}'::jsonb, '[]'::jsonb, now() - interval '1 day'
  FROM essays e
  JOIN essay_rubrics r ON r.board = 'FGV' AND r.active = true
 WHERE e.prompt = 'O papel dos bancos públicos no desenvolvimento regional'
   AND NOT EXISTS (SELECT 1 FROM essay_corrections c WHERE c.essay_id = e.id);

COMMIT;

-- ==========================================================================
\echo ''
\echo '================= RESUMO DOS DADOS DE DEMONSTRACAO ================='
SELECT 'usuários'         AS entidade, count(*)::text AS total FROM users
UNION ALL SELECT 'cursos publicados', count(*)::text FROM courses WHERE published
UNION ALL SELECT 'aulas',             count(*)::text FROM lessons
UNION ALL SELECT 'produtos',          count(*)::text FROM products
UNION ALL SELECT 'acessos liberados', count(*)::text FROM entitlements
UNION ALL SELECT 'pagamentos',        count(*)::text FROM payments
UNION ALL SELECT 'pedidos checkout',  count(*)::text FROM checkout_orders
UNION ALL SELECT 'leads',             count(*)::text FROM leads
UNION ALL SELECT 'eventos de lead',   count(*)::text FROM lead_events
UNION ALL SELECT 'materiais no ar',   count(*)::text FROM lead_magnets WHERE active
UNION ALL SELECT 'concursos',         count(*)::text FROM contests
UNION ALL SELECT 'redações',          count(*)::text FROM essays
UNION ALL SELECT 'continue assistindo', count(*)::text FROM lesson_playback
UNION ALL SELECT 'favoritos',         count(*)::text FROM lesson_favorites;
