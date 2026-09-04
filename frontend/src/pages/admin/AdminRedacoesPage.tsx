import { useMutation, useQuery } from '@tanstack/react-query'
import { useDeferredValue, useState } from 'react'
import { api, apiPage, ApiError } from '../../api/http'
import { Button, Input, Skeleton } from '../../ui/basics'
import { dataBR } from '../../ui/format'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { useToast } from '../../ui/Toast'

interface Aluno {
  id: string
  email: string
  fullName: string
  status: string
}

interface Cota {
  competencia: string
  total: number
  usadas: number
  restantes: number
}

interface ItemDaFila {
  id: string
  prompt: string
  board: string | null
  dueAt: string
  diasRestantes: number
  vencida: boolean
}

/**
 * Cota de redação. Documento Mestre V4.0, seção 14.
 *
 * A cota nasce em zero e cresce por três caminhos: renovação mensal da
 * mentoria, compra avulsa e concessão manual. As duas primeiras dependem do
 * pagamento; esta tela é a terceira — sem ela, o aluno leva "sua cota acabou"
 * no primeiro envio e não há como conceder a primeira.
 */
export function AdminRedacoesPage() {
  const [busca, setBusca] = useState('')
  const [selecionado, setSelecionado] = useState<Aluno | null>(null)
  const buscaAtiva = useDeferredValue(busca)

  const fila = useQuery({
    queryKey: ['admin-fila-correcao'],
    queryFn: () => api<ItemDaFila[]>('/corrections/queue?limit=10'),
  })

  const alunos = useQuery({
    queryKey: ['admin-alunos-cota', buscaAtiva],
    queryFn: () => apiPage<Aluno>(`/admin/students?search=${encodeURIComponent(buscaAtiva)}&size=8`),
    enabled: buscaAtiva.trim().length >= 2,
  })

  return (
    <section>
      <h1 className="adm-titulo">Redações</h1>
      <p className="adm-sub">
        Fila de correção e concessão de cota. Cada correção concedida custa a hora de um
        corretor — é a cota que mantém a margem da mentoria de pé.
      </p>

      <h2 style={{ fontSize: '1rem', margin: '0 0 var(--bb-s3)' }}>Aguardando correção</h2>
      <QueryBoundary query={fila} empty="Nenhuma redação na fila.">
        {(itens) => (
          <div className="adm-tabela-wrap" style={{ marginBottom: 'var(--bb-s6)' }}>
            <table className="adm-tabela">
              <thead>
                <tr>
                  <th>Tema</th>
                  <th>Banca</th>
                  <th>Prazo</th>
                </tr>
              </thead>
              <tbody>
                {itens.map((r) => (
                  <tr key={r.id}>
                    <td>{r.prompt}</td>
                    <td>{r.board ?? '—'}</td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      {r.vencida ? (
                        <span style={{ color: 'var(--bb-danger)', fontWeight: 600 }}>
                          Vencida há {Math.abs(r.diasRestantes)}d
                        </span>
                      ) : (
                        <>
                          {dataBR(r.dueAt)}{' '}
                          <span style={{ color: 'var(--bb-text-dim)' }}>({r.diasRestantes}d)</span>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </QueryBoundary>

      <h2 style={{ fontSize: '1rem', margin: '0 0 var(--bb-s3)' }}>Conceder cota</h2>

      <div className="adm-filtros">
        <Input
          label="Buscar aluno"
          value={busca}
          onChange={(e) => {
            setBusca(e.target.value)
            setSelecionado(null)
          }}
          placeholder="nome ou e-mail"
        />
      </div>

      {alunos.isFetching && <Skeleton height={60} />}

      {!selecionado && alunos.data && (
        <div className="adm-tabela-wrap">
          <table className="adm-tabela">
            <tbody>
              {alunos.data.data.map((a) => (
                <tr key={a.id}>
                  <td>
                    <b>{a.fullName}</b>
                    <br />
                    <span style={{ color: 'var(--bb-text-dim)', fontSize: '0.8rem' }}>{a.email}</span>
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <Button
                      ghost
                      style={{ padding: '3px 11px', fontSize: '0.78rem' }}
                      onClick={() => setSelecionado(a)}
                    >
                      Selecionar
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selecionado && <Concessao aluno={selecionado} onFechar={() => setSelecionado(null)} />}
    </section>
  )
}

function Concessao({ aluno, onFechar }: { aluno: Aluno; onFechar: () => void }) {
  const { toast, toastErro } = useToast()
  const [quantidade, setQuantidade] = useState('4')
  const [motivo, setMotivo] = useState('')

  const cota = useQuery({
    queryKey: ['admin-cota', aluno.id],
    queryFn: () => api<Cota>(`/admin/essays/quota/${aluno.id}`),
  })

  const conceder = useMutation({
    mutationFn: () =>
      api<Cota>(`/admin/essays/quota/${aluno.id}`, {
        method: 'POST',
        body: JSON.stringify({ amount: Number(quantidade), reason: motivo }),
      }),
    onSuccess: (nova) => {
      toast(`Cota concedida. ${aluno.fullName} tem ${nova.restantes} correções disponíveis.`)
      setMotivo('')
      void cota.refetch()
    },
    onError: (err) =>
      toastErro(err instanceof ApiError ? err.message : 'Não foi possível conceder a cota.'),
  })

  return (
    <div className="adm-form" style={{ maxWidth: 560 }}>
      <p style={{ margin: 0 }}>
        <b>{aluno.fullName}</b>
        <br />
        <span style={{ color: 'var(--bb-text-dim)', fontSize: '0.85rem' }}>{aluno.email}</span>
      </p>

      {cota.data && (
        <p style={{ margin: 0, fontSize: '0.88rem', color: 'var(--bb-text-dim)' }}>
          Competência {cota.data.competencia}: <b style={{ color: 'var(--bb-text)' }}>{cota.data.restantes}</b>{' '}
          restantes de {cota.data.total} ({cota.data.usadas} usadas).
        </p>
      )}

      <Input
        label="Quantas correções"
        type="number"
        min={1}
        max={50}
        value={quantidade}
        onChange={(e) => setQuantidade(e.target.value)}
      />

      {/* O motivo é obrigatório porque cota é dinheiro: concessão sem motivo
          registrado vira um número que ninguém explica na conciliação do mês
          seguinte. */}
      <Input
        label="Motivo"
        value={motivo}
        onChange={(e) => setMotivo(e.target.value)}
        placeholder="renovação da mentoria, cortesia, compra avulsa…"
        required
      />

      <div className="adm-acoes">
        <Button disabled={conceder.isPending || !motivo.trim()} onClick={() => conceder.mutate()}>
          {conceder.isPending ? 'Concedendo…' : 'Conceder'}
        </Button>
        <Button ghost onClick={onFechar}>
          Trocar de aluno
        </Button>
      </div>
    </div>
  )
}
