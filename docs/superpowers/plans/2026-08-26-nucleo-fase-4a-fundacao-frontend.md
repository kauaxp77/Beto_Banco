# Fase 4a — Fundação do Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mover o refresh token para cookie HttpOnly no backend e construir a fundação do novo frontend (`frontend/`) com autenticação completa, guardas de rota, design system mínimo e as telas de aluno (/login, /esqueci-senha, /definir-senha/:token, /dashboard, /perfil).

**Architecture:** O backend passa a entregar o refresh token exclusivamente em cookie `HttpOnly; SameSite=Lax` com path restrito a `/api/v1/auth`; o corpo das respostas de auth deixa de expor o valor. O frontend novo vive em `frontend/` (Vite + React 19 + TypeScript), com access token só em memória, um cliente HTTP com refresh de promise única compartilhada, react-query para dados de servidor e guardas que apenas melhoram UX — quem nega é o backend.

**Tech Stack:** Spring Boot 3.5.6 (backend existente) · Vite 7 + React 19 + TypeScript · react-router-dom 7 · @tanstack/react-query 5 · vitest + @testing-library/react + msw (testes).

**Spec:** `docs/superpowers/specs/2026-08-26-nucleo-pagamento-acesso-design.md` (§6.1–6.2, §8, §9)

## Global Constraints

- Java 21; Maven wrapper com `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot"`.
- Envelope obrigatório: `{success, data}` / `{success:false, error:{code,...}}` (spec §8.1).
- Access token NUNCA em localStorage; apenas em memória (spec §6.2).
- Refresh token NUNCA visível a JavaScript: cookie `HttpOnly`; nome `bb_refresh`; path `/api/v1/auth`; `SameSite=Lax`; `Secure` configurável por perfil (`false` em dev/test).
- `frontend-react/` permanece intocado (spec §9.1).
- Identidade git dos commits: `git -c user.name="kauaxp77" -c user.email="wendesonkaua11@gmail.com"`.
- Tema escuro padrão: fundo `#1A1C20`, dourado `#C4A15A` (spec §9.6); contraste AA verificado sobre fundo escuro.
- Todo teste backend roda com `./mvnw test` (Testcontainers postgres:17-alpine).

---

### Task 1: Cookie HttpOnly no backend — login/refresh/logout

**Files:**
- Create: `backend/src/main/java/com/betobanco/auth/service/RefreshCookies.java`
- Modify: `backend/src/main/java/com/betobanco/auth/controller/AuthController.java`
- Modify: `backend/src/main/java/com/betobanco/auth/dto/TokenResponse.java`
- Modify: `backend/src/main/resources/application.yml` (chave `betobanco.auth.cookie-secure: true`), `application-dev.yml` e `application-test.yml` (`cookie-secure: false`)
- Test: `backend/src/test/java/com/betobanco/auth/RefreshCookieTest.java`
- Modify: `backend/src/test/java/com/betobanco/auth/AuthEndpointsTest.java` (trocar leituras de `$.data.refreshToken` por cookie)

**Interfaces:**
- Produces: cookie `bb_refresh` emitido no login e no refresh; `POST /auth/refresh` e `POST /auth/logout` leem o cookie (sem corpo); `TokenResponse` sem campo `refreshToken` no JSON (`@JsonInclude(NON_NULL)` + valor null).
- Consumes: `RefreshTokenService.emitir/rotacionar/revogar` (existentes, inalterados).

- [ ] **Step 1: Failing test**

