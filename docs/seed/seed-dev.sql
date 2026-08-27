-- =========================================================================
-- Seed de desenvolvimento do Beto Banco.
-- Popula o banco LOCAL com dados ficticios em volume (>= 15 por tabela de
-- dominio) para visualizar telas, graficos e fluxos.
--
-- Idempotente: rodar duas vezes nao duplica (ON CONFLICT / verificacoes).
-- NAO rode em producao. Pre-requisitos: migracoes ate V7 aplicadas e o
-- usuario aluno.demo@teste.local criado (fonte do hash de senha).
--
-- Todos os usuarios seed usam a MESMA senha do aluno.demo (DemoTeste123!).
-- Rodar: docker exec -i betobanco-postgres psql -U betobanco -d betobanco \
--            -v ON_ERROR_STOP=1 < docs/seed/seed-dev.sql
-- =========================================================================

BEGIN;

-- ---------- 1. Usuarios: 15 alunos + 1 professor (instrutor) ----------
DO $$
DECLARE
  hash_senha TEXT;
  role_student UUID;
  role_instructor UUID;
  uid UUID;
  i INT;
  nomes TEXT[] := ARRAY[
    'Ana Beatriz Souza','Bruno Carvalho Lima','Camila Ferreira Rocha',
    'Diego Almeida Santos','Elaine Cristina Moraes','Felipe Augusto Nunes',
    'Gabriela Martins Costa','Henrique Barbosa Silva','Isabela Rodrigues Melo',
    'Joao Pedro Cavalcanti','Karina Oliveira Duarte','Lucas Gabriel Teixeira',
    'Mariana Lopes Andrade','Nicolas Ribeiro Farias','Patricia Gomes Azevedo'];
BEGIN
  SELECT password_hash INTO hash_senha FROM users WHERE email = 'aluno.demo@teste.local';
  IF hash_senha IS NULL THEN
    RAISE EXCEPTION 'usuario aluno.demo@teste.local nao existe; crie-o antes do seed';
  END IF;
  SELECT id INTO role_student FROM roles WHERE name = 'ROLE_STUDENT';
  SELECT id INTO role_instructor FROM roles WHERE name = 'ROLE_INSTRUCTOR';

  FOR i IN 1..15 LOOP
    INSERT INTO users (email, password_hash, full_name, created_at)
    VALUES ('aluno' || i || '@demo.local', hash_senha, nomes[i],
            now() - (random() * 60 || ' days')::interval)
    ON CONFLICT (email) DO NOTHING;

    SELECT id INTO uid FROM users WHERE email = 'aluno' || i || '@demo.local';
    INSERT INTO user_roles (user_id, role_id) VALUES (uid, role_student)
    ON CONFLICT DO NOTHING;
    INSERT INTO students (id, phone)
    VALUES (uid, '(11) 9' || lpad(floor(random() * 100000000)::text, 8, '0'))
    ON CONFLICT (id) DO NOTHING;
  END LOOP;

  INSERT INTO users (email, password_hash, full_name)
  VALUES ('professor@demo.local', hash_senha, 'Beto Fernandes')
  ON CONFLICT (email) DO NOTHING;
  SELECT id INTO uid FROM users WHERE email = 'professor@demo.local';
  INSERT INTO user_roles (user_id, role_id) VALUES (uid, role_instructor)
  ON CONFLICT DO NOTHING;
END $$;

