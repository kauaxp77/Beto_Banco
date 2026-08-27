import { getAccessToken, setAccessToken } from './token'

const BASE: string = import.meta.env.VITE_API_URL ?? '/api/v1'

export interface FieldError {
  field: string
  message: string
}

/** Erro do envelope da API: `code` e contrato estavel; `message` e exibivel. */
export class ApiError extends Error {
  constructor(
    readonly code: string,
    readonly status: number,
    message: string,
    readonly fieldErrors?: FieldError[],
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

interface ErrorBody {
  code?: string
  message?: string
  fieldErrors?: FieldError[]
}

interface Envelope<T> {
  success: boolean
  data?: T
  error?: ErrorBody
}

let refreshEmAndamento: Promise<boolean> | null = null

/**
 * Renova o access token usando o cookie HttpOnly. Promise UNICA compartilhada
 * (spec 9.3): o refresh token rotaciona a cada uso, entao dois refreshes
 * concorrentes fariam o segundo chegar com token ja substituido — o backend
 * trata como roubo e derruba todas as sessoes do usuario.
 */
export function refreshSession(): Promise<boolean> {
  refreshEmAndamento ??= (async () => {
    try {
      const res = await fetch(`${BASE}/auth/refresh`, {
        method: 'POST',
        credentials: 'include',
      })
      if (!res.ok) {
        setAccessToken(null)
        return false
      }
      const body = (await res.json()) as Envelope<{ accessToken: string }>
      setAccessToken(body.data?.accessToken ?? null)
      return getAccessToken() !== null
    } catch {
      setAccessToken(null)
      return false
    }
  })().finally(() => {
    refreshEmAndamento = null
  })
  return refreshEmAndamento
}

function executar(path: string, init: RequestInit): Promise<Response> {
  const headers = new Headers(init.headers)
  const token = getAccessToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  return fetch(`${BASE}${path}`, {
    ...init,
    headers,
    // O cookie de refresh tem path /api/v1/auth: so os endpoints de auth
    // precisam (e devem) enviar credenciais.
    credentials: path.startsWith('/auth') ? 'include' : 'same-origin',
  })
}

async function desembrulhar<T>(res: Response): Promise<T> {
  let body: Envelope<T> | null = null
  try {
    body = (await res.json()) as Envelope<T>
  } catch {
    // 204 e afins: sem corpo.
  }
  if (res.ok && (body === null || body.success)) {
    return body?.data as T
  }
  const err = body?.error ?? {}
  throw new ApiError(
    err.code ?? 'UNKNOWN',
    res.status,
    err.message ?? 'Erro inesperado',
    err.fieldErrors,
  )
}

/** Chama a API e devolve o `data` do envelope, ou lanca {@link ApiError}. */
export async function api<T = unknown>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await executar(path, init)

  if (res.status === 401 && !path.startsWith('/auth')) {
    const renovado = await refreshSession()
    if (renovado) {
      return desembrulhar<T>(await executar(path, init))
    }
  }

  return desembrulhar<T>(res)
}
