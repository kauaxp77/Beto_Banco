import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../../api/http'
import { Button, Input } from '../../ui/basics'
import { nomeAmigavel } from '../../ui/format'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { useToast } from '../../ui/Toast'
import type { CourseAdminRow } from './AdminCoursesPage'
import { StatusPill } from './AdminLayout'

interface MaterialContent {
  id: string
  title: string
  url: string
}

interface LessonContent {
  id: string
  title: string
  description: string | null
  videoUrl: string | null
  durationSeconds: number | null
  position: number
  published: boolean
  materials: MaterialContent[]
  questionCount: number
}

interface QuestionAdmin {
  id: string
  statement: string
  options: string[]
  correctIndex: number
  explanation: string | null
  position: number
}

interface ModuleContent {
  id: string
  title: string
  position: number
  lessons: LessonContent[]
}

interface CourseContent {
  id: string
  title: string
  modules: ModuleContent[]
}

interface ProductRow {
  id: string
  sku: string
  name: string
}

/** Formulario de aula: usado tanto para criar quanto para editar. */
interface FormAula {
  moduleId: string
  lessonId: string | null
  title: string
  videoUrl: string
  durationMin: string
  description: string
}

const LETRAS = ['A', 'B', 'C', 'D', 'E']

/** Banco de questoes de uma aula: listar, adicionar e remover. */
function PainelQuestoes({ lessonId, aoMudar }: { lessonId: string; aoMudar: () => void }) {
  const { toastErro } = useToast()
  const queryClient = useQueryClient()
  const [enunciado, setEnunciado] = useState('')
  const [alternativas, setAlternativas] = useState(['', '', '', '', ''])
  const [correta, setCorreta] = useState(0)
  const [comentario, setComentario] = useState('')

  const query = useQuery({
    queryKey: ['admin-questoes', lessonId],
    queryFn: () => api<QuestionAdmin[]>(`/admin/courses/lessons/${lessonId}/questions`),
  })

  function invalidar() {
    void queryClient.invalidateQueries({ queryKey: ['admin-questoes', lessonId] })
    aoMudar()
  }

  const criar = useMutation({
    mutationFn: () => {
      const options = alternativas.map((a) => a.trim()).filter((a) => a !== '')
      return api(`/admin/courses/lessons/${lessonId}/questions`, {
        method: 'POST',
        body: JSON.stringify({
          statement: enunciado.trim(),
          options,
          correctIndex: correta,
          explanation: comentario.trim() || null,
          position: (query.data ?? []).length,
        }),
      })
    },
    onSuccess: () => {
      setEnunciado('')
      setAlternativas(['', '', '', '', ''])
      setCorreta(0)
      setComentario('')
      invalidar()
    },
    onError: () => toastErro('Não foi possível salvar a questão. Confira as alternativas.'),
  })

  const remover = useMutation({
    mutationFn: (id: string) => api(`/admin/courses/questions/${id}`, { method: 'DELETE' }),
    onSuccess: invalidar,
    onError: () => toastErro('Não foi possível remover a questão.'),
  })

  const preenchidas = alternativas.filter((a) => a.trim() !== '').length

  return (
    <div style={{ marginTop: 12 }}>
      <h4 style={{ margin: '0 0 8px', fontSize: '0.88rem' }}>Questões desta aula</h4>

      <QueryBoundary query={query} empty="Nenhuma questão ainda — adicione a primeira abaixo.">
        {(questoes) => (
          <ol style={{ paddingLeft: 18, margin: '0 0 12px' }}>
            {questoes.map((q) => (
              <li key={q.id} style={{ marginBottom: 8, fontSize: '0.88rem' }}>
                {q.statement}{' '}
                <span style={{ color: 'var(--bb-success)', fontWeight: 600 }}>
                  (gabarito: {LETRAS[q.correctIndex]})
                </span>{' '}
                <Button
                  ghost
                  style={{ padding: '2px 10px', fontSize: '0.75rem' }}
                  disabled={remover.isPending}
                  onClick={() => remover.mutate(q.id)}
                >
                  Remover
                </Button>
              </li>
            ))}
          </ol>
        )}
      </QueryBoundary>

      <form
        onSubmit={(e) => {
          e.preventDefault()
          criar.mutate()
        }}
      >
        <div className="bb-field">
          <label htmlFor={`enunciado-${lessonId}`}>Enunciado</label>
          <textarea
            id={`enunciado-${lessonId}`}
            value={enunciado}
            onChange={(e) => setEnunciado(e.target.value)}
            rows={3}
            maxLength={4000}
            required
            style={{
              width: '100%',
              background: 'var(--bb-surface-2)',
              border: '1px solid var(--bb-border)',
              borderRadius: 'var(--bb-r1)',
              color: 'var(--bb-text)',
              padding: '10px 12px',
              fontFamily: 'inherit',
            }}
          />
        </div>
        {alternativas.map((alt, i) => (
          <Input
            key={i}
            label={`Alternativa ${LETRAS[i]}${i >= 2 ? ' (opcional)' : ''}`}
            value={alt}
            onChange={(e) => {
              const novas = [...alternativas]
              novas[i] = e.target.value
              setAlternativas(novas)
            }}
          />
        ))}
        <div className="bb-field">
          <label htmlFor={`correta-${lessonId}`}>Alternativa correta</label>
          <select
            id={`correta-${lessonId}`}
            value={correta}
            onChange={(e) => setCorreta(Number(e.target.value))}
            style={{
              background: 'var(--bb-surface-2)',
              border: '1px solid var(--bb-border)',
              borderRadius: 'var(--bb-r1)',
              color: 'var(--bb-text)',
              padding: '9px 12px',
            }}
          >
            {alternativas.map(
              (alt, i) =>
                alt.trim() !== '' && (
                  <option key={i} value={i}>
                    {LETRAS[i]}
                  </option>
                ),
            )}
          </select>
        </div>
        <Input
          label="Comentário do gabarito (opcional)"
          value={comentario}
          onChange={(e) => setComentario(e.target.value)}
        />
        <Button
          type="submit"
          disabled={!enunciado.trim() || preenchidas < 2 || criar.isPending}
        >
          Adicionar questão
        </Button>
      </form>
    </div>
  )
}

