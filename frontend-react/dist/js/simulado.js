const navToggle = document.querySelector('.nav-toggle');
const navMain = document.querySelector('.nav-main');

if (navToggle && navMain) {
  navToggle.addEventListener('click', () => {
    const isOpen = navMain.classList.toggle('nav-open');
    navToggle.setAttribute('aria-expanded', String(isOpen));
  });

  window.addEventListener('resize', () => {
    if (window.innerWidth > 740 && navMain.classList.contains('nav-open')) {
      navMain.classList.remove('nav-open');
      navToggle.setAttribute('aria-expanded', 'false');
    }
  });
}

const details = document.querySelectorAll('.faq-item');
details.forEach((item) => {
  item.addEventListener('toggle', () => {
    if (item.open) {
      details.forEach((other) => {
        if (other !== item) other.open = false;
      });
    }
  });
});

const quizBank = {
  basico: [
    { id: 1, prompt: 'De acordo com a Constituição Federal, o Sistema Financeiro Nacional é estruturado de forma a:', options: ['Promover o desenvolvimento equilibrado do País', 'Garantir lucros irrestritos aos bancos estrangeiros', 'Isentar operações de bolsa de qualquer tributação', 'Assegurar exclusividade do Estado na emissão de moeda criptográfica', 'Subordinar o CMN à CVM em questões de câmbio'], correctAnswer: 0, explanation: 'Segundo o Art. 192 da CF, o SFN será estruturado de forma a promover o desenvolvimento equilibrado do País e a servir aos interesses da coletividade.' },
    { id: 2, prompt: 'Assinale a alternativa que indica o órgão ou entidade considerado o órgão máximo do Sistema Financeiro Nacional.', options: ['Banco Central do Brasil (Bacen)', 'Comissão de Valores Mobiliários (CVM)', 'Conselho Monetário Nacional (CMN)', 'Banco do Brasil S.A.', 'Conselho de Recursos do Sistema Financeiro Nacional'], correctAnswer: 2, explanation: 'O CMN é o órgão máximo do SFN, responsável pela formulação da política da moeda e do crédito.' },
    { id: 3, prompt: 'Qual é o órgão responsável privativamente no Brasil pela emissão de papel-moeda e moeda metálica?', options: ['Casa da Moeda do Brasil', 'Conselho Monetário Nacional', 'Tesouro Nacional', 'Banco Central do Brasil', 'Ministério da Fazenda'], correctAnswer: 3, explanation: 'Compete privativamente ao BCB a emissão de moeda-papel e moeda metálica, por delegação do CMN.' },
    { id: 4, prompt: 'A CVM (Comissão de Valores Mobiliários) tem por atribuição principal:', options: ['Fiscalizar o mercado de capitais brasileiro.', 'Determinar as metas de inflação do governo.', 'Autorizar o funcionamento de seguradoras.', 'Fixar as diretrizes do crédito rural e industrial.', 'Emitir o real digital.'], correctAnswer: 0, explanation: 'A CVM disciplina e fiscaliza o mercado de capitais (ações, debêntures etc).' },
    { id: 5, prompt: '(Caixa - CESGRANRIO) No Sistema Financeiro Nacional (SFN), os bancos comerciais são as principais instituições que depositam:', options: ['Apenas depósitos de poupança.', 'Bens preciosos e letras de câmbio mobiliário exclusivas.', 'Depósitos à vista, pois possuem a prerrogativa de criar circulação de moeda escritural.', 'Somente cotas de previdência fechada (fundos de pensão).', 'Resseguros e capitalização de amortização estruturada.'], correctAnswer: 2, explanation: 'Os bancos comerciais focam no crédito de curto prazo e depósitos à vista (criando moeda escritural).' },
    { id: 6, prompt: 'Os Fundos Garantidores de Crédito (FGC) garantem, até determinado limite, os depósitos feitos em instituições financeiras associadas. Qual o limite ordinário atual por CPF/CNPJ por instituição?', options: ['R$ 50.000', 'R$ 100.000', 'R$ 250.000', 'R$ 500.000', 'Sem limite (garantia total)'], correctAnswer: 2, explanation: 'O FGC garante até R$ 250.000,00 por CPF/CNPJ em cada instituição (ou conglomerado financeiro).' },
    { id: 7, prompt: 'Taxa Selic Over é a taxa básica de juros da economia. Ela reflete a taxa:', options: ['Média apurada no recadastramento imobiliário.', 'Média das operações compromissadas de um dia útil com lastro em títulos públicos federais.', 'Fixada pelas assembleias de condomínio dos gestores do Bacen.', 'Que baliza exclusivamente as transferências internacionais de divisas.', 'De descontos aplicada aos fornecedores do Governo Federal.'], correctAnswer: 1, explanation: 'A Selic Over reflete os juros de operações compromissadas de um dia com lastro em títulos públicos.' },
    { id: 8, prompt: 'O SPB (Sistema de Pagamentos Brasileiro) moderno, após sua reestruturação em 2002, transferiu o risco operacional e de crédito para quem?', options: ['Para os clientes, que enviam as TEDs.', 'Para o Fundo Garantidor de Crédito em primeira estância.', 'Para o Governo Federal (Tesouro).', 'Diretamente para os bancos através das reservas bancárias mantidas no Bacen, com liquidação bruta em tempo real (LBTR).', 'Para a Federação Brasileira de Bancos (Febraban).'], correctAnswer: 3, explanation: 'O SPB introduziu as Transferências de Reservas (STR) em LBTR, diminuindo o risco sistêmico e focando a liquidação nas contas no BC.' },
    { id: 9, prompt: '(BB - Cesgranrio) O órgão que regulamenta a Previdência Complementar Fechada (Fundos de Pensão) é o:', options: ['Susep', 'Previc', 'CNSP', 'Bacen', 'CVM'], correctAnswer: 1, explanation: 'A PREVIC (Superintendência Nacional de Previdência Complementar) fiscaliza as EFPC (fundos de pensão).' },
    { id: 10, prompt: 'O Pix (Arranjo de Pagamentos Instantâneos) funciona 24/7/365. Quem é o gestor do SPI (Sistema de Pagamentos Instantâneos)?', options: ['A própria Febraban.', 'O Nubank (pioneiro).', 'TecBan (Banco24Horas).', 'O Banco Central do Brasil.', 'A Associação de Cartões de Crédito (Abecs).'], correctAnswer: 3, explanation: 'O Bacen é o único gestor e operador do SPI e do DICT (Diretório de Identificadores de Contas Transacionais).' },
    { id: 11, prompt: 'De acordo com o Código de Defesa do Consumidor (CDC), a responsabilidade da instituição financeira por fraudes praticadas por terceiros nas operações de seus clientes (fortuito interno) é:', options: ['Inexistente', 'Subjetiva (depende de prova de dolo)', 'Objetiva', 'Solidária apenas se houver coação', 'Condicionada ao registro de B.O. pelo banco'], correctAnswer: 2, explanation: 'Súmula 479 do STJ: "As IFs respondem objetivamente pelos danos fortuitos internos relativos a fraudes".' },
    { id: 12, prompt: 'Sobre lavagem de dinheiro (Lei 9.613/98 e atualizações), a etapa onde o criminoso tenta distanciar os recursos de sua origem ilícita em uma série de transações para dificultar rastreamentos chama-se:', options: ['Colocação (Placement)', 'Ocultação ou Mascaramento (Layering)', 'Integração (Integration)', 'Dumping (Omissão)', 'Churning'], correctAnswer: 1, explanation: 'A Ocultação (Layering) é a série de transações feitas para quebrar a cadeia de evidências ante os investigadores.' },
    { id: 13, prompt: '(Caixa CEF) Consideram-se infrações aos princípios da ética bancária, EXCETO:', options: ['Venda Casada.', 'Acesso a dados de clientes sem motivação profissional justificada.', 'Tratamento imparcial aos consumidores e uso da clareza nos contratos.', 'Omissão de informações relevantes ou de riscos associados a investimentos.', 'Atuação em conflito de interesses na venda de consórcios.'], correctAnswer: 2, explanation: 'Tratamento imparcial e claro é um princípio, não uma infração ética!' },
    { id: 14, prompt: 'Nos princípios ESG aplicados aos relatórios bancários, a sigla E (Environmental) envolve práticas para gerenciar riscos socioambientais. Assinale a ação ligada ao eixo Ambiental:', options: ['Aumento nas cadeiras de conselheiras mulheres (Diversidade).', 'Negar repasse de crédito rural a fazendas embargadas pelo IBAMA por desmatamento.', 'Distribuição equitativa de bônus aos caixas executivos.', 'Auditoria fiscal independente nas demonstrações contábeis trimestrais.', 'Aprovação de código de ética para estagiários e jovens aprendizes.'], correctAnswer: 1, explanation: 'O impedimento de crédito a desmatadores visa controlar o risco climático e atende o critério de sustentabilidade ambiental.' },
    { id: 15, prompt: 'Qual modelo de acesso seguro (Logon) exige que o cliente utilize mais de um fator distinto para acessar a conta (ex: Senha + SMS/Biometria)?', options: ['Single Sign-On (SSO)', 'MFA (Multi-Factor Authentication)', 'Firewall de Aplicação Web', 'Open Finance Token', 'Criptografia Assimétrica Básica'], correctAnswer: 1, explanation: 'MFA requer pelo menos duas provas diferentes de identidade antes de conceder acesso.' }
  ],
  intermediario: [
    { id: 1, prompt: '(BB/Caixa) Em Matemática Financeira, numa aplicação no regime de juros compostos, a taxa equivalente a 10% a.m em 2 meses é igual a:', options: ['20,00%', '21,00%', '21,50%', '22,00%', '100%'], correctAnswer: 1, explanation: '(1 + 0,10)² - 1 = 1,21 - 1 = 0,21 (ou 21%).' },
    { id: 2, prompt: 'Qual é o título público federal indexado à variação da inflação (IPCA) acrescido de juros reais pré-fixados, destinado à pessoa física pelo programa Tesouro Direto?', options: ['Tesouro Prefixado (LTN)', 'Tesouro Selic (LFT)', 'Tesouro IPCA+ (NTN-B Principal / NTN-B)', 'Tesouro IGPM', 'CDB de balcão do BNDES'], correctAnswer: 2, explanation: 'O Tesouro IPCA+ garante o rendimento indexado à inflação mais uma taxa fixa.' },
    { id: 3, prompt: '(Basa) No mercado de câmbio brasileiro, a cotação da moeda estrangeira (regime atual adotado) ocorre pelo sistema de:', options: ['Câmbio fixo', 'Bandas cambiais diagonais', 'Câmbio flutuante (sujo/administrado)', 'Paridade fixa e lastro compulsório', 'Câmbio indexado exclusivamente pelo Euro'], correctAnswer: 2, explanation: 'O Brasil adota o câmbio flutuante, porém com atuações do Bacen pontuais (dirty float).' },
    { id: 4, prompt: 'Sobre CDB (Certificado de Depósito Bancário), marque a alternativa correta:', options: ['É isento de IOF se resgatado no 1º dia.', 'A alíquota de Imposto de Renda é regressiva, começando em 22,50% e podendo cair até 15% após 720 dias.', 'O risco de crédito de um CDB é o Risco Brasil (Risco Soberano).', 'Apenas bancos de desenvolvimento podem emiti-los.', 'A rentabilidade é unicamente pós-fixada pela Selic.'], correctAnswer: 1, explanation: 'Aplica-se a tabela regressiva de IR (22,5% a 15%) sobre os ganhos de capital no resgate.' },
    { id: 5, prompt: '(BNB/Conhecimentos Bancários) O que é e para que serve a Taxa Referencial (TR)?', options: ['Foi criada para limitar a inflação e hoje baliza a rentabilidade das Letras do Tesouro.', 'É uma taxa de juros básica livre usada entre os correntistas no PIX.', 'Taxa diária apurada na compra de ouro.', 'Taxa baseada livremente nas LCIs da Caixa', 'Originalmente desindexador, hoje é referencial de correção para cadernetas de poupança (quando Selic > 8,5% a.a.) e FCVS.'], correctAnswer: 4, explanation: 'A TR compõe o rendimento de algumas aplicações tradicionais, especialmente da Caderneta de Poupança e de contas de FGTS.' },
    { id: 6, prompt: 'Em relação a fundos de investimento: a taxa cobrada pelo administrador para cobrir os custos operacionais (mesmo que o fundo apresente prejuízo) é a:', options: ['Taxa de Performance', 'Taxa de Administração', 'Taxa de Saída e Carregamento', 'Taxa Selic Fixação', 'Taxa de Cotas Alavancadas'], correctAnswer: 1, explanation: 'A taxa de administração é anual e cobrada para a gestão do fundo (em percentual do patrimônio líquido), independentemente do resultado.' },
    { id: 7, prompt: 'Títulos de Crédito Comercial do tipo CPR (Cédula de Produto Rural). Qual a finalidade clássica?', options: ['Captar recursos de lojistas de shopping center.', 'Financiar a atividade rural provendo adiantamento ao produtor.', 'Lastrear a emissão de debêntures não-conversíveis de construtoras.', 'Sustentar programas de crédito educativo pelo FIES.', 'Nenhuma das anteriores.'], correctAnswer: 1, explanation: 'A CPR é usada para facilitar o fomento à agricultura e pecuária.' },
    { id: 8, prompt: '(BB) Um corretor vendeu um título e aplicou o conceito de "Cross-selling". Na técnica de vendas em bancos, o que isso significa?', options: ['Tentar persuadir o cliente a investir uma quantia maior no mesmo produto já contratado (venda cruzada).', 'Oferecer um produto financeiro complementar a uma aquisição principal (ex: Consórcio + Seguro de Vida).', 'Promover abatimento de taxas ou descontos generalizados nas taxas do cartão.', 'Venda cancelada por arrependimento em até 7 dias, prevista pelo código civil (o famoso charge-back transversal).', 'Cobrança do seguro habitacional de presteza transversal obrigatório (venda casada).'], correctAnswer: 1, explanation: 'Cross-selling significa oferecer produtos complementares (venda cruzada de outro produto).' },
    { id: 9, prompt: 'Um sistema de amortização em que, na quitação do financiamento, o valor pago das parcelas é sempre IGUAL ou constante do início ao fim é o:', options: ['SAC (Sistema de Amortização Constante)', 'SaaC', 'Sistema Americano', 'Price (Sistema de prestação Constante/Frances)', 'Sistema Bullet'], correctAnswer: 3, explanation: 'Na Price, a prestação total (juros+amortização) é fixa e constante, diferentemente do SAC em que cai ao longo do tempo.' },
    { id: 10, prompt: '(BNDES) A respeito das taxas de mercado, o CDI (Certificado de Depósito Interbancário) tem como participantes que transacionam entre si:', options: ['Exclusivamente pessoas físicas de varejo e os Bancos Comerciais.', 'Apenas instituições financeiras ou entes autorizados, sem a participação do público em geral (transferência interbancária).', 'Governo e corretoras.', 'As empresas de Criptoativos e as adquirentes de maquineta.', 'Febraban e as Fintechs independentes de capital.'], correctAnswer: 1, explanation: 'Os DIs servem para o lastro repassando liquidez exclusivamente entre instituições financeiras (bancos fechando o caixa).' },
    { id: 11, prompt: 'Lei da Transparência de Pix - A opção MEU PIX / LIMITES serve principalmente para que o usuário:', options: ['Consiga transferir dinheiro para fora do País.', 'Aumente imediatamente a emissão do limite em caso de sequestro relâmpago, para salvaguardar a vida.', 'Estipule seus limites diários e noturnos como uma barreira antifraude autogerenciável (o aumento sofre carência e a redução é na hora).', 'Defina um IP dinâmico para transacionar sempre sem rastro no SIGIL.', 'Transfira sua chave para um amigo.'], correctAnswer: 2, explanation: 'A redução de limites é imediata, já os acréscimos dependem de prazo estipulado por regras de segurança do Bacen.' },
    { id: 12, prompt: 'As agências de classificação de risco (Rating) como Moody`s, Fitch e S&P Global operam avaliando:', options: ['A chance (probabilidade) de empresas/governos honrarem ou darem calote em suas emissões e compromissos.', 'O limite de transferência PIX diário da Caixa Econômica aos pensionistas.', 'Os padrões de layout do sistema financeiro aberto (Open Finance) no Brasil.', 'Se os bancos usam mais ou menos papel impresso sob políticas ESG estritamente de lixo zero.', 'Exclusivamente a taxa de mortalidade interbancária mensal.'], correctAnswer: 0, explanation: 'Agências de rating medem o risco de inadimplência de empresas e países e fornecem uma "nota".' },
    { id: 13, prompt: '(Técnicas de Venda) No funil de vendas tradicional da jornada bancária de clientes entrantes: A etapa final onde o cliente finalmente assina o contrato e se compromete chama-se:', options: ['Atração (Lead)', 'Descoberta e engajamento', 'Follow up ou nutrição do prospect', 'Fechamento ou Conversão', 'Cross-buying e cross-marketing'], correctAnswer: 3, explanation: 'A conversão/fechamento é a efetiva consumação do acordo da compra/concessão de crédito.' },
    { id: 14, prompt: '(Caixa Econômica) O Fundo de Garantia do Tempo de Serviço (FGTS) é primordialmente destinado pelo seu Conselho Curador (CCFGTS) para o financiamento de programas de:', options: ['Startups unicórnios focadas em inteligência artificial e criptografia.', 'Construção de usinas termelétricas exclusivas no Sul.', 'Habitação, Saneamento Ambiental e Infraestrutura Urbana.', 'Renovação da frota pesada das transportadoras (BNDES Finame).', 'Consórcios unifamiliares internacionais (compra de bens móveis via remessas no exterior).'], correctAnswer: 2, explanation: 'Foco central da alocação de ativos do fundo vai para grandes obras estatais/privadas de habitação e saneamento.' },
    { id: 15, prompt: 'Uma grande atualização nas relações bancárias mais modernas introduziu o Open Finance, onde o pilar fundamental perante as I.Fs. envolvidas é o do:', options: ['Compartilhamento dos dados transacionais do cliente exclusivamente através de consentimento prévio, claro e formal, com duração especificada pelo usuário.', 'Compartilhamento total automático aprovado pela Febraban em que os bancos veem tudo de todos independentemente do usuário aceitar ou não para garantir livre concorrência.', 'Integração de carteiras físicas em moeda Cripto atrelada ao Real (CBDC).', 'Ocultação de taxas no momento de se mudar de porto bancário sob anonimidade de IP.', 'Privatização dos dados rumo à nuvem Amazon Web Services em todos os entes públicos compulsórios de crédito.'], correctAnswer: 0, explanation: 'O pilar master do Open Finance é o Consentimento expresso das informações pelo próprio titular (consumidor).' }
  ],
  avancado: [
    { id: 1, prompt: '(BNDES / Avançado) As Letras Financeiras (LF) são títulos de prazo médio ou logo para captação por Instituições Financeiras. Qual a restrição primária notável às LFs?', options: ['FGC (garantia). Elas costumam não ter cobertura do Fundo Garantidor de Créditos e carregam prazos mínimos prolongados.', 'Não podem ser distribuídas de jeito nenhum para o investidor pessoa física.', 'Apenas a Caixa as usa, proibidas a demais entidades de fomento cooperativo financeiro de atuarem em repique.', 'Sofrem confisco se o IPCA ultrapassar 10% a.a.', 'Só valem no mercado asiático de eurobônus.'], correctAnswer: 0, explanation: 'Aos emissores é interessante captar dívidas seniores longas, mas o PF deve estar ciente de que a subscrição de Letra Financeira NENHUMA possui garantia ordinária do FGC (CDBs/LCIs possuem).' },
    { id: 2, prompt: 'Na mecânica de Precificação de Derivativos - Swap (Troca), num clássico swap DI x Pré, a parte que ficou FIXADA pelo Pré-Fixado apostou ativamente que, no período contratado:', options: ['A inflação IPCA seria estável gerando deflação negativa real.', 'A taxa DI iria subir muito e exceder as expectativas e o custo total pós-fixado a arruinaria, garantindo estabilidade via pré-fixação ou achando que as DI caem forte frente ao prêmio.', 'A bolsa americana bateria records.', 'O ouro ia depreciar até ficar abaixo de US$2k/t.Oz.', 'Que as commodities subissem acima de T+4.'], correctAnswer: 1, explanation: 'Na ponta contratual pré-fixada, ele trava seu fluxo apostando que ao firmar o contrato assim a subida de DI pós não iria o afetar OU lucrará na variação (o recebedor Pré ganha se as taxas DI caírem muito e o pré fica superior).' },
    { id: 3, prompt: '(BACEN) Basiléia III. O arcabouço de regulação prudencial focou majoritariamente em:', options: ['Flexibilizar regras de capital para liberar emissores de cartões a dobrarem anuidades.', 'Focar na abolição do spread bancário.', 'Maior necessidade de índice captação (Capital Requirement) aumentando a qualidade de capital (tier 1), além de métricas rigorosas de Liquidez (LCR/NSFR) e métrica de alavancagem.', 'Desobrigar bancos pequenos de terem provisionamento contra liquidações de devedores duvidosos.', 'Criação exclusiva da Criptomoeda governamental e eliminação do FGC.'], correctAnswer: 2, explanation: 'A crise de 2008 forçou exigências de fundos estritos Tier1 mais robustos e indicadores focados em sobrevida sob crises.' },
    { id: 4, prompt: 'A Selic Meta é determinada pelo(a):', options: ['Presidente da República diretamente, através de decreto Executivo trimensal.', 'COPOM (Comitê de Política Monetária do BCB) em reuniões distribuídas ao longo de um calendário anual em cerca de 8 encontros.', 'Conselho Nacional de Seguros Privados.', 'Assembleia Extraordinária de quotistas das maiores Gestoras.', 'Superintendência de Relações Institucionais em Washington (FMI) em colaboração de Veto do Mercosul.'], correctAnswer: 1, explanation: 'O Comitê de Política Monetária do Banco Central do Brasil define.' },
    { id: 5, prompt: '(CMN/Resolução 4.893 e Cibersegurança) Em requisitos severos da rede nacional SFN, a legislação prevê incidentes. Em falhas super graves, qual a exigência da comunicação e aviso contido no plano das IFs?', options: ['As I.F.s devem comunicar e reportar os registros incidentais de altíssima relevância ao Banco Central o mais célere e estritamente possível, guardando confidencialidade se julgado seguro assim.', 'Não devem comunicar terceiros e apagar logs rapidamente pra não sofrer fuga de capitais informando só e se, sofrer ataque com dano patrimonial superado a R$5bi.', 'Informar a CVM primeiro via portal CVMWEB independente e exclusivo.', 'Publicar notad no Diário Oficial em menos de 10 minutos após ser Hacked.', 'Cortar a internet e aguardar perícia da polícia federal bloqueando senhas Master sem informar o BC.'], correctAnswer: 0, explanation: 'Sistemas que reportem incidentes relevantes de segurança cibernética ou paralisações deverão estar prontamente disponíveis sob aviso estrito à alta administração e repasse oficial à Autarquia Central Brasileira (Bacen).' },
    { id: 6, prompt: 'Operação de Redesconto: Qual a finalidade primordial dessa ferramenta clássica da autoridade monetária (BCB)?', options: ['Redescoberta de ouro estocados na base da C.M.B. para inflar a base emissiva monetária nacional provendo subsídios (quantitative easing).', 'Descontar antecipadamente faturas fiscais (Tributárias) pagas via DARF das MEI da zona rural ou ribeirinhas (Inclusão financeira).', 'Fornecer liquidez, usualmente de curto e curtíssimo prazos, pontualmente aos bancos que estão em grave desenquadramento no fluxo de fechamento de caixa frente aos seus depósitos/compulsórios transferidos.', 'Conceder crédito subsidiado, de longo tempo a empresas grandes (Eike-empresas) para construção ou exportação via "funding BNDES".', 'Limpar as letras ruins das balanças estrangeiras.'], correctAnswer: 2, explanation: 'O redesconto é um empréstimo salvaguarda do Banco Central aos caixas problemáticos momentâneos ou por quebra de liquidações noturnas.' },
    { id: 7, prompt: '(Caixa) ETP - Elementos Teóricos de Probabilidade Bancária e Scoring. Numa modelagem clássica de Regressão Logística utilizada pra deferir ou reprovar grandes cartões (modelagem Credit Scoring), a variável de Score de resposta (Y ou 1) simboliza usualmente:', options: ['O Risco de Lavagem de dinheiro ou probabilidade terrorista global.', 'A Probabilidade do indivíduo (Cliente) ser um "Bom ou Mau" pagador a depender do corte, configurando Defaulting Probability (Chance de inadimplir o prazo nos próximos meses).', 'Os anos do indivíduo até uma idade X, determinando apólice universal auto-referenciada (prob. Demográfica/tábuas biométricas atuariais que se aplicam puramente em seguros gerais).', 'A chance da Selic variar e por isso negar concessão.', 'Nenhuma dessas alternativas tangenciam Estatística nas IFs modernas operadas só via Pix Inteligente.'], correctAnswer: 1, explanation: 'No Credit Score, o retorno medido num regressor qualitativo binário expressa chance de Default - PD.' },
    { id: 8, prompt: 'Bens fungíveis num contrato típico de Depósito Bancário e Mútuo de conta-corrente na lei comercial brasileira. O correntista tem a plena ciência de que:', options: ['A nota da moeda (ex: R$100 do seriado de série X09088) continua sendo dele, guardada no cofre, e o Banco é infiel depositário se passar adiante.', 'Uma vez depositado e aceito a transação em moeda em conta corrente pela I.F e transferida à massa contabilizada para fluxo escritural bancário, o banco as detém e se responsabiliza a devolução apenas do valor integral pecuniário equivalente, sob garantias plenas mútuas, se misturando as disponibilidades bancárias do banco num contexto fungível clássico.', 'É Crime federal emitir DOC. Os depósitos só são aceitos estritamente se TED (Transferência expressa).', 'Os depósitos em Mútuo devem ter aval fixo fiador caucionado em B3.', 'Ele está financiando debêntures conversíveis no exterior compulsoriamente.'], correctAnswer: 1, explanation: 'Depósito na conta converte o dinheiro depositado em massa monetária própria da entidade fungível, e em contrapartida a I.F fornece o direito material / saldo de restituição exata a qualquer tempo do total ao correntista.' },
    { id: 9, prompt: '(Mercado de Capitais). Initial Public Offering (IPO) no escopo brasileiro - S.A (Sociedade Anônima) quer abrir sua operação. Qual das alternativas traz ritos essenciais?', options: ['Basta anunciar por meio de panfletos físicos para os investidores da B3 e abrir livro de promessas verbais com corretor PJ e no dia dar o Start nas rodadas.', 'Não precisa da CVM e sim só do Banco Central assinar na Junta Comercial, não existindo mais Prospecto Inicial obrigatório ou regulatório a ser publicamente exido à população, devendo só ser publicado a Receita Federal.', 'É essencial registrar a oferta na CVM e elaborar prospectos de emissão (Prospecção/Fact-sheet), passando pelos trâmites de roadshow e de bookbuilding nos subscritores / agentes coordenadores.', 'Elas distribuem tokens na rede Blockchain livre que viram papéis na mesa americana automaticamente via depositary.', 'FGC ressarce perdas acima de R$10M.'], correctAnswer: 2, explanation: 'Uma IPO requer obrigatoriamente elaboração e apresentação dos extensivos relatórios (prospecto e aval da CVM) e coleta de preços formatada pelas instituições (Bookbuilding).' },
    { id: 10, prompt: '(Compliance) O que seria PLD/CFT nos treinamentos contínuos do SFN?', options: ['Programa Lúdico Dinâmico e Confirmação Física Temporária', 'Prevenção à Lavagem de Dinheiro e ao Combate ao Financiamento do Terrorismo/Proliferação as Armas de Destruição em Massa.', 'Projeções Lucrativas Diárias (para as CDIs) e Cotação de Futuras Treasury.', 'Prêmio de Liquidação Duvidosa contra Fuga de Taxa', 'N.d.a'], correctAnswer: 1, explanation: 'Anti-Money Laundering / Combating the Financing of Terrorism ou AML (Prevenção e combate na tríade).' },
    { id: 11, prompt: '(Certificações) Um consultor bancário indica investimento de alto risco para cliente perfil Ultra-Conservador sem a assinatura de ciência ou termo de adequação (Suitability) e o cliente choca as reservas emergenciais para renda passiva duvidosa com perdas reais e acionará a Justiça, baseando-se que ele sofreu infração ao pilar de qual conduta do ANBIMA / Mercado Financeiro?', options: ['Insider Trading de Suborno Corporativo.', 'Conheça seu Cliente - Suitability and API risk mismatch.', 'Spoofing em Leilões reversos e vazamentos sigilosos - Data leaking da plataforma open banking.', 'Mark to market defasado (M²M).', 'Front-Running com propinas.'], correctAnswer: 1, explanation: 'A falta de adequação ao perfil de investimento do investidor por parte dos repassadores rompe toda a prerrogativa regimental do conheça o perfil (Suitability/API).' },
    { id: 12, prompt: '(Políticas Econômicas - Ação contracionista) Em cenário de hiperdemanda agregada (inflação ascendente preocupante descolada dos limites - além do teto da meta (Centro IPCA 3% a.a.+- 1.5), o Bacen se orienta para apertos monetários. Qual a resposta conjunta das 3 vias da política monetária restritiva coerente que o Governo / Banco exerceria pra retirar moeda?', options: ['Diminuir alíquotas compulsórias, Baixar Selic, Comprar títulos na Bolsa e Reduzir prazos e parcelamentos nos bancos facilitando crédito aos atacadistas e microcrédito abundante para fomento gerando giro.', 'Aumentar Compulsórios, Aumentar a faixa / Taxa básica meta SELIC no Copom encarecendo os créditos, e promover a Venda / colocação de Títulos Públicos (Operações de Mercado Aberto/Open Market) retirando massa primária, contendo o M1 circulacional no giro primário bancário mitigando consumo superavitário de demanda.', 'Emitir mais papel moeda urgente e congelar salários da poupança limitando taxas fixadas via controle direto da lei sem uso do CMN (Confisco de mercado temporário de Collor), gerando asfixia sem ferramentas de mercado.', 'Burlar as taxas subsidiadas criando taxa paralela referencial a Dólar p/ atração externa do Federal Reserve.'], correctAnswer: 1, explanation: 'Para tirar poder de fluidez inflacionário-demanda: Selic para cima, Compulsório Bancário retido subindo à margem e OMA engolindo moedas e vendendo papéis (Venda TPF -> Absorvem $ retirado de circulação).' },
    { id: 13, prompt: 'Garantias Bancárias (Fiança x Aval). A Fiança bancária é modalidade prestada nos moldes mais robustos nas concessões financeiras em garantias estritas. Numa Fianças em Contratos:', options: ['O Fiança Bancária é acessório formal via carta firmada onde a instituição não-autônoma garante a terceiro obrigação principal civil gerando carta de fiança se aquele cliente final na relação for insolvente na esfera civil (Lei ou Obrigação Geral). Já o Aval é voltado diretamente aos compromissos de Títulos de Crédito formais autônomo e cambiaria.', 'Criam reservas no Banco Central em paralelo que retém lastro se falidos os conselhos.', 'Penhora Judicial apenas da pessoa física (fiador civil).', 'Apenas Cédulas habitacionais cedem cartas formalizadas (Obrigações habitacionais - LCI).', 'A fiança vale estritamente dentro da caderneta.'], correctAnswer: 0, explanation: 'Fiança engloba contrato cível complexificado; Aval vincula a obrigações diretas de responsabilidade cambiária em notas e títulos com co-solidariedade instantânea.' },
    { id: 14, prompt: '(Caixa - Saneamento e Minha Casa Minha Vida). De forma geral, com base no regramento governamental de habitações subsidiadas os recursos pesados desse repasse para a infraestrutura bancar e os juros reduzirem vertiginosamente provém do:', options: ['Fundos constitucionais FCO / FNE da amazônia somado ao Caixa Tesouro Nacional puro e exclusivo sem outros vieses de tributação livre que o Governo arrecada cobrando ICMS alto do estado (repasses indiretos na íntegra para MCMV Faixa 1).', 'Operações internacionais de dívidas captadas no MIGA mundial em Euro repassados para Fundo Nacional.', 'FGTS e do seu direcionamento legal massivo aos fundos para Habitação (Subsidia o sistema) provendo subsídio cruzado entre saques de cotas longas para capitalização das fundações de obras e construção unindo políticas habitacionais / fundos integrados sob gestão central.', 'Lucro privado das S.A repassado via CPMF.'], correctAnswer: 2, explanation: 'Grande força do trator habitacional brasileiro / CEF deriva do Orçamento monstruoso Operacional / Programático do próprio FGTS de onde são drenados subsídios fortíssimos de construção e empréstimos de habitação imobiliários populacionais na curva.' },
    { id: 15, prompt: '(Atualidades Cripto/Drex/Blockchain) Qual é o principal objetivo do Bacen em instituir o real digital batizado como "Drex"?', options: ['Transformar e liquidar no atacado e varejo negócios digitais modernos / Tokenizaçao, onde o Dinheiro no BlockChain com tecnologia DLT servirá diretamente com Contratos Inteligentes que se executam automático e liquidações simultâneas eficientes em frações instantâneas (Dinheiro Programável amparado institucionalmente).', 'Criar altcoin em bolsas asiáticas para que a balança de exportação renda super dividendos na Flórida (Mineração agressiva das estatais elétricas Itaipu).', 'Ocultar ou anonimizar transferências para competir com Moedas Privacidade do universo restrito / deep, visando atrair capitais internacionais (sigilo total na blockchain pra não pagar IR) atraindo divisas para o Bacen.', 'Proibir uso do Bitcoin no brasil forçando pena prisional aos investidores substituindo à força por Drex.', 'Destruir todos os PIX gradualmente até 2026 acabando e revogando a liquidez em L.B.T.R forçando uso de Carteiras Digitais Offshores controladas unicamente pela Febraban monopolista e corretoras das ilhas Cayman.'], correctAnswer: 0, explanation: 'O Drex vem para criar uma forma programável soberana, escalando em segurança com *smart contracts* focados no atacado financeiro e posteriormente simplificados com as tecnologias descentralizadas distribuídas.' }
  ]
};