-- ---------- 2. Produtos: catalogo com 16 itens ----------
INSERT INTO products (sku, name, description, price_cents, active) VALUES
  ('SKU-BB-2026',      'Mentoria Protocolo BB 2026',            'Preparação completa para escriturário do Banco do Brasil.', 49700, true),
  ('SKU-CAIXA-2026',   'Mentoria Protocolo Caixa 2026',         'Preparação completa para técnico bancário da Caixa.',       49700, true),
  ('SKU-BNB-2026',     'Mentoria Protocolo BNB 2026',           'Preparação para o Banco do Nordeste.',                      44700, true),
  ('SKU-BACEN-2026',   'Mentoria Protocolo Bacen 2026',         'Preparação para o Banco Central.',                          89700, true),
  ('SKU-BNDES-2026',   'Mentoria Protocolo BNDES 2026',         'Preparação para o BNDES.',                                  79700, true),
  ('SKU-BRB-2026',     'Mentoria Protocolo BRB 2026',           'Preparação para o Banco de Brasília.',                      39700, true),
  ('SKU-BANRISUL',     'Mentoria Protocolo Banrisul',           'Preparação para o Banrisul.',                               39700, true),
  ('SKU-BASA-2026',    'Mentoria Protocolo Basa 2026',          'Preparação para o Banco da Amazônia.',                      39700, true),
  ('SKU-BB-TI',        'Mentoria Protocolo BB TI',              'Trilha de TI para o Banco do Brasil.',                      59700, true),
  ('SKU-MAT-FIN',      'Matemática Financeira do Zero',         'Curso intensivo de matemática financeira.',                 19700, true),
  ('SKU-PORT-BANCAS',  'Português para Bancas',                 'Português focado em CESGRANRIO e CEBRASPE.',                19700, true),
  ('SKU-CONH-BANC',    'Conhecimentos Bancários Express',       'Revisão acelerada de conhecimentos bancários.',             14700, true),
  ('SKU-INF-CESG',     'Informática CESGRANRIO',                'Informática cobrada nas provas bancárias.',                 14700, true),
  ('SKU-SIMULADOS-PRO','Bateria de Simulados Pro',              'Simulados inéditos no padrão da banca.',                    9700,  true),
  ('SKU-COMBO-2EM1',   'Combo 2 em 1 — BB e Caixa',             'Os dois principais concursos bancários em um combo.',       79700, true),
  ('SKU-COMBO-3EM1',   'Combo 3 em 1 — BB, Caixa e BNB',        'Preparação tripla com desconto.',                           99700, true)
ON CONFLICT (sku) DO NOTHING;

-- ---------- 3. Cursos: 15 (13 publicados, 2 rascunhos) ----------
INSERT INTO courses (title, slug, description, published) VALUES
  ('Mentoria Protocolo BB 2026',      'seed-bb-2026',      'A preparação definitiva para o Banco do Brasil.',   true),
  ('Mentoria Protocolo Caixa 2026',   'seed-caixa-2026',   'A preparação definitiva para a Caixa.',             true),
  ('Mentoria Protocolo BNB 2026',     'seed-bnb-2026',     'A preparação definitiva para o BNB.',               true),
  ('Mentoria Protocolo Bacen 2026',   'seed-bacen-2026',   'Rumo ao Banco Central.',                            true),
  ('Mentoria Protocolo BNDES 2026',   'seed-bndes-2026',   'Rumo ao BNDES.',                                    true),
  ('Mentoria Protocolo BRB 2026',     'seed-brb-2026',     'Rumo ao Banco de Brasília.',                        true),
  ('Mentoria Protocolo Banrisul',     'seed-banrisul',     'Rumo ao Banrisul.',                                 true),
  ('Mentoria Protocolo Basa 2026',    'seed-basa-2026',    'Rumo ao Banco da Amazônia.',                        true),
  ('Mentoria Protocolo BB TI',        'seed-bb-ti',        'Trilha de tecnologia para o BB.',                   true),
  ('Matemática Financeira do Zero',   'seed-mat-fin',      'Da regra de três ao Sistema Price.',                true),
  ('Português para Bancas',           'seed-port-bancas',  'Interpretação e gramática no padrão das bancas.',   true),
  ('Conhecimentos Bancários Express', 'seed-conh-banc',    'O essencial de conhecimentos bancários.',           true),
  ('Informática CESGRANRIO',          'seed-inf-cesg',     'Informática que cai na prova.',                     true),
  ('Bateria de Simulados Pro',        'seed-simulados',    'Simulados comentados no padrão da banca.',          false),
  ('Atualidades do Mercado Financeiro','seed-atualidades', 'Em produção: atualidades para as provas de 2026.',  false)
