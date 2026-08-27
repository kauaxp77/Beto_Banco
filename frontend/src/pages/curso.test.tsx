import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { setAccessToken } from '../api/token'
import { CursoPage } from './CursoPage'

const ok = (data: unknown) =>
  new Response(JSON.stringify({ success: true, data }), { status: 200 })

afterEach(() => {
  vi.unstubAllGlobals()
  setAccessToken(null)
})

const detalhe = {
  id: 'c1',
  title: 'MENTORIA_PROTOCOLO_BB',
  description: null,
  coverUrl: null,
  modules: [
    {
      id: 'm1',
      title: 'Módulo 1',
      position: 0,
      lessons: [
        {
          id: 'a1',
          title: 'Boas-vindas',
          description: 'Comece por aqui.',
          videoUrl: 'https://www.youtube.com/watch?v=abc123def45',
          durationSeconds: 600,
          position: 0,
          completed: true,
          materials: [{ id: 'mat1', title: 'Apostila em PDF', url: 'https://cdn.x/a.pdf' }],
        },
        {
          id: 'a2',
          title: 'Edital comentado',
          description: null,
          videoUrl: null,
          durationSeconds: null,
          position: 1,
          completed: false,
          materials: [],
        },
      ],
    },
  ],
}

const discussao = {
  comments: [
    {
      id: 'com1',
      parentId: null,
      body: 'Professor, cai na prova?',
      authorName: 'Ana Aluna',
      instructor: false,
      mine: false,
      createdAt: '2026-08-26T10:00:00Z',
    },
    {
      id: 'com2',
      parentId: 'com1',
      body: 'Cai sim, foco total!',
      authorName: 'Beto Fernandes',
      instructor: true,
      mine: false,
      createdAt: '2026-08-26T11:00:00Z',
    },
  ],
  helpfulCount: 12,
  notHelpfulCount: 1,
  myRating: null,
}

/** Roteia curso e discussao; devolve tambem as chamadas capturadas. */
function stubCurso() {
  const chamadas: { url: string; method?: string }[] = []
  vi.stubGlobal(
    'fetch',
    vi.fn((url: RequestInfo | URL, init?: RequestInit) => {
      const u = String(url)
      chamadas.push({ url: u, method: init?.method })
      if (u.includes('/discussion')) return Promise.resolve(ok(discussao))
      if (u.includes('/complete') || u.includes('/rating') || u.includes('/comments')) {
        return Promise.resolve(ok(null))
      }
      return Promise.resolve(ok(detalhe))
    }),
  )
  return chamadas
}

function renderCurso() {
  setAccessToken('tok')
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/curso/c1']}>
        <Routes>
          <Route path="/curso/:id" element={<CursoPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('CursoPage', () => {
  it('abre na primeira aula nao concluida e lista o conteudo', async () => {
    stubCurso()
    renderCurso()

    expect(await screen.findByText('Mentoria Protocolo BB')).toBeInTheDocument()
    expect(screen.getByText('Módulo 1')).toBeInTheDocument()

    // A aula ativa e a nao concluida (a2), que nao tem video.
    expect(screen.getByRole('heading', { name: 'Edital comentado' })).toBeInTheDocument()
    expect(screen.getByText(/ainda não tem vídeo/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /concluir aula/i })).toBeInTheDocument()
  })

  it('trocar de aula carrega o player, materiais e discussao', async () => {
    stubCurso()
    renderCurso()

    // Troca para a aula 1 (com video do YouTube).
    await userEvent.click(await screen.findByRole('button', { name: /Boas-vindas/ }))
    expect(screen.getByTitle('Boas-vindas')).toHaveAttribute(
      'src',
      'https://www.youtube-nocookie.com/embed/abc123def45',
    )

    // Materiais complementares da aula.
    expect(screen.getByRole('link', { name: /Apostila em PDF/ })).toHaveAttribute(
      'href',
      'https://cdn.x/a.pdf',
    )

    // Discussao: comentario do aluno, resposta com selo de professor e votos.
    expect(await screen.findByText('Professor, cai na prova?')).toBeInTheDocument()
    expect(screen.getByText('Cai sim, foco total!')).toBeInTheDocument()
    expect(screen.getByText('Professor')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Sim \(12\)/ })).toBeInTheDocument()
  })

  it('concluir dispara DELETE quando a aula ja esta concluida', async () => {
    const chamadas = stubCurso()
    renderCurso()

    await userEvent.click(await screen.findByRole('button', { name: /Boas-vindas/ }))
    await userEvent.click(screen.getByRole('button', { name: /desfazer/i }))
    await waitFor(() =>
      expect(
        chamadas.some((c) => c.url.includes('/lessons/a1/complete') && c.method === 'DELETE'),
      ).toBe(true),
    )
  })

  it('comentar envia POST com o texto e busca filtra as aulas', async () => {
    const chamadas = stubCurso()
    renderCurso()

    // Comentar na aula ativa.
    const campo = await screen.findByPlaceholderText(/escreva sua dúvida/i)
    await userEvent.type(campo, 'Tenho uma dúvida na questão 3')
    await userEvent.click(screen.getByRole('button', { name: /^comentar$/i }))
    await waitFor(() =>
      expect(
        chamadas.some(
          (c) => c.url.includes('/lessons/a2/comments') && c.method === 'POST',
        ),
      ).toBe(true),
    )

    // Busca: "edital" deixa so a aula 2 na lista.
    await userEvent.type(screen.getByPlaceholderText(/buscar aula/i), 'edital')
    expect(screen.queryByRole('button', { name: /Boas-vindas/ })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Edital comentado/ })).toBeInTheDocument()
  })
})