const STORAGE_KEY = 'simuladoEliteState';
const RESULT_KEY = 'simuladoEliteResult';
const QUIZ_DURATION = 15 * 60;
const ACTIVE_QUESTION_COUNT = 15;

function parseStoredState() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? JSON.parse(stored) : null;
  } catch (error) {
    return null;
  }
}

function saveState(state) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function resetQuizState() {
  localStorage.removeItem(STORAGE_KEY);
}

function buildInitialState(level) {
  const selectedQuestions = quizBank[level] || quizBank.basico;
  return {
    quizId: `simulado-${level}-2026`,
    currentIndex: 0,
    answers: {},
    review: [],
    timeLeft: QUIZ_DURATION,
    completed: false,
    questions: selectedQuestions.slice(0, ACTIVE_QUESTION_COUNT).map((question) => ({ ...question }))
  };
}

function ensureState(forceReset = false, level = 'basico') {
  const storedState = parseStoredState();
  if (forceReset || !storedState || storedState.questions?.length !== ACTIVE_QUESTION_COUNT || storedState.quizId !== `simulado-${level}-2026`) {
    const state = buildInitialState(level);
    saveState(state);
    return state;
  }
  return storedState;
}

function computeResults(state) {
  const total = state.questions.length;
  let correct = 0;
  let wrong = 0;
  let blank = 0;
  const review = state.questions.map((question) => {
    const selectedIndex = state.answers[question.id];
    if (selectedIndex === undefined) {
      blank += 1;
      return {
        ...question,
        selectedIndex: undefined,
        isBlank: true,
        isCorrect: false
      };
    }

    if (selectedIndex === question.correctAnswer) {
      correct += 1;
      return {
        ...question,
        selectedIndex,
        isBlank: false,
        isCorrect: true
      };
    }

    wrong += 1;
    return {
      ...question,
      selectedIndex,
      isBlank: false,
      isCorrect: false
    };
  });

  const score = Math.round((correct / total) * 100);
  const performance = score >= 85 ? 'Excelente' : score >= 70 ? 'Muito bom' : score >= 50 ? 'Bom' : 'Ainda dá para evoluir';
  const feedback = score >= 85
    ? 'Você mostrou excelente domínio. Continue assim e mantenha a consistência.'
    : score >= 70
      ? 'Muito bom! Seu raciocínio está forte. Ajuste detalhes para chegar ao topo.'
      : score >= 50
        ? 'Você está no caminho certo. Foque nas questões mais desafiadoras e pratique mais.'
        : 'Atenção: a revisão precisa ser mais intensa. Cada simulado é uma oportunidade de crescer.';

  return {
    score,
    correct,
    wrong,
    blank,
    total,
    performance,
    feedback,
    timeLeft: state.timeLeft,
    reviewedCount: state.review.length,
    questions: review
  };
}

