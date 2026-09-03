/**
 * Documento Mestre V4.0 -- Secao 22, LGPD.
 *
 *   "Banner de cookies com recusa tao facil quanto o aceite. Analytics so
 *    dispara apos consentimento."
 *   "Caixa pre-marcada nao e consentimento valido." (secao 16)
 *
 * Duas regras se traduzem em codigo aqui:
 *   1. O padrao e recusa. Enquanto nao ha registro, `permitido()` devolve false
 *      para tudo que nao seja estritamente necessario -- ausencia de resposta
 *      nunca vira aceite.
 *   2. Recusar custa um clique, igual a aceitar. Nada de "gerenciar preferencias"
 *      escondido atras de dois niveis de dialogo.
 */

const CHAVE = 'plataforma.consentimento.v1';

/** Versao do texto exibido. Trocar o texto exige novo consentimento. */
export const VERSAO_TEXTO = '1.0.0';

export const FINALIDADES = {
  /** Sessao, seguranca e checkout. Nao depende de consentimento. */
  NECESSARIO: 'NECESSARIO',
  COOKIE_ANALYTICS: 'COOKIE_ANALYTICS',
  COOKIE_MARKETING: 'COOKIE_MARKETING',
};

function ler() {
  try {
    const bruto = localStorage.getItem(CHAVE);
    if (!bruto) return null;
    const registro = JSON.parse(bruto);
    // Texto novo, consentimento novo: o titular concordou com outra coisa.
    if (registro.versao !== VERSAO_TEXTO) return null;
    return registro;
  } catch {
    return null;
  }
}

/** true apenas quando ha decisao registrada para a versao de texto atual. */
export function jaDecidiu() {
  return ler() !== null;
}

export function permitido(finalidade) {
  if (finalidade === FINALIDADES.NECESSARIO) return true;
  const registro = ler();
  return registro ? registro[finalidade] === true : false;
}

/**
 * Grava a decisao e avisa a aplicacao. O analytics escuta este evento para
 * comecar (ou parar) a enviar -- nada e disparado antes disso.
 */
export function registrar({ analytics, marketing }) {
  const registro = {
    versao: VERSAO_TEXTO,
    [FINALIDADES.COOKIE_ANALYTICS]: analytics === true,
    [FINALIDADES.COOKIE_MARKETING]: marketing === true,
    decidido_em: new Date().toISOString(),
  };
  localStorage.setItem(CHAVE, JSON.stringify(registro));
  window.dispatchEvent(new CustomEvent('plataforma:consentimento', { detail: registro }));
  return registro;
}

export const aceitarTudo = () => registrar({ analytics: true, marketing: true });
export const recusarTudo = () => registrar({ analytics: false, marketing: false });

/**
 * Secao 22 -- "revogar consentimento" e um direito do titular, entao a revogacao
 * precisa ser tao acessivel quanto o aceite, e nao um pedido por e-mail. Apaga o
 * registro local e limpa o que ja tinha sido gravado sob aquele consentimento.
 */
export function revogar() {
  localStorage.removeItem(CHAVE);
  Object.keys(localStorage)
    .filter((chave) => chave.startsWith('plataforma.analytics.'))
    .forEach((chave) => localStorage.removeItem(chave));
  window.dispatchEvent(new CustomEvent('plataforma:consentimento', { detail: null }));
}

/** Texto exibido no banner. Vai junto com o registro de aceite no servidor. */
export const TEXTO_DO_BANNER =
  'Usamos cookies necessarios para manter voce conectado e concluir compras. ' +
  'Com sua autorizacao, tambem usamos cookies de analise para entender como a ' +
  'plataforma e usada e de marketing para medir nossas campanhas. Voce pode ' +
  'mudar essa escolha a qualquer momento no seu perfil.';
