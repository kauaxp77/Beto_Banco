import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { setAccessToken } from '../api/token'
import { SessionProvider } from '../auth/session'
import { ToastProvider } from '../ui/Toast'
import { DefinePasswordPage } from './DefinePasswordPage'
import { ForgotPasswordPage } from './ForgotPasswordPage'
import { LoginPage } from './LoginPage'

const ok = (data: unknown) =>
  new Response(JSON.stringify({ success: true, data }), { status: 200 })
const noContent = () => new Response(null, { status: 204 })
const fail = (status: number, code: string, message: string) =>
  new Response(
    JSON.stringify({ success: false, error: { code, message, status } }),
    { status },
  )

afterEach(() => {
  vi.unstubAllGlobals()
  setAccessToken(null)
})

const eu = { id: '1', email: 'a@a.com', fullName: 'Aluno', roles: ['ROLE_STUDENT'] }

function renderApp(caminho: string) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <SessionProvider>
          <MemoryRouter initialEntries={[caminho]}>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/esqueci-senha" element={<ForgotPasswordPage />} />
              <Route path="/definir-senha/:token" element={<DefinePasswordPage />} />
              <Route path="/dashboard" element={<p>tela do dashboard</p>} />
            </Routes>
          </MemoryRouter>
        </SessionProvider>
      </ToastProvider>
    </QueryClientProvider>,
  )
}

/** fetch stub: refresh do bootstrap falha (deslogado) e o resto e roteado. */
function stubFetch(rotas: (url: string) => Response | null) {
  vi.stubGlobal(
    'fetch',
    vi.fn((url: RequestInfo | URL) => {
      const u = String(url)
      if (u.includes('/auth/refresh')) {
        return Promise.resolve(fail(401, 'UNAUTHORIZED', 'sem sessão'))
      }
      const res = rotas(u)
      return Promise.resolve(res ?? fail(404, 'RESOURCE_NOT_FOUND', 'não achei ' + u))
    }),
  )
}

describe('LoginPage', () => {
  it('login valido navega para o dashboard', async () => {
    stubFetch((u) => {
      if (u.includes('/auth/login')) return ok({ accessToken: 'tok', expiresIn: 900 })
      if (u.includes('/auth/me')) return ok(eu)
      return null
    })
    renderApp('/login')

    await userEvent.type(await screen.findByLabelText(/e-mail/i), 'a@a.com')
    await userEvent.type(screen.getByLabelText(/senha/i), 'senha-forte-123')
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }))

    await waitFor(() =>
      expect(screen.getByText('tela do dashboard')).toBeInTheDocument(),
    )
  })

  it('credenciais erradas mostram a mensagem da API', async () => {
    stubFetch((u) => {
      if (u.includes('/auth/login')) {
        return fail(401, 'UNAUTHORIZED', 'Credenciais inválidas')
      }
      return null
    })
    renderApp('/login')

    await userEvent.type(await screen.findByLabelText(/e-mail/i), 'a@a.com')
    await userEvent.type(screen.getByLabelText(/senha/i), 'errada')
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }))

    await waitFor(() =>
      expect(screen.getByText('Credenciais inválidas')).toBeInTheDocument(),
    )
  })
})

describe('ForgotPasswordPage', () => {
  it('mostra a MESMA confirmacao exista o e-mail ou nao', async () => {
    stubFetch((u) => (u.includes('/auth/forgot-password') ? noContent() : null))
    renderApp('/esqueci-senha')

    await userEvent.type(await screen.findByLabelText(/e-mail/i), 'x@x.com')
    await userEvent.click(screen.getByRole('button', { name: /enviar/i }))

    await waitFor(() =>
      expect(screen.getByText(/se o e-mail existir/i)).toBeInTheDocument(),
    )
  })
})

describe('DefinePasswordPage', () => {
  it('envia o token da URL e volta para o login', async () => {
    const chamadas: string[] = []
    stubFetch((u) => {
      if (u.includes('/auth/reset-password')) {
        chamadas.push(u)
        return noContent()
      }
      return null
    })
    const fetchMock = vi.mocked(globalThis.fetch)
    renderApp('/definir-senha/tok-abc-123')

    await userEvent.type(await screen.findByLabelText(/^nova senha$/i), 'senha-nova-123')
    await userEvent.type(screen.getByLabelText(/confirmar/i), 'senha-nova-123')
    await userEvent.click(screen.getByRole('button', { name: /definir/i }))

    await waitFor(() => expect(chamadas).toHaveLength(1))
    const corpo = JSON.parse(
      String((fetchMock.mock.calls.find((c) => String(c[0]).includes('reset-password')))?.[1]?.body),
    )
    expect(corpo).toEqual({ token: 'tok-abc-123', password: 'senha-nova-123' })

    await waitFor(() => expect(screen.getByLabelText(/e-mail/i)).toBeInTheDocument())
  })

  it('senhas diferentes nao enviam nada', async () => {
    stubFetch(() => null)
    renderApp('/definir-senha/tok-abc-123')

    await userEvent.type(await screen.findByLabelText(/^nova senha$/i), 'senha-nova-123')
    await userEvent.type(screen.getByLabelText(/confirmar/i), 'outra-coisa')
    await userEvent.click(screen.getByRole('button', { name: /definir/i }))

    expect(await screen.findByText(/senhas não conferem/i)).toBeInTheDocument()
  })
})
