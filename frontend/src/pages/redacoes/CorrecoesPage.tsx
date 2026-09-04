import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api, ApiError } from '../../api/http'
import { Button, EmptyState, Skeleton } from '../../ui/basics'
import { dataBR } from '../../ui/format'
import { useToast } from '../../ui/Toast'
import { criteriosDa } from './RedacoesPage'
import './redacoes.css'

interface ItemDaFila {
  id: string
  prompt: string
  board: string | null
  status: string
  submittedAt: string
  dueAt: string
  diasRestantes: number
  vencida: boolean
}

interface Rubrica {
  board: string
  name: string
  criteria: string
}

/**
 * Fila do corretor. Documento Mestre V4.0, seção 14.
 *
 * A API entrega do prazo mais próximo de vencer para o mais distante, e marca
 * o que já estourou: no topo também está o que vence amanhã, então destacar o
 * vencido é o que separa "urgente" de "atrasado".
 */
export function CorrecoesPage() {
  const fila = useQuery({
    queryKey: ['fila-correcao'],
    queryFn: () => api<ItemDaFila[]>('/corrections/queue?limit=30'),
  })

  const rubricas = useQuery({
    queryKey: ['rubricas'],
    queryFn: () => api<Rubrica[]>('/me/essays/rubrics'),
    staleTime: 5 * 60 * 1000,
  })

  return (
    <section>
      <header className="rd-cabecalho">
        <h1>Fila de correção</h1>
        <p>
          Do prazo mais próximo de vencer para o mais distante. A nota é sua: a IA só sugere
          rascunho e não publica nota.
        </p>
      </header>

      {fila.isPending ? (
        <Skeleton height={180} />
      ) : (fila.data ?? []).length === 0 ? (
        <EmptyState>Nenhuma redação aguardando correção.</EmptyState>
      ) : (
        <ul className="rd-lista">
          {(fila.data ?? []).map((r) => (
            <ItemParaCorrigir key={r.id} item={r} rubricas={rubricas.data ?? []} />
          ))}
        </ul>
      )}
    </section>
  )
}

function ItemParaCorrigir({ item, rubricas }: { item: ItemDaFila; rubricas: Rubrica[] }) {
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()
  const [corrigindo, setCorrigindo] = useState(false)

  function aoFalhar(err: unknown) {
    toastErro(err instanceof ApiError ? err.message : 'Não foi possível concluir a ação.')
  }

  const assumir = useMutation({
    mutationFn: () => api(`/corrections/${item.id}/claim`, { method: 'POST' }),
    onSuccess: () => {
      setCorrigindo(true)
      void queryClient.invalidateQueries({ queryKey: ['fila-correcao'] })
    },
    onError: aoFalhar,
  })

  const criterios = criteriosDa(item.board, rubricas)

  return (
    <li className="rd-item">
      <div className="rd-item-topo">
        <h2>{item.prompt}</h2>
        <span className={`rd-etiqueta rd-etiqueta--${item.status === 'IN_REVIEW' ? 'corrigindo' : 'enviada'}`}>
          {item.status === 'IN_REVIEW' ? 'Em correção' : 'Na fila'}
        </span>
      </div>

      <div className="rd-meta">
        {item.board && <span>Banca {item.board}</span>}
        <span>Enviada em {dataBR(item.submittedAt)}</span>
        {item.vencida ? (
          <span className="rd-prazo--vencido">
            Vencida há {Math.abs(item.diasRestantes)} dia{Math.abs(item.diasRestantes) === 1 ? '' : 's'}
          </span>
        ) : (
          <span className={item.diasRestantes <= 2 ? 'rd-prazo--perto' : undefined}>
            Vence em {item.diasRestantes} dia{item.diasRestantes === 1 ? '' : 's'} ({dataBR(item.dueAt)})
          </span>
        )}
      </div>

      {!corrigindo ? (
        <Button
          style={{ marginTop: 'var(--bb-s3)', padding: '5px 14px', fontSize: '0.84rem' }}
          disabled={assumir.isPending}
          onClick={() => assumir.mutate()}
        >
          {assumir.isPending ? 'Assumindo…' : 'Assumir e corrigir'}
        </Button>
      ) : (
        <FormularioDeDevolutiva
          essayId={item.id}
          board={item.board}
          criterios={criterios}
          onPronto={() => {
            setCorrigindo(false)
            toast('Devolutiva publicada.')
            void queryClient.invalidateQueries({ queryKey: ['fila-correcao'] })
          }}
          onErro={aoFalhar}
        />
      )}
    </li>
  )
}

