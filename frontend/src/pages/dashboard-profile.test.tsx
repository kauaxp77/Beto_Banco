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

const cursoResumo = {
  id: 'c1',
  title: 'MENTORIA_PROTOCOLO_BB',
  slug: 'mentoria-protocolo-bb',
  description: null,
  coverUrl: null,
  totalLessons: 10,
  completedLessons: 4,
  nextLessonId: 'a5',
}

const aviso = {
  id: 'av1',
  courseId: null,
  courseTitle: null,
  title: 'Aula extra sábado',
  body: 'Ao vivo às 10h no canal.',
  createdAt: '2026-08-27T09:00:00Z',
}

interface StubExtras {
  avisos?: unknown[]
  stats?: unknown
  trilhas?: unknown[]
  certificados?: unknown[]
  depoimentos?: unknown[]
}

const SEM_STATS = { currentStreak: 0, bestStreak: 0, activeDaysLast30: 0, studiedToday: false }

function stubDashboard(cursos: unknown[], entitlements: unknown[], extras: StubExtras = {}) {
  stubLogado((u) => {
    if (u.includes('/courses/announcements')) return ok(extras.avisos ?? [])
    if (u.includes('/courses/me/stats')) return ok(extras.stats ?? SEM_STATS)
    if (u.includes('/courses/me/tracks')) return ok(extras.trilhas ?? [])
    if (u.includes('/courses/me/certificates')) return ok(extras.certificados ?? [])
    if (u.includes('/courses/me/testimonials')) return ok(extras.depoimentos ?? [])
    if (u.includes('/courses/me')) return ok(cursos)
    if (u.includes('/students/me/entitlements')) return ok(entitlements)
    return null
  })
}

describe('DashboardPage', () => {
  it('lista os cursos liberados com progresso e nome legivel', async () => {
    stubDashboard([cursoResumo], [entitlement])
    renderApp('/dashboard')

    expect(await screen.findByText('Mentoria Protocolo BB')).toBeInTheDocument()
    expect(screen.getByText('4 de 10 aulas')).toBeInTheDocument()
    expect(screen.getByText('40%')).toBeInTheDocument()
    expect(screen.getByText(/continuar/i)).toBeInTheDocument()
  })

  it('mostra os avisos do professor acima dos cursos', async () => {
    stubDashboard([cursoResumo], [entitlement], { avisos: [aviso] })
    renderApp('/dashboard')

    expect(await screen.findByText('Aula extra sábado')).toBeInTheDocument()
    expect(screen.getByText(/geral/i)).toBeInTheDocument()
  })

  it('mostra constancia, trilhas e certificados quando existem', async () => {
    stubDashboard([cursoResumo], [entitlement], {
      stats: { currentStreak: 5, bestStreak: 9, activeDaysLast30: 12, studiedToday: true },
      trilhas: [
        {
          productId: 'p9',
          title: 'COMBO_2EM1_BB_E_CAIXA',
          totalLessons: 24,
          completedLessons: 6,
          courses: [
            { id: 'c1', title: 'Curso BB', totalLessons: 12, completedLessons: 6 },
            { id: 'c2', title: 'Curso Caixa', totalLessons: 12, completedLessons: 0 },
          ],
        },
      ],
      certificados: [
        {
          code: 'BB-ABCDE-FGHJK',
          courseId: 'c1',
          courseTitle: 'Curso BB',
          hours: 8,
          issuedAt: '2026-08-20T00:00:00Z',
        },
      ],
    })
    renderApp('/dashboard')

    expect(await screen.findByText(/5 dias seguidos/)).toBeInTheDocument()
    expect(screen.getByText('Combo 2em1 BB e Caixa')).toBeInTheDocument()
    expect(screen.getByText(/25% · 6\/24 aulas/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /🎓 Curso BB/ })).toHaveAttribute(
      'href',
      '/certificado/BB-ABCDE-FGHJK',
    )
  })

  it('sem cursos mas com compra mostra os produtos aguardando conteudo', async () => {
    stubDashboard([], [entitlement])
    renderApp('/dashboard')

    expect(await screen.findByText('Mentoria Completa')).toBeInTheDocument()
    expect(screen.getByText(/estão sendo preparadas/i)).toBeInTheDocument()
  })

  it('sem cursos nem produtos mostra o empty state', async () => {
    stubDashboard([], [])
    renderApp('/dashboard')

    expect(
      await screen.findByText(/ainda não tem cursos liberados/i),
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
