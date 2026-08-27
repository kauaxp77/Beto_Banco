import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useSession } from '../auth/session'

export function AppShell() {
  const { status, user, logout } = useSession()
  const navigate = useNavigate()

  async function sair() {
    await logout()
    navigate('/login')
  }

  const ehAdmin = user?.roles.includes('ROLE_ADMIN') ?? false

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <header
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 'var(--bb-s5)',
          padding: 'var(--bb-s4) var(--bb-s5)',
          background: 'var(--bb-surface)',
          borderBottom: '1px solid var(--bb-border)',
        }}
      >
        <Link
          to="/"
          style={{ color: 'var(--bb-gold)', fontWeight: 700, textDecoration: 'none' }}
        >
          Beto Banco
        </Link>

        {status === 'in' && (
          <nav style={{ display: 'flex', gap: 'var(--bb-s4)', flex: 1 }} aria-label="Principal">
            <NavLink to="/dashboard" style={{ color: 'var(--bb-text)' }}>
              Meus produtos
            </NavLink>
            <NavLink to="/perfil" style={{ color: 'var(--bb-text)' }}>
              Perfil
            </NavLink>
            {ehAdmin && (
              <NavLink to="/admin/dashboard" style={{ color: 'var(--bb-text)' }}>
                Admin
              </NavLink>
            )}
          </nav>
        )}

        {status === 'in' ? (
          <button
            type="button"
            onClick={sair}
            style={{
              background: 'transparent',
              color: 'var(--bb-text-dim)',
              border: '1px solid var(--bb-border)',
              borderRadius: 'var(--bb-r1)',
              padding: 'var(--bb-s1) var(--bb-s3)',
              cursor: 'pointer',
            }}
          >
            Sair
          </button>
        ) : (
          <Link to="/login" style={{ marginLeft: 'auto' }}>
            Entrar
          </Link>
        )}
      </header>

      <main style={{ flex: 1, padding: 'var(--bb-s5)', maxWidth: 960, width: '100%', margin: '0 auto' }}>
        <Outlet />
      </main>
    </div>
  )
}
