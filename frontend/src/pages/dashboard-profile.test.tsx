import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { setAccessToken } from '../api/token'
import { SessionProvider } from '../auth/session'
import { ToastProvider } from '../ui/Toast'
import { DashboardPage } from './DashboardPage'
import { ProfilePage } from './ProfilePage'

const ok = (data: unknown) =>
  new Response(JSON.stringify({ success: true, data }), { status: 200 })

const eu = { id: '1', email: 'a@a.com', fullName: 'Aluno', roles: ['ROLE_STUDENT'] }

afterEach(() => {
  vi.unstubAllGlobals()
  setAccessToken(null)
})

/** Sessao logada: refresh e /auth/me respondem; o resto vem das rotas. */
function stubLogado(rotas: (url: string, init?: RequestInit) => Response | null) {
  vi.stubGlobal(
    'fetch',
    vi.fn((url: RequestInfo | URL, init?: RequestInit) => {
      const u = String(url)
      if (u.includes('/auth/refresh')) return Promise.resolve(ok({ accessToken: 't', expiresIn: 900 }))
      if (u.includes('/auth/me')) return Promise.resolve(ok(eu))
      const res = rotas(u, init)
      if (!res) throw new Error('rota sem stub: ' + u)
      return Promise.resolve(res)
    }),
  )
}

function renderApp(caminho: string) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <SessionProvider>
          <MemoryRouter initialEntries={[caminho]}>
            <Routes>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/perfil" element={<ProfilePage />} />
            </Routes>
          </MemoryRouter>
        </SessionProvider>
      </ToastProvider>
    </QueryClientProvider>,
  )
}

const entitlement = {
  entitlementId: 'e1',
  productId: 'p1',
  sku: 'SKU-X',
  productName: 'Mentoria Completa',
  source: 'PAYMENT',
  grantedAt: '2026-08-26T00:00:00Z',
  expiresAt: null,
}

describe('DashboardPage', () => {
  it('lista os produtos liberados', async () => {
    stubLogado((u) => (u.includes('/students/me/entitlements') ? ok([entitlement]) : null))
    renderApp('/dashboard')

    expect(await screen.findByText('Mentoria Completa')).toBeInTheDocument()
    expect(screen.getByText('SKU-X')).toBeInTheDocument()
  })

  it('sem produtos mostra o empty state', async () => {
    stubLogado((u) => (u.includes('/students/me/entitlements') ? ok([]) : null))
    renderApp('/dashboard')

    expect(
      await screen.findByText(/ainda não tem produtos liberados/i),
    ).toBeInTheDocument()
  })
})

describe('ProfilePage', () => {
  it('carrega o perfil e salva alteracoes', async () => {
    const puts: RequestInit[] = []
    stubLogado((u, init) => {
      if (u.includes('/students/me') && init?.method === 'PUT') {
        puts.push(init)
        return ok({ id: '1', email: 'a@a.com', fullName: 'Novo Nome', phone: '119999' })
      }
      if (u.includes('/students/me')) {
        return ok({ id: '1', email: 'a@a.com', fullName: 'Aluno', phone: null })
      }
      return null
    })
    renderApp('/perfil')

    const nome = await screen.findByLabelText(/nome/i)
    await waitFor(() => expect(nome).toHaveValue('Aluno'))

    await userEvent.clear(nome)
    await userEvent.type(nome, 'Novo Nome')
    await userEvent.type(screen.getByLabelText(/telefone/i), '119999')
    await userEvent.click(screen.getByRole('button', { name: /salvar/i }))

    await waitFor(() => expect(puts).toHaveLength(1))
    expect(JSON.parse(String(puts[0].body))).toEqual({
      fullName: 'Novo Nome',
      phone: '119999',
    })
    expect(await screen.findByText(/perfil atualizado/i)).toBeInTheDocument()

    // E-mail e somente leitura: identidade nao se edita por aqui.
    expect(screen.getByLabelText(/e-mail/i)).toHaveAttribute('readonly')
  })
})
