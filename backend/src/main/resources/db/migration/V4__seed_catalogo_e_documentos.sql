-- =============================================================================
-- Seed minimo do tenant raiz.
--
-- Carreiras conforme a secao 07: Bancaria, Educacional e Administrativa entram no
-- MVP (a Fase 2 pede "catalogo das 3 carreiras do MVP"); Tribunais fica para a
-- Fase 3 porque depende do banco de questoes, e Policial para a Fase 4 porque
-- exige conteudo especifico e TAF.
-- =============================================================================

INSERT INTO carreira (tenant_id, nome, slug, descricao, ordem, ativo) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Bancaria',       'bancaria',       'Bancos publicos e de fomento. Ticket alto, editais previsiveis.', 1, TRUE),
    ('00000000-0000-0000-0000-000000000001', 'Educacional',    'educacional',    'Magisterio e areas de apoio educacional.',                        2, TRUE),
    ('00000000-0000-0000-0000-000000000001', 'Administrativa', 'administrativa', 'Cargos administrativos das tres esferas. Maior volume de vagas.',  3, TRUE),
    ('00000000-0000-0000-0000-000000000001', 'Tribunais',      'tribunais',      'Tribunais e Ministerio Publico. Liberado na Fase 3.',              4, FALSE),
    ('00000000-0000-0000-0000-000000000001', 'Policial',       'policial',       'Carreiras policiais, com TAF. Liberado na Fase 4.',                5, FALSE);

INSERT INTO orgao (tenant_id, nome, sigla, esfera, uf, site_url) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Banco do Brasil',                          'BB',      'FEDERAL',   NULL, 'https://www.bb.com.br'),
    ('00000000-0000-0000-0000-000000000001', 'Caixa Economica Federal',                  'CAIXA',   'FEDERAL',   NULL, 'https://www.caixa.gov.br'),
    ('00000000-0000-0000-0000-000000000001', 'Banco Central do Brasil',                  'BACEN',   'FEDERAL',   NULL, 'https://www.bcb.gov.br'),
    ('00000000-0000-0000-0000-000000000001', 'Banco Nacional de Desenvolvimento Economico e Social', 'BNDES', 'FEDERAL', NULL, 'https://www.bndes.gov.br'),
    ('00000000-0000-0000-0000-000000000001', 'Banco do Nordeste do Brasil',              'BNB',     'FEDERAL',   NULL, 'https://www.bnb.gov.br'),
    ('00000000-0000-0000-0000-000000000001', 'Banco da Amazonia',                        'BASA',    'FEDERAL',   NULL, 'https://www.bancoamazonia.com.br'),
    ('00000000-0000-0000-0000-000000000001', 'Banco de Brasilia',                        'BRB',     'DISTRITAL', 'DF', 'https://www.brb.com.br'),
    ('00000000-0000-0000-0000-000000000001', 'Banco do Estado do Rio Grande do Sul',     'BANRISUL','ESTADUAL',  'RS', 'https://www.banrisul.com.br'),
    ('00000000-0000-0000-0000-000000000001', 'Banco do Estado do Para',                  'BANPARA', 'ESTADUAL',  'PA', 'https://www.banpara.b.br'),
    ('00000000-0000-0000-0000-000000000001', 'Banco do Estado do Espirito Santo',        'BANESTES','ESTADUAL',  'ES', 'https://www.banestes.com.br'),
    ('00000000-0000-0000-0000-000000000001', 'Banco do Estado de Sergipe',               'BANESE',  'ESTADUAL',  'SE', 'https://www.banese.com.br'),
    ('00000000-0000-0000-0000-000000000001', 'Banco de Desenvolvimento de Minas Gerais', 'BDMG',    'ESTADUAL',  'MG', 'https://www.bdmg.mg.gov.br'),
    ('00000000-0000-0000-0000-000000000001', 'Banco de Desenvolvimento do Espirito Santo','BANDES', 'ESTADUAL',  'ES', 'https://www.bandes.com.br'),
    ('00000000-0000-0000-0000-000000000001', 'Banco Regional de Desenvolvimento do Extremo Sul', 'BRDE', 'ESTADUAL', 'RS', 'https://www.brde.com.br');

