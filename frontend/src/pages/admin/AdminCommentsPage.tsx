import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api, apiPage } from '../../api/http'
import { Button } from '../../ui/basics'
import { Paginacao } from '../../ui/Paginacao'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { useToast } from '../../ui/Toast'
import { dataCurta, StatusPill } from './AdminLayout'

interface CommentRow {
  id: string
  lessonId: string
  lessonTitle: string
  parentId: string | null
  body: string
  authorName: string
  authorEmail: string | null
  status: string
  createdAt: string
}

const STATUS = ['', 'VISIBLE', 'HIDDEN']

export function AdminCommentsPage() {
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [respondendo, setRespondendo] = useState<CommentRow | null>(null)
  const [resposta, setResposta] = useState('')

  const query = useQuery({
    queryKey: ['admin-comentarios', status, page],
    queryFn: () =>
      apiPage<CommentRow>(`/admin/courses/comments?status=${status}&page=${page}`),
    placeholderData: (anterior) => anterior,
  })

  const invalidar = () =>
    void queryClient.invalidateQueries({ queryKey: ['admin-comentarios'] })

  const moderar = useMutation({
    mutationFn: ({ id, acao }: { id: string; acao: 'hide' | 'show' }) =>
      api(`/admin/courses/comments/${id}/${acao}`, { method: 'POST' }),
    onSuccess: invalidar,
    onError: () => toastErro('Não foi possível moderar. Tente de novo.'),
  })

  const responder = useMutation({
    mutationFn: () =>
      api(`/admin/courses/comments/${respondendo?.id}/reply`, {
        method: 'POST',
        body: JSON.stringify({ body: resposta.trim() }),
      }),
    onSuccess: () => {
      toast('Resposta publicada na aula.')
      setRespondendo(null)
      setResposta('')
      invalidar()
    },
    onError: () => toastErro('Não foi possível responder. Tente de novo.'),
  })

  return (
    <section>
      <h1 className="adm-titulo">Comentários</h1>
      <p className="adm-sub">
        Dúvidas dos alunos nas aulas. Responda como professor, ou oculte o que não deve
        aparecer — nada é apagado.
      </p>

      <div className="adm-filtros">
        <div className="bb-field">
          <label htmlFor="filtro-status-com">Status</label>
          <select
            id="filtro-status-com"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value)
              setPage(0)
            }}
          >
            {STATUS.map((s) => (
              <option key={s} value={s}>
                {s === '' ? 'Todos' : s === 'VISIBLE' ? 'Visíveis' : 'Ocultos'}
              </option>
            ))}
          </select>
        </div>
      </div>

      {respondendo && (
        <form
          className="adm-form"
          onSubmit={(e) => {
            e.preventDefault()
            responder.mutate()
          }}
        >
          <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>
            Responder a {respondendo.authorName}
          </h2>
          <blockquote
            style={{
              margin: '0 0 12px',
              padding: '10px 14px',
              borderLeft: '3px solid var(--bb-gold)',
              color: 'var(--bb-text-dim)',
              fontSize: '0.88rem',
            }}
          >
            {respondendo.body}
          </blockquote>
          <div className="bb-field">
            <label htmlFor="texto-resposta">Sua resposta (aparece como Professor)</label>
            <textarea
              id="texto-resposta"
              value={resposta}
              onChange={(e) => setResposta(e.target.value)}
              rows={3}
              maxLength={4000}
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
          <div className="adm-acoes">
            <Button type="submit" disabled={!resposta.trim() || responder.isPending}>
              Publicar resposta
            </Button>
            <Button ghost onClick={() => setRespondendo(null)}>
              Cancelar
            </Button>
          </div>
        </form>
      )}

      <QueryBoundary query={query}>
        {(pagina) =>
          pagina.data.length === 0 ? (
            <p className="bb-state">Nenhum comentário com esses filtros.</p>
          ) : (
            <>
              <div className="adm-tabela-wrap">
                <table className="adm-tabela">
                  <thead>
                    <tr>
                      <th>Quando</th>
                      <th>Aluno</th>
                      <th>Aula</th>
                      <th>Comentário</th>
                      <th>Status</th>
                      <th>Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pagina.data.map((c) => (
                      <tr key={c.id}>
                        <td style={{ whiteSpace: 'nowrap' }}>{dataCurta(c.createdAt)}</td>
                        <td>
                          {c.authorName}
                          {c.parentId && (
                            <span style={{ color: 'var(--bb-text-dim)' }}> (resposta)</span>
                          )}
                        </td>
                        <td>{c.lessonTitle}</td>
                        <td style={{ maxWidth: 320 }}>{c.body}</td>
                        <td>
                          <StatusPill valor={c.status === 'VISIBLE' ? 'ACTIVE' : 'BLOCKED'} />
                        </td>
                        <td style={{ whiteSpace: 'nowrap' }}>
                          <Button
                            ghost
                            style={{ padding: '2px 10px', fontSize: '0.78rem' }}
                            onClick={() => setRespondendo(c)}
                          >
                            Responder
                          </Button>{' '}
                          <Button
                            ghost
                            style={{ padding: '2px 10px', fontSize: '0.78rem' }}
                            disabled={moderar.isPending}
                            onClick={() =>
                              moderar.mutate({
                                id: c.id,
                                acao: c.status === 'VISIBLE' ? 'hide' : 'show',
                              })
                            }
                          >
                            {c.status === 'VISIBLE' ? 'Ocultar' : 'Reexibir'}
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Paginacao meta={pagina.pagination} onPage={setPage} />
            </>
          )
        }
      </QueryBoundary>
    </section>
  )
}
