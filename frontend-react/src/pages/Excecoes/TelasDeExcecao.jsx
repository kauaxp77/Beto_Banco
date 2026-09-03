import { Link } from 'react-router-dom';
import './TelasDeExcecao.css';

/**
 * Documento Mestre V4.0 -- Secao 06, "Telas de excecao".
 *
 *   "Precisam ser desenhadas, nao improvisadas: 404, 500, sessao expirada,
 *    curso sem aula publicada, busca sem resultado, video indisponivel,
 *    pagamento recusado, offline."
 *
 * As oito estao aqui, montadas sobre a mesma base para que compartilhem
 * espacamento, hierarquia e comportamento de foco. Cada uma responde tres
 * perguntas na mesma ordem: o que aconteceu, se a culpa e do usuario, e qual e
 * o proximo passo concreto. Uma tela de erro sem proximo passo e um beco.
 */
function Excecao({ codigo, titulo, explicacao, acoes, tom = 'neutra', children }) {
  return (
    <main className="excecao" id="conteudo">
      <div className="excecao__caixa">
        {codigo && <p className={`excecao__codigo excecao__codigo--${tom}`}>{codigo}</p>}
        {/* tabIndex -1 + autofocus leva o leitor de tela direto ao titulo do erro,
            em vez de recomecar a leitura pelo topo da pagina. */}
        <h1 className="excecao__titulo" tabIndex={-1} ref={(no) => no?.focus()}>
          {titulo}
        </h1>
        <p className="excecao__explicacao">{explicacao}</p>
        {children}
        <div className="excecao__acoes">{acoes}</div>
      </div>
    </main>
  );
}

/* ---------------------------------------------------------------------------
   404
   --------------------------------------------------------------------------- */
export function PaginaNaoEncontrada() {
  return (
    <Excecao
      codigo="404"
      titulo="Esta pagina nao existe"
      explicacao="O endereco pode ter mudado, ou o link que trouxe voce aqui esta desatualizado."
      acoes={
        <>
          <Link className="botao botao--primario" to="/">Ir para a home</Link>
          <Link className="botao botao--secundario" to="/busca">Buscar concursos e cursos</Link>
        </>
      }
    />
  );
}

/* ---------------------------------------------------------------------------
   500
   --------------------------------------------------------------------------- */
export function ErroInterno({ traceId }) {
  return (
    <Excecao
      codigo="500"
      tom="critica"
      titulo="Alguma coisa quebrou do nosso lado"
      explicacao="Nao foi voce. A falha ja foi registrada e a equipe foi avisada automaticamente."
      acoes={
        <>
          <button type="button" className="botao botao--primario" onClick={() => window.location.reload()}>
            Tentar de novo
          </button>
          <Link className="botao botao--secundario" to="/">Ir para a home</Link>
        </>
      }
    >
      {/* O trace_id (secao 23) percorre a requisicao inteira. Mostrar ao usuario
          transforma "deu erro" em um chamado que o suporte consegue rastrear. */}
      {traceId && (
        <p className="excecao__trace">
          Codigo da ocorrencia: <code className="dado">{traceId}</code>
        </p>
      )}
    </Excecao>
  );
}

/* ---------------------------------------------------------------------------
   Sessao expirada -- secao 20, access de 15 min
   --------------------------------------------------------------------------- */
export function SessaoExpirada() {
  return (
    <Excecao
      titulo="Sua sessao expirou"
      explicacao="Por seguranca, encerramos sessoes paradas ha muito tempo. Entre de novo para continuar de onde parou."
      acoes={<Link className="botao botao--primario" to="/login">Entrar novamente</Link>}
    />
  );
}

/* ---------------------------------------------------------------------------
   Curso sem aula publicada
   --------------------------------------------------------------------------- */
export function CursoSemAula({ tituloDoCurso }) {
  return (
    <Excecao
      titulo="Este curso ainda nao tem aulas publicadas"
      explicacao={
        `As aulas de ${tituloDoCurso || 'este curso'} estao em producao. ` +
        'Voce recebe um e-mail assim que a primeira for ao ar -- seu prazo de acesso so comeca a contar a partir dela.'
      }
      acoes={<Link className="botao botao--secundario" to="/dashboard">Voltar para minha jornada</Link>}
    />
  );
}

/* ---------------------------------------------------------------------------
   Busca sem resultado
   --------------------------------------------------------------------------- */
export function BuscaSemResultado({ termo, aoLimpar }) {
  return (
    <Excecao
      titulo="Nenhum resultado para esta busca"
      explicacao={
        termo
          ? `Nao encontramos nada para "${termo}". Tente um termo mais curto, ou remova alguns filtros.`
          : 'Tente um termo mais curto, ou remova alguns filtros.'
      }
      acoes={
        <>
          {aoLimpar && (
            <button type="button" className="botao botao--secundario" onClick={aoLimpar}>
              Limpar filtros
            </button>
          )}
          <Link className="botao botao--texto" to="/concursos">Ver todos os concursos</Link>
        </>
      }
    />
  );
}

/* ---------------------------------------------------------------------------
   Video indisponivel -- secao 10
   --------------------------------------------------------------------------- */
export function VideoIndisponivel({ aoTentarNovamente }) {
  return (
    <Excecao
      tom="atencao"
      titulo="O video nao carregou"
      explicacao="Pode ser instabilidade na sua conexao ou no servidor de video. Seu progresso na aula esta salvo."
      acoes={
        <>
          <button type="button" className="botao botao--primario" onClick={aoTentarNovamente}>
            Recarregar o player
          </button>
          <Link className="botao botao--secundario" to="/suporte">Falar com o suporte</Link>
        </>
      }
    />
  );
}

/* ---------------------------------------------------------------------------
   Pagamento recusado -- secao 12
   --------------------------------------------------------------------------- */
export function PagamentoRecusado({ aoTentarNovamente }) {
  return (
    <Excecao
      tom="critica"
      titulo="O pagamento nao foi aprovado"
      explicacao="O banco emissor recusou a transacao. Isso quase sempre e limite disponivel ou algum dado divergente do cadastro do cartao -- nao houve cobranca."
      acoes={
        <>
          <button type="button" className="botao botao--primario" onClick={aoTentarNovamente}>
            Tentar outro pagamento
          </button>
          <Link className="botao botao--secundario" to="/suporte">Falar com o suporte</Link>
        </>
      }
    >
      <ul className="excecao__lista">
        <li>Confira o limite disponivel e os dados do cartao.</li>
        <li>Pix a vista tem 10% de desconto e aprovacao imediata.</li>
        <li>Se preferir, parcele em ate 12 vezes no cartao.</li>
      </ul>
    </Excecao>
  );
}

/* ---------------------------------------------------------------------------
   Offline -- secao 09, PWA
   --------------------------------------------------------------------------- */
export function SemConexao() {
  return (
    <Excecao
      tom="atencao"
      titulo="Voce esta sem conexao"
      explicacao="Os materiais que voce ja baixou continuam disponiveis. As aulas em video precisam de internet."
      acoes={
        <button type="button" className="botao botao--primario" onClick={() => window.location.reload()}>
          Tentar de novo
        </button>
      }
    />
  );
}
