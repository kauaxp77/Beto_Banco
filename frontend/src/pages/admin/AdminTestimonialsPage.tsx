import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../../api/http'
import { Button } from '../../ui/basics'
import { nomeAmigavel } from '../../ui/format'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { useToast } from '../../ui/Toast'
import { dataCurta, StatusPill } from './AdminLayout'

interface TestimonialRow {
  id: string
  authorName: string
  authorEmail: string | null
  courseId: string | null
  courseTitle: string | null
  body: string
  status: string
  createdAt: string
}

const STATUS = ['', 'PENDING', 'APPROVED', 'HIDDEN']
const ROTULOS: Record<string, string> = {
  '': 'Todos',
  PENDING: 'Pendentes',
  APPROVED: 'Aprovados',
  HIDDEN: 'Ocultos',
}

export function AdminTestimonialsPage() {
  const { toastErro } = useToast()
  const queryClient = useQueryClient()
  const [status, setStatus] = useState('PENDING')

  const query = useQuery({
    queryKey: ['admin-depoimentos', status],
    queryFn: () => api<TestimonialRow[]>(`/admin/courses/testimonials?status=${status}`),
  })

  const moderar = useMutation({
    mutationFn: ({ id, novo }: { id: string; novo: string }) =>
      api(`/admin/courses/testimonials/${id}/status/${novo}`, { method: 'POST' }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['admin-depoimentos'] }),
    onError: () => toastErro('Não foi possível moderar. Tente de novo.'),
  })

  return (
    <section>
      <h1 className="adm-titulo">Depoimentos</h1>
      <p className="adm-sub">
        Prova social enviada pelos próprios alunos. Aprovado fica disponível para o site;
        oculto sai do ar sem apagar.
      </p>

      <div className="adm-filtros">
        <div className="bb-field">
          <label htmlFor="filtro-status-dep">Status</label>
          <select
            id="filtro-status-dep"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            {STATUS.map((s) => (
              <option key={s} value={s}>
                {ROTULOS[s]}
              </option>
            ))}
          </select>
        </div>
      </div>

      <QueryBoundary query={query} empty="Nenhum depoimento com esse filtro.">
        {(depoimentos) => (
          <div className="adm-tabela-wrap">
            <table className="adm-tabela">
              <thead>
                <tr>
                  <th>Quando</th>
                  <th>Aluno</th>
                  <th>Curso</th>
                  <th>Depoimento</th>
                  <th>Status</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {depoimentos.map((d) => (
                  <tr key={d.id}>
                    <td style={{ whiteSpace: 'nowrap' }}>{dataCurta(d.createdAt)}</td>
                    <td>
                      {d.authorName}
                      {d.authorEmail && (
                        <>
                          <br />
                          <span style={{ color: 'var(--bb-text-dim)', fontSize: '0.8rem' }}>
                            {d.authorEmail}
                          </span>
                        </>
                      )}
                    </td>
                    <td>{d.courseTitle ? nomeAmigavel(d.courseTitle) : '—'}</td>
                    <td style={{ maxWidth: 340 }}>{d.body}</td>
                    <td>
                      <StatusPill
                        valor={
                          d.status === 'APPROVED'
                            ? 'ACTIVE'
                            : d.status === 'HIDDEN'
                              ? 'BLOCKED'
                              : 'PENDING'
                        }
                      />
                    </td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      {d.status !== 'APPROVED' && (
                        <Button
                          ghost
                          style={{ padding: '2px 10px', fontSize: '0.78rem' }}
                          disabled={moderar.isPending}
                          onClick={() => moderar.mutate({ id: d.id, novo: 'APPROVED' })}
                        >
                          Aprovar
                        </Button>
                      )}{' '}
                      {d.status !== 'HIDDEN' && (
                        <Button
                          ghost
                          style={{ padding: '2px 10px', fontSize: '0.78rem' }}
                          disabled={moderar.isPending}
                          onClick={() => moderar.mutate({ id: d.id, novo: 'HIDDEN' })}
                        >
                          Ocultar
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </QueryBoundary>
    </section>
  )
}