```java
package com.betobanco.auth;

import com.betobanco.support.PostgresTestBase;
import com.betobanco.users.api.UserDirectory;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RefreshCookieTest extends PostgresTestBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserDirectory usuarios;

    private MvcResult logar(String email) throws Exception {
        usuarios.registrar(email, "senha-forte-123", "Aluno Cookie");
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"senha-forte-123\"}"))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    void loginEmiteCookieHttpOnlyESemRefreshNoCorpo() throws Exception {
        MvcResult res = logar("cookie1@aluno.com");
        Cookie cookie = res.getResponse().getCookie("bb_refresh");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(res.getResponse().getContentAsString()).doesNotContain("refreshToken");
    }

    @Test
    void refreshUsaCookieERotaciona() throws Exception {
        Cookie cookie = logar("cookie2@aluno.com").getResponse().getCookie("bb_refresh");

        MvcResult renovado = mockMvc.perform(post("/auth/refresh").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        Cookie novo = renovado.getResponse().getCookie("bb_refresh");
        assertThat(novo).isNotNull();
        assertThat(novo.getValue()).isNotEqualTo(cookie.getValue());

        // O cookie antigo ja rotacionou: reusar e indicio de roubo -> 401.
        mockMvc.perform(post("/auth/refresh").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutLimpaOCookieERevogaASessao() throws Exception {
        Cookie cookie = logar("cookie3@aluno.com").getResponse().getCookie("bb_refresh");

        MvcResult saida = mockMvc.perform(post("/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent()).andReturn();
        Cookie limpo = saida.getResponse().getCookie("bb_refresh");
        assertThat(limpo.getMaxAge()).isZero();

        mockMvc.perform(post("/auth/refresh").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshSemCookieDevolve401() throws Exception {
        mockMvc.perform(post("/auth/refresh")).andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Run** `./mvnw test -Dtest=RefreshCookieTest` — Expected: FAIL (cookie ausente / 400 por corpo obrigatório).

- [ ] **Step 3: Implement**

`RefreshCookies.java` (componente do módulo auth):

```java
package com.betobanco.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Fabrica dos cookies de refresh. HttpOnly: o valor nunca e visivel a
 * JavaScript — XSS no frontend nao rouba sessao. Path restrito a /api/v1/auth:
 * o navegador so envia o cookie para os endpoints de auth, nunca para o resto
 * da API. SameSite=Lax pressupoe API em subdominio do mesmo dominio (spec 6.2).
 */
@Component
public class RefreshCookies {

    public static final String NOME = "bb_refresh";

    private final boolean secure;
    private final Duration validade;

    public RefreshCookies(@Value("${betobanco.auth.cookie-secure:true}") boolean secure,
                          @Value("${betobanco.auth.refresh-token-days:30}") long dias) {
        this.secure = secure;
        this.validade = Duration.ofDays(dias);
    }

    public ResponseCookie emitir(String valor) {
        return builder(valor).maxAge(validade).build();
    }

