/**
 * Access token exclusivamente em memoria (spec 6.2). Nunca localStorage:
 * qualquer XSS leria o storage inteiro; uma variavel de modulo morre com a
 * aba e nao e enumeravel de fora.
 */
let accessToken: string | null = null

export function setAccessToken(token: string | null): void {
  accessToken = token
}

export function getAccessToken(): string | null {
  return accessToken
}