function finishQuiz(state, source = 'manual') {
  const results = computeResults(state);
  localStorage.setItem(RESULT_KEY, JSON.stringify({ ...results, source }));
  localStorage.removeItem(STORAGE_KEY);
  window.location.href = 'resultado.html';
}

function initQuizPage() {
  const questionCounter = document.getElementById('questionCounter');
  const questionText = document.getElementById('questionText');
  const optionList = document.getElementById('optionList');
  const questionNav = document.getElementById('questionNav');
  const timerElement = document.getElementById('timer');
  const progressBar = document.getElementById('progressBar');
  const timeBar = document.getElementById('timeBar');
  const reviewBar = document.getElementById('reviewBar');
  const prevButton = document.getElementById('prevQuestion');
  const nextButton = document.getElementById('nextQuestion');
  const toggleReviewButton = document.getElementById('toggleReviewBtn');
  const finishButton = document.getElementById('finishQuizBtn');
  const finishHeaderButton = document.getElementById('finishHeaderBtn');
  const finishHeaderLink = document.getElementById('finishHeaderLink');
  const questionStatus = document.getElementById('questionStatus');
  const progressLabel = document.getElementById('progressLabel');

  if (!questionCounter || !questionText || !optionList || !questionNav || !timerElement) {
    return;
  }

  const urlParams = new URLSearchParams(window.location.search);
  const level = urlParams.get('level') || 'basico';
  const state = ensureState(urlParams.get('reset') === '1', level);

  function renderQuiz() {
    const total = state.questions.length;
    const question = state.questions[state.currentIndex];
    const answeredCount = Object.keys(state.answers).length;
    const reviewCount = state.review.length;
    const progressPercent = Math.round((answeredCount / total) * 100);
    const timePercent = Math.round((state.timeLeft / QUIZ_DURATION) * 100);
    const reviewPercent = Math.round((reviewCount / total) * 100);

    questionCounter.textContent = `Questão ${state.currentIndex + 1} de ${total}`;
    questionText.textContent = question.prompt;
    questionStatus.textContent = state.review.includes(question.id) ? 'Marcada para revisar' : (state.answers[question.id] !== undefined ? 'Respondida' : 'Em aberto');
    progressLabel.textContent = `${answeredCount}/${total} respondidas`;

    optionList.innerHTML = '';
    question.options.forEach((option, index) => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'option-btn';
      if (state.answers[question.id] === index) {
        button.classList.add('selected');
      }
      if (state.review.includes(question.id)) {
        button.classList.add('review');
      }
      button.innerHTML = `<span class="option-label">${String.fromCharCode(65 + index)})</span><span>${option}</span>`;
      button.addEventListener('click', () => {
        state.answers[question.id] = index;
        saveState(state);
        renderQuiz();
      });
      optionList.appendChild(button);
    });

    questionNav.innerHTML = '';
    state.questions.forEach((item, index) => {
      const button = document.createElement('button');
      button.type = 'button';
      button.textContent = index + 1;
      if (index === state.currentIndex) {
        button.classList.add('active');
      }
      if (state.answers[item.id] !== undefined) {
        button.classList.add('answered');
      }
      if (state.review.includes(item.id)) {
        button.classList.add('review');
      }
      button.addEventListener('click', () => {
        state.currentIndex = index;
        saveState(state);
        renderQuiz();
      });
      questionNav.appendChild(button);
    });

    prevButton.disabled = state.currentIndex === 0;
    nextButton.style.display = state.currentIndex === total - 1 ? 'none' : 'inline-flex';
    nextButton.disabled = state.currentIndex === total - 1;
    toggleReviewButton.textContent = state.review.includes(question.id) ? 'Remover da revisão' : 'Marcar para revisar';

    progressBar.style.width = `${progressPercent}%`;
    timeBar.style.width = `${Math.max(0, timePercent)}%`;
    reviewBar.style.width = `${reviewPercent}%`;
    timerElement.textContent = `${String(Math.floor(state.timeLeft / 60)).padStart(2, '0')}:${String(state.timeLeft % 60).padStart(2, '0')}`;
  }

  function stepTime() {
    state.timeLeft -= 1;
    if (state.timeLeft <= 0) {
      state.timeLeft = 0;
      saveState(state);
      finishQuiz(state, 'timeout');
      return;
    }
    saveState(state);
    renderQuiz();
  }

  prevButton.addEventListener('click', () => {
    if (state.currentIndex > 0) {
      state.currentIndex -= 1;
      saveState(state);
      renderQuiz();
    }
  });

  nextButton.addEventListener('click', () => {
    if (state.currentIndex < state.questions.length - 1) {
      state.currentIndex += 1;
      saveState(state);
      renderQuiz();
    }
  });

  toggleReviewButton.addEventListener('click', () => {
    const question = state.questions[state.currentIndex];
    const index = state.review.indexOf(question.id);
    if (index >= 0) {
      state.review.splice(index, 1);
    } else {
      state.review.push(question.id);
    }
    saveState(state);
    renderQuiz();
  });

  finishButton?.addEventListener('click', () => finishQuiz(state, 'manual'));
  finishHeaderButton?.addEventListener('click', () => finishQuiz(state, 'manual'));
  finishHeaderLink?.addEventListener('click', () => finishQuiz(state, 'manual'));

  renderQuiz();
  window.setInterval(stepTime, 1000);
}

