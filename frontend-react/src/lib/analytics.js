/**
 * Documento Mestre V4.0 -- Secao 25, Analytics.
 *
 * A V3.0 pedia "conversao" e "ticket medio" no dashboard sem nenhuma
 * instrumentacao que produzisse esses numeros. Os oito eventos da secao 25 estao
 * declarados abaixo com as propriedades que cada um exige, para que a mesma
 * pergunta nunca seja respondida por dois eventos com nomes diferentes.
 *
 * Regra que atravessa o arquivo inteiro (secao 25, ultima linha):
 *   "Nenhum evento dispara antes do consentimento de cookie."
 * Antes do aceite, os eventos ficam em uma fila em memoria; se o titular
 * recusar, a fila e descartada sem sair do navegador.
 */

import { FINALIDADES, permitido } from './consentimento';

/** Secao 25 -- evento: momento -> propriedades esperadas. */
export const EVENTOS = {
  view_curso: ['curso', 'carreira', 'origem'],
  lead_capturado: ['isca', 'pagina'],
  checkout_iniciado: ['curso', 'valor', 'cupom'],
  compra_aprovada: ['valor', 'metodo', 'curso'],
  aula_iniciada: ['aula', 'curso', 'dispositivo'],
  aula_concluida: ['aula', 'tempo_total'],
  simulado_enviado: ['simulado', 'nota', 'duracao'],
  redacao_enviada: ['tema', 'cota_restante'],
};

const CHAVE_ANONIMO = 'plataforma.analytics.anonimo_id';
const filaPendente = [];

/** UTM padronizada em toda campanha (secao 25). Lida uma vez, na primeira visita. */
function utmDaSessao() {
  const guardada = sessionStorage.getItem('plataforma.utm');
  if (guardada) return JSON.parse(guardada);

  const parametros = new URLSearchParams(window.location.search);
  const utm = {};
  ['utm_source', 'utm_medium', 'utm_campaign', 'utm_content', 'utm_term'].forEach((chave) => {
    const valor = parametros.get(chave);
    if (valor) utm[chave] = valor;
  });
  sessionStorage.setItem('plataforma.utm', JSON.stringify(utm));
  return utm;
}

/**
 * Id anonimo por navegador. So e criado depois do consentimento -- gerar antes
 * ja seria identificar o visitante sem autorizacao.
 */
function anonimoId() {
  let id = localStorage.getItem(CHAVE_ANONIMO);
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem(CHAVE_ANONIMO, id);
  }
  return id;
}

function despachar(evento) {
  // Secao 25 -- "GA4 + Meta Pixel via server-side tagging para sobreviver a
  // bloqueadores". O envio sai para o nosso proprio backend, que repassa; assim
  // o bloqueador de anuncios do aluno nao apaga a metrica de negocio.
  const url = `${import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1'}/eventos`;
  const corpo = JSON.stringify({ ...evento, anonimo_id: anonimoId(), utm: utmDaSessao() });

  // sendBeacon sobrevive ao fechamento da aba -- importante para aula_concluida
  // e checkout_iniciado, que acontecem justamente na saida.
  if (navigator.sendBeacon) {
    navigator.sendBeacon(url, new Blob([corpo], { type: 'application/json' }));
    return;
  }
  fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: corpo, keepalive: true })
    .catch(() => {
      // Analytics nunca pode quebrar a tela. Evento perdido e evento perdido.
    });
}

/**
 * Registra um evento da secao 25.
 *
 * @param {keyof EVENTOS} nome
 * @param {object} propriedades
 */
export function registrar(nome, propriedades = {}) {
  if (!EVENTOS[nome]) {
    // Evento fora da lista da secao 25 quase sempre e nome divergente para algo
    // que ja existe. Avisa em desenvolvimento e nao envia.
    if (import.meta.env.DEV) {
      console.warn(`[analytics] evento "${nome}" nao esta na secao 25. Use um dos: ${Object.keys(EVENTOS).join(', ')}`);
    }
    return;
  }

  const evento = { nome, propriedades, ocorrido_em: new Date().toISOString() };

  if (!permitido(FINALIDADES.COOKIE_ANALYTICS)) {
    filaPendente.push(evento);
    return;
  }
  despachar(evento);
}

/**
 * Chamado uma vez na subida do app. Ao registrar o consentimento, esvazia a fila
 * acumulada; ao recusar, descarta -- o que estava na fila nunca sai daqui.
 */
export function iniciarAnalytics() {
  window.addEventListener('plataforma:consentimento', (e) => {
    const concedeu = e.detail?.[FINALIDADES.COOKIE_ANALYTICS] === true;
    const pendentes = filaPendente.splice(0, filaPendente.length);
    if (concedeu) pendentes.forEach(despachar);
  });

  if (permitido(FINALIDADES.COOKIE_ANALYTICS)) {
    filaPendente.splice(0, filaPendente.length).forEach(despachar);
  }
}
