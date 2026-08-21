-- ==========================================
-- APROVAÇÃO PASSO A PASSO - HOTFIX V4
-- ==========================================

-- 1. Remove a obrigatoriedade da foreign key se o simulado for "Dinâmico" (Sem ID fixo da tabela simulados)
ALTER TABLE public.attempts ALTER COLUMN simulado_id DROP NOT NULL;

-- 2. Adiciona o nível/tipo da tentativa ("GERAL", "DIFICIL", etc)
ALTER TABLE public.attempts ADD COLUMN IF NOT EXISTS level TEXT DEFAULT 'GERAL';
