import { useQuery } from '@tanstack/react-query'
import { api } from '../api/http'
import { Badge, Card } from '../ui/basics'
import { QueryBoundary } from '../ui/QueryBoundary'

interface EntitlementResponse {
  entitlementId: string
  productId: string
  sku: string | null
  productName: string | null
  source: string
  grantedAt: string
  expiresAt: string | null
}

export function DashboardPage() {
  const query = useQuery({
    queryKey: ['meus-entitlements'],
    queryFn: () => api<EntitlementResponse[]>('/students/me/entitlements'),
  })

  return (
    <section>
      <h1>Meus produtos</h1>
      <QueryBoundary
        query={query}
        empty="Você ainda não tem produtos liberados. Assim que um pagamento for confirmado, eles aparecem aqui."
      >
        {(itens) => (
          <div
            style={{
              display: 'grid',
              gap: 'var(--bb-s4)',
              gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
            }}
          >
            {itens.map((e) => (
              <Card key={e.entitlementId}>
                <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>
                  {e.productName ?? 'Produto'}
                </h2>
                {e.sku && <Badge>{e.sku}</Badge>}
                <p style={{ color: 'var(--bb-text-dim)', fontSize: '0.85rem' }}>
                  Liberado em {new Date(e.grantedAt).toLocaleDateString('pt-BR')}
                </p>
              </Card>
            ))}
          </div>
        )}
      </QueryBoundary>
    </section>
  )
}