function initResultsPage() {
  const resultTitle = document.getElementById('resultTitle');
  const resultScore = document.getElementById('resultScore');
  const resultSummary = document.getElementById('resultSummary');
  const resultMeter = document.getElementById('resultMeter');
  const reviewList = document.getElementById('reviewList');
  const resultStats = document.getElementById('resultStats');

  if (!resultTitle || !resultScore || !resultSummary || !reviewList || !resultStats) {
    return;
  }

  const storedResult = parseStoredResult();
  if (!storedResult) {
    resultTitle.textContent = 'Ainda não há resultado para mostrar.';
    resultScore.textContent = '0%';
    resultSummary.textContent = 'Complete o simulado para gerar sua análise.';
    resultMeter.style.width = '0%';
    reviewList.innerHTML = '<p class="result-empty">Complete o simulado para ver a revisão detalhada.</p>';
    return;
  }

  resultTitle.textContent = `${storedResult.performance}! Você fechou a prova com ${storedResult.score}%`;
  resultScore.textContent = `${storedResult.score}%`;
  resultSummary.textContent = storedResult.feedback;
  resultMeter.style.width = `${storedResult.score}%`;

  resultStats.innerHTML = `
    <tr><td>Acertos</td><td>${storedResult.correct}</td></tr>
    <tr><td>Erros</td><td>${storedResult.wrong}</td></tr>
    <tr><td>Brancos</td><td>${storedResult.blank}</td></tr>
    <tr><td>Tempo restante</td><td>${String(Math.floor(storedResult.timeLeft / 60)).padStart(2, '0')}:${String(storedResult.timeLeft % 60).padStart(2, '0')}</td></tr>
    <tr><td>Questões revisadas</td><td>${storedResult.reviewedCount}</td></tr>
  `;

  reviewList.innerHTML = '';
  storedResult.questions.forEach((question) => {
    const card = document.createElement('article');
    card.className = 'review-card';
    const userAnswer = question.selectedIndex === undefined ? 'Sem resposta' : question.options[question.selectedIndex];
    const correctAnswer = question.options[question.correctAnswer];
    card.innerHTML = `
      <div class="review-card-head">
        <h4>${question.prompt}</h4>
        <span class="answer-pill ${question.isCorrect ? 'correct' : 'wrong'}">${question.isCorrect ? 'Acertou' : 'Errou'}</span>
      </div>
      <p><strong>Sua resposta:</strong> ${userAnswer}</p>
      <p><strong>Resposta correta:</strong> ${correctAnswer}</p>
      <p class="review-explanation">${question.explanation}</p>
    `;
    reviewList.appendChild(card);
  });
}

