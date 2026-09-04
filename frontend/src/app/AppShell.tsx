import { useQuery } from '@tanstack/react-query'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { api } from '../api/http'
import { useSession } from '../auth/session'
import { Marca } from '../ui/Marca'
import { IconeSair, IconeSino } from '../ui/icons'
import './shell.css'

interface AnnouncementItem {
  id: string
}

/**
 * Sino de avisos.
 *
 * Mostra a contagem real de anúncios do professor, e não um enfeite: um sino
 * que nunca acende ensina a pessoa a ignorá-lo, e aí ele deixa de servir no dia
 * em que houver algo importante.
 *
 * Reaproveita a chave de cache do dashboard, então a barra não gera uma
 * segunda requisição em cima da que a tela já faz.
 */
function SinoDeAvisos() {
  const query = useQuery({
    queryKey: ['meus-anuncios'],
    queryFn: () => api<AnnouncementItem[]>('/courses/announcements'),
  })

  const quantos = query.data?.length ?? 0
  const rotulo = quantos === 0
    ? 'Avisos — nenhum no momento'
    : `Avisos — ${quantos} ${quantos === 1 ? 'novo' : 'novos'}`

  return (
    <Link to="/dashboard" className="shell-sino" aria-label={rotulo} title={rotulo}>
      <IconeSino size={19} />
      {quantos > 0 && (
        <span className="shell-sino-contador" aria-hidden="true">
          {quantos > 9 ? '9+' : quantos}
        </span>
      )}
    </Link>
  )
}

export function AppShell() {
  const { status, user, logout } = useSession()
  const navigate = useNavigate()

  async function sair() {
    await logout()
    navigate('/login')
  }

  const ehAdmin = user?.roles.includes('ROLE_ADMIN') ?? false
  // Admin tambem corrige: o backend aceita CORRECTOR ou ADMIN na fila.
  const ehCorretor = ehAdmin || (user?.roles.includes('ROLE_CORRECTOR') ?? false)

  return (
    <div className="shell">
      <header className="shell-topo">
        <Link to="/" className="shell-logo" aria-label="Aprovação passo a passo">
          <Marca variante="compacta" tamanho={32} />
        </Link>

        {status === 'in' && (
          <>
            <nav className="shell-nav" aria-label="Principal">
              <NavLink to="/dashboard">Meus cursos</NavLink>
              <NavLink to="/concursos">Concursos</NavLink>
              <NavLink to="/redacoes">Redações</NavLink>
              {ehCorretor && <NavLink to="/correcoes">Correções</NavLink>}
              <NavLink to="/perfil">Perfil</NavLink>
              {ehAdmin && <NavLink to="/admin/dashboard">Admin</NavLink>}
            </nav>

            <div className="shell-acoes">
              <SinoDeAvisos />
              <button type="button" className="shell-sair" onClick={sair}>
                <IconeSair size={17} />
                <span>Sair</span>
              </button>
            </div>
          </>
        )}

        {status !== 'in' && (
          <Link to="/login" className="shell-entrar">
            Entrar
          </Link>
        )}
      </header>

      <main className="shell-conteudo">
        <Outlet />
      </main>
    </div>
  )
}
