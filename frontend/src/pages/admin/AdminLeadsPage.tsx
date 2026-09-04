import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useDeferredValue, useState } from 'react'
import { api, apiPage, ApiError } from '../../api/http'
import { Button, Input, Skeleton } from '../../ui/basics'
import { dataHoraBR, moeda } from '../../ui/format'
import { Paginacao } from '../../ui/Paginacao'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { useToast } from '../../ui/Toast'

interface LeadResumo {
  id: string
  name: string
  email: string
  whatsapp: string | null
  status: string
  ownerId: string | null
  firstSeenAt: string
  lastSeenAt: string
}

interface Evento {
  source: string
  magnetId: string | null
  productId: string | null
  amountCents: number | null
  reason: string | null
  occurredAt: string
}

interface LeadDetalhe extends LeadResumo {
  notes: string | null
  history: Evento[]
}

const ETAPAS = ['NEW', 'CONTACTED', 'NEGOTIATING', 'WON', 'LOST'] as const

const ROTULO: Record<string, string> = {
  NEW: 'Novo',
  CONTACTED: 'Contatado',
  NEGOTIATING: 'Negociando',
  WON: 'Ganho',
  LOST: 'Perdido',
}

const ORIGEM: Record<string, string> = {
  MATERIAL: 'Baixou material',
  PAGAMENTO_RECUSADO: 'Pagamento recusado',
  PAGAMENTO_CANCELADO: 'Pagamento não concluído',
  MANUAL: 'Cadastro manual',
}

/** Documento Mestre Premium V3.0, seções 11 e 9. */
export function AdminLeadsPage() {
  const [status, setStatus] = useState('')
  const [busca, setBusca] = useState('')
  const [page, setPage] = useState(0)
  const [aberto, setAberto] = useState<string | null>(null)
  const buscaAtiva = useDeferredValue(busca)

  const funil = useQuery({
    queryKey: ['admin-funil'],
    queryFn: () => api<Record<string, number>>('/admin/leads/funnel'),
  })

  const lista = useQuery({
    queryKey: ['admin-leads', status, buscaAtiva, page],
    queryFn: () =>
      apiPage<LeadResumo>(
        `/admin/leads?status=${status}&search=${encodeURIComponent(buscaAtiva)}&page=${page}`,
      ),
    placeholderData: (anterior) => anterior,
  })

  return (
    <section>
      <h1 className="adm-titulo">Leads</h1>
      <p className="adm-sub">
        Quem deixou contato e ainda não comprou. A lista vem do contato mais recente para o
        mais antigo: intenção de compra envelhece — quem teve o cartão recusado hoje de manhã
        atende; quem baixou um PDF há três semanas já esqueceu do assunto.
      </p>

      {/* O funil é o resumo antes do detalhe: diz onde o gargalo está antes de
          obrigar a ler a lista inteira. Clicar filtra. */}
      <div className="adm-tiles" style={{ marginBottom: 'var(--bb-s5)' }}>
        {ETAPAS.map((etapa) => (
          <button
            key={etapa}
            type="button"
            className="adm-tile"
            onClick={() => {
              setStatus(status === etapa ? '' : etapa)
              setPage(0)
            }}
            style={{
              cursor: 'pointer',
              textAlign: 'left',
              borderColor: status === etapa ? 'var(--bb-gold)' : undefined,
            }}
          >
            <span className="valor">{funil.data?.[etapa] ?? '—'}</span>
            <span className="rotulo">{ROTULO[etapa]}</span>
          </button>
        ))}
      </div>

      <div className="adm-filtros">
        <Input
          label="Buscar lead"
          value={busca}
          onChange={(e) => {
            setBusca(e.target.value)
            setPage(0)
          }}
          placeholder="nome ou e-mail"
        />
      </div>

      <QueryBoundary query={lista} empty="Nenhum lead com esse filtro.">
        {(pagina) => (
          <>
            <div className="adm-tabela-wrap">
              <table className="adm-tabela">
                <thead>
                  <tr>
                    <th>Pessoa</th>
                    <th>WhatsApp</th>
                    <th>Etapa</th>
                    <th>Último contato</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {pagina.data.map((l) => (
                    <LinhaDeLead
                      key={l.id}
                      lead={l}
                      aberto={aberto === l.id}
                      onAbrir={() => setAberto(aberto === l.id ? null : l.id)}
                    />
                  ))}
                </tbody>
              </table>
            </div>
            <Paginacao meta={pagina.pagination} onPage={setPage} />
          </>
        )}
      </QueryBoundary>
    </section>
  )
}