function FormularioDeDevolutiva({
  essayId,
  board,
  criterios,
  onPronto,
  onErro,
}: {
  essayId: string
  board: string | null
  criterios: Array<{ code: string; title: string; max_score: number }>
  onPronto: () => void
  onErro: (err: unknown) => void
}) {
  const [notas, setNotas] = useState<Record<string, string>>({})
  const [comentario, setComentario] = useState('')
  const [audioUrl, setAudioUrl] = useState('')

  const publicar = useMutation({
    mutationFn: () =>
      api(`/corrections/${essayId}/publish`, {
        method: 'POST',
        body: JSON.stringify({
          scores: Object.fromEntries(
            Object.entries(notas)
              .filter(([, v]) => v !== '')
              .map(([k, v]) => [k, Number(v)]),
          ),
          comment: comentario || null,
          audioUrl: audioUrl || null,
          annotations: null,
        }),
      }),
    onSuccess: onPronto,
    onError: onErro,
  })

  if (criterios.length === 0) {
    return (
      <p className="rd-dica" style={{ marginTop: 'var(--bb-s3)' }}>
        Esta redação foi enviada sem banca, e sem rubrica não há critérios para pontuar. Defina
        a banca da redação antes de publicar a devolutiva.
      </p>
    )
  }

  const total = Object.values(notas)
    .filter((v) => v !== '')
    .reduce((soma, v) => soma + Number(v), 0)

  const completo = criterios.every((c) => notas[c.code] !== undefined && notas[c.code] !== '')

  return (
    <div className="rd-devolutiva">
      <p style={{ margin: '0 0 var(--bb-s3)', fontSize: '0.86rem', color: 'var(--bb-text-dim)' }}>
        Rubrica {board}. Total parcial: <b style={{ color: 'var(--bb-gold)' }}>{total}</b>
      </p>

      <div className="rd-notas">
        {criterios.map((c) => (
          <label key={c.code} className="rd-nota-campo">
            <span>
              {c.title}
              <small>
                {c.max_score > 0
                  ? `Código ${c.code} · máximo ${c.max_score}`
                  : /* Teto zero é o critério de desconto do Cebraspe: não soma,
                       só subtrai, e por isso não tem limite superior. */
                    `Código ${c.code} · desconto, sem teto`}
              </small>
            </span>
            <input
              type="number"
              step="0.5"
              min="0"
              max={c.max_score > 0 ? c.max_score : undefined}
              value={notas[c.code] ?? ''}
              onChange={(e) => setNotas({ ...notas, [c.code]: e.target.value })}
              style={{
                background: 'var(--bb-bg)',
                border: '1px solid var(--bb-border)',
                borderRadius: 10,
                color: 'var(--bb-text)',
                font: 'inherit',
                padding: '8px 10px',
                textAlign: 'right',
              }}
            />
          </label>
        ))}
      </div>

      <div style={{ marginTop: 'var(--bb-s4)' }}>
        <label
          htmlFor={`comentario-${essayId}`}
          style={{ display: 'block', marginBottom: 5, fontSize: '0.76rem', letterSpacing: '0.05em', textTransform: 'uppercase', color: 'var(--bb-text-dim)' }}
        >
          Comentário para o aluno
        </label>
        <textarea
          id={`comentario-${essayId}`}
          value={comentario}
          onChange={(e) => setComentario(e.target.value)}
          rows={4}
          placeholder="O que funcionou, o que custou ponto e o que fazer na próxima."
          style={{
            width: '100%',
            background: 'var(--bb-bg)',
            border: '1px solid var(--bb-border)',
            borderRadius: 10,
            color: 'var(--bb-text)',
            font: 'inherit',
            fontSize: '0.92rem',
            padding: '10px 12px',
          }}
        />
      </div>

      <div style={{ marginTop: 'var(--bb-s3)' }}>
        <label
          htmlFor={`audio-${essayId}`}
          style={{ display: 'block', marginBottom: 5, fontSize: '0.76rem', letterSpacing: '0.05em', textTransform: 'uppercase', color: 'var(--bb-text-dim)' }}
        >
          Áudio da devolutiva (opcional)
        </label>
        <input
          id={`audio-${essayId}`}
          type="url"
          value={audioUrl}
          onChange={(e) => setAudioUrl(e.target.value)}
          placeholder="https://…"
          style={{
            width: '100%',
            background: 'var(--bb-bg)',
            border: '1px solid var(--bb-border)',
            borderRadius: 10,
            color: 'var(--bb-text)',
            font: 'inherit',
            fontSize: '0.92rem',
            padding: '9px 12px',
          }}
        />
      </div>

      <Button
        style={{ marginTop: 'var(--bb-s4)' }}
        disabled={publicar.isPending || !completo}
        title={completo ? undefined : 'Informe a nota de cada critério da rubrica'}
        onClick={() => publicar.mutate()}
      >
        {publicar.isPending ? 'Publicando…' : 'Publicar devolutiva'}
      </Button>
    </div>
  )
}
