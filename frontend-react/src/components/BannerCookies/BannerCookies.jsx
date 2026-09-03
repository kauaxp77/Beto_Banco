import { useEffect, useRef, useState } from 'react';
import {
  TEXTO_DO_BANNER,
  aceitarTudo,
  jaDecidiu,
  registrar,
  recusarTudo,
} from '../../lib/consentimento';
import './BannerCookies.css';

/**
 * Documento Mestre V4.0 -- Secao 22.
 *
 *   "Banner de cookies com recusa tao facil quanto o aceite. Analytics so
 *    dispara apos consentimento."
 *
 * Decisoes de interface que vem direto dessa exigencia:
 *   - "Recusar" e "Aceitar" tem o mesmo peso visual e estao lado a lado. O
 *     padrao escuro de deixar a recusa como link cinza no rodape e exatamente o
 *     que a LGPD nao aceita como consentimento livre.
 *   - As caixas nascem desmarcadas (secao 16: "caixa pre-marcada nao e
 *     consentimento valido").
 *   - Fechar sem escolher nao existe: nao ha botao de X, e o Esc nao dispensa o
 *     banner -- ausencia de resposta nao pode virar aceite tacito.
 */
export default function BannerCookies() {
  const [visivel, setVisivel] = useState(() => !jaDecidiu());
  const [detalhado, setDetalhado] = useState(false);
  const [analytics, setAnalytics] = useState(false);
  const [marketing, setMarketing] = useState(false);
  const primeiroBotao = useRef(null);

  useEffect(() => {
    if (visivel) primeiroBotao.current?.focus();
  }, [visivel]);

  // Reabre quando o titular revoga o consentimento pelo perfil.
  useEffect(() => {
    const aoMudar = (e) => setVisivel(e.detail === null);
    window.addEventListener('plataforma:consentimento', aoMudar);
    return () => window.removeEventListener('plataforma:consentimento', aoMudar);
  }, []);

  if (!visivel) return null;

  const decidir = (acao) => {
    acao();
    setVisivel(false);
  };

  return (
    <section
      className="banner-cookies"
      role="dialog"
      aria-modal="false"
      aria-labelledby="banner-cookies-titulo"
      aria-describedby="banner-cookies-texto"
    >
      <div className="banner-cookies__conteudo">
        <h2 id="banner-cookies-titulo" className="banner-cookies__titulo">
          Sua privacidade
        </h2>
        <p id="banner-cookies-texto" className="banner-cookies__texto">
          {TEXTO_DO_BANNER}{' '}
          <a href="/legal/politica-privacidade">Politica de privacidade</a>
        </p>

        {detalhado && (
          <fieldset className="banner-cookies__opcoes">
            <legend className="apenas-leitor">Escolha quais cookies autorizar</legend>

            <label className="banner-cookies__opcao banner-cookies__opcao--fixa">
              <input type="checkbox" checked disabled />
              <span>
                <strong>Necessarios</strong>
                <small>Manter voce conectado e concluir compras. Nao podem ser desligados.</small>
              </span>
            </label>

            <label className="banner-cookies__opcao">
              <input
                type="checkbox"
                checked={analytics}
                onChange={(e) => setAnalytics(e.target.checked)}
              />
              <span>
                <strong>Analise</strong>
                <small>Entender quais paginas e aulas sao mais usadas.</small>
              </span>
            </label>

            <label className="banner-cookies__opcao">
              <input
                type="checkbox"
                checked={marketing}
                onChange={(e) => setMarketing(e.target.checked)}
              />
              <span>
                <strong>Marketing</strong>
                <small>Medir o resultado das nossas campanhas.</small>
              </span>
            </label>
          </fieldset>
        )}

        <div className="banner-cookies__acoes">
          {/* Recusar vem primeiro na ordem de tabulacao e tem o mesmo peso visual. */}
          <button
            ref={primeiroBotao}
            type="button"
            className="botao botao--secundario"
            onClick={() => decidir(recusarTudo)}
          >
            Recusar todos
          </button>

          {detalhado ? (
            <button
              type="button"
              className="botao botao--secundario"
              onClick={() => decidir(() => registrar({ analytics, marketing }))}
            >
              Salvar escolha
            </button>
          ) : (
            <button
              type="button"
              className="botao botao--texto"
              onClick={() => setDetalhado(true)}
              aria-expanded={detalhado}
            >
              Escolher
            </button>
          )}

          <button
            type="button"
            className="botao botao--primario"
            onClick={() => decidir(aceitarTudo)}
          >
            Aceitar todos
          </button>
        </div>
      </div>
    </section>
  );
}