function LinhaDeLead({
  lead,
  aberto,
  onAbrir,
}: {
  lead: LeadResumo
  aberto: boolean
  onAbrir: () => void
}) {
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()

  function recarregar() {
    void queryClient.invalidateQueries({ queryKey: ['admin-leads'] })
    void queryClient.invalidateQueries({ queryKey: ['admin-funil'] })
    void queryClient.invalidateQueries({ queryKey: ['admin-lead', lead.id] })
  }

  function aoFalhar(err: unknown) {
    toastErro(err instanceof ApiError ? err.message : 'Não foi possível concluir a ação.')
  }

  const mudarEtapa = useMutation({
    mutationFn: (novo: string) => api(`/admin/leads/${lead.id}/status/${novo}`, { method: 'POST' }),
    onSuccess: recarregar,
    onError: aoFalhar,
  })

  const assumir = useMutation({
    mutationFn: () => api(`/admin/leads/${lead.id}/claim`, { method: 'POST' }),
    onSuccess: () => {
      toast('Lead atribuído a você.')
      recarregar()
    },
    onError: aoFalhar,
  })

  // Estado final: a API recusa reabrir, e desabilitar o seletor evita que o
  // erro só apareça depois do clique.
  const fechado = lead.status === 'WON' || lead.status === 'LOST'

  return (
    <>
      <tr>
        <td>
          <b>{lead.name}</b>
          <br />
          <span style={{ color: 'var(--bb-text-dim)', fontSize: '0.8rem' }}>{lead.email}</span>
        </td>
        <td style={{ whiteSpace: 'nowrap' }}>
          {lead.whatsapp ? (
            <a
              href={`https://wa.me/55${lead.whatsapp.replace(/\D/g, '')}`}
              target="_blank"
              rel="noreferrer noopener"
            >
              {lead.whatsapp}
            </a>
          ) : (
            '—'
          )}
        </td>
        <td>
          <select
            value={lead.status}
            disabled={fechado || mudarEtapa.isPending}
            onChange={(e) => mudarEtapa.mutate(e.target.value)}
            aria-label={`Etapa de ${lead.name}`}
            style={{
              background: 'var(--bb-surface)',
              border: '1px solid var(--bb-border)',
              borderRadius: 'var(--bb-r1)',
              color: 'var(--bb-text)',
              font: 'inherit',
              fontSize: '0.84rem',
              padding: '5px 9px',
            }}
          >
            {ETAPAS.map((e) => (
              <option key={e} value={e}>{ROTULO[e]}</option>
            ))}
          </select>
        </td>
        <td style={{ whiteSpace: 'nowrap' }}>{dataHoraBR(lead.lastSeenAt)}</td>
        <td style={{ whiteSpace: 'nowrap' }}>
          <Button ghost style={{ padding: '3px 11px', fontSize: '0.78rem' }} onClick={onAbrir}>
            {aberto ? 'Fechar' : 'Histórico'}
          </Button>{' '}
          {!lead.ownerId && (
            <Button
              ghost
              style={{ padding: '3px 11px', fontSize: '0.78rem' }}
              disabled={assumir.isPending}
              onClick={() => assumir.mutate()}
            >
              Assumir
            </Button>
          )}
        </td>
      </tr>

      {aberto && (
        <tr>
          <td colSpan={5}>
            <Historico leadId={lead.id} />
          </td>
        </tr>
      )}
    </>
  )
}

function Historico({ leadId }: { leadId: string }) {
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()
  const [notas, setNotas] = useState<string | null>(null)

  const query = useQuery({
    queryKey: ['admin-lead', leadId],
    queryFn: () => api<LeadDetalhe>(`/admin/leads/${leadId}`),
  })

  const anotar = useMutation({
    mutationFn: (texto: string) =>
      api(`/admin/leads/${leadId}/notes`, {
        method: 'PUT',
        body: JSON.stringify({ notes: texto }),
      }),
    onSuccess: () => {
      toast('Anotação salva.')
      void queryClient.invalidateQueries({ queryKey: ['admin-lead', leadId] })
    },
    onError: (err) =>
      toastErro(err instanceof ApiError ? err.message : 'Não foi possível salvar a anotação.'),
  })

  if (query.isPending) return <Skeleton height={120} />
  if (query.isError || !query.data) return <p>Não foi possível carregar o histórico.</p>

  const lead = query.data
  const texto = notas ?? lead.notes ?? ''

  return (
    <div style={{ display: 'grid', gap: 'var(--bb-s4)', padding: 'var(--bb-s2) 0' }}>
      <div>
        <h3 style={{ margin: '0 0 8px', fontSize: '0.92rem' }}>
          Histórico · desde {dataHoraBR(lead.firstSeenAt)}
        </h3>
        <ul style={{ margin: 0, paddingLeft: 18, display: 'grid', gap: 6 }}>
          {lead.history.map((e, i) => (
            <li key={`${e.occurredAt}-${i}`} style={{ fontSize: '0.86rem' }}>
              <b>{ORIGEM[e.source] ?? e.source}</b>
              {e.amountCents !== null && <> · {moeda(e.amountCents)}</>}
              {e.reason && <> · {e.reason}</>}
              <span style={{ color: 'var(--bb-text-dim)' }}> — {dataHoraBR(e.occurredAt)}</span>
            </li>
          ))}
        </ul>
      </div>

      <div>
        <label
          htmlFor={`notas-${leadId}`}
          style={{ display: 'block', marginBottom: 5, fontSize: '0.82rem', color: 'var(--bb-text-dim)' }}
        >
          O que foi conversado
        </label>
        <textarea
          id={`notas-${leadId}`}
          value={texto}
          onChange={(e) => setNotas(e.target.value)}
          rows={3}
          maxLength={4000}
          style={{
            width: '100%',
            background: 'var(--bb-surface)',
            border: '1px solid var(--bb-border)',
            borderRadius: 'var(--bb-r1)',
            color: 'var(--bb-text)',
            font: 'inherit',
            fontSize: '0.88rem',
            padding: '9px 11px',
          }}
        />
        <Button
          style={{ marginTop: 8, padding: '5px 14px', fontSize: '0.82rem' }}
          disabled={anotar.isPending || texto === (lead.notes ?? '')}
          onClick={() => anotar.mutate(texto)}
        >
          Salvar anotação
        </Button>
      </div>
    </div>
  )
}