function parseStoredResult() {
  try {
    const stored = localStorage.getItem(RESULT_KEY);
    return stored ? JSON.parse(stored) : null;
  } catch (error) {
    return null;
  }
}

window.addEventListener('DOMContentLoaded', () => {
  if (document.getElementById('questionCounter')) {
    initQuizPage();
  }

  if (document.getElementById('resultTitle')) {
    initResultsPage();
  }
  if (document.getElementById('resultsGrid')) {
    initResultsGallery();
  }

  const swiperContainer = document.querySelector('.plans-carousel.swiper');
  if (!swiperContainer || typeof Swiper === 'undefined') return;

  new Swiper(swiperContainer, {
    loop: true,
    speed: 600,
    grabCursor: true,
    slidesPerView: 1,
    spaceBetween: 24,
    centeredSlides: true,
    autoHeight: false,
    allowTouchMove: true,
    draggable: true,
    autoplay: {
      delay: 3000,
      disableOnInteraction: false,
      pauseOnMouseEnter: true,
    },
    pagination: {
      el: '.swiper-pagination',
      clickable: true,
    },
    navigation: {
      nextEl: '.swiper-button-next',
      prevEl: '.swiper-button-prev',
    },
    breakpoints: {
      760: {
        slidesPerView: 1.2,
        spaceBetween: 28,
      },
      1024: {
        slidesPerView: 1.4,
        spaceBetween: 32,
      },
      1280: {
        slidesPerView: 1.6,
        spaceBetween: 36,
      },
    },
  });
});

