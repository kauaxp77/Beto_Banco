import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { api, refreshSession } from '../api/http'
import { setAccessToken } from '../api/token'

export interface SessionUser {
  id: string
  email: string
  fullName: string
  roles: string[]
}

export interface SessionState {
  user: SessionUser | null
  /** 'loading' durante o bootstrap: nenhum guard decide antes disso. */
  status: 'loading' | 'in' | 'out'
  login: (email: string, senha: string) => Promise<void>
  logout: () => Promise<void>
}

export const SessionContext = createContext<SessionState | null>(null)

export function useSession(): SessionState {
  const ctx = useContext(SessionContext)
  if (!ctx) throw new Error('useSession fora do SessionProvider')
  return ctx
}

interface MeResponse {
  id: string
  email: string
  fullName: string
  roles: string[]
}

interface LoginData {
  accessToken: string
  expiresIn: number
  tokenType: string
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(null)
  const [status, setStatus] = useState<SessionState['status']>('loading')

  // Bootstrap: o access token vive so em memoria, entao um F5 o perde. O
  // cookie HttpOnly sobrevive — tentar um refresh silencioso e o que mantem
  // o aluno logado entre recargas.
  useEffect(() => {
    let ativo = true
    ;(async () => {
      const renovado = await refreshSession()
      if (!renovado) {
        if (ativo) setStatus('out')
        return
      }
      try {
        const me = await api<MeResponse>('/auth/me')
        if (ativo) {
          setUser(me)
          setStatus('in')
        }
      } catch {
        if (ativo) setStatus('out')
      }
    })()
    return () => {
      ativo = false
    }
  }, [])

  const value = useMemo<SessionState>(
    () => ({
      user,
      status,
      login: async (email, senha) => {
        const tokens = await api<LoginData>('/auth/login', {
          method: 'POST',
          body: JSON.stringify({ email, password: senha }),
        })
        setAccessToken(tokens.accessToken)
        const me = await api<MeResponse>('/auth/me')
        setUser(me)
        setStatus('in')
      },
      logout: async () => {
        try {
          await api('/auth/logout', { method: 'POST' })
        } finally {
          setAccessToken(null)
          setUser(null)
          setStatus('out')
        }
      },
    }),
    [user, status],
  )

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}
