import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../../api/http'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { brl } from './AdminLayout'

interface Dashboard {
  totalAlunos: number
  alunosBloqueados: number
  produtosAtivos: number
  entitlementsAtivos: number
  pagamentosAprovados: number
  receitaAprovadaCents: number
  webhooksAguardandoAtencao: number
}

const IconeAlerta = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M12 9v4m0 4h.01M10.3 4.6 2.9 18a2 2 0 0 0 1.7 3h14.8a2 2 0 0 0 1.7-3L13.7 4.6a2 2 0 0 0-3.4 0Z" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)

export function AdminDashboardPage() {
  const query = useQuery({
    queryKey: ['admin-dashboard'],
    queryFn: () => api<Dashboard>('/admin/dashboard'),
  })

  return (
    <section>
      <h1 className="adm-titulo">Dashboard</h1>
      <p className="adm-sub">Visão geral da plataforma, agregada de todos os módulos.</p>

      <QueryBoundary query={query}>
        {(d) => (
          <>
            {d.webhooksAguardandoAtencao > 0 && (
              <p className="adm-alerta" role="alert">
                <IconeAlerta />
                <span>
                  {d.webhooksAguardandoAtencao} webhooks aguardando atenção —{' '}
                  <Link to="/admin/webhooks" style={{ color: 'var(--bb-gold)' }}>
                    resolver agora
                  </Link>
                </span>
              </p>
            )}

            <div className="adm-tiles">
              <div className="adm-tile">
                <span className="valor">{d.totalAlunos}</span>
                <span className="rotulo">Alunos</span>
              </div>
              <div className="adm-tile">
                <span className="valor">{d.alunosBloqueados}</span>
                <span className="rotulo">Bloqueados</span>
              </div>
              <div className="adm-tile">
                <span className="valor">{d.produtosAtivos}</span>
                <span className="rotulo">Produtos à venda</span>
              </div>
              <div className="adm-tile">
                <span className="valor">{d.entitlementsAtivos}</span>
                <span className="rotulo">Acessos vigentes</span>
              </div>
              <div className="adm-tile">
                <span className="valor">{d.pagamentosAprovados}</span>
                <span className="rotulo">Pagamentos aprovados</span>
              </div>
              <div className="adm-tile">
                <span className="valor">{brl.format(d.receitaAprovadaCents / 100)}</span>
                <span className="rotulo">Receita aprovada</span>
              </div>
            </div>
          </>
        )}
      </QueryBoundary>
    </section>
  )
}
