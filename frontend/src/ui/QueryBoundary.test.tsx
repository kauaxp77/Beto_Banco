import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it } from 'vitest'
import { QueryBoundary } from './QueryBoundary'

function Harness({ fn, empty }: { fn: () => Promise<unknown>; empty?: ReactNode }) {
  // Key fixa e segura: cada teste monta seu proprio QueryClient.
  const query = useQuery({ queryKey: ['q'], queryFn: fn, retry: false })
  return (
    <QueryBoundary query={query} empty={empty}>
      {(data) => <p>dados: {JSON.stringify(data)}</p>}
    </QueryBoundary>
  )
}

function renderCom(fn: () => Promise<unknown>, empty?: ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <Harness fn={fn} empty={empty} />
    </QueryClientProvider>,
  )
}

describe('QueryBoundary', () => {
  it('mostra skeleton enquanto carrega', () => {
    renderCom(() => new Promise(() => {}))
    expect(screen.getByLabelText(/carregando/i)).toBeInTheDocument()
  })

  it('mostra ErrorState quando a query falha', async () => {
    renderCom(() => Promise.reject(new Error('quebrou')))
    await waitFor(() =>
      expect(screen.getByText(/algo deu errado/i)).toBeInTheDocument(),
    )
  })

  it('mostra EmptyState quando a lista vem vazia', async () => {
    renderCom(() => Promise.resolve([]), <p>nada por aqui</p>)
    await waitFor(() => expect(screen.getByText('nada por aqui')).toBeInTheDocument())
  })

  it('entrega os dados aos filhos no sucesso', async () => {
    renderCom(() => Promise.resolve({ ok: 1 }))
    await waitFor(() =>
      expect(screen.getByText('dados: {"ok":1}')).toBeInTheDocument(),
    )
  })
})
