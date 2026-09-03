-- =============================================================================
-- Secao 07 -- Busca
--
-- "Busca unica sobre concursos, cursos e posts; filtros por carreira, orgao,
--  banca, escolaridade, faixa salarial e status do edital; ordenacao por
--  relevancia ou data de inscricao; tolerancia a acento e erro de digitacao.
--  Implementacao: tsvector + pg_trgm no PostgreSQL -- sem servico externo ate
--  50 mil documentos."
--
-- unaccent() nao e IMMUTABLE por padrao (depende do dicionario carregado), e o
-- PostgreSQL recusa funcao STABLE em coluna gerada e em indice. O wrapper abaixo
-- fixa o dicionario, o que torna a chamada deterministica e indexavel.
-- =============================================================================

CREATE OR REPLACE FUNCTION imutavel_unaccent(text)
RETURNS text
LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT AS
$$ SELECT public.unaccent('public.unaccent'::regdictionary, $1) $$;

CREATE OR REPLACE FUNCTION texto_busca(VARIADIC partes text[])
RETURNS tsvector
LANGUAGE sql IMMUTABLE PARALLEL SAFE AS
$$ SELECT to_tsvector('portuguese', imutavel_unaccent(coalesce(array_to_string(partes, ' '), ''))) $$;

-- -----------------------------------------------------------------------------
-- Colunas geradas: o indice acompanha o dado, sem trigger para manter.
-- -----------------------------------------------------------------------------
ALTER TABLE concurso ADD COLUMN busca tsvector
    GENERATED ALWAYS AS (texto_busca(nome, banca, escolaridade, beneficios)) STORED;
CREATE INDEX ix_concurso_busca ON concurso USING GIN (busca);
CREATE INDEX ix_concurso_trgm ON concurso USING GIN (imutavel_unaccent(nome) gin_trgm_ops);

ALTER TABLE curso ADD COLUMN busca tsvector
    GENERATED ALWAYS AS (texto_busca(titulo, subtitulo, descricao)) STORED;
CREATE INDEX ix_curso_busca ON curso USING GIN (busca);
CREATE INDEX ix_curso_trgm ON curso USING GIN (imutavel_unaccent(titulo) gin_trgm_ops);

ALTER TABLE post ADD COLUMN busca tsvector
    GENERATED ALWAYS AS (texto_busca(titulo, resumo, corpo)) STORED;
CREATE INDEX ix_post_busca ON post USING GIN (busca);
CREATE INDEX ix_post_trgm ON post USING GIN (imutavel_unaccent(titulo) gin_trgm_ops);

ALTER TABLE orgao ADD COLUMN busca tsvector
    GENERATED ALWAYS AS (texto_busca(nome, sigla)) STORED;
CREATE INDEX ix_orgao_busca ON orgao USING GIN (busca);

-- -----------------------------------------------------------------------------
-- Visao unificada. Uma consulta cobre os tres tipos; o filtro por tipo e opcional.
-- -----------------------------------------------------------------------------
CREATE VIEW vw_busca AS
    SELECT c.tenant_id,
           'CONCURSO'::text          AS tipo,
           c.id,
           c.slug,
           c.nome                    AS titulo,
           c.banca                   AS subtitulo,
           c.status::text            AS status,
           c.salario_centavos,
           c.escolaridade,
           c.orgao_id,
           c.inscricao_inicio        AS data_ordenacao,
           c.busca,
           imutavel_unaccent(c.nome) AS titulo_sem_acento
      FROM concurso c
    UNION ALL
    SELECT k.tenant_id,
           'CURSO',
           k.id,
           k.slug,
           k.titulo,
           k.subtitulo,
           CASE WHEN k.publicado_em IS NULL THEN 'RASCUNHO' ELSE 'PUBLICADO' END,
           k.preco_centavos,
           NULL,
           NULL,
           k.publicado_em::date,
           k.busca,
           imutavel_unaccent(k.titulo)
      FROM curso k
    UNION ALL
    SELECT p.tenant_id,
           'POST',
           p.id,
           p.slug,
           p.titulo,
           p.resumo,
           CASE WHEN p.publicado_em IS NULL THEN 'RASCUNHO' ELSE 'PUBLICADO' END,
           NULL,
           NULL,
           NULL,
           p.publicado_em::date,
           p.busca,
           imutavel_unaccent(p.titulo)
      FROM post p;

COMMENT ON VIEW vw_busca IS
  'Secao 07 -- busca unica. Relevancia: ts_rank_cd(busca, query) somado a similarity() do trigrama, que cobre o erro de digitacao que o tsvector sozinho nao pega.';
