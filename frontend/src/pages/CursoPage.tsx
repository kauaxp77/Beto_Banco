import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/http'
import { Button } from '../ui/basics'
import { nomeAmigavel } from '../ui/format'
import { QueryBoundary } from '../ui/QueryBoundary'
import { playerDe } from '../ui/video'
import './cursos.css'

interface MaterialItem {
  id: string
  title: string
  url: string
}

interface LessonItem {
  id: string
  title: string
  description: string | null
  videoUrl: string | null
  durationSeconds: number | null
  position: number
  completed: boolean
  materials: MaterialItem[]
  questionCount: number
}

interface ModuleItem {
  id: string
  title: string
  position: number
  lessons: LessonItem[]
}

interface CourseDetail {
  id: string
  title: string
  description: string | null
  coverUrl: string | null
  modules: ModuleItem[]
}

interface CommentItem {
  id: string
  parentId: string | null
  body: string
  authorName: string
  instructor: boolean
  mine: boolean
  createdAt: string
}

interface Discussion {
  comments: CommentItem[]
  helpfulCount: number
  notHelpfulCount: number
  myRating: boolean | null
}

function duracao(segundos: number | null): string {
  if (!segundos) return ''
  return `${Math.round(segundos / 60)} min`
}

/** Busca sem acento e sem caixa: "financeiro" acha "Financeiro". */
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()
}

function Player({ aula }: { aula: LessonItem }) {
  const player = playerDe(aula.videoUrl)
  return (
    <div className="aula-player">
      {player === null ? (
        <p className="sem-video">Esta aula ainda não tem vídeo publicado.</p>
      ) : player.tipo === 'iframe' ? (
        <iframe
          key={player.src}
          src={player.src}
          title={aula.title}
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
          allowFullScreen
        />
      ) : (
        <video key={player.src} src={player.src} controls />
      )}
    </div>
  )
}

interface QuizQuestion {
  id: string
  statement: string
  options: string[]
  position: number
}

interface QuizAttemptSummary {
  id: string
  correctCount: number
  totalCount: number
  createdAt: string
}

interface QuizData {
  questions: QuizQuestion[]
  myAttempts: QuizAttemptSummary[]
}

interface QuizResultItem {
  questionId: string
  myIndex: number
  correctIndex: number
  correct: boolean
  explanation: string | null
}

interface QuizResult {
  correctCount: number
  totalCount: number
  scorePct: number
  items: QuizResultItem[]
}

const LETRAS = ['A', 'B', 'C', 'D', 'E']

