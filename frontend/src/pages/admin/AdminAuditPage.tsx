import { useQuery } from '@tanstack/react-query'
import { useDeferredValue, useState } from 'react'
import { apiPage } from '../../api/http'
import { Input } from '../../ui/basics'
import { Paginacao } from '../../ui/Paginacao'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { dataCurta } from './AdminLayout'

interface AuditRow {
  id: string
  actorUserId: string | null
  action: string
  entityType: string | null
  entityId: string | null
  metadata: string | null
  createdAt: string
}

export function AdminAuditPage() {
  const [acao, setAcao] = useState('')
  const [page, setPage] = useState(0)
  const acaoAtiva = useDeferredValue(acao)

  const query = useQuery({
    queryKey: ['admin-auditoria', acaoAtiva, page],
    queryFn: () =>
      apiPage<AuditRow>(`/admin/audit-logs?action=${encodeURIComponent(acaoAtiva)}&page=${page}`),
    placeholderData: (anterior) => anterior,
  })

  return (
    <section>
      <h1 className="adm-titulo">Auditoria</h1>
      <p className="adm-sub">Trilha imutável de quem fez o quê — somente leitura.</p>

      <div className="adm-filtros">
        <Input
          label="Filtrar por ação"
          value={acao}
          onChange={(e) => {
            setAcao(e.target.value.toUpperCase())
            setPage(0)
          }}
          placeholder="ex.: ACCESS_GRANTED"
        />
      </div>

      <QueryBoundary query={query}>
        {(pagina) =>
          pagina.data.length === 0 ? (
            <p className="bb-state">Nenhum registro para esse filtro.</p>
          ) : (
            <>
              <div className="adm-tabela-wrap">
                <table className="adm-tabela">
                  <thead>
                    <tr>
                      <th>Quando</th>
                      <th>Ação</th>
                      <th>Entidade</th>
                      <th>Ator</th>
                      <th>Detalhes</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pagina.data.map((a) => (
                      <tr key={a.id}>
                        <td>{dataCurta(a.createdAt)}</td>
                        <td>
                          <span className="adm-status adm-status--atencao">{a.action}</span>
                        </td>
                        <td>
                          {a.entityType ?? '—'}
                          {a.entityId ? ` · ${a.entityId.slice(0, 8)}…` : ''}
                        </td>
                        <td>{a.actorUserId ? `${a.actorUserId.slice(0, 8)}…` : 'sistema'}</td>
                        <td style={{ maxWidth: 320, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {a.metadata ?? '—'}
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
