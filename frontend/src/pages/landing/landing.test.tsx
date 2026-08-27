import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { LandingPage } from './LandingPage'

const ok = (data: unknown) =>
  new Response(JSON.stringify({ success: true, data }), { status: 200 })

afterEach(() => vi.unstubAllGlobals())

function renderLanding() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('LandingPage', () => {
  it('mostra os produtos reais do catalogo quando a API responde', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve(
          ok([
            {
              id: 'p1',
              sku: 'SKU-CAIXA',
              name: 'Mentoria Caixa 2026',
              description: 'Pacote completo.',
              priceCents: 49700,
              currency: 'BRL',
            },
          ]),
        ),
      ),
    )
    renderLanding()

    expect(await screen.findByText('Mentoria Caixa 2026')).toBeInTheDocument()
    expect(screen.getByText('R$ 497,00')).toBeInTheDocument()
    // Com catalogo real, a vitrine estatica sai de cena.
    expect(screen.queryByText('BNDES 2026')).not.toBeInTheDocument()
  })

  it('cai na vitrine estatica quando a API esta fora do ar', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new Error('offline'))))
    renderLanding()

    expect(await screen.findByText('BNDES 2026')).toBeInTheDocument()
    expect(screen.getByText('Caixa Econômica Federal')).toBeInTheDocument()
  })

  it('conteudo institucional independe da API', () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new Error('offline'))))
    renderLanding()

    expect(screen.getByRole('heading', { level: 1 }).textContent).toMatch(
      /um passo de cada vez/i,
    )
    expect(screen.getAllByText(/25\+/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/7 dias de garantia/i).length).toBeGreaterThan(0)
  })
})