ON CONFLICT (slug) DO NOTHING;

-- ---------- 4. Vinculos curso <-> produto (17) ----------
INSERT INTO course_products (course_id, product_id)
SELECT c.id, p.id FROM (VALUES
  ('seed-bb-2026',      'SKU-BB-2026'),
  ('seed-caixa-2026',   'SKU-CAIXA-2026'),
  ('seed-bnb-2026',     'SKU-BNB-2026'),
  ('seed-bacen-2026',   'SKU-BACEN-2026'),
  ('seed-bndes-2026',   'SKU-BNDES-2026'),
  ('seed-brb-2026',     'SKU-BRB-2026'),
  ('seed-banrisul',     'SKU-BANRISUL'),
  ('seed-basa-2026',    'SKU-BASA-2026'),
  ('seed-bb-ti',        'SKU-BB-TI'),
  ('seed-mat-fin',      'SKU-MAT-FIN'),
  ('seed-port-bancas',  'SKU-PORT-BANCAS'),
  ('seed-conh-banc',    'SKU-CONH-BANC'),
  ('seed-inf-cesg',     'SKU-INF-CESG'),
  ('seed-simulados',    'SKU-SIMULADOS-PRO'),
  ('seed-bb-2026',      'SKU-COMBO-2EM1'),
  ('seed-caixa-2026',   'SKU-COMBO-2EM1'),
  ('seed-bb-2026',      'SKU-COMBO-3EM1'),
  ('seed-caixa-2026',   'SKU-COMBO-3EM1'),
  ('seed-bnb-2026',     'SKU-COMBO-3EM1')
) AS v(slug, sku)
JOIN courses c ON c.slug = v.slug
JOIN products p ON p.sku = v.sku
ON CONFLICT (course_id, product_id) DO NOTHING;

-- ---------- 5. Modulos (3 por curso = 45) e aulas (4 por modulo = 180) ----------
DO $$
DECLARE
  curso RECORD;
  modulo_id UUID;
  m INT;
  a INT;
  titulos_mod TEXT[] := ARRAY['Fundamentos', 'Aprofundamento', 'Reta Final'];
  ja_tem BIGINT;
BEGIN
  FOR curso IN SELECT id, title FROM courses WHERE slug LIKE 'seed-%' LOOP
    SELECT count(*) INTO ja_tem FROM course_modules WHERE course_id = curso.id;
    CONTINUE WHEN ja_tem > 0;  -- idempotencia

    FOR m IN 1..3 LOOP
      INSERT INTO course_modules (course_id, title, position)
      VALUES (curso.id, 'Módulo ' || m || ' — ' || titulos_mod[m], m - 1)
      RETURNING id INTO modulo_id;

      FOR a IN 1..4 LOOP
        INSERT INTO lessons (module_id, title, description, video_url,
                             duration_seconds, position, published)
        VALUES (modulo_id,
                'Aula ' || ((m - 1) * 4 + a) || ' — ' || (ARRAY[
                  'Visão geral e edital', 'Teoria essencial',
                  'Resolução de questões', 'Revisão e mapa mental'])[a],
                CASE WHEN a = 1 THEN 'Comece por aqui: panorama do que cai na prova.' END,
                CASE WHEN random() < 0.85
                     THEN 'https://www.youtube.com/watch?v=jNQXAC9IVRw' END,
                (600 + floor(random() * 2400))::int,
                a - 1, true);
      END LOOP;
    END LOOP;
  END LOOP;
END $$;

-- ---------- 6. Materiais complementares (~40% das aulas) ----------
INSERT INTO lesson_materials (lesson_id, title, url, position)
SELECT l.id,
       (ARRAY['Apostila da aula (PDF)', 'Slides da aula (PDF)',
              'Lista de questões (PDF)', 'Resumo esquematizado (PDF)'])[1 + floor(random() * 4)::int],
       'https://materiais.betobanco.dev/' || l.id || '.pdf',
       0