export function AdminCourseContentPage() {
  const { id } = useParams<{ id: string }>()
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()

  const curso = useQuery({
    queryKey: ['admin-curso', id],
    queryFn: () => api<CourseAdminRow>(`/admin/courses/${id}`),
    enabled: !!id,
  })
  const conteudo = useQuery({
    queryKey: ['admin-curso-conteudo', id],
    queryFn: () => api<CourseContent>(`/admin/courses/${id}/content`),
    enabled: !!id,
  })
  const produtos = useQuery({
    queryKey: ['admin-produtos'],
    queryFn: () => api<ProductRow[]>('/admin/products'),
  })

  const [novoModulo, setNovoModulo] = useState('')
  const [produtoId, setProdutoId] = useState('')
  const [formAula, setFormAula] = useState<FormAula | null>(null)
  const [matAula, setMatAula] = useState<string | null>(null)
  const [matTitulo, setMatTitulo] = useState('')
  const [matUrl, setMatUrl] = useState('')
  const [quizAula, setQuizAula] = useState<string | null>(null)

  function invalidar() {
    void queryClient.invalidateQueries({ queryKey: ['admin-curso', id] })
    void queryClient.invalidateQueries({ queryKey: ['admin-curso-conteudo', id] })
  }

  const chamar = useMutation({
    mutationFn: ({ path, method, body }: { path: string; method: string; body?: unknown }) =>
      api(path, { method, body: body === undefined ? undefined : JSON.stringify(body) }),
    onSuccess: () => invalidar(),
    onError: () => toastErro('A operação falhou. Tente de novo.'),
  })

  function publicar(atual: CourseAdminRow, published: boolean) {
    chamar.mutate({
      path: `/admin/courses/${atual.id}`,
      method: 'PUT',
      body: {
        title: atual.title,
        description: atual.description,
        coverUrl: atual.coverUrl,
        published,
      },
    })
    toast(published ? 'Curso publicado — visível para quem comprou.' : 'Curso despublicado.')
  }

  function criarModulo(e: FormEvent) {
    e.preventDefault()
    const posicao = conteudo.data?.modules.length ?? 0
    chamar.mutate({
      path: `/admin/courses/${id}/modules`,
      method: 'POST',
      body: { title: novoModulo.trim(), position: posicao },
    })
    setNovoModulo('')
  }

  function salvarAula(e: FormEvent) {
    e.preventDefault()
    if (!formAula) return
    const minutos = Number.parseInt(formAula.durationMin, 10)
    const body = {
      title: formAula.title.trim(),
      description: formAula.description.trim() || null,
      videoUrl: formAula.videoUrl.trim() || null,
      durationSeconds: Number.isFinite(minutos) ? minutos * 60 : null,
      position:
        formAula.lessonId !== null
          ? conteudo.data?.modules
              .flatMap((m) => m.lessons)
              .find((a) => a.id === formAula.lessonId)?.position ?? 0
          : conteudo.data?.modules.find((m) => m.id === formAula.moduleId)?.lessons.length ?? 0,
      published: true,
    }
    chamar.mutate(
      formAula.lessonId === null
        ? { path: `/admin/courses/modules/${formAula.moduleId}/lessons`, method: 'POST', body }
        : { path: `/admin/courses/lessons/${formAula.lessonId}`, method: 'PUT', body },
    )
    setFormAula(null)
  }

  return (
    <section>
      <Link to="/admin/cursos" className="voltar-cursos">
        ← Cursos
      </Link>

      <QueryBoundary query={curso}>
        {(c) => (
          <>
            <h1 className="adm-titulo" style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
              {nomeAmigavel(c.title)}
              <StatusPill valor={c.published ? 'ACTIVE' : 'PENDING'} />
            </h1>
            <p className="adm-sub">
              {c.published
                ? 'Publicado: alunos com o produto vinculado já veem este curso.'
                : 'Rascunho: invisível para os alunos até você publicar.'}
            </p>

            <div className="adm-acoes" style={{ marginBottom: 'var(--bb-s5)' }}>
              <Button onClick={() => publicar(c, !c.published)} disabled={chamar.isPending}>
                {c.published ? 'Despublicar' : 'Publicar curso'}
              </Button>
            </div>

            {/* ---------- produtos que liberam este curso ---------- */}
            <div className="adm-form" style={{ maxWidth: 'none' }}>
              <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>Quem tem acesso</h2>
              <p style={{ color: 'var(--bb-text-dim)', fontSize: '0.88rem' }}>
                Comprou qualquer um destes produtos → enxerga o curso.
              </p>
              {c.productIds.length === 0 && (
                <p className="adm-alerta" role="alert" style={{ fontSize: '0.88rem' }}>
                  Nenhum produto vinculado: mesmo publicado, nenhum aluno vê este curso.
                </p>
              )}
              <ul style={{ paddingLeft: 18 }}>
                {c.productIds.map((pid) => {
                  const p = (produtos.data ?? []).find((x) => x.id === pid)
                  return (
                    <li key={pid} style={{ marginBottom: 8 }}>
                      {p ? `${nomeAmigavel(p.name)} (${p.sku})` : pid}{' '}
                      <Button
                        ghost
                        style={{ padding: '2px 10px', fontSize: '0.78rem' }}
                        onClick={() =>
                          chamar.mutate({
                            path: `/admin/courses/${c.id}/products/${pid}`,
                            method: 'DELETE',
                          })
                        }
                      >
                        Desvincular
                      </Button>
                    </li>
                  )
                })}
              </ul>
              <div className="adm-filtros">
                <div className="bb-field">
                  <label htmlFor="vincular-produto">Vincular produto</label>
                  <select
                    id="vincular-produto"
                    value={produtoId}
                    onChange={(e) => setProdutoId(e.target.value)}
                  >
                    <option value="">Selecione…</option>
                    {(produtos.data ?? [])
                      .filter((p) => !c.productIds.includes(p.id))
                      .map((p) => (
                        <option key={p.id} value={p.id}>
                          {nomeAmigavel(p.name)} ({p.sku})
                        </option>
                      ))}
                  </select>
                </div>
                <Button
                  disabled={!produtoId || chamar.isPending}
                  onClick={() => {
                    chamar.mutate({
                      path: `/admin/courses/${c.id}/products`,
                      method: 'POST',
                      body: { productId: produtoId },
                    })
                    setProdutoId('')
                  }}
                >
                  Vincular
                </Button>
              </div>
            </div>
          </>
        )}
      </QueryBoundary>

      {/* ---------- modulos e aulas ---------- */}
      <QueryBoundary query={conteudo}>
        {(arvore) => (
          <div style={{ marginTop: 'var(--bb-s5)' }}>
            <h2 style={{ fontSize: '1.05rem' }}>Módulos e aulas</h2>

            {arvore.modules.map((modulo) => (
              <div key={modulo.id} className="adm-form" style={{ maxWidth: 'none' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
                  <h3 style={{ margin: 0, fontSize: '0.95rem', flex: 1 }}>{modulo.title}</h3>
                  <Button
                    ghost
                    style={{ padding: '4px 12px', fontSize: '0.8rem' }}
                    onClick={() =>
                      setFormAula({
                        moduleId: modulo.id,
                        lessonId: null,
                        title: '',
                        videoUrl: '',
                        durationMin: '',
                        description: '',
                      })
                    }
                  >
                    + Aula
                  </Button>
                  <Button
                    ghost
                    style={{ padding: '4px 12px', fontSize: '0.8rem' }}
                    onClick={() => {
                      if (modulo.lessons.length > 0) {
                        toastErro('Remova as aulas do módulo antes de excluí-lo.')
                        return
                      }
                      chamar.mutate({
                        path: `/admin/courses/modules/${modulo.id}`,
                        method: 'DELETE',
                      })
                    }}
                  >
                    Excluir módulo
                  </Button>
                </div>

                {modulo.lessons.length > 0 && (
                  <table className="adm-tabela" style={{ minWidth: 0, marginTop: 12 }}>
                    <tbody>
                      {modulo.lessons.map((aula) => (
                        <tr key={aula.id}>
                          <td>{aula.title}</td>
                          <td style={{ color: 'var(--bb-text-dim)', fontSize: '0.82rem' }}>
                            {aula.videoUrl ? 'vídeo' : 'sem vídeo'}
                            {aula.durationSeconds
                              ? ` · ${Math.round(aula.durationSeconds / 60)} min`
                              : ''}
                            {aula.materials.length > 0 && ` · ${aula.materials.length} 📎`}
                            {aula.questionCount > 0 && ` · ${aula.questionCount} questões`}
                          </td>
                          <td style={{ whiteSpace: 'nowrap' }}>
                            <Button
                              ghost
                              style={{ padding: '2px 10px', fontSize: '0.78rem' }}
                              onClick={() =>
                                setFormAula({
                                  moduleId: modulo.id,
                                  lessonId: aula.id,
                                  title: aula.title,
                                  videoUrl: aula.videoUrl ?? '',
                                  durationMin: aula.durationSeconds
                                    ? String(Math.round(aula.durationSeconds / 60))
                                    : '',
                                  description: aula.description ?? '',
                                })
                              }
                            >
                              Editar
                            </Button>{' '}
                            <Button
                              ghost
                              style={{ padding: '2px 10px', fontSize: '0.78rem' }}
                              onClick={() =>
                                setMatAula(matAula === aula.id ? null : aula.id)
                              }
                            >
                              Materiais
                            </Button>{' '}
                            <Button
                              ghost
                              style={{ padding: '2px 10px', fontSize: '0.78rem' }}
                              onClick={() =>
                                setQuizAula(quizAula === aula.id ? null : aula.id)
                              }
                            >
                              Questões
                            </Button>{' '}
                            <Button
                              ghost
                              style={{ padding: '2px 10px', fontSize: '0.78rem' }}
                              onClick={() =>
                                chamar.mutate({
                                  path: `/admin/courses/lessons/${aula.id}`,
                                  method: 'DELETE',
                                })
                              }
                            >
                              Excluir
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}

                {matAula !== null &&
                  modulo.lessons.some((a) => a.id === matAula) &&
                  (() => {
                    const aula = modulo.lessons.find((a) => a.id === matAula)!
                    return (
                      <div style={{ marginTop: 12 }}>
                        <h4 style={{ margin: '0 0 8px', fontSize: '0.88rem' }}>
                          Materiais de "{aula.title}"
                        </h4>
                        {aula.materials.length === 0 ? (
                          <p style={{ color: 'var(--bb-text-dim)', fontSize: '0.85rem' }}>
                            Nenhum material ainda.
                          </p>
                        ) : (
                          <ul style={{ paddingLeft: 18 }}>
                            {aula.materials.map((m) => (
                              <li key={m.id} style={{ marginBottom: 6, fontSize: '0.88rem' }}>
                                {m.title}{' '}
                                <Button
                                  ghost
                                  style={{ padding: '2px 10px', fontSize: '0.75rem' }}
                                  disabled={chamar.isPending}
                                  onClick={() =>
                                    chamar.mutate({
                                      path: `/admin/courses/materials/${m.id}`,
                                      method: 'DELETE',
                                    })
                                  }
                                >
                                  Remover
                                </Button>
                              </li>
                            ))}
                          </ul>
                        )}
                        <form
                          className="adm-filtros"
                          onSubmit={(e) => {
                            e.preventDefault()
                            chamar.mutate({
                              path: `/admin/courses/lessons/${aula.id}/materials`,
                              method: 'POST',
                              body: {
                                title: matTitulo.trim(),
                                url: matUrl.trim(),
                                position: aula.materials.length,
                              },
                            })
                            setMatTitulo('')
                            setMatUrl('')
                          }}
                        >
                          <Input
                            label="Título do material"
                            value={matTitulo}
                            onChange={(e) => setMatTitulo(e.target.value)}
                            placeholder="Apostila em PDF"
                          />
                          <Input
                            label="URL"
                            value={matUrl}
                            onChange={(e) => setMatUrl(e.target.value)}
                            placeholder="https://…/apostila.pdf"
                          />
                          <Button
                            type="submit"
                            disabled={!matTitulo.trim() || !matUrl.trim() || chamar.isPending}
                          >
                            Adicionar material
                          </Button>
                        </form>
                      </div>
                    )
                  })()}

                {quizAula !== null && modulo.lessons.some((a) => a.id === quizAula) && (
                  <PainelQuestoes lessonId={quizAula} aoMudar={invalidar} />
                )}

                {formAula?.moduleId === modulo.id && (
                  <form onSubmit={salvarAula} style={{ marginTop: 12 }}>
                    <h4 style={{ margin: '0 0 8px', fontSize: '0.88rem' }}>
                      {formAula.lessonId ? 'Editar aula' : 'Nova aula'}
                    </h4>
                    <Input
                      label="Título da aula"
                      value={formAula.title}
                      onChange={(e) => setFormAula({ ...formAula, title: e.target.value })}
                      required
                    />
                    <Input
                      label="URL do vídeo (YouTube, Vimeo ou MP4)"
                      value={formAula.videoUrl}
                      onChange={(e) => setFormAula({ ...formAula, videoUrl: e.target.value })}
                      placeholder="https://www.youtube.com/watch?v=…"
                    />
                    <Input
                      label="Duração (minutos)"
                      inputMode="numeric"
                      value={formAula.durationMin}
                      onChange={(e) => setFormAula({ ...formAula, durationMin: e.target.value })}
                    />
                    <Input
                      label="Descrição (opcional)"
                      value={formAula.description}
                      onChange={(e) => setFormAula({ ...formAula, description: e.target.value })}
                    />
                    <div className="adm-acoes">
                      <Button type="submit" disabled={chamar.isPending}>
                        Salvar aula
                      </Button>
                      <Button ghost onClick={() => setFormAula(null)}>
                        Cancelar
                      </Button>
                    </div>
                  </form>
                )}
              </div>
            ))}

            <form className="adm-filtros" onSubmit={criarModulo}>
              <div className="bb-field" style={{ minWidth: 260 }}>
                <label htmlFor="novo-modulo">Novo módulo</label>
                <input
                  id="novo-modulo"
                  value={novoModulo}
                  onChange={(e) => setNovoModulo(e.target.value)}
                  placeholder="Ex.: Módulo 1 — Conhecimentos Bancários"
                  style={{
                    background: 'var(--bb-surface-2)',
                    border: '1px solid var(--bb-border)',
                    borderRadius: 'var(--bb-r1)',
                    color: 'var(--bb-text)',
                    padding: '9px 12px',
                    width: '100%',
                  }}
                />
              </div>
              <Button type="submit" disabled={!novoModulo.trim() || chamar.isPending}>
                Adicionar módulo
              </Button>
            </form>
          </div>
        )}
      </QueryBoundary>
    </section>
  )
}
