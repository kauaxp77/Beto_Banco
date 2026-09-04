import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { api, ApiError } from '../../api/http'
import { Button, EmptyState, Skeleton } from '../../ui/basics'
import { dataBR } from '../../ui/format'
import { useToast } from '../../ui/Toast'
import './redacoes.css'

interface Cota {
  competencia: string
  total: number
  usadas: number
  restantes: number
}

interface Redacao {
  id: string
  prompt: string
  board: string | null
  status: string
  submittedAt: string
  dueAt: string
  diasRestantes: number
  rewriteOf: string | null
}

interface Criterio {
  code: string
  title: string
  max_score: number
}

interface Rubrica {
  board: string
  name: string
  criteria: string
}

interface Devolutiva {
  scores: string
  totalScore: number | null
  comment: string | null
  audioUrl: string | null
  annotations: string | null
  completedAt: string | null
}

const ETIQUETA: Record<string, { rotulo: string; classe: string }> = {
  SUBMITTED: { rotulo: 'Na fila', classe: 'enviada' },
  IN_REVIEW: { rotulo: 'Em correção', classe: 'corrigindo' },
  CORRECTED: { rotulo: 'Corrigida', classe: 'pronta' },
  REWRITE_SUBMITTED: { rotulo: 'Reescrita enviada', classe: 'enviada' },
  CANCELLED: { rotulo: 'Cancelada', classe: 'enviada' },
}

/** Redações do aluno. Documento Mestre V4.0, seção 14. */
export function RedacoesPage() {
  const cota = useQuery({ queryKey: ['minha-cota'], queryFn: () => api<Cota>('/me/essays/quota') })
  const minhas = useQuery({ queryKey: ['minhas-redacoes'], queryFn: () => api<Redacao[]>('/me/essays') })
  const rubricas = useQuery({
    queryKey: ['rubricas'],
    queryFn: () => api<Rubrica[]>('/me/essays/rubrics'),
    staleTime: 5 * 60 * 1000,
  })

  return (
    <section>
      <header className="rd-cabecalho">
        <h1>Minhas redações</h1>
        <p>
          Envie o texto e receba a devolutiva por critério da banca, com nota e comentário de um
          corretor humano. O prazo é de sete dias a partir do envio.
        </p>
      </header>

      {cota.isPending ? <Skeleton height={92} /> : <CartaoDaCota cota={cota.data} />}

      {cota.data && cota.data.restantes > 0 && (
        <Formulario rubricas={rubricas.data ?? []} />
      )}

      {minhas.isPending ? (
        <Skeleton height={160} />
      ) : (minhas.data ?? []).length === 0 ? (
        <EmptyState>Você ainda não enviou nenhuma redação.</EmptyState>
      ) : (
        <ul className="rd-lista">
          {(minhas.data ?? []).map((r) => (
            <ItemDeRedacao key={r.id} redacao={r} rubricas={rubricas.data ?? []} />
          ))}
        </ul>
      )}
    </section>
  )
}

function CartaoDaCota({ cota }: { cota: Cota | undefined }) {
  if (!cota) return null
  const vazia = cota.restantes === 0
  const competencia = new Date(`${cota.competencia}T12:00:00`)

  return (
    <div className={`rd-cota ${vazia ? 'rd-cota--vazia' : ''}`.trim()}>
      <span className="rd-cota-numero">{cota.restantes}</span>
      <p className="rd-cota-texto">
        {vazia ? (
          <>
            <b>Sua cota desta competência acabou</b> ({cota.usadas} de {cota.total} usadas). Ela
            renova no dia 1º; você também pode comprar uma correção avulsa.
          </>
        ) : (
          <>
            <b>
              correç{cota.restantes === 1 ? 'ão' : 'ões'} restante
              {cota.restantes === 1 ? '' : 's'}
            </b>{' '}
            em {competencia.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })} —{' '}
            {cota.usadas} de {cota.total} usadas. A cota não acumula de um mês para o outro.
          </>
        )}
      </p>
    </div>
  )
}

function Formulario({ rubricas }: { rubricas: Rubrica[] }) {
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()
  const [prompt, setPrompt] = useState('')
  const [board, setBoard] = useState('')
  const [fileUrl, setFileUrl] = useState('')

  const enviar = useMutation({
    mutationFn: () =>
      api('/me/essays', {
        method: 'POST',
        body: JSON.stringify({ prompt, board: board || null, fileUrl }),
      }),
    onSuccess: () => {
      toast('Redação enviada. O prazo de correção é de sete dias.')
      setPrompt('')
      setFileUrl('')
      void queryClient.invalidateQueries({ queryKey: ['minhas-redacoes'] })
      void queryClient.invalidateQueries({ queryKey: ['minha-cota'] })
    },
    onError: (err) =>
      toastErro(err instanceof ApiError ? err.message : 'Não foi possível enviar a redação.'),
  })

  function submeter(e: FormEvent) {
    e.preventDefault()
    enviar.mutate()
  }

  return (
    <form className="rd-form" onSubmit={submeter}>
      <h2>Enviar redação</h2>

      <div>
        <label htmlFor="rd-tema">Tema proposto</label>
        <textarea
          id="rd-tema"
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
          rows={2}
          maxLength={500}
          placeholder="Os desafios da inclusão financeira no Brasil"
          required
        />
      </div>

      <div>
        <label htmlFor="rd-banca">Banca</label>
        <select id="rd-banca" value={board} onChange={(e) => setBoard(e.target.value)}>
          <option value="">Sem banca específica</option>
          {rubricas.map((r) => (
            <option key={r.board} value={r.board}>{r.board} — {r.name}</option>
          ))}
        </select>
        <p className="rd-dica">
          A banca define os critérios da correção. Cada uma pontua coisas diferentes.
        </p>
      </div>

      <div>
        <label htmlFor="rd-arquivo">Link do arquivo</label>
        <input
          id="rd-arquivo"
          type="url"
          value={fileUrl}
          onChange={(e) => setFileUrl(e.target.value)}
          placeholder="https://…"
          required
        />
        <p className="rd-dica">
          O arquivo sobe para o seu armazenamento e chega aqui como link — o texto não passa
          pela plataforma, no mesmo padrão dos materiais de aula.
        </p>
      </div>

      {/* A cota é debitada no ENVIO, não na conclusão: o custo do corretor
          começa quando a redação entra na fila. Dizer isso antes do clique
          evita a surpresa depois. */}
      <p className="rd-dica">Enviar consome uma correção da sua cota.</p>

      <div>
        <Button type="submit" disabled={enviar.isPending || !prompt.trim() || !fileUrl.trim()}>
          {enviar.isPending ? 'Enviando…' : 'Enviar para correção'}
        </Button>
      </div>
    </form>
  )
}