FROM lessons l
JOIN course_modules cm ON cm.id = l.module_id
JOIN courses c ON c.id = cm.course_id AND c.slug LIKE 'seed-%'
WHERE random() < 0.4
  AND NOT EXISTS (SELECT 1 FROM lesson_materials lm WHERE lm.lesson_id = l.id);

-- ---------- 7. Entitlements: cada aluno seed compra 2 a 4 produtos (~45) ----------
INSERT INTO entitlements (user_id, product_id, source, source_ref, granted_at)
SELECT u.id, p.id, 'MANUAL', 'seed-dev',
       now() - (random() * 45 || ' days')::interval
FROM users u
CROSS JOIN products p
WHERE u.email LIKE 'aluno%@demo.local'
  AND random() < 0.20
ON CONFLICT (user_id, product_id) WHERE revoked_at IS NULL DO NOTHING;

-- Garante pelo menos 1 produto por aluno seed.
INSERT INTO entitlements (user_id, product_id, source, source_ref)
SELECT u.id, (SELECT id FROM products WHERE sku = 'SKU-BB-2026'), 'MANUAL', 'seed-dev'
FROM users u
WHERE u.email LIKE 'aluno%@demo.local'
ON CONFLICT (user_id, product_id) WHERE revoked_at IS NULL DO NOTHING;

-- ---------- 8. Progresso: alunos concluem ~35% das aulas acessiveis ----------
INSERT INTO lesson_progress (user_id, lesson_id, completed_at)
SELECT DISTINCT e.user_id, l.id,
       now() - (random() * 30 || ' days')::interval
FROM entitlements e
JOIN course_products cp ON cp.product_id = e.product_id
JOIN course_modules cm ON cm.course_id = cp.course_id
JOIN lessons l ON l.module_id = cm.id
WHERE e.source_ref = 'seed-dev'
  AND e.revoked_at IS NULL
  AND random() < 0.35
ON CONFLICT (user_id, lesson_id) DO NOTHING;

-- ---------- 9. Comentarios (~60) + respostas do professor (~20) ----------
DO $$
DECLARE
  prof UUID;
  c RECORD;
  duvidas TEXT[] := ARRAY[
    'Professor, esse assunto cai muito na CESGRANRIO?',
    'Não entendi a resolução da questão 3, pode detalhar?',
    'Existe material complementar sobre esse tema?',
    'Essa aula vale também para o concurso da Caixa?',
    'Qual a diferença disso para o que caiu na prova de 2024?',
    'Consegui resolver todas as questões depois dessa aula. Obrigado!',
    'A dica do mapa mental salvou minha revisão.',
    'Poderia indicar quantas questões devo fazer por dia?',
    'Esse tema costuma aparecer na prova discursiva?',
    'Tive dificuldade no início, mas a repetição ajudou demais.'];
  respostas TEXT[] := ARRAY[
    'Cai sim! É um dos temas favoritos da banca. Foco total.',
    'Boa pergunta! Refaça o passo a passo dos 12min em diante que fica claro.',
    'Está nos materiais da aula, logo abaixo do vídeo.',
    'Vale sim, o conteúdo é o mesmo — muda só o estilo da banca.',
    'Excelente! Continue nesse ritmo que a aprovação vem.'];