function Simulado({ lessonId }: { lessonId: string }) {
  const queryClient = useQueryClient()
  const [respostas, setRespostas] = useState<Record<string, number>>({})
  const [resultado, setResultado] = useState<QuizResult | null>(null)

  const query = useQuery({
    queryKey: ['quiz', lessonId],
    queryFn: () => api<QuizData>(`/courses/lessons/${lessonId}/quiz`),
  })

  const entregar = useMutation({
    mutationFn: () =>
      api<QuizResult>(`/courses/lessons/${lessonId}/quiz/submit`, {
        method: 'POST',
        body: JSON.stringify({
          answers: Object.entries(respostas).map(([questionId, answerIndex]) => ({
            questionId,
            answerIndex,
          })),
        }),
      }),
    onSuccess: (r) => {
      setResultado(r)
      void queryClient.invalidateQueries({ queryKey: ['quiz', lessonId] })
      void queryClient.invalidateQueries({ queryKey: ['curso'] })
      void queryClient.invalidateQueries({ queryKey: ['meus-cursos'] })
      void queryClient.invalidateQueries({ queryKey: ['minhas-stats'] })
    },
  })

  return (
    <QueryBoundary query={query}>
      {(quiz) => {
        if (quiz.questions.length === 0) return null
        const correcaoDe = (id: string) =>
          resultado?.items.find((i) => i.questionId === id) ?? null
        const todasRespondidas = quiz.questions.every((q) => respostas[q.id] !== undefined)

        return (
          <section className="simulado" aria-label="Questões da aula">
            <div className="simulado-cabeca">
              <h3>Questões da aula ({quiz.questions.length})</h3>
              {quiz.myAttempts.length > 0 && !resultado && (
                <span className="dim-txt">
                  Última tentativa: {quiz.myAttempts[0].correctCount}/
                  {quiz.myAttempts[0].totalCount} acertos
                </span>
              )}
            </div>

            {resultado && (
              <p
                className={`simulado-placar ${resultado.scorePct >= 70 ? 'bom' : 'ruim'}`}
                role="status"
              >
                Você acertou {resultado.correctCount} de {resultado.totalCount} (
                {resultado.scorePct}%)
                {resultado.scorePct >= 70 ? ' — acima da meta de 70%! 🎯' : ' — siga treinando!'}
              </p>
            )}

            <ol className="questoes">
              {quiz.questions.map((q, numero) => {
                const correcao = correcaoDe(q.id)
                return (
                  <li key={q.id} className="questao">
                    <p className="enunciado">
                      <strong>Questão {numero + 1}.</strong> {q.statement}
                    </p>
                    <div role="radiogroup" aria-label={`Alternativas da questão ${numero + 1}`}>
                      {q.options.map((opcao, i) => {
                        const marcada = respostas[q.id] === i
                        const classe =
                          correcao === null
                            ? marcada
                              ? 'alternativa marcada'
                              : 'alternativa'
                            : i === correcao.correctIndex
                              ? 'alternativa certa'
                              : marcada
                                ? 'alternativa errada'
                                : 'alternativa'
                        return (
                          <button
                            key={i}
                            type="button"
                            role="radio"
                            aria-checked={marcada}
                            className={classe}
                            disabled={correcao !== null}
                            onClick={() => setRespostas({ ...respostas, [q.id]: i })}
                          >
                            <span className="letra">{LETRAS[i]}</span>
                            {opcao}
                          </button>
                        )
                      })}
                    </div>
                    {correcao?.explanation && (
                      <p className="gabarito-comentado">
                        <strong>Comentário:</strong> {correcao.explanation}
                      </p>
                    )}
                  </li>
                )
              })}
            </ol>

            {resultado === null ? (
              <Button
                disabled={!todasRespondidas || entregar.isPending}
                onClick={() => entregar.mutate()}
              >
                {entregar.isPending
                  ? 'Corrigindo…'
                  : todasRespondidas
                    ? 'Entregar respostas'
                    : `Responda todas para entregar (${Object.keys(respostas).length}/${quiz.questions.length})`}
              </Button>
            ) : (
              <Button
                ghost
                onClick={() => {
                  setRespostas({})
                  setResultado(null)
                }}
              >
                Refazer questões
              </Button>
            )}
          </section>
        )
      }}
    </QueryBoundary>
  )
}

function Discussao({ lessonId }: { lessonId: string }) {
  const queryClient = useQueryClient()
  const [texto, setTexto] = useState('')
  const [respondendo, setRespondendo] = useState<CommentItem | null>(null)

  const query = useQuery({
    queryKey: ['discussao', lessonId],
    queryFn: () => api<Discussion>(`/courses/lessons/${lessonId}/discussion`),
  })

  const invalidar = () =>
    void queryClient.invalidateQueries({ queryKey: ['discussao', lessonId] })

  const comentar = useMutation({
    mutationFn: () =>
      api(`/courses/lessons/${lessonId}/comments`, {
        method: 'POST',
        body: JSON.stringify({ body: texto.trim(), parentId: respondendo?.id ?? null }),
      }),
    onSuccess: () => {
      setTexto('')
      setRespondendo(null)
      invalidar()
    },
  })

  const votar = useMutation({
    mutationFn: (helpful: boolean) =>
      api(`/courses/lessons/${lessonId}/rating`, {
        method: 'PUT',
        body: JSON.stringify({ helpful }),
      }),
    onSuccess: invalidar,
  })

  function enviar(e: FormEvent) {
    e.preventDefault()
    if (texto.trim()) comentar.mutate()
  }

  return (
    <QueryBoundary query={query}>
      {(d) => {
        const raizes = d.comments.filter((c) => c.parentId === null)
        const respostasDe = (id: string) => d.comments.filter((c) => c.parentId === id)
        return (
          <div className="discussao">
            <div className="avaliacao" role="group" aria-label="Esta aula foi útil?">
              <span>Esta aula foi útil?</span>
              <button
                type="button"
                className={`voto ${d.myRating === true ? 'ativo' : ''}`}
                disabled={votar.isPending}
                onClick={() => votar.mutate(true)}
              >
                👍 Sim ({d.helpfulCount})
              </button>
              <button
                type="button"
                className={`voto ${d.myRating === false ? 'ativo' : ''}`}
                disabled={votar.isPending}
                onClick={() => votar.mutate(false)}
              >
                👎 Não ({d.notHelpfulCount})
              </button>
            </div>

            <h3>Dúvidas e comentários</h3>
            {raizes.length === 0 && (
              <p className="bb-state">Nenhum comentário ainda. Puxe a conversa!</p>
            )}
            <ul className="comentarios">
              {raizes.map((c) => (
                <li key={c.id}>
                  <Comentario comentario={c} onResponder={() => setRespondendo(c)} />
                  {respostasDe(c.id).length > 0 && (
                    <ul className="respostas">
                      {respostasDe(c.id).map((r) => (
                        <li key={r.id}>
                          <Comentario comentario={r} />
                        </li>
                      ))}
                    </ul>
                  )}
                </li>
              ))}
            </ul>

            <form className="comentario-form" onSubmit={enviar}>
              {respondendo && (
                <p className="respondendo">
                  Respondendo a <strong>{respondendo.authorName}</strong>{' '}
                  <button type="button" onClick={() => setRespondendo(null)}>
                    cancelar
                  </button>
                </p>
              )}
              <label htmlFor="novo-comentario" className="sr-only">
                Escreva um comentário
              </label>
              <textarea
                id="novo-comentario"
                value={texto}
                onChange={(e) => setTexto(e.target.value)}
                placeholder="Escreva sua dúvida ou comentário…"
                rows={3}
                maxLength={4000}
              />
              <Button type="submit" disabled={!texto.trim() || comentar.isPending}>
                {respondendo ? 'Responder' : 'Comentar'}
              </Button>
            </form>
          </div>
        )
      }}
    </QueryBoundary>
  )
}