-- -----------------------------------------------------------------------------
-- Secao 14 -- rubricas configuraveis por banca. Nota por criterio, nao so total.
-- Os pesos abaixo espelham o padrao publico de cada banca e sao editaveis no admin.
-- -----------------------------------------------------------------------------
INSERT INTO rubrica (tenant_id, banca, nome, criterios_json) VALUES
    ('00000000-0000-0000-0000-000000000001', 'CEBRASPE', 'Cebraspe -- discursiva padrao', '[
        {"codigo":"AP","titulo":"Apresentacao e estrutura textual","nota_max":10},
        {"codigo":"DC","titulo":"Desenvolvimento do tema e coerencia","nota_max":10},
        {"codigo":"DT","titulo":"Dominio tecnico do conteudo","nota_max":20},
        {"codigo":"NG","titulo":"Nota de gramatica (desconto por erro)","nota_max":0}
     ]'::jsonb),
    ('00000000-0000-0000-0000-000000000001', 'FGV', 'FGV -- discursiva padrao', '[
        {"codigo":"AT","titulo":"Apresentacao e estrutura","nota_max":5},
        {"codigo":"CT","titulo":"Conteudo e argumentacao","nota_max":15},
        {"codigo":"LG","titulo":"Linguagem e norma culta","nota_max":10}
     ]'::jsonb),
    ('00000000-0000-0000-0000-000000000001', 'FCC', 'FCC -- discursiva padrao', '[
        {"codigo":"CO","titulo":"Conteudo","nota_max":15},
        {"codigo":"ES","titulo":"Estrutura","nota_max":5},
        {"codigo":"EX","titulo":"Expressao"    ,"nota_max":10}
     ]'::jsonb),
    ('00000000-0000-0000-0000-000000000001', 'CESGRANRIO', 'Cesgranrio -- discursiva padrao', '[
        {"codigo":"AB","titulo":"Abordagem do tema","nota_max":10},
        {"codigo":"AR","titulo":"Articulacao e coesao","nota_max":10},
        {"codigo":"CG","titulo":"Correcao gramatical","nota_max":10}
     ]'::jsonb);

-- -----------------------------------------------------------------------------
-- Secao 22 -- documentos versionados. O corpo definitivo entra pelo admin; estas
-- linhas existem para que o aceite ja tenha a que apontar desde o primeiro cadastro.
-- -----------------------------------------------------------------------------
INSERT INTO documento_legal (tenant_id, tipo, versao, corpo) VALUES
    ('00000000-0000-0000-0000-000000000001', 'TERMOS_DE_USO', '1.0.0',
     'Rascunho. Precisa de revisao juridica antes da Fase 1 ir ao ar. Deve conter, no minimo: prazo de acesso de 12 meses (nao vitalicio), direito de arrependimento em 7 dias corridos (CDC art. 49), politica de garantia de 8 a 30 dias, limite de 2 dispositivos simultaneos e vedacao a compartilhamento de conta.'),
    ('00000000-0000-0000-0000-000000000001', 'POLITICA_PRIVACIDADE', '1.0.0',
     'Rascunho. Precisa de revisao juridica. Deve listar: dados coletados (nome, e-mail, WhatsApp, CPF, IP, historico de estudo), base legal e retencao de cada um conforme a secao 22, direitos do titular, contato do encarregado (DPO) e a lista de operadores (InfinityPay, Panda Video, provedor de e-mail, provedor de IA).'),
    ('00000000-0000-0000-0000-000000000001', 'POLITICA_COOKIES', '1.0.0',
     'Rascunho. Cookies estritamente necessarios dispensam consentimento. Analytics e marketing so apos aceite explicito, com recusa tao facil quanto o aceite.');
