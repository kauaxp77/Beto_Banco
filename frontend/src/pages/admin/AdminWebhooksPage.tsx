import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { api, apiPage } from '../../api/http'
import { Button, Input } from '../../ui/basics'
import { Paginacao } from '../../ui/Paginacao'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { useToast } from '../../ui/Toast'
import { dataCurta, StatusPill } from './AdminLayout'

interface WebhookRow {
  id: string
  provider: string
  eventId: string
  eventType: string | null
  status: string
  attempts: number
  errorMessage: string | null
  payload: string
  receivedAt: string
  processedAt: string | null
}

interface ProductOption {
  id: string
  sku: string
  name: string
}

const STATUS = ['', 'MANUAL', 'FAILED', 'RECEIVED', 'PROCESSED', 'IGNORED']

export function AdminWebhooksPage() {
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [resolvendo, setResolvendo] = useState<WebhookRow | null>(null)
  const [email, setEmail] = useState('')
  const [produtoId, setProdutoId] = useState('')

  const query = useQuery({
    queryKey: ['admin-webhooks', status, page],
    queryFn: () => apiPage<WebhookRow>(`/admin/webhooks?status=${status}&page=${page}`),
    placeholderData: (anterior) => anterior,
  })

  const produtos = useQuery({
    queryKey: ['admin-produtos-select'],
    queryFn: () => api<ProductOption[]>('/admin/products'),
    enabled: resolvendo !== null,
  })

  const recarregar = () => void queryClient.invalidateQueries({ queryKey: ['admin-webhooks'] })

  const reprocessar = useMutation({
    mutationFn: (id: string) => api(`/admin/webhooks/${id}/reprocess`, { method: 'POST' }),
    onSuccess: () => {
      toast('Evento devolvido para a fila.')
      recarregar()
    },
    onError: () => toastErro('Não foi possível reprocessar.'),
  })

  const resolver = useMutation({
    mutationFn: () =>
      api(`/admin/webhooks/${resolvendo!.id}/resolve-manually`, {
        method: 'POST',
        body: JSON.stringify({ email, productId: produtoId }),
      }),
    onSuccess: () => {
      toast('Evento resolvido: acesso concedido manualmente.')
      setResolvendo(null)
      setEmail('')
      setProdutoId('')
      recarregar()
    },
    onError: () => toastErro('Não foi possível resolver manualmente.'),
  })

  function submeterResolucao(e: FormEvent) {
    e.preventDefault()
    resolver.mutate()
  }

  const intervenivel = (s: string) => s === 'FAILED' || s === 'MANUAL'

  return (
    <section>
      <h1 className="adm-titulo">Webhooks</h1>
      <p className="adm-sub">
        A fila de atenção: eventos que o processamento automático não resolveu.
      </p>

      <div className="adm-filtros">
        <div className="bb-field">
          <label htmlFor="filtro-status-wh">Status</label>
          <select
            id="filtro-status-wh"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value)
              setPage(0)
            }}
          >
            {STATUS.map((s) => (
              <option key={s} value={s}>
                {s === '' ? 'Todos' : s}
              </option>
            ))}
          </select>
        </div>
      </div>

      {resolvendo && (
        <form className="adm-form" onSubmit={submeterResolucao}>
          <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>
            Resolver manualmente · {resolvendo.eventId}
          </h2>
          <p style={{ color: 'var(--bb-text-dim)', fontSize: '0.88rem' }}>
            Faz na mão o que o processador faria: cria o aluno se preciso, concede o acesso e
            encerra o evento. Fica registrado em auditoria no seu nome.
          </p>
          <Input
            label="E-mail do comprador"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <div className="bb-field">
            <label htmlFor="resolver-produto">Produto a liberar</label>
            <select
              id="resolver-produto"
              value={produtoId}
              onChange={(e) => setProdutoId(e.target.value)}
              required
            >
              <option value="">Selecione…</option>
              {(produtos.data ?? []).map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({p.sku})
                </option>
              ))}
            </select>
          </div>
          <div className="adm-acoes">
            <Button type="submit" disabled={!email || !produtoId || resolver.isPending}>
              Conceder e encerrar
            </Button>
            <Button ghost onClick={() => setResolvendo(null)}>
              Cancelar
            </Button>
          </div>
        </form>
      )}

      <QueryBoundary query={query}>
        {(pagina) =>
          pagina.data.length === 0 ? (
            <p className="bb-state">Nenhum evento com esses filtros — fila limpa.</p>
          ) : (
            <>
              <div className="adm-tabela-wrap">
                <table className="adm-tabela">
                  <thead>
                    <tr>
                      <th>Evento</th>
                      <th>Tipo</th>
                      <th>Status</th>
                      <th>Tentativas</th>
                      <th>Erro</th>
                      <th>Recebido</th>
                      <th>Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pagina.data.map((w) => (
                      <tr key={w.id}>
                        <td>{w.eventId}</td>
                        <td>{w.eventType ?? '—'}</td>
                        <td>
                          <StatusPill valor={w.status} />
                        </td>
                        <td>{w.attempts}</td>
                        <td style={{ maxWidth: 260, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {w.errorMessage ?? '—'}
                        </td>
                        <td>{dataCurta(w.receivedAt)}</td>
                        <td>
                          {intervenivel(w.status) && (
                            <span className="adm-acoes">
                              <Button
                                ghost
                                style={{ padding: '4px 12px', fontSize: '0.8rem' }}
                                disabled={reprocessar.isPending}
                                onClick={() => reprocessar.mutate(w.id)}
                              >
                                Reprocessar
                              </Button>
                              <Button
                                ghost
                                style={{ padding: '4px 12px', fontSize: '0.8rem' }}
                                onClick={() => setResolvendo(w)}
                              >
                                Resolver
                              </Button>
                            </span>
                          )}
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
