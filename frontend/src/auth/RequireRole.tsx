import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useSession } from './session'

export function RequireRole({ role, children }: { role: string; children: ReactNode }) {
  const { status, user } = useSession()

  if (status === 'loading') return <p aria-busy="true">Carregando…</p>
  if (status === 'out') return <Navigate to="/login" replace />
  if (!user?.roles.includes(role)) {
    // Logado sem a role: mensagem, nao redirect — mandar para /login um
    // usuario ja autenticado so produziria um loop confuso.
    return <p>Sem permissão para acessar esta área.</p>
  }
  return <>{children}</>
}