function ItemDeRedacao({ redacao, rubricas }: { redacao: Redacao; rubricas: Rubrica[] }) {
  const [aberta, setAberta] = useState(false)
  const etiqueta = ETIQUETA[redacao.status] ?? { rotulo: redacao.status, classe: 'enviada' }
  const corrigida = redacao.status === 'CORRECTED'

  return (
    <li className="rd-item">
      <div className="rd-item-topo">
        <h2>{redacao.prompt}</h2>
        <span className={`rd-etiqueta rd-etiqueta--${etiqueta.classe}`}>{etiqueta.rotulo}</span>
      </div>

      <div className="rd-meta">
        {redacao.board && <span>Banca {redacao.board}</span>}
        <span>Enviada em {dataBR(redacao.submittedAt)}</span>
        {!corrigida && <Prazo dias={redacao.diasRestantes} ate={redacao.dueAt} />}
        {redacao.rewriteOf && <span>Reescrita</span>}
      </div>

      {corrigida && (
        <>
          <Button
            ghost
            style={{ marginTop: 'var(--bb-s3)', padding: '4px 13px', fontSize: '0.82rem' }}
            onClick={() => setAberta(!aberta)}
          >
            {aberta ? 'Fechar devolutiva' : 'Ver devolutiva'}
          </Button>
          {aberta && <DevolutivaDaRedacao id={redacao.id} board={redacao.board} rubricas={rubricas} />}
        </>
      )}
    </li>
  )
}

function Prazo({ dias, ate }: { dias: number; ate: string }) {
  if (dias < 0) {
    return <span className="rd-prazo--vencido">Prazo vencido em {dataBR(ate)}</span>
  }
  const classe = dias <= 2 ? 'rd-prazo--perto' : undefined
  return (
    <span className={classe}>
      Devolutiva até {dataBR(ate)} ({dias} dia{dias === 1 ? '' : 's'})
    </span>
  )
}

function DevolutivaDaRedacao({
  id,
  board,
  rubricas,
}: {
  id: string
  board: string | null
  rubricas: Rubrica[]
}) {
  const query = useQuery({
    queryKey: ['redacao', id],
    queryFn: () => api<{ devolutiva: Devolutiva | null }>(`/me/essays/${id}`),
  })

  if (query.isPending) return <Skeleton height={120} />

  const devolutiva = query.data?.devolutiva
  if (!devolutiva) {
    return <p className="rd-dica">A devolutiva ainda não foi publicada.</p>
  }

  const criterios = criteriosDa(board, rubricas)
  const notas = leJson<Record<string, number>>(devolutiva.scores) ?? {}

  return (
    <div className="rd-devolutiva">
      <p className="rd-nota-total">
        <strong>{devolutiva.totalScore ?? '—'}</strong>
        <span style={{ color: 'var(--bb-text-dim)', fontSize: '0.88rem' }}>
          nota final{devolutiva.completedAt && ` · corrigida em ${dataBR(devolutiva.completedAt)}`}
        </span>
      </p>

      <ul className="rd-criterios">
        {Object.entries(notas).map(([codigo, nota]) => {
          const criterio = criterios.find((c) => c.code === codigo)
          const teto = criterio?.max_score ?? 0
          return (
            <li key={codigo} className="rd-criterio">
              <span>{criterio?.title ?? codigo}</span>
              <span style={{ fontVariantNumeric: 'tabular-nums' }}>
                {nota}
                {teto > 0 && ` / ${teto}`}
              </span>
              {teto > 0 && (
                <span className="rd-barra" aria-hidden="true">
                  <span style={{ width: `${Math.min(100, (nota / teto) * 100)}%` }} />
                </span>
              )}
            </li>
          )
        })}
      </ul>

      {devolutiva.comment && <p className="rd-comentario">{devolutiva.comment}</p>}

      {devolutiva.audioUrl && (
        <audio controls src={devolutiva.audioUrl} style={{ marginTop: 'var(--bb-s3)', width: '100%' }}>
          Seu navegador não reproduz áudio.
        </audio>
      )}
    </div>
  )
}

/** Os critérios vêm da rubrica da banca; sem banca, não há teto a exibir. */
export function criteriosDa(board: string | null, rubricas: Rubrica[]): Criterio[] {
  if (!board) return []
  const rubrica = rubricas.find((r) => r.board === board)
  return rubrica ? leJson<Criterio[]>(rubrica.criteria) ?? [] : []
}

/** O backend guarda esses campos como JSON em texto; aqui eles voltam a ser objeto. */
export function leJson<T>(texto: string | null | undefined): T | null {
  if (!texto) return null
  try {
    return JSON.parse(texto) as T
  } catch {
    return null
  }
}
