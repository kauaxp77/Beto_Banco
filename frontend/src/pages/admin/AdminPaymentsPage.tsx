import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { apiPage } from '../../api/http'
import { Paginacao } from '../../ui/Paginacao'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { brl, dataCurta, StatusPill } from './AdminLayout'

interface PaymentRow {
  id: string
  provider: string
  providerTransactionId: string
  buyerEmail: string
  buyerName: string | null
  amountCents: number
  currency: string
  status: string
  approvedAt: string | null
  createdAt: string
}

const STATUS = ['', 'APPROVED', 'PENDING', 'CANCELLED', 'REFUNDED', 'CHARGEBACK', 'FAILED']

export function AdminPaymentsPage() {
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)

  const query = useQuery({
    queryKey: ['admin-pagamentos', status, page],
    queryFn: () => apiPage<PaymentRow>(`/admin/payments?status=${status}&page=${page}`),
    placeholderData: (anterior) => anterior,
  })

  return (
    <section>
      <h1 className="adm-titulo">Pagamentos</h1>
      <p className="adm-sub">Registro financeiro vindo dos webhooks — somente leitura.</p>

      <div className="adm-filtros">
        <div className="bb-field">
          <label htmlFor="filtro-status-pag">Status</label>
          <select
            id="filtro-status-pag"
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

      <QueryBoundary query={query}>
        {(pagina) =>
          pagina.data.length === 0 ? (
            <p className="bb-state">Nenhum pagamento com esses filtros.</p>
          ) : (
            <>
              <div className="adm-tabela-wrap">
                <table className="adm-tabela">
                  <thead>
                    <tr>
                      <th>Transação</th>
                      <th>Comprador</th>
                      <th>Valor</th>
                      <th>Status</th>
                      <th>Criado</th>
                      <th>Aprovado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pagina.data.map((p) => (
                      <tr key={p.id}>
                        <td>
                          {p.provider} · {p.providerTransactionId}
                        </td>
                        <td>{p.buyerEmail}</td>
                        <td>{brl.format(p.amountCents / 100)}</td>
                        <td>
                          <StatusPill valor={p.status} />
                        </td>
                        <td>{dataCurta(p.createdAt)}</td>
                        <td>{dataCurta(p.approvedAt)}</td>
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
