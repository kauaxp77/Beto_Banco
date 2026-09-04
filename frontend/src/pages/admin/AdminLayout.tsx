import { Link, NavLink, Outlet } from 'react-router-dom'
import { Marca } from '../../ui/Marca'
import './admin.css'

const LINKS = [
  { para: '/admin/dashboard', rotulo: 'Dashboard' },
  { para: '/admin/alunos', rotulo: 'Alunos' },
  { para: '/admin/pagamentos', rotulo: 'Pagamentos' },
  { para: '/admin/webhooks', rotulo: 'Webhooks' },
  { para: '/admin/produtos', rotulo: 'Produtos' },
  { para: '/admin/cursos', rotulo: 'Cursos' },
  { para: '/admin/comentarios', rotulo: 'Comentários' },
  { para: '/admin/relatorios', rotulo: 'Relatórios' },
  { para: '/admin/anuncios', rotulo: 'Anúncios' },
  { para: '/admin/convites', rotulo: 'Convites' },
  { para: '/admin/depoimentos', rotulo: 'Depoimentos' },
  { para: '/admin/auditoria', rotulo: 'Auditoria' },
]

export function AdminLayout() {
  return (
    <div className="adm">
      <aside className="adm-side">
        <Link to="/admin/dashboard" className="adm-logo" aria-label="Aprovação passo a passo — administração">
          <Marca variante="compacta" tamanho={28} />
          <span className="adm-logo-sufixo">Admin</span>
        </Link>
        <nav className="adm-nav" aria-label="Administração">
          {LINKS.map((l) => (
            <NavLink key={l.para} to={l.para}>
              {l.rotulo}
            </NavLink>
          ))}
        </nav>
        <Link to="/dashboard" className="voltar">
          ← Área do aluno
        </Link>
      </aside>
      <main className="adm-main">
        <Outlet />
      </main>
    </div>
  )
}

/** Utilitarios de apresentacao compartilhados pelas telas admin. */
export const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function dataCurta(iso: string | null | undefined): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

export function StatusPill({ valor }: { valor: string }) {
  const tom =
    ['ACTIVE', 'APPROVED', 'PROCESSED', 'SENT'].includes(valor)
      ? 'adm-status--ok'
      : ['BLOCKED', 'CHARGEBACK', 'FAILED', 'MANUAL'].includes(valor)
        ? 'adm-status--erro'
        : ['PENDING', 'RECEIVED', 'REFUNDED', 'CANCELLED', 'IGNORED', 'PROCESSING'].includes(valor)
          ? 'adm-status--atencao'
          : ''
  return <span className={`adm-status ${tom}`.trim()}>{valor}</span>
}
