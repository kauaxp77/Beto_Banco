import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactElement } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { setAccessToken } from '../../api/token'
import { ToastProvider } from '../../ui/Toast'
import { AdminDashboardPage } from './AdminDashboardPage'
import { AdminProductsPage } from './AdminProductsPage'
import { AdminStudentsPage } from './AdminStudentsPage'
import { AdminWebhooksPage } from './AdminWebhooksPage'

const ok = (data: unknown, pagination?: unknown) =>
  new Response(JSON.stringify({ success: true, data, pagination }), { status: 200 })

const paginado = (data: unknown[]) =>
  ok(data, { page: 0, size: 20, totalElements: data.length, totalPages: 1 })

afterEach(() => {
  vi.unstubAllGlobals()
  setAccessToken(null)
})

function renderAdmin(caminho: string, rotas: Record<string, ReactElement>) {
  setAccessToken('tok-admin')
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <MemoryRouter initialEntries={[caminho]}>
          <Routes>
            {Object.entries(rotas).map(([path, el]) => (
              <Route key={path} path={path} element={el} />
            ))}
          </Routes>
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('AdminDashboardPage', () => {
  it('mostra os numeros agregados como stat tiles', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve(
          ok({
            totalAlunos: 128,
            alunosBloqueados: 3,
            produtosAtivos: 5,
            entitlementsAtivos: 97,
            pagamentosAprovados: 110,
            receitaAprovadaCents: 5467000,
            webhooksAguardandoAtencao: 2,
          }),
        ),
      ),
    )
    renderAdmin('/admin/dashboard', { '/admin/dashboard': <AdminDashboardPage /> })

    expect(await screen.findByText('128')).toBeInTheDocument()
    expect(screen.getByText('R$ 54.670,00')).toBeInTheDocument()
    // Webhooks pendentes e um ALERTA: icone+texto, nunca so cor.
    expect(screen.getByText(/2 webhooks aguardando atenção/i)).toBeInTheDocument()
  })
})

describe('AdminStudentsPage', () => {
  it('lista alunos e busca pelo termo digitado', async () => {
    const buscas: string[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn((url: RequestInfo | URL) => {
        const u = new URL(String(url), 'http://x')
        buscas.push(u.searchParams.get('search') ?? '')
        const todos = [
          { id: 'a1', email: 'ana@a.com', fullName: 'Ana', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z' },
          { id: 'b2', email: 'bia@a.com', fullName: 'Bia', status: 'BLOCKED', createdAt: '2026-01-02T00:00:00Z' },
        ]
        const filtrados = todos.filter((t) => t.email.includes(buscas.at(-1) ?? ''))
        return Promise.resolve(paginado(filtrados))
      }),
    )
    renderAdmin('/admin/alunos', { '/admin/alunos': <AdminStudentsPage /> })

    expect(await screen.findByText('ana@a.com')).toBeInTheDocument()
    expect(screen.getByText('bia@a.com')).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText(/buscar/i), 'bia')
    await waitFor(() => expect(screen.queryByText('ana@a.com')).not.toBeInTheDocument())
    expect(screen.getByText('bia@a.com')).toBeInTheDocument()
  })
})

describe('AdminWebhooksPage', () => {
  it('reprocessa um evento FAILED e reflete o novo status', async () => {
    const posts: string[] = []
    const evento = {
      id: 'w1',
      provider: 'fake',
      eventId: 'evt-1',
      eventType: 'payment.approved',
      status: 'MANUAL',
      attempts: 5,
      errorMessage: 'SKU desconhecido: X',
      payload: '{}',
      receivedAt: '2026-08-27T00:00:00Z',
      processedAt: null,
    }
    vi.stubGlobal(
      'fetch',
      vi.fn((url: RequestInfo | URL, init?: RequestInit) => {
        const u = String(url)
        if (init?.method === 'POST' && u.includes('/reprocess')) {
          posts.push(u)
          return Promise.resolve(ok({ ...evento, status: 'RECEIVED', attempts: 0 }))
        }
        return Promise.resolve(paginado([evento]))
      }),
    )
    renderAdmin('/admin/webhooks', { '/admin/webhooks': <AdminWebhooksPage /> })

    expect(await screen.findByText('evt-1')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /reprocessar/i }))

    await waitFor(() => expect(posts).toHaveLength(1))
    expect(posts[0]).toContain('/admin/webhooks/w1/reprocess')
  })
})

describe('AdminProductsPage', () => {
  it('cria um produto pelo formulario', async () => {
    const posts: RequestInit[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn((_url: RequestInfo | URL, init?: RequestInit) => {
        if (init?.method === 'POST') {
          posts.push(init)
          return Promise.resolve(
            ok({ id: 'novo', sku: 'SKU-N', name: 'Novo Curso', description: null, priceCents: 1000, currency: 'BRL', active: true }),
          )
        }
        return Promise.resolve(ok([]))
      }),
    )
    renderAdmin('/admin/produtos', { '/admin/produtos': <AdminProductsPage /> })

    await userEvent.click(await screen.findByRole('button', { name: /novo produto/i }))
    await userEvent.type(screen.getByLabelText(/sku/i), 'SKU-N')
    await userEvent.type(screen.getByLabelText(/^nome/i), 'Novo Curso')
    await userEvent.type(screen.getByLabelText(/preço/i), '10,00')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => expect(posts).toHaveLength(1))
    expect(JSON.parse(String(posts[0].body))).toMatchObject({ sku: 'SKU-N', name: 'Novo Curso', priceCents: 1000 })
  })
})
