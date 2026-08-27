import { afterEach, describe, expect, it, vi } from 'vitest'
import { api, apiPage } from './http'
import { getAccessToken, setAccessToken } from './token'

const ok = (data: unknown) =>
  new Response(JSON.stringify({ success: true, data }), { status: 200 })

const fail = (status: number, code: string) =>
  new Response(JSON.stringify({ success: false, error: { code, message: code, status } }), {
    status,
  })

afterEach(() => {
  vi.unstubAllGlobals()
  setAccessToken(null)
})

describe('api', () => {
  it('desembrulha o envelope e envia Authorization quando ha token', async () => {
    setAccessToken('abc')
    const spy = vi.fn().mockResolvedValue(ok({ nome: 'x' }))
    vi.stubGlobal('fetch', spy)

    await expect(api('/students/me')).resolves.toEqual({ nome: 'x' })

    const [, init] = spy.mock.calls[0] as [string, RequestInit]
    expect(new Headers(init.headers).get('Authorization')).toBe('Bearer abc')
  })

  it('em 401 renova UMA vez e refaz a requisicao', async () => {
    setAccessToken('velho')
    const spy = vi
      .fn()
      .mockResolvedValueOnce(fail(401, 'UNAUTHORIZED')) // primeira tentativa
      .mockResolvedValueOnce(ok({ accessToken: 'novo', expiresIn: 900 })) // refresh
      .mockResolvedValueOnce(ok({ nome: 'x' })) // retry
    vi.stubGlobal('fetch', spy)

    await expect(api('/students/me')).resolves.toEqual({ nome: 'x' })
    expect(getAccessToken()).toBe('novo')
    expect(spy).toHaveBeenCalledTimes(3)
  })

  it('varios 401 simultaneos compartilham UM refresh', async () => {
    setAccessToken('velho')
    let refreshes = 0
    vi.stubGlobal(
      'fetch',
      vi.fn((url: RequestInfo | URL) => {
        if (String(url).includes('/auth/refresh')) {
          refreshes++
          return Promise.resolve(ok({ accessToken: 'novo', expiresIn: 900 }))
        }
        if (getAccessToken() === 'velho') return Promise.resolve(fail(401, 'UNAUTHORIZED'))
        return Promise.resolve(ok({}))
      }),
    )

    await Promise.all([api('/a'), api('/b'), api('/c')])

    // O refresh rotaciona: dois refreshes concorrentes seriam interpretados
    // como roubo pelo backend e derrubariam o usuario (spec 9.3).
    expect(refreshes).toBe(1)
  })

  it('quando o refresh falha, propaga o 401 e limpa o token', async () => {
    setAccessToken('velho')
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.resolve(fail(401, 'UNAUTHORIZED'))),
    )

    await expect(api('/students/me')).rejects.toMatchObject({ status: 401 })
    expect(getAccessToken()).toBeNull()
  })

  it('apiPage preserva data E pagination do envelope paginado', async () => {
    setAccessToken('abc')
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            success: true,
            data: [{ id: '1' }],
            pagination: { page: 0, size: 20, totalElements: 41, totalPages: 3 },
          }),
          { status: 200 },
        ),
      ),
    )

    const pagina = await apiPage<{ id: string }>('/admin/payments')
    expect(pagina.data).toEqual([{ id: '1' }])
    expect(pagina.pagination.totalPages).toBe(3)
  })

  it('erro do envelope vira ApiError com code e fieldErrors', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            success: false,
            error: {
              code: 'VALIDATION_ERROR',
              message: 'Campos inválidos',
              status: 422,
              fieldErrors: [{ field: 'email', message: 'obrigatório' }],
            },
          }),
          { status: 422 },
        ),
      ),
    )

    await expect(api('/auth/register', { method: 'POST' })).rejects.toMatchObject({
      code: 'VALIDATION_ERROR',
      status: 422,
      fieldErrors: [{ field: 'email', message: 'obrigatório' }],
    })
  })
})
