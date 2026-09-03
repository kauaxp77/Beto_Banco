/**
 * Cliente da API. Documento Mestre V4.0, secao 19.
 *
 * Convencoes que este arquivo implementa do lado do cliente:
 *   - Base /api/v1; JSON em snake_case; datas ISO 8601 com fuso.
 *   - Erro no formato RFC 7807 -- convertido em ErroDaApi, com `tipo` estavel.
 *   - Idempotency-Key em todo POST que cria pedido ou pagamento.
 *   - Access token de 15 min renovado por refresh rotativo (secao 20).
 */

const BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

const CHAVE_ACCESS = 'plataforma.access_token';
const CHAVE_REFRESH = 'plataforma.refresh_token';

/**
 * Erro tipado. O `tipo` vem do campo RFC 7807 `type` e e a chave que a interface
 * usa para decidir o que mostrar -- o `detail` e texto para humano e pode mudar
 * sem quebrar a tela.
 */
export class ErroDaApi extends Error {
  constructor({ status, tipo, titulo, detalhe, erros }) {
    super(detalhe || titulo || 'Falha na requisicao');
    this.name = 'ErroDaApi';
    this.status = status;
    this.tipo = tipo;
    this.titulo = titulo;
    this.detalhe = detalhe;
    this.erros = erros || [];
  }

  /** Erro de campo, para pintar o formulario sem precisar adivinhar pelo texto. */
  erroDoCampo(campo) {
    return this.erros.find((e) => e.campo === campo)?.mensagem;
  }
}

export const tokens = {
  access: () => localStorage.getItem(CHAVE_ACCESS),
  refresh: () => localStorage.getItem(CHAVE_REFRESH),
  guardar({ access_token, refresh_token }) {
    if (access_token) localStorage.setItem(CHAVE_ACCESS, access_token);
    if (refresh_token) localStorage.setItem(CHAVE_REFRESH, refresh_token);
  },
  limpar() {
    localStorage.removeItem(CHAVE_ACCESS);
    localStorage.removeItem(CHAVE_REFRESH);
  },
};

/**
 * Uma renovacao por vez.
 *
 * Sem esta trava, tres chamadas que recebem 401 ao mesmo tempo disparam tres
 * refresh. O segundo e o terceiro usam um token ja rotacionado, o backend trata
 * isso como reuso e derruba a familia inteira (secao 20) -- o usuario e expulso
 * por um comportamento normal do proprio app.
 */
let renovacaoEmCurso = null;