const skeletons = document.querySelectorAll('.skeleton');
skeletons.forEach((element) => {
  element.style.minHeight = `${element.dataset.height || 120}px`;
});

// Intersection observer for reveal animations
const animTargets = document.querySelectorAll('.plan-card, .compare-col, .prof-panel, .feature-item');
const io = new IntersectionObserver((entries) => {
  entries.forEach((entry) => {
    if (entry.isIntersecting) {
      entry.target.classList.add('visible');
      io.unobserve(entry.target);
    }
  });
}, { threshold: 0.12 });
animTargets.forEach((el) => io.observe(el));

// Tilt effect on cards (mouse move)
document.querySelectorAll('.plan-card').forEach((card) => {
  const inner = document.createElement('div');
  inner.className = 'card-inner';
  while (card.firstChild) inner.appendChild(card.firstChild);
  card.appendChild(inner);

  card.addEventListener('mousemove', (e) => {
    const rect = card.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width - 0.5;
    const y = (e.clientY - rect.top) / rect.height - 0.5;
    inner.style.transform = `rotateX(${(-y * 6).toFixed(2)}deg) rotateY(${(x * 8).toFixed(2)}deg) translateZ(8px)`;
  });

  card.addEventListener('mouseleave', () => { inner.style.transform = 'none'; });
});

