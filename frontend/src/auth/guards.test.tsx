import { render, screen } from '@testing-library/react'
import type { ReactNode } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { RequireAuth } from './RequireAuth'
import { RequireRole } from './RequireRole'
import { SessionContext, type SessionState, type SessionUser } from './session'

const aluno: SessionUser = {
  id: '1',
  email: 'a@a.com',
  fullName: 'Aluno',
  roles: ['ROLE_STUDENT'],
}

function renderCom(estado: Partial<SessionState>, ui: ReactNode) {
  const base: SessionState = {
    user: null,
    status: 'out',
    login: async () => {},
    logout: async () => {},
    ...estado,
  }
  return render(
    <SessionContext.Provider value={base}>
      <MemoryRouter initialEntries={['/protegida']}>
        <Routes>
          <Route path="/login" element={<p>tela de login</p>} />
          <Route path="/protegida" element={ui} />
        </Routes>
      </MemoryRouter>
    </SessionContext.Provider>,
  )
}

describe('RequireAuth', () => {
  it('deslogado redireciona para /login', () => {
    renderCom({ status: 'out' }, (
      <RequireAuth>
        <p>conteudo secreto</p>
      </RequireAuth>
    ))
    expect(screen.getByText('tela de login')).toBeInTheDocument()
    expect(screen.queryByText('conteudo secreto')).not.toBeInTheDocument()
  })

  it('logado renderiza o conteudo', () => {
    renderCom({ status: 'in', user: aluno }, (
      <RequireAuth>
        <p>conteudo secreto</p>
      </RequireAuth>
    ))
    expect(screen.getByText('conteudo secreto')).toBeInTheDocument()
  })

  it('durante o bootstrap nao decide nada', () => {
    renderCom({ status: 'loading' }, (
      <RequireAuth>
        <p>conteudo secreto</p>
      </RequireAuth>
    ))
    expect(screen.queryByText('conteudo secreto')).not.toBeInTheDocument()
    expect(screen.queryByText('tela de login')).not.toBeInTheDocument()
  })
})

describe('RequireRole', () => {
  it('sem a role mostra "Sem permissão" (quem nega de verdade e o backend)', () => {
    renderCom({ status: 'in', user: aluno }, (
      <RequireRole role="ROLE_ADMIN">
        <p>painel admin</p>
      </RequireRole>
    ))
    expect(screen.getByText(/sem permissão/i)).toBeInTheDocument()
    expect(screen.queryByText('painel admin')).not.toBeInTheDocument()
  })

  it('com a role renderiza o conteudo', () => {
    renderCom(
      { status: 'in', user: { ...aluno, roles: ['ROLE_ADMIN'] } },
      (
        <RequireRole role="ROLE_ADMIN">
          <p>painel admin</p>
        </RequireRole>
      ),
    )
    expect(screen.getByText('painel admin')).toBeInTheDocument()
  })
})