function Comentario({
  comentario,
  onResponder,
}: {
  comentario: CommentItem
  onResponder?: () => void
}) {
  return (
    <article className={`comentario ${comentario.instructor ? 'professor' : ''}`}>
      <header>
        <strong>{comentario.authorName}</strong>
        {comentario.instructor && <span className="tag-professor">Professor</span>}
        <time dateTime={comentario.createdAt}>
          {new Date(comentario.createdAt).toLocaleDateString('pt-BR')}
        </time>
      </header>
      <p>{comentario.body}</p>
      {onResponder && (
        <button type="button" className="link-responder" onClick={onResponder}>
          Responder
        </button>
      )}
    </article>
  )
}

export function CursoPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [aulaAtualId, setAulaAtualId] = useState<string | null>(null)
  const [busca, setBusca] = useState('')

  const query = useQuery({
    queryKey: ['curso', id],
    queryFn: () => api<CourseDetail>(`/courses/${id}`),
    enabled: !!id,
  })

  const todasAulas = query.data?.modules.flatMap((m) => m.lessons) ?? []
  // Sem selecao explicita, cai na primeira aula nao concluida (ou na primeira).
  const aulaAtual =
    todasAulas.find((a) => a.id === aulaAtualId) ??
    todasAulas.find((a) => !a.completed) ??
    todasAulas[0] ??
    null

  const alternarConclusao = useMutation({
    mutationFn: (aula: LessonItem) =>
      api(`/courses/lessons/${aula.id}/complete`, {
        method: aula.completed ? 'DELETE' : 'POST',
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['curso', id] })
      void queryClient.invalidateQueries({ queryKey: ['meus-cursos'] })
    },
  })

  const emitirCertificado = useMutation({
    mutationFn: () => api<{ code: string }>(`/courses/${id}/certificate`, { method: 'POST' }),
    onSuccess: (cert) => {
      void queryClient.invalidateQueries({ queryKey: ['meus-certificados'] })
      navigate(`/certificado/${cert.code}`)
    },
  })

  function concluirEAvancar(aula: LessonItem) {
    alternarConclusao.mutate(aula)
    if (!aula.completed) {
      const i = todasAulas.findIndex((a) => a.id === aula.id)
      const proxima = todasAulas.slice(i + 1).find((a) => !a.completed)
      if (proxima) setAulaAtualId(proxima.id)
    }
  }

  // Atalhos ←/→ trocam de aula, ignorando quando o foco esta num campo.
  useEffect(() => {
    function aoTeclar(e: KeyboardEvent) {
      const alvo = e.target as HTMLElement
      if (['INPUT', 'TEXTAREA', 'SELECT'].includes(alvo.tagName)) return
      if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return
      const atualId = aulaAtual?.id
      if (!atualId) return
      const i = todasAulas.findIndex((a) => a.id === atualId)
      const destino = e.key === 'ArrowRight' ? todasAulas[i + 1] : todasAulas[i - 1]
      if (destino) setAulaAtualId(destino.id)
    }
    window.addEventListener('keydown', aoTeclar)
    return () => window.removeEventListener('keydown', aoTeclar)
  })

  const termo = normalizar(busca.trim())

  return (
    <section>
      <Link to="/dashboard" className="voltar-cursos">
        ← Meus cursos
      </Link>

      <QueryBoundary query={query}>
        {(curso) => (
          <>
            <h1 style={{ marginTop: 0 }}>{nomeAmigavel(curso.title)}</h1>

            {todasAulas.length > 0 && todasAulas.every((a) => a.completed) && (
              <p className="curso-completo" role="status">
                🎓 Curso 100% concluído!{' '}
                <Button
                  disabled={emitirCertificado.isPending}
                  onClick={() => emitirCertificado.mutate()}
                >
                  {emitirCertificado.isPending ? 'Emitindo…' : 'Emitir certificado'}
                </Button>
              </p>
            )}

            {todasAulas.length === 0 ? (
              <p className="bb-state">As aulas deste curso estão sendo preparadas.</p>
            ) : (
              <div className="aula-layout">
                <div>
                  {aulaAtual && (
                    <>
                      {/* Aula so de questoes dispensa a moldura de video vazia. */}
                      {(aulaAtual.videoUrl || aulaAtual.questionCount === 0) && (
                        <Player aula={aulaAtual} />
                      )}
                      <div className="aula-info">
                        <h2>{aulaAtual.title}</h2>
                        <Button
                          ghost={aulaAtual.completed}
                          disabled={alternarConclusao.isPending}
                          onClick={() => concluirEAvancar(aulaAtual)}
                        >
                          {aulaAtual.completed ? 'Concluída ✓ (desfazer)' : 'Concluir aula'}
                        </Button>
                      </div>
                      {aulaAtual.description && (
                        <p style={{ color: 'var(--bb-text-dim)', maxWidth: '68ch' }}>
                          {aulaAtual.description}
                        </p>
                      )}

                      {aulaAtual.questionCount > 0 && (
                        <Simulado key={`quiz-${aulaAtual.id}`} lessonId={aulaAtual.id} />
                      )}

                      {aulaAtual.materials.length > 0 && (
                        <div className="materiais">
                          <h3>Materiais da aula</h3>
                          <ul>
                            {aulaAtual.materials.map((m) => (
                              <li key={m.id}>
                                <a href={m.url} target="_blank" rel="noreferrer">
                                  📎 {m.title}
                                </a>
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}

                      <Discussao key={`disc-${aulaAtual.id}`} lessonId={aulaAtual.id} />
                    </>
                  )}
                </div>

                <nav className="aula-conteudo" aria-label="Conteúdo do curso">
                  <div className="busca-aulas">
                    <label htmlFor="busca-curso" className="sr-only">
                      Buscar aula
                    </label>
                    <input
                      id="busca-curso"
                      type="search"
                      value={busca}
                      onChange={(e) => setBusca(e.target.value)}
                      placeholder="Buscar aula…"
                    />
                  </div>
                  {curso.modules.map((modulo) => {
                    const aulasFiltradas = modulo.lessons.filter(
                      (a) => termo === '' || normalizar(a.title).includes(termo),
                    )
                    if (aulasFiltradas.length === 0) return null
                    return (
                      <details key={modulo.id} className="aula-modulo" open>
                        <summary>{modulo.title}</summary>
                        {aulasFiltradas.map((aula) => (
                          <button
                            key={aula.id}
                            type="button"
                            className={`aula-item ${aula.id === aulaAtual?.id ? 'ativa' : ''}`}
                            onClick={() => setAulaAtualId(aula.id)}
                          >
                            <span
                              className={`marcador ${aula.completed ? 'feita' : ''}`}
                              aria-hidden="true"
                            >
                              {aula.completed ? '✓' : ''}
                            </span>
                            <span>{aula.title}</span>
                            <span className="duracao">
                              {aula.questionCount > 0
                                ? `${aula.questionCount} questões`
                                : duracao(aula.durationSeconds)}
                            </span>
                          </button>
                        ))}
                      </details>
                    )
                  })}
                </nav>
              </div>
            )}
          </>
        )}
      </QueryBoundary>
    </section>
  )
}
