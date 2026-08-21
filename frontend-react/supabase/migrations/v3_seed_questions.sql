-- ==========================================
-- APROVAÇÃO PASSO A PASSO - MIGRATION SEEDER
-- ==========================================

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'FACIL', 'De acordo com a Constituição Federal, o Sistema Financeiro Nacional é estruturado de forma a:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Promover o desenvolvimento equilibrado do País', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Garantir lucros irrestritos aos bancos estrangeiros', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Isentar operações de bolsa de qualquer tributação', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Assegurar exclusividade do Estado na emissão de moeda criptográfica', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Subordinar o CMN à CVM em questões de câmbio', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'FACIL', 'Assinale a alternativa que indica o órgão ou entidade considerado o órgão máximo do Sistema Financeiro Nacional.', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Banco Central do Brasil (Bacen)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Comissão de Valores Mobiliários (CVM)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Conselho Monetário Nacional (CMN)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Banco do Brasil S.A.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Conselho de Recursos do Sistema Financeiro Nacional', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'FACIL', 'Qual é o órgão responsável privativamente no Brasil pela emissão de papel-moeda e moeda metálica?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Casa da Moeda do Brasil', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Conselho Monetário Nacional', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Tesouro Nacional', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Banco Central do Brasil', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Ministério da Fazenda', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'FACIL', 'A CVM (Comissão de Valores Mobiliários) tem por atribuição principal:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Fiscalizar o mercado de capitais brasileiro.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Determinar as metas de inflação do governo.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Autorizar o funcionamento de seguradoras.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Fixar as diretrizes do crédito rural e industrial.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Emitir o real digital.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'FACIL', '(Caixa - CESGRANRIO) No Sistema Financeiro Nacional (SFN), os bancos comerciais são as principais instituições que depositam:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Apenas depósitos de poupança.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Bens preciosos e letras de câmbio mobiliário exclusivas.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Depósitos à vista, pois possuem a prerrogativa de criar circulação de moeda escritural.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Somente cotas de previdência fechada (fundos de pensão).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Resseguros e capitalização de amortização estruturada.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'FACIL', 'Os Fundos Garantidores de Crédito (FGC) garantem, até determinado limite, os depósitos feitos em instituições financeiras associadas. Qual o limite ordinário atual por CPF/CNPJ por instituição?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'R$ 50.000', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'R$ 100.000', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'R$ 250.000', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'R$ 500.000', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Sem limite (garantia total)', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'FACIL', 'Taxa Selic Over é a taxa básica de juros da economia. Ela reflete a taxa:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Média apurada no recadastramento imobiliário.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Média das operações compromissadas de um dia útil com lastro em títulos públicos federais.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Fixada pelas assembleias de condomínio dos gestores do Bacen.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Que baliza exclusivamente as transferências internacionais de divisas.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'De descontos aplicada aos fornecedores do Governo Federal.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'FACIL', 'O SPB (Sistema de Pagamentos Brasileiro) moderno, após sua reestruturação em 2002, transferiu o risco operacional e de crédito para quem?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Para os clientes, que enviam as TEDs.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Para o Fundo Garantidor de Crédito em primeira estância.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Para o Governo Federal (Tesouro).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Diretamente para os bancos através das reservas bancárias mantidas no Bacen, com liquidação bruta em tempo real (LBTR).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Para a Federação Brasileira de Bancos (Febraban).', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'FACIL', '(BB - Cesgranrio) O órgão que regulamenta a Previdência Complementar Fechada (Fundos de Pensão) é o:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Susep', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Previc', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'CNSP', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Bacen', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'CVM', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'FACIL', 'O Pix (Arranjo de Pagamentos Instantâneos) funciona 24/7/365. Quem é o gestor do SPI (Sistema de Pagamentos Instantâneos)?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'A própria Febraban.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'O Nubank (pioneiro).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'TecBan (Banco24Horas).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'O Banco Central do Brasil.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'A Associação de Cartões de Crédito (Abecs).', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'FACIL', 'De acordo com o Código de Defesa do Consumidor (CDC), a responsabilidade da instituição financeira por fraudes praticadas por terceiros nas operações de seus clientes (fortuito interno) é:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Inexistente', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Subjetiva (depende de prova de dolo)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Objetiva', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Solidária apenas se houver coação', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Condicionada ao registro de B.O. pelo banco', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'FACIL', 'Sobre lavagem de dinheiro (Lei 9.613/98 e atualizações), a etapa onde o criminoso tenta distanciar os recursos de sua origem ilícita em uma série de transações para dificultar rastreamentos chama-se:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Colocação (Placement)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Ocultação ou Mascaramento (Layering)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Integração (Integration)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Dumping (Omissão)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Churning', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'FACIL', '(Caixa CEF) Consideram-se infrações aos princípios da ética bancária, EXCETO:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Venda Casada.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Acesso a dados de clientes sem motivação profissional justificada.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Tratamento imparcial aos consumidores e uso da clareza nos contratos.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Omissão de informações relevantes ou de riscos associados a investimentos.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Atuação em conflito de interesses na venda de consórcios.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'FACIL', 'Nos princípios ESG aplicados aos relatórios bancários, a sigla E (Environmental) envolve práticas para gerenciar riscos socioambientais. Assinale a ação ligada ao eixo Ambiental:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Aumento nas cadeiras de conselheiras mulheres (Diversidade).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Negar repasse de crédito rural a fazendas embargadas pelo IBAMA por desmatamento.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Distribuição equitativa de bônus aos caixas executivos.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Auditoria fiscal independente nas demonstrações contábeis trimestrais.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Aprovação de código de ética para estagiários e jovens aprendizes.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'FACIL', 'Qual modelo de acesso seguro (Logon) exige que o cliente utilize mais de um fator distinto para acessar a conta (ex: Senha + SMS/Biometria)?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Single Sign-On (SSO)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'MFA (Multi-Factor Authentication)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Firewall de Aplicação Web', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Open Finance Token', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Criptografia Assimétrica Básica', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'MEDIO', '(BB/Caixa) Em Matemática Financeira, numa aplicação no regime de juros compostos, a taxa equivalente a 10% a.m em 2 meses é igual a:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, '20,00%', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, '21,00%', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, '21,50%', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, '22,00%', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, '100%', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'MEDIO', 'Qual é o título público federal indexado à variação da inflação (IPCA) acrescido de juros reais pré-fixados, destinado à pessoa física pelo programa Tesouro Direto?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Tesouro Prefixado (LTN)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Tesouro Selic (LFT)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Tesouro IPCA+ (NTN-B Principal / NTN-B)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Tesouro IGPM', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'CDB de balcão do BNDES', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'MEDIO', '(Basa) No mercado de câmbio brasileiro, a cotação da moeda estrangeira (regime atual adotado) ocorre pelo sistema de:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Câmbio fixo', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Bandas cambiais diagonais', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Câmbio flutuante (sujo/administrado)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Paridade fixa e lastro compulsório', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Câmbio indexado exclusivamente pelo Euro', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'MEDIO', 'Sobre CDB (Certificado de Depósito Bancário), marque a alternativa correta:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'É isento de IOF se resgatado no 1º dia.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'A alíquota de Imposto de Renda é regressiva, começando em 22,50% e podendo cair até 15% após 720 dias.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'O risco de crédito de um CDB é o Risco Brasil (Risco Soberano).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Apenas bancos de desenvolvimento podem emiti-los.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'A rentabilidade é unicamente pós-fixada pela Selic.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'MEDIO', '(BNB/Conhecimentos Bancários) O que é e para que serve a Taxa Referencial (TR)?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Foi criada para limitar a inflação e hoje baliza a rentabilidade das Letras do Tesouro.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'É uma taxa de juros básica livre usada entre os correntistas no PIX.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Taxa diária apurada na compra de ouro.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Taxa baseada livremente nas LCIs da Caixa', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Originalmente desindexador, hoje é referencial de correção para cadernetas de poupança (quando Selic > 8,5% a.a.) e FCVS.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'MEDIO', 'Em relação a fundos de investimento: a taxa cobrada pelo administrador para cobrir os custos operacionais (mesmo que o fundo apresente prejuízo) é a:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Taxa de Performance', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Taxa de Administração', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Taxa de Saída e Carregamento', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Taxa Selic Fixação', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Taxa de Cotas Alavancadas', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'MEDIO', 'Títulos de Crédito Comercial do tipo CPR (Cédula de Produto Rural). Qual a finalidade clássica?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Captar recursos de lojistas de shopping center.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Financiar a atividade rural provendo adiantamento ao produtor.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Lastrear a emissão de debêntures não-conversíveis de construtoras.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Sustentar programas de crédito educativo pelo FIES.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Nenhuma das anteriores.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'MEDIO', '(BB) Um corretor vendeu um título e aplicou o conceito de "Cross-selling". Na técnica de vendas em bancos, o que isso significa?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Tentar persuadir o cliente a investir uma quantia maior no mesmo produto já contratado (venda cruzada).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Oferecer um produto financeiro complementar a uma aquisição principal (ex: Consórcio + Seguro de Vida).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Promover abatimento de taxas ou descontos generalizados nas taxas do cartão.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Venda cancelada por arrependimento em até 7 dias, prevista pelo código civil (o famoso charge-back transversal).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Cobrança do seguro habitacional de presteza transversal obrigatório (venda casada).', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'MEDIO', 'Um sistema de amortização em que, na quitação do financiamento, o valor pago das parcelas é sempre IGUAL ou constante do início ao fim é o:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'SAC (Sistema de Amortização Constante)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'SaaC', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Sistema Americano', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Price (Sistema de prestação Constante/Frances)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Sistema Bullet', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'MEDIO', '(BNDES) A respeito das taxas de mercado, o CDI (Certificado de Depósito Interbancário) tem como participantes que transacionam entre si:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Exclusivamente pessoas físicas de varejo e os Bancos Comerciais.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Apenas instituições financeiras ou entes autorizados, sem a participação do público em geral (transferência interbancária).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Governo e corretoras.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'As empresas de Criptoativos e as adquirentes de maquineta.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Febraban e as Fintechs independentes de capital.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'MEDIO', 'Lei da Transparência de Pix - A opção MEU PIX / LIMITES serve principalmente para que o usuário:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Consiga transferir dinheiro para fora do País.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Aumente imediatamente a emissão do limite em caso de sequestro relâmpago, para salvaguardar a vida.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Estipule seus limites diários e noturnos como uma barreira antifraude autogerenciável (o aumento sofre carência e a redução é na hora).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Defina um IP dinâmico para transacionar sempre sem rastro no SIGIL.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Transfira sua chave para um amigo.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'MEDIO', 'As agências de classificação de risco (Rating) como Moody`s, Fitch e S&P Global operam avaliando:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'A chance (probabilidade) de empresas/governos honrarem ou darem calote em suas emissões e compromissos.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'O limite de transferência PIX diário da Caixa Econômica aos pensionistas.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Os padrões de layout do sistema financeiro aberto (Open Finance) no Brasil.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Se os bancos usam mais ou menos papel impresso sob políticas ESG estritamente de lixo zero.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Exclusivamente a taxa de mortalidade interbancária mensal.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'MEDIO', '(Técnicas de Venda) No funil de vendas tradicional da jornada bancária de clientes entrantes: A etapa final onde o cliente finalmente assina o contrato e se compromete chama-se:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Atração (Lead)', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Descoberta e engajamento', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Follow up ou nutrição do prospect', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Fechamento ou Conversão', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Cross-buying e cross-marketing', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'MEDIO', '(Caixa Econômica) O Fundo de Garantia do Tempo de Serviço (FGTS) é primordialmente destinado pelo seu Conselho Curador (CCFGTS) para o financiamento de programas de:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Startups unicórnios focadas em inteligência artificial e criptografia.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Construção de usinas termelétricas exclusivas no Sul.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Habitação, Saneamento Ambiental e Infraestrutura Urbana.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Renovação da frota pesada das transportadoras (BNDES Finame).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Consórcios unifamiliares internacionais (compra de bens móveis via remessas no exterior).', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'MEDIO', 'Uma grande atualização nas relações bancárias mais modernas introduziu o Open Finance, onde o pilar fundamental perante as I.Fs. envolvidas é o do:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Compartilhamento dos dados transacionais do cliente exclusivamente através de consentimento prévio, claro e formal, com duração especificada pelo usuário.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Compartilhamento total automático aprovado pela Febraban em que os bancos veem tudo de todos independentemente do usuário aceitar ou não para garantir livre concorrência.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Integração de carteiras físicas em moeda Cripto atrelada ao Real (CBDC).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Ocultação de taxas no momento de se mudar de porto bancário sob anonimidade de IP.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Privatização dos dados rumo à nuvem Amazon Web Services em todos os entes públicos compulsórios de crédito.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'DIFICIL', '(BNDES / Avançado) As Letras Financeiras (LF) são títulos de prazo médio ou logo para captação por Instituições Financeiras. Qual a restrição primária notável às LFs?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'FGC (garantia). Elas costumam não ter cobertura do Fundo Garantidor de Créditos e carregam prazos mínimos prolongados.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Não podem ser distribuídas de jeito nenhum para o investidor pessoa física.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Apenas a Caixa as usa, proibidas a demais entidades de fomento cooperativo financeiro de atuarem em repique.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Sofrem confisco se o IPCA ultrapassar 10% a.a.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Só valem no mercado asiático de eurobônus.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'DIFICIL', 'Na mecânica de Precificação de Derivativos - Swap (Troca), num clássico swap DI x Pré, a parte que ficou FIXADA pelo Pré-Fixado apostou ativamente que, no período contratado:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'A inflação IPCA seria estável gerando deflação negativa real.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'A taxa DI iria subir muito e exceder as expectativas e o custo total pós-fixado a arruinaria, garantindo estabilidade via pré-fixação ou achando que as DI caem forte frente ao prêmio.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'A bolsa americana bateria records.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'O ouro ia depreciar até ficar abaixo de US$2k/t.Oz.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Que as commodities subissem acima de T+4.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'DIFICIL', '(BACEN) Basiléia III. O arcabouço de regulação prudencial focou majoritariamente em:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Flexibilizar regras de capital para liberar emissores de cartões a dobrarem anuidades.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Focar na abolição do spread bancário.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Maior necessidade de índice captação (Capital Requirement) aumentando a qualidade de capital (tier 1), além de métricas rigorosas de Liquidez (LCR/NSFR) e métrica de alavancagem.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Desobrigar bancos pequenos de terem provisionamento contra liquidações de devedores duvidosos.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Criação exclusiva da Criptomoeda governamental e eliminação do FGC.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'DIFICIL', 'A Selic Meta é determinada pelo(a):', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Presidente da República diretamente, através de decreto Executivo trimensal.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'COPOM (Comitê de Política Monetária do BCB) em reuniões distribuídas ao longo de um calendário anual em cerca de 8 encontros.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Conselho Nacional de Seguros Privados.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Assembleia Extraordinária de quotistas das maiores Gestoras.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Superintendência de Relações Institucionais em Washington (FMI) em colaboração de Veto do Mercosul.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'DIFICIL', '(CMN/Resolução 4.893 e Cibersegurança) Em requisitos severos da rede nacional SFN, a legislação prevê incidentes. Em falhas super graves, qual a exigência da comunicação e aviso contido no plano das IFs?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'As I.F.s devem comunicar e reportar os registros incidentais de altíssima relevância ao Banco Central o mais célere e estritamente possível, guardando confidencialidade se julgado seguro assim.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Não devem comunicar terceiros e apagar logs rapidamente pra não sofrer fuga de capitais informando só e se, sofrer ataque com dano patrimonial superado a R$5bi.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Informar a CVM primeiro via portal CVMWEB independente e exclusivo.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Publicar notad no Diário Oficial em menos de 10 minutos após ser Hacked.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Cortar a internet e aguardar perícia da polícia federal bloqueando senhas Master sem informar o BC.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'DIFICIL', 'Operação de Redesconto: Qual a finalidade primordial dessa ferramenta clássica da autoridade monetária (BCB)?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Redescoberta de ouro estocados na base da C.M.B. para inflar a base emissiva monetária nacional provendo subsídios (quantitative easing).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Descontar antecipadamente faturas fiscais (Tributárias) pagas via DARF das MEI da zona rural ou ribeirinhas (Inclusão financeira).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Fornecer liquidez, usualmente de curto e curtíssimo prazos, pontualmente aos bancos que estão em grave desenquadramento no fluxo de fechamento de caixa frente aos seus depósitos/compulsórios transferidos.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Conceder crédito subsidiado, de longo tempo a empresas grandes (Eike-empresas) para construção ou exportação via "funding BNDES".', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Limpar as letras ruins das balanças estrangeiras.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'DIFICIL', '(Caixa) ETP - Elementos Teóricos de Probabilidade Bancária e Scoring. Numa modelagem clássica de Regressão Logística utilizada pra deferir ou reprovar grandes cartões (modelagem Credit Scoring), a variável de Score de resposta (Y ou 1) simboliza usualmente:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'O Risco de Lavagem de dinheiro ou probabilidade terrorista global.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'A Probabilidade do indivíduo (Cliente) ser um "Bom ou Mau" pagador a depender do corte, configurando Defaulting Probability (Chance de inadimplir o prazo nos próximos meses).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Os anos do indivíduo até uma idade X, determinando apólice universal auto-referenciada (prob. Demográfica/tábuas biométricas atuariais que se aplicam puramente em seguros gerais).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'A chance da Selic variar e por isso negar concessão.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Nenhuma dessas alternativas tangenciam Estatística nas IFs modernas operadas só via Pix Inteligente.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'DIFICIL', 'Bens fungíveis num contrato típico de Depósito Bancário e Mútuo de conta-corrente na lei comercial brasileira. O correntista tem a plena ciência de que:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'A nota da moeda (ex: R$100 do seriado de série X09088) continua sendo dele, guardada no cofre, e o Banco é infiel depositário se passar adiante.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Uma vez depositado e aceito a transação em moeda em conta corrente pela I.F e transferida à massa contabilizada para fluxo escritural bancário, o banco as detém e se responsabiliza a devolução apenas do valor integral pecuniário equivalente, sob garantias plenas mútuas, se misturando as disponibilidades bancárias do banco num contexto fungível clássico.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'É Crime federal emitir DOC. Os depósitos só são aceitos estritamente se TED (Transferência expressa).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Os depósitos em Mútuo devem ter aval fixo fiador caucionado em B3.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Ele está financiando debêntures conversíveis no exterior compulsoriamente.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'DIFICIL', '(Mercado de Capitais). Initial Public Offering (IPO) no escopo brasileiro - S.A (Sociedade Anônima) quer abrir sua operação. Qual das alternativas traz ritos essenciais?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Basta anunciar por meio de panfletos físicos para os investidores da B3 e abrir livro de promessas verbais com corretor PJ e no dia dar o Start nas rodadas.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Não precisa da CVM e sim só do Banco Central assinar na Junta Comercial, não existindo mais Prospecto Inicial obrigatório ou regulatório a ser publicamente exido à população, devendo só ser publicado a Receita Federal.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'É essencial registrar a oferta na CVM e elaborar prospectos de emissão (Prospecção/Fact-sheet), passando pelos trâmites de roadshow e de bookbuilding nos subscritores / agentes coordenadores.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Elas distribuem tokens na rede Blockchain livre que viram papéis na mesa americana automaticamente via depositary.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'FGC ressarce perdas acima de R$10M.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'DIFICIL', '(Compliance) O que seria PLD/CFT nos treinamentos contínuos do SFN?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Programa Lúdico Dinâmico e Confirmação Física Temporária', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Prevenção à Lavagem de Dinheiro e ao Combate ao Financiamento do Terrorismo/Proliferação as Armas de Destruição em Massa.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Projeções Lucrativas Diárias (para as CDIs) e Cotação de Futuras Treasury.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Prêmio de Liquidação Duvidosa contra Fuga de Taxa', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'N.d.a', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'DIFICIL', '(Certificações) Um consultor bancário indica investimento de alto risco para cliente perfil Ultra-Conservador sem a assinatura de ciência ou termo de adequação (Suitability) e o cliente choca as reservas emergenciais para renda passiva duvidosa com perdas reais e acionará a Justiça, baseando-se que ele sofreu infração ao pilar de qual conduta do ANBIMA / Mercado Financeiro?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Insider Trading de Suborno Corporativo.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Conheça seu Cliente - Suitability and API risk mismatch.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Spoofing em Leilões reversos e vazamentos sigilosos - Data leaking da plataforma open banking.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Mark to market defasado (M²M).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Front-Running com propinas.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'DIFICIL', '(Políticas Econômicas - Ação contracionista) Em cenário de hiperdemanda agregada (inflação ascendente preocupante descolada dos limites - além do teto da meta (Centro IPCA 3% a.a.+- 1.5), o Bacen se orienta para apertos monetários. Qual a resposta conjunta das 3 vias da política monetária restritiva coerente que o Governo / Banco exerceria pra retirar moeda?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Diminuir alíquotas compulsórias, Baixar Selic, Comprar títulos na Bolsa e Reduzir prazos e parcelamentos nos bancos facilitando crédito aos atacadistas e microcrédito abundante para fomento gerando giro.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Aumentar Compulsórios, Aumentar a faixa / Taxa básica meta SELIC no Copom encarecendo os créditos, e promover a Venda / colocação de Títulos Públicos (Operações de Mercado Aberto/Open Market) retirando massa primária, contendo o M1 circulacional no giro primário bancário mitigando consumo superavitário de demanda.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Emitir mais papel moeda urgente e congelar salários da poupança limitando taxas fixadas via controle direto da lei sem uso do CMN (Confisco de mercado temporário de Collor), gerando asfixia sem ferramentas de mercado.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Burlar as taxas subsidiadas criando taxa paralela referencial a Dólar p/ atração externa do Federal Reserve.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'DIFICIL', 'Garantias Bancárias (Fiança x Aval). A Fiança bancária é modalidade prestada nos moldes mais robustos nas concessões financeiras em garantias estritas. Numa Fianças em Contratos:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'O Fiança Bancária é acessório formal via carta firmada onde a instituição não-autônoma garante a terceiro obrigação principal civil gerando carta de fiança se aquele cliente final na relação for insolvente na esfera civil (Lei ou Obrigação Geral). Já o Aval é voltado diretamente aos compromissos de Títulos de Crédito formais autônomo e cambiaria.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Criam reservas no Banco Central em paralelo que retém lastro se falidos os conselhos.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Penhora Judicial apenas da pessoa física (fiador civil).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Apenas Cédulas habitacionais cedem cartas formalizadas (Obrigações habitacionais - LCI).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'A fiança vale estritamente dentro da caderneta.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'DIFICIL', '(Caixa - Saneamento e Minha Casa Minha Vida). De forma geral, com base no regramento governamental de habitações subsidiadas os recursos pesados desse repasse para a infraestrutura bancar e os juros reduzirem vertiginosamente provém do:', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Fundos constitucionais FCO / FNE da amazônia somado ao Caixa Tesouro Nacional puro e exclusivo sem outros vieses de tributação livre que o Governo arrecada cobrando ICMS alto do estado (repasses indiretos na íntegra para MCMV Faixa 1).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Operações internacionais de dívidas captadas no MIGA mundial em Euro repassados para Fundo Nacional.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'FGTS e do seu direcionamento legal massivo aos fundos para Habitação (Subsidia o sistema) provendo subsídio cruzado entre saques de cotas longas para capitalização das fundações de obras e construção unindo políticas habitacionais / fundos integrados sob gestão central.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Lucro privado das S.A repassado via CPMF.', false);
END $$;

DO $$
DECLARE q_id UUID := gen_random_uuid();
BEGIN
  INSERT INTO public.questions (id, banca, ano, materia, dificuldade, enunciado, status) VALUES (q_id, 'CESGRANRIO', 2024, 'Conhecimentos Bancários', 'DIFICIL', '(Atualidades Cripto/Drex/Blockchain) Qual é o principal objetivo do Bacen em instituir o real digital batizado como "Drex"?', 'PUBLICADA');

  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Transformar e liquidar no atacado e varejo negócios digitais modernos / Tokenizaçao, onde o Dinheiro no BlockChain com tecnologia DLT servirá diretamente com Contratos Inteligentes que se executam automático e liquidações simultâneas eficientes em frações instantâneas (Dinheiro Programável amparado institucionalmente).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Criar altcoin em bolsas asiáticas para que a balança de exportação renda super dividendos na Flórida (Mineração agressiva das estatais elétricas Itaipu).', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Ocultar ou anonimizar transferências para competir com Moedas Privacidade do universo restrito / deep, visando atrair capitais internacionais (sigilo total na blockchain pra não pagar IR) atraindo divisas para o Bacen.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Proibir uso do Bitcoin no brasil forçando pena prisional aos investidores substituindo à força por Drex.', false);
  INSERT INTO public.question_options (question_id, text, is_correct) VALUES (q_id, 'Destruir todos os PIX gradualmente até 2026 acabando e revogando a liquidez em L.B.T.R forçando uso de Carteiras Digitais Offshores controladas unicamente pela Febraban monopolista e corretoras das ilhas Cayman.', false);
END $$;

