-- ==========================================
-- APROVAÇÃO PASSO A PASSO 2.0 - SCHEMA
-- ==========================================

-- ENUMS
CREATE TYPE user_role AS ENUM ('ALUNO', 'PROFESSOR', 'ADMIN', 'SUPER_ADMIN');
CREATE TYPE question_difficulty AS ENUM ('FACIL', 'MEDIO', 'DIFICIL', 'MUITO_DIFICIL');
CREATE TYPE question_status AS ENUM ('RASCUNHO', 'REVISAO', 'PUBLICADA', 'ARQUIVADA');
CREATE TYPE simulado_level AS ENUM ('BASICO', 'INTERMEDIARIO', 'AVANCADO', 'DINAMICO');

-- ==========================================
-- 1. PROFILES (Extensão do auth.users do Supabase)
-- ==========================================
CREATE TABLE public.profiles (
  id UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
  full_name TEXT NOT NULL,
  role user_role DEFAULT 'ALUNO'::user_role NOT NULL,
  avatar_url TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- ==========================================
-- 2. SUPABASE ROW LEVEL SECURITY (RLS) - Profiles
-- ==========================================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Perfis visíveis para todos logados"
ON public.profiles FOR SELECT
TO authenticated USING (true);

CREATE POLICY "Usuários editam próprio perfil"
ON public.profiles FOR UPDATE
TO authenticated USING (auth.uid() = id);

-- Trigger para criar Profile ao dar Singup no auth.users
CREATE OR REPLACE FUNCTION public.handle_new_user() 
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.profiles (id, full_name, role)
  VALUES (new.id, new.raw_user_meta_data->>'full_name', 'ALUNO');
  RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE PROCEDURE public.handle_new_user();

-- ==========================================
-- 3. QUESTÕES E OPÇÕES (Quiz Builder)
-- ==========================================
CREATE TABLE public.questions (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  banca TEXT,
  concurso TEXT,
  cargo TEXT,
  ano INTEGER,
  materia TEXT NOT NULL,
  assunto TEXT,
  dificuldade question_difficulty DEFAULT 'MEDIO'::question_difficulty NOT NULL,
  enunciado TEXT NOT NULL,
  explicacao TEXT, -- Comentário do professor após errar
  status question_status DEFAULT 'RASCUNHO'::question_status NOT NULL,
  created_by UUID REFERENCES public.profiles(id),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE TABLE public.question_options (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  question_id UUID REFERENCES public.questions(id) ON DELETE CASCADE,
  text TEXT NOT NULL,
  is_correct BOOLEAN DEFAULT false NOT NULL
);

-- RLS das Questões
ALTER TABLE public.questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.question_options ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Todos podem ler questões publicadas"
ON public.questions FOR SELECT
TO authenticated USING (status = 'PUBLICADA' OR auth.uid() = created_by);

CREATE POLICY "Apenas admin/professor podem inserir/editar questões"
ON public.questions FOR ALL
TO authenticated USING (
  EXISTS (
    SELECT 1 FROM public.profiles WHERE profiles.id = auth.uid() AND (profiles.role = 'ADMIN' OR profiles.role = 'PROFESSOR' OR profiles.role = 'SUPER_ADMIN')
  )
);

CREATE POLICY "Todos podem ler opções"
ON public.question_options FOR SELECT TO authenticated USING (true);

-- ==========================================
-- 4. SIMULADOS (Estruturação)
-- ==========================================
CREATE TABLE public.simulados (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  title TEXT NOT NULL,
  description TEXT,
  level simulado_level DEFAULT 'BASICO'::simulado_level NOT NULL,
  questions_count INTEGER DEFAULT 15,
  created_by UUID REFERENCES public.profiles(id),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- RLS
ALTER TABLE public.simulados ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Simulados são públicos aos autenticados" 
ON public.simulados FOR SELECT TO authenticated USING (true);

-- ==========================================
-- 5. MEUS DESEMPENHOS (Progresso do Aluno)
-- ==========================================
CREATE TABLE public.attempts (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  student_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
  simulado_id UUID REFERENCES public.simulados(id) ON DELETE CASCADE NOT NULL,
  score NUMERIC(5,2) DEFAULT 0 NOT NULL,
  time_spent INTEGER DEFAULT 0 NOT NULL, -- segundos usados
  completed_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE TABLE public.attempt_answers (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  attempt_id UUID REFERENCES public.attempts(id) ON DELETE CASCADE NOT NULL,
  question_id UUID REFERENCES public.questions(id) NOT NULL,
  selected_option_id UUID REFERENCES public.question_options(id),
  is_correct BOOLEAN NOT NULL
);

-- RLS
ALTER TABLE public.attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.attempt_answers ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Alunos só visualizam as próprias tentativas"
ON public.attempts FOR SELECT 
TO authenticated USING (student_id = auth.uid());

CREATE POLICY "Alunos inserem suas tentativas"
ON public.attempts FOR INSERT
TO authenticated WITH CHECK (student_id = auth.uid());

CREATE POLICY "Alunos leem/inserem respostas de suas tentativas"
ON public.attempt_answers FOR ALL
TO authenticated USING (
  EXISTS (
    SELECT 1 FROM public.attempts WHERE attempts.id = attempt_answers.attempt_id AND attempts.student_id = auth.uid()
  )
);
