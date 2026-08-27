import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../../api/http'
import { Button } from '../../ui/basics'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { useToast } from '../../ui/Toast'
import { dataCurta, StatusPill } from './AdminLayout'

interface StudentDetail {
  id: string
  email: string
  fullName: string
  phone: string | null
  status: string
  roles: string[]
  createdAt: string
}

interface EntitlementItem {
  entitlementId: string
  productId: string
  sku: string | null
  productName: string | null
  source: string
  grantedAt: string
  expiresAt: string | null
}

interface ProductOption {
  id: string
  sku: string
  name: string
}

export function AdminStudentDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()
  const [produtoId, setProdutoId] = useState('')

  const aluno = useQuery({
    queryKey: ['admin-aluno', id],
    queryFn: () => api<StudentDetail>(`/admin/students/${id}`),
  })

  // A visao de entitlements do aluno reusa o catalogo do admin para o select.
  const produtos = useQuery({
    queryKey: ['admin-produtos-select'],
    queryFn: () => api<ProductOption[]>('/admin/products'),
  })

  const recarregar = () => {
    void queryClient.invalidateQueries({ queryKey: ['admin-aluno', id] })
    void queryClient.invalidateQueries({ queryKey: ['admin-aluno-entitlements', id] })
  }

  const entitlements = useQuery({
    queryKey: ['admin-aluno-entitlements', id],
    // Nao ha endpoint proprio de listagem por aluno para o admin; a lista de
    // acessos vigentes vem junto do detalhe na fase seguinte. Por ora, o
    // que o admin concede/revoga aqui reflete na hora via invalidacao.
    queryFn: () => api<EntitlementItem[]>(`/admin/students/${id}/entitlements`),
    retry: false,
  })

  const mudarStatus = useMutation({
    mutationFn: (novo: string) =>
      api(`/admin/students/${id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status: novo }),
      }),
    onSuccess: () => {
      toast('Status atualizado.')
      recarregar()
    },
    onError: () => toastErro('Não foi possível atualizar o status.'),
  })

  const conceder = useMutation({
    mutationFn: () =>
      api(`/admin/students/${id}/entitlements`, {
        method: 'POST',
        body: JSON.stringify({ productId: produtoId }),
      }),
    onSuccess: () => {
      toast('Acesso concedido.')
      recarregar()
    },
    onError: () => toastErro('Não foi possível conceder o acesso.'),
  })

  const revogar = useMutation({
    mutationFn: (eid: string) =>
      api(`/admin/students/${id}/entitlements/${eid}`, { method: 'DELETE' }),
    onSuccess: () => {
      toast('Acesso revogado.')
      recarregar()
    },
    onError: () => toastErro('Não foi possível revogar o acesso.'),
  })

  return (
    <section>
      <p>
        <Link to="/admin/alunos" style={{ color: 'var(--bb-text-dim)' }}>
          ← Alunos
        </Link>
      </p>

      <QueryBoundary query={aluno}>
        {(a) => (
          <>
            <h1 className="adm-titulo">{a.fullName}</h1>
            <p className="adm-sub">
              {a.email} · <StatusPill valor={a.status} /> · desde {dataCurta(a.createdAt)}
            </p>

            <div className="adm-detalhe-grid">
              <div className="adm-form">
                <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>Conta</h2>
                <p style={{ color: 'var(--bb-text-dim)', fontSize: '0.9rem' }}>
                  Telefone: {a.phone ?? '—'}
                  <br />
                  Roles: {a.roles.join(', ')}
                </p>
                <div className="adm-acoes">
                  {a.status === 'ACTIVE' ? (
                    <Button
                      ghost
                      disabled={mudarStatus.isPending}
                      onClick={() => mudarStatus.mutate('BLOCKED')}
                    >
                      Bloquear acesso
                    </Button>
                  ) : (
                    <Button
                      disabled={mudarStatus.isPending}
                      onClick={() => mudarStatus.mutate('ACTIVE')}
                    >
                      Desbloquear
                    </Button>
                  )}
                </div>
              </div>

              <div className="adm-form">
                <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>Conceder acesso manual</h2>
                <div className="bb-field">
                  <label htmlFor="conceder-produto">Produto</label>
                  <select
                    id="conceder-produto"
                    value={produtoId}
                    onChange={(e) => setProdutoId(e.target.value)}
                  >
                    <option value="">Selecione…</option>
                    {(produtos.data ?? []).map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name} ({p.sku})
                      </option>
                    ))}
                  </select>
                </div>
                <Button disabled={!produtoId || conceder.isPending} onClick={() => conceder.mutate()}>
                  Conceder
                </Button>

                {entitlements.data && entitlements.data.length > 0 && (
                  <div style={{ marginTop: 'var(--bb-s4)' }}>
                    <h3 style={{ fontSize: '0.9rem', color: 'var(--bb-text-dim)' }}>
                      Acessos vigentes
                    </h3>
                    <ul style={{ paddingLeft: 18, margin: 0 }}>
                      {entitlements.data.map((e) => (
                        <li key={e.entitlementId} style={{ marginBottom: 8 }}>
                          {e.productName ?? e.productId} · {e.source}{' '}
                          <Button
                            ghost
                            style={{ padding: '2px 10px', fontSize: '0.78rem' }}
                            disabled={revogar.isPending}
                            onClick={() => revogar.mutate(e.entitlementId)}
                          >
                            Revogar
                          </Button>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            </div>
          </>
        )}
      </QueryBoundary>
    </section>
  )
}
