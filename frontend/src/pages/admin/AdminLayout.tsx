import { Link, NavLink, Outlet } from 'react-router-dom'
import './admin.css'

const LINKS = [
  { para: '/admin/dashboard', rotulo: 'Dashboard' },
  { para: '/admin/alunos', rotulo: 'Alunos' },
  { para: '/admin/pagamentos', rotulo: 'Pagamentos' },
  { para: '/admin/webhooks', rotulo: 'Webhooks' },
  { para: '/admin/produtos', rotulo: 'Produtos' },
  { para: '/admin/auditoria', rotulo: 'Auditoria' },
]

export function AdminLayout() {
  return (
    <div className="adm">
      <aside className="adm-side">
        <Link to="/admin/dashboard" className="adm-logo">
          Beto <em>Banco</em> · Admin
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
