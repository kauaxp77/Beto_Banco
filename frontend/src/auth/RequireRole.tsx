import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useSession } from './session'

/**
 * Aceita um papel ou uma lista deles.
 *
 * A lista existe porque algumas areas do backend admitem mais de um papel — a
 * fila de correcao aceita CORRECTOR ou ADMIN. Um guard mais estrito que a API
 * produz exatamente o que ele deveria evitar: um menu que leva a uma tela de
 * "sem permissao".
 */
export function RequireRole({
  role,
  children,
}: {
  role: string | string[]
  children: ReactNode
}) {
  const { status, user } = useSession()

  if (status === 'loading') return <p aria-busy="true">Carregando…</p>
  if (status === 'out') return <Navigate to="/login" replace />

  const aceitos = Array.isArray(role) ? role : [role]
  if (!aceitos.some((r) => user?.roles.includes(r))) {
    // Logado sem a role: mensagem, nao redirect — mandar para /login um
    // usuario ja autenticado so produziria um loop confuso.
    return <p>Sem permissão para acessar esta área.</p>
  }
  return <>{children}</>
}
