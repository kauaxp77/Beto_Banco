import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useSession } from './session'

/**
 * Guard de UX, nao de seguranca: quem nega acesso de verdade e o backend.
 * Remover este componente por engano produz uma tela vazia, nunca vazamento.
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { status } = useSession()
  const location = useLocation()

  if (status === 'loading') return <p aria-busy="true">Carregando…</p>
  if (status === 'out') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <>{children}</>
}
