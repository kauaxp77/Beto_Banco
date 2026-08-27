import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api, apiPage } from '../../api/http'
import { GraficoArea, GraficoBarras, GraficoDonut } from '../../ui/charts'
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

interface PaymentRow {
  amountCents: number
  status: string
  approvedAt: string | null
  createdAt: string
}

const DIAS_JANELA = 30
/** 3 paginas de 100 cobrem a janela com folga na escala atual. */
const MAX_PAGINAS = 3

async function buscarPagamentos(): Promise<PaymentRow[]> {
  const linhas: PaymentRow[] = []
  for (let page = 0; page < MAX_PAGINAS; page++) {
    const pagina = await apiPage<PaymentRow>(`/admin/payments?status=&page=${page}&size=100`)
    linhas.push(...pagina.data)
    if (page + 1 >= pagina.pagination.totalPages) break
  }
  return linhas
}

const chaveDia = (iso: string) => iso.slice(0, 10)

const rotuloDia = (chave: string) =>
  new Date(`${chave}T12:00:00`).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })

/** Ultimos N dias como chaves ISO, do mais antigo para hoje. */
function janelaDias(n: number): string[] {
  const dias: string[] = []
  const hoje = new Date()
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(hoje)
    d.setDate(hoje.getDate() - i)
    dias.push(d.toISOString().slice(0, 10))
  }
  return dias
}

function serieDiaria(
  pagamentos: PaymentRow[],
  incluir: (p: PaymentRow) => boolean,
  valor: (p: PaymentRow) => number,
) {
  const porDia = new Map<string, number>()
  for (const p of pagamentos) {
    if (!incluir(p)) continue
    const dia = chaveDia(p.approvedAt ?? p.createdAt)
    porDia.set(dia, (porDia.get(dia) ?? 0) + valor(p))
  }
  return janelaDias(DIAS_JANELA).map((dia) => ({
    rotulo: rotuloDia(dia),
    valor: porDia.get(dia) ?? 0,
  }))
}

const CORES_STATUS: Record<string, { rotulo: string; cor: string }> = {
  APPROVED: { rotulo: 'Aprovado', cor: 'var(--bb-success)' },
  PENDING: { rotulo: 'Pendente', cor: 'var(--bb-gold)' },
  CANCELLED: { rotulo: 'Cancelado', cor: 'var(--bb-text-dim)' },
  REFUNDED: { rotulo: 'Reembolsado', cor: '#d97706' },
  CHARGEBACK: { rotulo: 'Chargeback', cor: 'var(--bb-danger)' },
  FAILED: { rotulo: 'Falhou', cor: 'var(--bb-danger)' },
}

function fatiasPorStatus(pagamentos: PaymentRow[]) {
  const contagem = new Map<string, number>()
  for (const p of pagamentos) {
    contagem.set(p.status, (contagem.get(p.status) ?? 0) + 1)
  }
  return [...contagem.entries()]
    .sort((a, b) => b[1] - a[1])
    .map(([status, valor]) => ({
      rotulo: CORES_STATUS[status]?.rotulo ?? status,
      cor: CORES_STATUS[status]?.cor ?? 'var(--bb-border)',
      valor,
    }))
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

  const pagamentos = useQuery({
    queryKey: ['admin-dashboard-pagamentos'],
    queryFn: buscarPagamentos,
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

      {/* Os graficos falham em separado: sem serie historica, os tiles acima continuam de pe. */}
      {pagamentos.isSuccess && pagamentos.data.length > 0 && (
        <div className="adm-graficos">
          <div className="adm-grafico-card adm-grafico-card--largo">
            <h2>Receita aprovada · últimos {DIAS_JANELA} dias</h2>
            <GraficoArea
              dados={serieDiaria(
                pagamentos.data,
                (p) => p.status === 'APPROVED',
                (p) => p.amountCents / 100,
              )}
              formatar={(v) => brl.format(v)}
              descricao={`Receita aprovada por dia nos últimos ${DIAS_JANELA} dias`}
            />
          </div>
          <div className="adm-grafico-card">
            <h2>Vendas por dia</h2>
            <GraficoBarras
              dados={serieDiaria(pagamentos.data, () => true, () => 1)}
              descricao={`Quantidade de vendas por dia nos últimos ${DIAS_JANELA} dias`}
            />
          </div>
          <div className="adm-grafico-card">
            <h2>Pagamentos por status</h2>
            <GraficoDonut
              dados={fatiasPorStatus(pagamentos.data)}
              descricao="Distribuição dos pagamentos por status"
            />
          </div>
        </div>
      )}
    </section>
  )
}