// Purchase modal flow
const modal = document.getElementById('purchaseModal');
const modalTitle = document.getElementById('modalTitle');
const modalBody = document.getElementById('modalBody');
const modalConfirm = document.getElementById('modalConfirm');
const modalCancel = document.getElementById('modalCancel');
const modalClose = document.querySelector('.modal-close');
let modalTarget = null;

if (modal && modalTitle && modalBody && modalConfirm && modalCancel && modalClose) {
  document.querySelectorAll('.plan-btn').forEach((btn) => {
    btn.addEventListener('click', (ev) => {
      ev.preventDefault();
      const plan = btn.dataset.plan || btn.textContent.trim();
      modalTarget = btn.href;
      modalTitle.textContent = `Confirmar: ${plan}`;
      modalBody.textContent = 'Você será direcionado para a página de pagamento com segurança. Deseja continuar?';
      modal.setAttribute('aria-hidden', 'false');
    });
  });

  function closeModal() {
    modal.setAttribute('aria-hidden', 'true');
    modalTarget = null;
  }

  modalConfirm.addEventListener('click', () => {
    if (modalTarget) {
      window.open(modalTarget, '_blank', 'noopener');
      closeModal();
    }
  });
  modalCancel.addEventListener('click', closeModal);
  modalClose.addEventListener('click', closeModal);
  modal.addEventListener('click', (e) => { if (e.target === modal) closeModal(); });

  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeModal(); });
}

