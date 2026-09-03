-- V16: busca unificada sobre concursos e cursos.
--
-- Documento Mestre V4.0, secao 07: "E o coracao da navegacao e nao existia na
-- V3.0. Requisitos: busca unica sobre concursos, cursos e posts; filtros por
-- carreira, orgao, banca, escolaridade, faixa salarial e status do edital;
-- ordenacao por relevancia ou data de inscricao; tolerancia a acento e erro de
-- digitacao. Implementacao: tsvector + pg_trgm no PostgreSQL -- sem servico
-- externo ate 50 mil documentos."
--
-- Cobre concursos e cursos. Posts ficam de fora porque o dominio de blog ainda
-- nao existe (secao 15); quando existir, entra como mais um ramo do UNION, sem
-- mexer em nada do que esta aqui.
--
-- POR QUE OS DOIS INDICES, E NAO UM
--
-- tsvector entende radical e proximidade: "inscricoes bancarias" acha
-- "inscricao para banco". Mas ele casa por palavra inteira depois do stemming,
-- entao "cesgranio" (erro de digitacao) nao casa com "cesgranrio" -- e erro de
-- digitacao e o caso mais comum de busca que nao devolve nada. O trigrama
-- resolve exatamente esse, e nao resolve o primeiro. Os dois juntos cobrem o
-- que o documento pede; qualquer um sozinho deixa metade de fora.

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- ---------------------------------------------------------------------------
-- unaccent() e STABLE, nao IMMUTABLE, porque depende do dicionario carregado —
-- e o PostgreSQL recusa funcao STABLE em coluna gerada e em indice.
--
-- O SET search_path fixa onde procurar o dicionario, o que torna a chamada
-- deterministica e permite declarar o wrapper IMMUTABLE com honestidade. O
-- schema "extensions" entra na lista porque e onde o Supabase instala extensao
-- por padrao; sem ele, a mesma migracao funcionaria local e falharia em
-- producao.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION imutavel_unaccent(text)
RETURNS text
LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
SET search_path = public, extensions, pg_catalog
AS $$ SELECT unaccent($1) $$;

COMMENT ON FUNCTION imutavel_unaccent(text) IS
    'Secao 07 -- tolerancia a acento. IMMUTABLE por causa do SET search_path, '
    'que fixa o dicionario; sem isso nao poderia ser usada em indice.';

CREATE OR REPLACE FUNCTION texto_de_busca(VARIADIC partes text[])
RETURNS tsvector
LANGUAGE sql IMMUTABLE PARALLEL SAFE
SET search_path = public, extensions, pg_catalog
AS $$
    SELECT to_tsvector('portuguese',
                       imutavel_unaccent(coalesce(array_to_string(partes, ' '), '')))
$$;

-- ---------------------------------------------------------------------------
-- Colunas geradas: o indice acompanha o dado sozinho.
--
-- Coluna gerada em vez de trigger de propósito: trigger e codigo que alguem
-- esquece de replicar em um INSERT feito por script, e o indice passa a mentir
-- silenciosamente. Coluna gerada nao tem como ficar dessincronizada.
-- ---------------------------------------------------------------------------
ALTER TABLE contests ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (texto_de_busca(name, board, education_level, benefits)) STORED;

CREATE INDEX contests_search_idx ON contests USING GIN (search_vector);
CREATE INDEX contests_trgm_idx ON contests USING GIN (imutavel_unaccent(name) gin_trgm_ops);

ALTER TABLE courses ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (texto_de_busca(title, description)) STORED;

CREATE INDEX courses_search_idx ON courses USING GIN (search_vector);
CREATE INDEX courses_trgm_idx ON courses USING GIN (imutavel_unaccent(title) gin_trgm_ops);

-- ---------------------------------------------------------------------------
-- A visao unificada.
--
-- Uma consulta cobre os dois tipos e o filtro por tipo e opcional — que e o que
-- a secao 07 quer dizer com "busca unica". Duas consultas separadas na
-- aplicacao devolveriam dois rankings independentes, e ordenar isso por
-- relevancia no cliente nao funciona: as escalas nao sao comparaveis.
-- ---------------------------------------------------------------------------
CREATE VIEW vw_search AS
    SELECT c.tenant_id,
           'CONTEST'::text            AS kind,
           c.id,
           c.slug,
           c.name                     AS title,
           a.acronym                  AS subtitle,
           c.status,
           c.salary_cents,
           c.education_level,
           c.board,
           c.agency_id,
           c.registration_end         AS sort_date,
           c.published_at IS NOT NULL AS published,
           c.search_vector,
           imutavel_unaccent(c.name)  AS title_unaccented
      FROM contests c
      JOIN agencies a ON a.id = c.agency_id

    UNION ALL

    -- tenant_id real, vindo da V13. Fixar a raiz aqui faria a busca de um
    -- segundo tenant enxergar os cursos do primeiro no dia em que a Fase 5
    -- ligar o isolamento.
    SELECT k.tenant_id,
           'COURSE',
           k.id,
           k.slug,
           k.title,
           NULL,
           CASE WHEN k.published THEN 'PUBLISHED' ELSE 'DRAFT' END,
           NULL,
           NULL,
           NULL,
           NULL,
           k.updated_at::date,
           k.published,
           k.search_vector,
           imutavel_unaccent(k.title)
      FROM courses k;

COMMENT ON VIEW vw_search IS
    'Secao 07 -- busca unica. Relevancia = ts_rank_cd(tsvector) + similarity() '
    'do trigrama: o primeiro entende radical, o segundo perdoa erro de '
    'digitacao. Posts entram como mais um ramo quando a secao 15 existir.';