async function renovarSessao() {
  const refresh_token = tokens.refresh();
  if (!refresh_token) throw new ErroDaApi({ status: 401, tipo: 'sem-sessao' });

  if (!renovacaoEmCurso) {
    renovacaoEmCurso = fetch(`${BASE}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token }),
    })
      .then(async (resposta) => {
        if (!resposta.ok) throw await paraErro(resposta);
        const dados = await resposta.json();
        tokens.guardar(dados);
        return dados;
      })
      .finally(() => {
        renovacaoEmCurso = null;
      });
  }
  return renovacaoEmCurso;
}

async function paraErro(resposta) {
  let corpo = {};
  try {
    corpo = await resposta.json();
  } catch {
    // Resposta sem corpo JSON (502 de proxy, timeout de gateway). O status ainda
    // e informacao util, entao seguimos com ele.
  }
  return new ErroDaApi({
    status: resposta.status,
    tipo: (corpo.type || '').split('/').pop() || `http-${resposta.status}`,
    titulo: corpo.title,
    detalhe: corpo.detail,
    erros: corpo.errors,
  });
}

/**
 * @param {object} opcoes
 * @param {boolean} [opcoes.autenticado] envia o access token e renova no 401
 * @param {string}  [opcoes.idempotencyKey] obrigatorio em POST /pedidos (secao 19)
 */
async function requisitar(caminho, { metodo = 'GET', corpo, autenticado = false, idempotencyKey, _retentativa } = {}) {
  const cabecalhos = { Accept: 'application/json' };
  if (corpo !== undefined) cabecalhos['Content-Type'] = 'application/json';
  if (idempotencyKey) cabecalhos['Idempotency-Key'] = idempotencyKey;
  if (autenticado) {
    const access = tokens.access();
    if (access) cabecalhos.Authorization = `Bearer ${access}`;
  }

  const resposta = await fetch(`${BASE}${caminho}`, {
    method: metodo,
    headers: cabecalhos,
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  });

  if (resposta.status === 401 && autenticado && !_retentativa) {
    try {
      await renovarSessao();
      return requisitar(caminho, { metodo, corpo, autenticado, idempotencyKey, _retentativa: true });
    } catch {
      tokens.limpar();
      // A tela de sessao expirada (secao 06) reage a este evento; nao redirecionamos
      // daqui para nao interromper um formulario preenchido pela metade.
      window.dispatchEvent(new CustomEvent('plataforma:sessao-expirada'));
      throw new ErroDaApi({ status: 401, tipo: 'sessao-expirada' });
    }
  }

  if (!resposta.ok) throw await paraErro(resposta);
  if (resposta.status === 204) return null;
  return resposta.json();
}

/** Chave de idempotencia por tentativa de compra. Sobrevive a recarga da pagina. */
export function chaveDeIdempotencia(escopo) {
  const chave = `plataforma.idem.${escopo}`;
  let valor = sessionStorage.getItem(chave);
  if (!valor) {
    valor = crypto.randomUUID();
    sessionStorage.setItem(chave, valor);
  }
  return valor;
}

export function descartarChaveDeIdempotencia(escopo) {
  sessionStorage.removeItem(`plataforma.idem.${escopo}`);
}

export const api = {
  // Autenticacao (secoes 19 e 20)
  login: (credenciais) =>
    requisitar('/auth/login', { metodo: 'POST', corpo: credenciais }).then((dados) => {
      tokens.guardar(dados);
      return dados;
    }),
  logout: async () => {
    const refresh_token = tokens.refresh();
    if (refresh_token) {
      await requisitar('/auth/logout', { metodo: 'POST', corpo: { refresh_token } }).catch(() => {});
    }
    tokens.limpar();
  },
  recuperarSenha: (email) => requisitar('/auth/senha/recuperar', { metodo: 'POST', corpo: { email } }),
  redefinirSenha: (token, nova_senha) =>
    requisitar('/auth/senha/redefinir', { metodo: 'POST', corpo: { token, nova_senha } }),
  sessoes: () => requisitar('/auth/sessoes', { autenticado: true }),
  encerrarSessao: (id) => requisitar(`/auth/sessoes/${id}`, { metodo: 'DELETE', autenticado: true }),

  // Catalogo publico (secoes 07, 11 e 19)
  carreiras: () => requisitar('/carreiras'),
  concursos: (parametros) => requisitar(`/concursos?${new URLSearchParams(limpar(parametros))}`),
  concurso: (slug) => requisitar(`/concursos/${slug}`),
  cursos: (parametros) => requisitar(`/cursos?${new URLSearchParams(limpar(parametros))}`),
  curso: (slug) => requisitar(`/cursos/${slug}`),
  buscar: (parametros) => requisitar(`/busca?${new URLSearchParams(limpar(parametros))}`),

  // Comercial (secao 12)
  criarPedido: (pedido) =>
    requisitar('/pedidos', {
      metodo: 'POST',
      corpo: pedido,
      idempotencyKey: chaveDeIdempotencia('checkout'),
    }),

  // Area do aluno (secoes 09, 10 e 19)
  minhasMatriculas: () => requisitar('/me/matriculas', { autenticado: true }),
  playerDaAula: (id) => requisitar(`/aulas/${id}/player`, { autenticado: true }),
  marcarProgresso: (id, segundos_vistos, concluido = false) =>
    requisitar(`/aulas/${id}/progresso`, {
      metodo: 'PUT',
      corpo: { segundos_vistos, concluido },
      autenticado: true,
    }),

  // LGPD -- portal do titular (secao 22)
  documentoLegal: (tipo) => requisitar(`/legal/${tipo}`),
  exportarMeusDados: () => requisitar('/me/dados', { autenticado: true }),
  meusConsentimentos: () => requisitar('/me/consentimentos', { autenticado: true }),
  registrarConsentimento: (finalidade, concedido, texto_aceito) =>
    requisitar('/me/consentimentos', {
      metodo: 'POST',
      corpo: { finalidade, concedido, texto_aceito },
      autenticado: true,
    }),
  excluirMinhaConta: () => requisitar('/me', { metodo: 'DELETE', autenticado: true }),
};

/** Remove chaves vazias para nao mandar `?banca=&status=` ao backend. */
function limpar(parametros = {}) {
  return Object.fromEntries(
    Object.entries(parametros).filter(([, v]) => v !== undefined && v !== null && v !== ''),
  );
}
