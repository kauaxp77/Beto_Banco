import { Component } from 'react';
import { ErroInterno } from '../../pages/Excecoes/TelasDeExcecao';

/**
 * Documento Mestre V4.0 -- Secao 06 (tela de excecao 500) e secao 23
 * (observabilidade: "Erros: Sentry no front e no back, com release atrelado ao
 * commit").
 *
 * Sem este limite, uma unica excecao de render desmonta a arvore inteira e o
 * aluno fica com a tela branca -- sem mensagem, sem caminho de volta e sem
 * nenhum registro do que aconteceu.
 */
export default class LimiteDeErro extends Component {
  constructor(props) {
    super(props);
    this.state = { falhou: false, traceId: null };
  }

  static getDerivedStateFromError() {
    return { falhou: true };
  }

  componentDidCatch(erro, info) {
    // O trace_id do backend chega no cabecalho X-Trace-Id; quando a falha e
    // puramente de render nao existe um, e geramos o nosso para que o usuario
    // tenha um codigo a informar no suporte.
    const traceId = window.__ultimoTraceId || crypto.randomUUID();
    this.setState({ traceId });

    if (window.Sentry) {
      window.Sentry.captureException(erro, { extra: { ...info, trace_id: traceId } });
    } else {
      console.error(`[${traceId}] Falha de render nao tratada`, erro, info);
    }
  }

  render() {
    if (this.state.falhou) {
      return <ErroInterno traceId={this.state.traceId} />;
    }
    return this.props.children;
  }
}