// Footer reveal animation
const footerObserver = new IntersectionObserver((entries) => {
  entries.forEach((entry) => {
    if (entry.isIntersecting) {
      entry.target.classList.add('visible');
      footerObserver.unobserve(entry.target);
    }
  });
}, { threshold: 0.18 });

const footerTargets = document.querySelectorAll('.site-footer, .footer-cta-card, .footer-bottom');
footerTargets.forEach((item, index) => {
  item.style.transitionDelay = `${index * 100}ms`;
  footerObserver.observe(item);
});

/* Results gallery: tabs filter + lightbox */
function initResultsGallery() {
  const tabs = document.querySelectorAll('.tab-btn');
  const grid = document.getElementById('resultsGrid');
  if (!grid) return;
  const cards = Array.from(grid.querySelectorAll('.result-card'));

  tabs.forEach((tab) => {
    tab.addEventListener('click', () => {
      tabs.forEach((t) => { t.classList.remove('active'); t.setAttribute('aria-selected', 'false'); });
      tab.classList.add('active');
      tab.setAttribute('aria-selected', 'true');
      const filter = tab.dataset.filter;
      cards.forEach((card) => {
        if (!filter || filter === 'all') {
          card.style.display = 'flex';
        } else if (card.classList.contains(filter)) {
          card.style.display = 'flex';
        } else {
          card.style.display = 'none';
        }
      });
    });
  });

  // Lightbox
  const lightbox = document.getElementById('resultsLightbox');
  const lightboxImage = document.getElementById('lightboxImage');
  const lightboxCaption = document.getElementById('lightboxCaption');
  if (!lightbox || !lightboxImage) return;

  cards.forEach((card) => {
    const img = card.querySelector('.result-thumb');
    if (!img) return;
    img.style.cursor = 'zoom-in';
    img.addEventListener('click', () => {
      lightboxImage.src = img.src;
      lightboxImage.alt = img.alt || '';
      lightboxCaption.textContent = `${card.dataset.name} — ${card.dataset.score} • ${card.dataset.type || ''}`;
      lightbox.setAttribute('aria-hidden', 'false');
    });
  });

  const lbClose = lightbox.querySelector('.modal-close');
  function closeLightbox() {
    lightbox.setAttribute('aria-hidden', 'true');
    lightboxImage.src = '';
    lightboxCaption.textContent = '';
  }
  if (lbClose) lbClose.addEventListener('click', closeLightbox);
  lightbox.addEventListener('click', (e) => { if (e.target === lightbox) closeLightbox(); });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeLightbox(); });
}

/* Modal Premium - Bloqueio de Cartões */
const premiumModal = document.getElementById('premiumModal');
if (premiumModal) {
  // Prevenir click nos botoes dos cartões com .card-bloqueado
  document.body.addEventListener('click', (e) => {
    const cardBloqueado = e.target.closest('.card-bloqueado');
    if (cardBloqueado) {
      e.preventDefault();
      e.stopPropagation();
      premiumModal.classList.add('active');
    }
  }, true);

  // Botões de fechar do modal Premium
  const closeBtns = premiumModal.querySelectorAll('.premium-btn-close');
  closeBtns.forEach((btn) => {
    btn.addEventListener('click', () => {
      premiumModal.classList.remove('active');
    });
  });

  premiumModal.addEventListener('click', (e) => { if (e.target === premiumModal) premiumModal.classList.remove('active'); });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && premiumModal.classList.contains('active')) premiumModal.classList.remove('active'); });
}
