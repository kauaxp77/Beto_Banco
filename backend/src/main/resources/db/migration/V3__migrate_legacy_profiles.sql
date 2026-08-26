-- V3: migra os usuarios do Supabase para o esquema proprio.
--
-- Roda em dois ambientes que nao se parecem: em producao, onde auth.users e
-- public.profiles existem; e num banco vazio de teste, onde nao existem. A
-- guarda to_regclass torna o script um no-op no segundo caso, em vez de
-- quebrar toda a suite.
--
-- Nada e apagado. As tabelas legadas seguem intactas: os sub-projetos 2 e 3
-- ainda dependem de questions, attempts e site_settings.

DO $$
DECLARE
    v_migrados INTEGER;
BEGIN
    IF to_regclass('public.profiles') IS NULL OR to_regclass('auth.users') IS NULL THEN
        RAISE NOTICE 'Esquema legado ausente; V3 nao tem o que migrar.';
        RETURN;
    END IF;

    -- 1. Usuarios. O UUID e preservado para manter validas as chaves
    --    estrangeiras legadas (attempts.student_id, questions.created_by).
    --    O prefixo {bcrypt} e obrigatorio: o DelegatingPasswordEncoder
    --    identifica o algoritmo por ele, e sem o prefixo todo login lanca
    --    excecao em vez de simplesmente recusar a senha.
    INSERT INTO users (id, email, password_hash, full_name, status, created_at)
    SELECT p.id,
           lower(trim(au.email)),
           CASE WHEN au.encrypted_password IS NULL OR au.encrypted_password = ''
                THEN NULL
                ELSE '{bcrypt}' || au.encrypted_password
           END,
           COALESCE(NULLIF(trim(p.full_name), ''), split_part(au.email, '@', 1)),
           'ACTIVE',
           COALESCE(p.created_at, now())
    FROM public.profiles p
    JOIN auth.users au ON au.id = p.id
    ON CONFLICT (id) DO NOTHING;

    GET DIAGNOSTICS v_migrados = ROW_COUNT;

    -- 2. Roles. ALUNO -> STUDENT, PROFESSOR -> INSTRUCTOR,
    --    ADMIN e SUPER_ADMIN -> ADMIN.
    INSERT INTO user_roles (user_id, role_id)
    SELECT p.id, r.id
    FROM public.profiles p
    JOIN roles r ON r.name = CASE p.role::text
                                 WHEN 'ALUNO'       THEN 'ROLE_STUDENT'
                                 WHEN 'PROFESSOR'   THEN 'ROLE_INSTRUCTOR'
                                 WHEN 'ADMIN'       THEN 'ROLE_ADMIN'
                                 WHEN 'SUPER_ADMIN' THEN 'ROLE_ADMIN'
                                 ELSE 'ROLE_STUDENT'
                             END
    WHERE EXISTS (SELECT 1 FROM users u WHERE u.id = p.id)
    ON CONFLICT (user_id, role_id) DO NOTHING;

    -- 3. Perfil de aluno so para quem e aluno. Admin e instrutor nao estudam.
    INSERT INTO students (id)
    SELECT p.id
    FROM public.profiles p
    WHERE p.role::text = 'ALUNO'
      AND EXISTS (SELECT 1 FROM users u WHERE u.id = p.id)
    ON CONFLICT (id) DO NOTHING;

    RAISE NOTICE 'V3: % usuario(s) legado(s) migrado(s).', v_migrados;
END $$;