BEGIN
  SELECT id INTO prof FROM users WHERE email = 'professor@demo.local';

  -- so roda uma vez
  IF EXISTS (SELECT 1 FROM lesson_comments lc JOIN users u ON u.id = lc.user_id
             WHERE u.email LIKE 'aluno%@demo.local') THEN
    RETURN;
  END IF;

  FOR c IN
    SELECT e.user_id, l.id AS lesson_id
    FROM entitlements e
    JOIN course_products cp ON cp.product_id = e.product_id
    JOIN course_modules cm ON cm.course_id = cp.course_id
    JOIN lessons l ON l.module_id = cm.id
    WHERE e.source_ref = 'seed-dev' AND e.revoked_at IS NULL
    ORDER BY random()
    LIMIT 60
  LOOP
    INSERT INTO lesson_comments (lesson_id, user_id, body, created_at)
    VALUES (c.lesson_id, c.user_id,
            duvidas[1 + floor(random() * array_length(duvidas, 1))::int],
            now() - (random() * 20 || ' days')::interval);
  END LOOP;

  -- Professor responde 1 em cada 3 comentarios raiz.
  INSERT INTO lesson_comments (lesson_id, user_id, parent_id, body, created_at)
  SELECT lc.lesson_id, prof, lc.id,
         respostas[1 + floor(random() * array_length(respostas, 1))::int],
         lc.created_at + interval '6 hours'
  FROM lesson_comments lc
  WHERE lc.parent_id IS NULL AND random() < 0.34;
END $$;

-- ---------- 10. Avaliacoes das aulas (~200, 85% uteis) ----------
INSERT INTO lesson_ratings (lesson_id, user_id, helpful)
SELECT DISTINCT l.id, e.user_id, random() < 0.85
FROM entitlements e
JOIN course_products cp ON cp.product_id = e.product_id
JOIN course_modules cm ON cm.course_id = cp.course_id
JOIN lessons l ON l.module_id = cm.id
WHERE e.source_ref = 'seed-dev' AND e.revoked_at IS NULL
  AND random() < 0.45
ON CONFLICT (user_id, lesson_id) DO NOTHING;

-- ---------- 11. Pagamentos: 45 nos ultimos 30 dias ----------
DO $$
DECLARE
  i INT;
  comprador RECORD;
  produto RECORD;
  st TEXT;
  criado TIMESTAMPTZ;
BEGIN
  IF EXISTS (SELECT 1 FROM payments WHERE provider_transaction_id LIKE 'seed-tx-%') THEN
    RETURN;
  END IF;

  FOR i IN 1..45 LOOP
    SELECT id, email, full_name INTO comprador FROM users
    WHERE email LIKE 'aluno%@demo.local' ORDER BY random() LIMIT 1;
    SELECT id, price_cents INTO produto FROM products ORDER BY random() LIMIT 1;

    st := (ARRAY['APPROVED','APPROVED','APPROVED','APPROVED','APPROVED','APPROVED',
                 'PENDING','CANCELLED','REFUNDED','APPROVED'])[1 + floor(random() * 10)::int];
    criado := now() - (random() * 30 || ' days')::interval;

    INSERT INTO payments (provider, provider_transaction_id, product_id, user_id,
                          buyer_email, buyer_name, amount_cents, status,
                          approved_at, created_at)
    VALUES ('fake', 'seed-tx-' || i, produto.id,
            CASE WHEN st = 'APPROVED' THEN comprador.id END,
            comprador.email, comprador.full_name, produto.price_cents, st,
            CASE WHEN st = 'APPROVED' THEN criado + interval '5 minutes' END,
            criado);
  END LOOP;
END $$;

-- ---------- 12. Splits: 2 por pagamento aprovado recente (30) ----------
INSERT INTO payment_splits (payment_id, recipient, amount_cents, percentage)
SELECT p.id, r.recipient,
       (p.amount_cents * r.pct / 100)::bigint, r.pct
FROM payments p
CROSS JOIN (VALUES ('PROFESSOR', 70.00), ('PLATAFORMA', 30.00)) AS r(recipient, pct)
WHERE p.provider_transaction_id LIKE 'seed-tx-%'
  AND p.status = 'APPROVED'
  AND NOT EXISTS (SELECT 1 FROM payment_splits ps WHERE ps.payment_id = p.id)
LIMIT 40;

