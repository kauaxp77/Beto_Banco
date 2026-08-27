import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { api, ApiError } from '../../api/http'
import { Button, Input } from '../../ui/basics'
import { nomeAmigavel } from '../../ui/format'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { useToast } from '../../ui/Toast'
import { dataCurta, StatusPill } from './AdminLayout'

interface ProductRow {
  id: string
  sku: string
  name: string
  active: boolean
}

interface InviteRow {
  entitlementId: string
  email: string
  fullName: string
  productId: string
  productName: string | null
  grantedAt: string
  expiresAt: string | null
  revoked: boolean
  contaNova: boolean
}

export function AdminInvitesPage() {
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()
  const [email, setEmail] = useState('')
  const [nome, setNome] = useState('')
  const [produtoId, setProdutoId] = useState('')
  const [validade, setValidade] = useState('')
  const [erro, setErro] = useState<string | undefined>()

  const query = useQuery({
    queryKey: ['admin-convites'],
    queryFn: () => api<InviteRow[]>('/admin/invites'),
  })
  const produtos = useQuery({
    queryKey: ['admin-produtos'],
    queryFn: () => api<ProductRow[]>('/admin/products'),
  })

  const convidar = useMutation({
    mutationFn: () => {
      const dias = Number.parseInt(validade, 10)
      return api<InviteRow>('/admin/invites', {
        method: 'POST',
        body: JSON.stringify({
          email: email.trim(),
          fullName: nome.trim() || null,
          productId: produtoId,
          validadeDias: Number.isFinite(dias) && dias > 0 ? dias : null,
        }),
      })
    },
    onSuccess: (r) => {
      toast(
        r.contaNova
          ? 'Convite enviado: conta criada e e-mail de primeiro acesso na fila.'
          : 'Acesso liberado para a conta existente; aluno avisado por e-mail.',
      )
      setEmail('')
      setNome('')
      setProdutoId('')
      setValidade('')
      setErro(undefined)
      void queryClient.invalidateQueries({ queryKey: ['admin-convites'] })
    },
    onError: (err) => {
      if (err instanceof ApiError) setErro(err.fieldErrors?.[0]?.message ?? err.message)
      else toastErro('Não foi possível convidar. Tente de novo.')
    },
  })

  function submeter(e: FormEvent) {
    e.preventDefault()
    convidar.mutate()
  }

  return (
    <section>
      <h1 className="adm-titulo">Convites</h1>
      <p className="adm-sub">
        Libere acesso sem pagamento — bolsista, parceiro, cortesia. Quem ainda não tem conta
        recebe o link de primeiro acesso por e-mail.
      </p>

      <form className="adm-form" onSubmit={submeter}>
        <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>Novo convite</h2>
        <Input
          label="E-mail do convidado"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={erro}
          required
        />
        <Input
          label="Nome (opcional)"
          value={nome}
          onChange={(e) => setNome(e.target.value)}
        />
        <div className="bb-field">
          <label htmlFor="convite-produto">Produto a liberar</label>
          <select
            id="convite-produto"
            value={produtoId}
            onChange={(e) => setProdutoId(e.target.value)}
            required
            style={{
              background: 'var(--bb-surface-2)',
              border: '1px solid var(--bb-border)',
              borderRadius: 'var(--bb-r1)',
              color: 'var(--bb-text)',
              padding: '9px 12px',
              width: '100%',
            }}
          >
            <option value="">Selecione…</option>
            {(produtos.data ?? []).map((p) => (
              <option key={p.id} value={p.id}>
                {nomeAmigavel(p.name)} ({p.sku})
              </option>
            ))}
          </select>
        </div>
        <Input
          label="Validade em dias (vazio = vitalício)"
          inputMode="numeric"
          placeholder="ex.: 30"
          value={validade}
          onChange={(e) => setValidade(e.target.value)}
        />
        <Button type="submit" disabled={!email.trim() || !produtoId || convidar.isPending}>
          Enviar convite
        </Button>
      </form>

      <QueryBoundary query={query} empty="Nenhum convite enviado ainda.">
        {(convites) => (
          <div className="adm-tabela-wrap">
            <table className="adm-tabela">
              <thead>
                <tr>
                  <th>Convidado</th>
                  <th>Produto</th>
                  <th>Concedido</th>
                  <th>Validade</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {convites.map((c) => (
                  <tr key={c.entitlementId}>
                    <td>
                      {c.fullName}
                      <br />
                      <span style={{ color: 'var(--bb-text-dim)', fontSize: '0.8rem' }}>
                        {c.email}
                      </span>
                    </td>
                    <td>{c.productName ? nomeAmigavel(c.productName) : '—'}</td>
                    <td style={{ whiteSpace: 'nowrap' }}>{dataCurta(c.grantedAt)}</td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      {c.expiresAt ? dataCurta(c.expiresAt) : 'Vitalício'}
                    </td>
                    <td>
                      <StatusPill valor={c.revoked ? 'BLOCKED' : 'ACTIVE'} />
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
