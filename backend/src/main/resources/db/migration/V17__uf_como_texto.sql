-- V17: `agencies.state` deixa de ser CHAR(2) e passa a TEXT com CHECK.
--
-- Dois motivos, e o segundo e o que importa em producao.
--
-- 1. Hibernate mapeia String para varchar. Com CHAR(2) no banco, a validacao de
--    schema recusa subir: "wrong column type encountered in column [state] in
--    table [agencies]; found [bpchar], but expecting [varchar]". O erro aparece
--    no boot, nao no teste.
--
-- 2. CHAR e preenchido com espacos a direita. Uma UF gravada como 'S' viraria
--    'S ' em silencio, e a comparacao com 'S' passaria a depender de onde ela
--    acontece: no Postgres bpchar ignora o espaco final, em Java a String nao.
--    O CHECK abaixo recusa o valor errado em vez de mascara-lo.
--
-- A alteracao nao reescreve dado util: os valores semeados na V15 tem exatamente
-- dois caracteres ou sao NULL.
ALTER TABLE agencies ALTER COLUMN state TYPE TEXT;

ALTER TABLE agencies ADD CONSTRAINT agencies_state_check
    CHECK (state IS NULL OR state ~ '^[A-Z]{2}$');