    public ResponseCookie limpar() {
        return builder("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder builder(String valor) {
        return ResponseCookie.from(NOME, valor)
                .httpOnly(true).secure(secure).sameSite("Lax").path("/api/v1/auth");
    }
}
```

`AuthController`: login adiciona `.header(HttpHeaders.SET_COOKIE, cookies.emitir(par.refreshToken()).toString())` e devolve `TokenResponse` sem o refresh; `refresh` e `logout` recebem `@CookieValue(value = RefreshCookies.NOME, required = false) String cookie` (sem `RefreshRequest`), devolvendo 401 (`BusinessException(UNAUTHORIZED, "Sessão inválida ou expirada")`) quando ausente. `TokenResponse` ganha `@JsonInclude(JsonInclude.Include.NON_NULL)` e um factory `bearerSomenteAccess(access, expiresIn)`; conferir se `betobanco.auth.refresh-token-days` já existe no yml (a validade do token opaco) e reutilizar a mesma chave.

- [ ] **Step 4: Run** `./mvnw test -Dtest='RefreshCookieTest,AuthEndpointsTest'` — Expected: RefreshCookieTest PASS; ajustar em `AuthEndpointsTest` os testes que liam `$.data.refreshToken` para usarem o cookie (mesma semântica: rotação, reuso detectado, logout 204). Depois `./mvnw test` completo.

- [ ] **Step 5: Commit** `feat(backend): refresh token em cookie HttpOnly (spec 6.2)`

---

### Task 2: Scaffold do frontend novo

**Files:**
- Create: `frontend/` via `npm create vite@latest frontend -- --template react-ts`
- Create: `frontend/src/styles/tokens.css`, `frontend/src/styles/global.css`
- Modify: `frontend/vite.config.ts` (proxy `/api` → `http://localhost:8080` em dev; vitest config)
- Create: `frontend/.env.example` (`VITE_API_URL=/api/v1`)

**Interfaces:**
- Produces: comando `npm run dev` (porta 5174), `npm test` (vitest), tokens CSS `--bb-bg:#1A1C20`, `--bb-surface:#22252B`, `--bb-gold:#C4A15A`, `--bb-text:#E8E6E1`, `--bb-text-dim:#9A9789`, `--bb-danger:#E5484D`, `--bb-success:#46A758`, espaçamento `--bb-s1..s6` (4/8/12/16/24/40px), raio `--bb-r1:8px`.

- [ ] **Step 1:** `cd` na raiz do repo e rodar `npm create vite@latest frontend -- --template react-ts`, depois `cd frontend && npm install && npm install react-router-dom @tanstack/react-query && npm install -D vitest @testing-library/react @testing-library/user-event @testing-library/jest-dom jsdom msw`.
- [ ] **Step 2:** Escrever `tokens.css`/`global.css` com as variáveis acima, `color-scheme: dark`, reset mínimo; importar em `main.tsx`. `vite.config.ts` com `server.port: 5174`, `server.proxy: {'/api': 'http://localhost:8080'}` e bloco `test` do vitest (`environment: 'jsdom'`, `setupFiles: './src/test/setup.ts'` com `@testing-library/jest-dom`).
- [ ] **Step 3:** `npm run build` e `npx vitest run` (zero testes ainda) — Expected: build OK.
- [ ] **Step 4: Commit** `feat(frontend): scaffold Vite + React 19 + TS com tokens do tema escuro`

---

### Task 3: Cliente HTTP com access token em memória e refresh de promise única

**Files:**
- Create: `frontend/src/api/token.ts`, `frontend/src/api/http.ts`
- Test: `frontend/src/api/http.test.ts`

**Interfaces:**
- Produces: `setAccessToken(t: string | null)`, `getAccessToken(): string | null` (token.ts); `api<T>(path: string, init?: RequestInit): Promise<T>` que devolve `data` do envelope ou lança `ApiError { code: string; status: number; message: string; fieldErrors?: {field:string; message:string}[] }`; `refreshSession(): Promise<boolean>` com promise única compartilhada.
- Consumes: endpoints reais (`/api/v1/...`), cookie gerenciado pelo navegador (`credentials: 'include'` apenas nos caminhos `/auth/`).

- [ ] **Step 1: Failing tests** (vitest, `fetch` mockado com `vi.stubGlobal`):

```ts
import { afterEach, describe, expect, it, vi } from 'vitest';
import { api, refreshSession } from './http';
import { setAccessToken, getAccessToken } from './token';

const ok = (data: unknown) => new Response(JSON.stringify({ success: true, data }), { status: 200 });
const fail = (status: number, code: string) =>
  new Response(JSON.stringify({ success: false, error: { code, message: code, status } }), { status });

afterEach(() => { vi.unstubAllGlobals(); setAccessToken(null); });

describe('api', () => {
  it('desembrulha o envelope e envia Authorization quando ha token', async () => {
    setAccessToken('abc');
    const spy = vi.fn().mockResolvedValue(ok({ nome: 'x' }));
    vi.stubGlobal('fetch', spy);
    await expect(api('/students/me')).resolves.toEqual({ nome: 'x' });
    const [, init] = spy.mock.calls[0];
    expect(new Headers(init.headers).get('Authorization')).toBe('Bearer abc');
  });

  it('em 401 renova UMA vez e refaz a requisicao', async () => {
    setAccessToken('velho');
    const spy = vi.fn()
      .mockResolvedValueOnce(fail(401, 'UNAUTHORIZED'))          // 1a tentativa
      .mockResolvedValueOnce(ok({ accessToken: 'novo', expiresIn: 900 })) // refresh
      .mockResolvedValueOnce(ok({ nome: 'x' }));                 // retry
    vi.stubGlobal('fetch', spy);
    await expect(api('/students/me')).resolves.toEqual({ nome: 'x' });
    expect(getAccessToken()).toBe('novo');
    expect(spy).toHaveBeenCalledTimes(3);
  });

  it('varios 401 simultaneos compartilham UM refresh', async () => {
    setAccessToken('velho');
    let refreshes = 0;
    vi.stubGlobal('fetch', vi.fn((url: string) => {
      if (String(url).includes('/auth/refresh')) {
        refreshes++;
        return Promise.resolve(ok({ accessToken: 'novo', expiresIn: 900 }));
      }
      if (getAccessToken() === 'velho') return Promise.resolve(fail(401, 'UNAUTHORIZED'));
      return Promise.resolve(ok({}));
    }));
    await Promise.all([api('/a'), api('/b'), api('/c')]);
    expect(refreshes).toBe(1);
  });

  it('quando o refresh falha, propaga o 401 e limpa o token', async () => {
    setAccessToken('velho');
    vi.stubGlobal('fetch', vi.fn((url: string) =>
      Promise.resolve(String(url).includes('/auth/refresh')
        ? fail(401, 'UNAUTHORIZED') : fail(401, 'UNAUTHORIZED'))));
    await expect(api('/students/me')).rejects.toMatchObject({ status: 401 });
    expect(getAccessToken()).toBeNull();
  });
});
```

- [ ] **Step 2:** `npx vitest run src/api/http.test.ts` — Expected: FAIL (módulo inexistente).
- [ ] **Step 3: Implement** — `token.ts` com variável de módulo; `http.ts`: base `import.meta.env.VITE_API_URL ?? '/api/v1'`; `credentials: 'include'` só quando o path começa com `/auth`; em `res.status === 401` e path não-auth, aguardar `refreshSession()` (uma promise module-level: se já existe, reutiliza; limpa no `finally`) e refazer uma única vez; `refreshSession` chama `POST /auth/refresh` com `credentials: 'include'`, guarda `accessToken` via `setAccessToken`, devolve `false` (e `setAccessToken(null)`) em falha; erros viram `ApiError` com os campos do envelope.
- [ ] **Step 4:** `npx vitest run` — Expected: PASS.
- [ ] **Step 5: Commit** `feat(frontend): cliente HTTP com refresh de promise unica`

---

### Task 4: Sessão, guardas de rota e shell

**Files:**
- Create: `frontend/src/auth/session.tsx` (contexto: `user: {id,email,fullName,roles} | null`, `status: 'loading'|'in'|'out'`, `login(email,senha)`, `logout()`, bootstrap que tenta `refreshSession()` + `GET /auth/me` ao montar)
- Create: `frontend/src/auth/RequireAuth.tsx`, `frontend/src/auth/RequireRole.tsx`
- Create: `frontend/src/app/AppShell.tsx` (header com logo/links, `<Outlet/>`), `frontend/src/app/router.tsx`, ajuste em `main.tsx` (QueryClientProvider + SessionProvider + RouterProvider)
- Test: `frontend/src/auth/guards.test.tsx`

**Interfaces:**
- Produces: `useSession()` com o shape acima; rotas: `/login`, `/esqueci-senha`, `/definir-senha/:token` públicas; `/dashboard`, `/perfil` sob `<RequireAuth>`; bloco `/admin/*` sob `<RequireRole role="ROLE_ADMIN">` (telas na Fase 4b).
- Consumes: `api`, `refreshSession`, `setAccessToken` (Task 3); `POST /auth/login` (`{email,password}` → `{accessToken,...}` + cookie), `GET /auth/me`, `POST /auth/logout`.

- [ ] **Step 1: Failing test** — render de `<RequireAuth>` com sessão `out` redireciona para `/login` (MemoryRouter + provider com estado injetável para teste); com sessão `in` renderiza o filho; `<RequireRole role="ROLE_ADMIN">` com aluno renderiza mensagem "Sem permissão" (não redireciona para login).
- [ ] **Step 2:** vitest — FAIL. **Step 3:** implementar. Guard em `status==='loading'` renderiza `<Skeleton/>` (nunca decide antes do bootstrap). **Step 4:** vitest PASS. **Step 5: Commit** `feat(frontend): sessao, guardas de rota e shell`

---

### Task 5: Design system mínimo + QueryBoundary

**Files:**
- Create: `frontend/src/ui/Button.tsx`, `Input.tsx`, `Card.tsx`, `Badge.tsx`, `Skeleton.tsx`, `EmptyState.tsx`, `ErrorState.tsx`, `Toast.tsx` (provider + `useToast()`), `QueryBoundary.tsx`
- Test: `frontend/src/ui/QueryBoundary.test.tsx`

**Interfaces:**
- Produces: `QueryBoundary({query, empty?, children})` — deriva loading→`Skeleton`, erro→`ErrorState` (com retry), lista vazia→`EmptyState`, sucesso→`children(data)`; componentes estilizados pelos tokens (spec §9.7: nenhum estado fica a cargo da disciplina da tela).
- Consumes: tokens CSS (Task 2), `useQuery` do react-query.

- [ ] **Step 1: Failing test** — QueryBoundary renderiza Skeleton em `isPending`, ErrorState em `isError`, EmptyState quando `data` é array vazio, children com dados.
- [ ] **Step 2:** FAIL → **Step 3:** implementar (componentes com `className` bb-*, foco visível `outline: 2px solid var(--bb-gold)`, labels associados nos inputs, `aria-live="polite"` nos erros de formulário) → **Step 4:** PASS → **Step 5: Commit** `feat(frontend): design system minimo e QueryBoundary`

---

### Task 6: Telas de autenticação

**Files:**
- Create: `frontend/src/pages/LoginPage.tsx`, `ForgotPasswordPage.tsx`, `DefinePasswordPage.tsx`
- Test: `frontend/src/pages/auth-pages.test.tsx`

**Interfaces:**
- Consumes: `useSession().login`; `POST /auth/forgot-password {email}` (sempre 204); `POST /auth/reset-password {token,password}` (204; token vem de `useParams`).
- Produces: após login, redirect para `state.from ?? '/dashboard'`; `/definir-senha/:token` atende primeiro acesso E recuperação (mesma tela, spec §8.4); mensagens de erro vêm de `ApiError.message`, exibidas em `aria-live`.

- [ ] **Step 1: Failing tests (msw):** login com sucesso chama `/auth/login` e navega; login 401 mostra "Credenciais inválidas"; esqueci-senha mostra a MESMA confirmação para qualquer e-mail ("Se o e-mail existir, você receberá o link"); definir-senha envia o token da URL e redireciona ao login com toast.
- [ ] **Step 2:** FAIL → **Step 3:** implementar → **Step 4:** PASS → **Step 5: Commit** `feat(frontend): telas de login, esqueci-senha e definir-senha`

---

### Task 7: Dashboard mínimo e perfil

**Files:**
- Create: `frontend/src/pages/DashboardPage.tsx` (produtos liberados: `GET /students/me/entitlements` via QueryBoundary, cards com `productName`/`sku`, EmptyState "Você ainda não tem produtos liberados")
- Create: `frontend/src/pages/ProfilePage.tsx` (`GET /students/me`; form nome/telefone → `PUT /students/me`; e-mail somente leitura)
- Test: `frontend/src/pages/dashboard-profile.test.tsx`

**Interfaces:**
- Consumes: DTOs reais: `EntitlementResponse {entitlementId, productId, sku, productName, source, grantedAt, expiresAt}`; `StudentResponse {id, email, fullName, phone}`; `PUT /students/me {fullName, phone}`.

- [ ] **Step 1: Failing tests (msw):** dashboard lista os cards; vazio mostra EmptyState; perfil carrega e salva mostrando toast de sucesso.
- [ ] **Step 2:** FAIL → **Step 3:** implementar → **Step 4:** PASS + `npm run build` → **Step 5: Commit** `feat(frontend): dashboard de produtos liberados e perfil`

---

### Task 8: Verificação de ponta a ponta

- [ ] **Step 1:** `docker compose up -d` + backend `spring-boot:run` (profile dev) + `npm run dev` no `frontend/`.
- [ ] **Step 2:** No navegador: registrar/logar com aluno de teste, F5 mantém a sessão (bootstrap via cookie), logout derruba, `/admin` como aluno mostra "Sem permissão", fluxo esqueci-senha lendo o link no MailHog (http://localhost:8025) e redefinindo.
- [ ] **Step 3:** `./mvnw test` (backend inteiro) + `npx vitest run` + `npm run build`.
- [ ] **Step 4: Commit final** `docs: plano da fase 4a executado` (marcar checkboxes do plano).

## Self-Review

- Cobertura: §6.2 (Task 1), §9.1 (Task 2), §9.2–9.3 (Tasks 3–4), §9.4 (Task 4), §9.6–9.7 (Task 5), §9.5 parcial — telas de aluno (Tasks 6–7); telas admin e porte da landing ficam para a Fase 4b (plano próprio).
- Tipos citados conferidos com os DTOs reais do backend (TokenResponse, StudentResponse, EntitlementResponse).
- Sem placeholders: cada task tem teste e implementação descritos com nomes exatos.