-- ---------- 13. Webhooks: 20 eventos com status variados ----------
INSERT INTO webhook_events (provider, event_id, event_type, payload, status,
                            attempts, received_at, processed_at)
SELECT 'fake', 'seed-evt-' || g, 'payment.approved',
       jsonb_build_object('transactionId', 'seed-tx-' || g, 'source', 'seed'),
       CASE
         WHEN g <= 14 THEN 'PROCESSED'
         WHEN g <= 17 THEN 'IGNORED'
         WHEN g <= 19 THEN 'FAILED'
         ELSE 'MANUAL'
       END,
       CASE WHEN g > 14 THEN 3 ELSE 1 END,
       now() - (random() * 30 || ' days')::interval,
       CASE WHEN g <= 14 THEN now() - (random() * 29 || ' days')::interval END
FROM generate_series(1, 20) g
ON CONFLICT (provider, event_id) DO NOTHING;

-- ---------- 14. Outbox de e-mail: 20 mensagens ----------
INSERT INTO email_outbox (to_address, template, payload, status, attempts,
                          sent_at, dedup_key, created_at)
SELECT 'aluno' || (1 + (g % 15)) || '@demo.local',
       CASE WHEN g % 3 = 0 THEN 'PRIMEIRO_ACESSO' ELSE 'ACESSO_LIBERADO' END,
       jsonb_build_object('nome', 'Aluno Seed ' || g, 'source', 'seed'),
       CASE WHEN g <= 17 THEN 'SENT' ELSE 'PENDING' END,
       1,
       CASE WHEN g <= 17 THEN now() - (random() * 25 || ' days')::interval END,
       'seed-mail-' || g,
       now() - (random() * 30 || ' days')::interval
FROM generate_series(1, 20) g
ON CONFLICT (dedup_key) DO NOTHING;

-- ---------- 15. Auditoria: 30 registros ----------
INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, result,
                        metadata, created_at)
SELECT (SELECT id FROM users WHERE email = 'aluno' || (1 + (g % 15)) || '@demo.local'),
       (ARRAY['PAYMENT_APPROVED','ACCESS_GRANTED','LOGIN_SUCCESS',
              'PAYMENT_REFUNDED','ACCESS_REVOKED'])[1 + (g % 5)],
       'Seed', 'seed-' || g, 'SUCCESS',
       jsonb_build_object('source', 'seed'),
       now() - (random() * 30 || ' days')::interval
FROM generate_series(1, 30) g
WHERE NOT EXISTS (SELECT 1 FROM audit_logs WHERE entity_type = 'Seed');

COMMIT;

-- Resumo do que existe apos o seed.
SELECT 'users' AS tabela, count(*) FROM users
UNION ALL SELECT 'students', count(*) FROM students
UNION ALL SELECT 'user_roles', count(*) FROM user_roles
UNION ALL SELECT 'products', count(*) FROM products
UNION ALL SELECT 'courses', count(*) FROM courses
UNION ALL SELECT 'course_products', count(*) FROM course_products
UNION ALL SELECT 'course_modules', count(*) FROM course_modules
UNION ALL SELECT 'lessons', count(*) FROM lessons
UNION ALL SELECT 'lesson_materials', count(*) FROM lesson_materials
UNION ALL SELECT 'lesson_progress', count(*) FROM lesson_progress
UNION ALL SELECT 'lesson_comments', count(*) FROM lesson_comments
UNION ALL SELECT 'lesson_ratings', count(*) FROM lesson_ratings
UNION ALL SELECT 'entitlements', count(*) FROM entitlements
UNION ALL SELECT 'payments', count(*) FROM payments
UNION ALL SELECT 'payment_splits', count(*) FROM payment_splits
UNION ALL SELECT 'webhook_events', count(*) FROM webhook_events
UNION ALL SELECT 'email_outbox', count(*) FROM email_outbox
UNION ALL SELECT 'audit_logs', count(*) FROM audit_logs
ORDER BY tabela;
