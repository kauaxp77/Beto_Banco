import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api, ApiError } from '../../api/http'
import { Button } from '../../ui/basics'
import { dataHoraBR } from '../../ui/format'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { useToast } from '../../ui/Toast'

interface Pendente {
  id: string
  name: string
  slug: string
  status: string
  verifiedAt: string | null
  daysSinceVerification: number
  published: boolean
}

interface Carreira {
  id: string
  name: string
}

/**
 * Fila de revisão de fichas. Documento Mestre V4.0, seção 11.
 *
 * "Ficha sem verificação há mais de 60 dias entra em fila de revisão no admin."
 * A API devolve a nunca-verificada primeiro — ela é o caso mais urgente, não o
 * menos: nunca foi conferida contra fonte nenhuma.
 */
export function AdminContestsPage() {
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()
  const [abrindo, setAbrindo] = useState<string | null>(null)

  const fila = useQuery({
    queryKey: ['admin-concursos-fila'],
    queryFn: () => api<Pendente[]>('/admin/contests/review-queue?limit=50'),
  })

  const carreiras = useQuery({
    queryKey: ['carreiras'],
    queryFn: () => api<Carreira[]>('/contests/careers'),
    staleTime: 5 * 60 * 1000,
  })

  function recarregar() {
    void queryClient.invalidateQueries({ queryKey: ['admin-concursos-fila'] })
  }

  function aoFalhar(err: unknown) {
    toastErro(err instanceof ApiError ? err.message : 'Não foi possível concluir a ação.')
  }

  const verificar = useMutation({
    mutationFn: ({ id, sourceUrl }: { id: string; sourceUrl: string }) =>
      api(`/admin/contests/${id}/verify`, {
        method: 'POST',
        body: JSON.stringify({ sourceUrl }),
      }),
    onSuccess: () => {
      toast('Verificação registrada.')
      recarregar()
    },
    onError: aoFalhar,
  })

  const publicar = useMutation({
    mutationFn: (id: string) => api(`/admin/contests/${id}/publish`, { method: 'POST' }),
    onSuccess: () => {
      toast('Ficha publicada.')
      recarregar()
    },
    onError: aoFalhar,
  })

  const definirCarreiras = useMutation({
    mutationFn: ({ id, careerIds }: { id: string; careerIds: string[] }) =>
      api(`/admin/contests/${id}/careers`, {
        method: 'PUT',
        body: JSON.stringify({ careerIds }),
      }),
    onSuccess: () => {
      toast('Carreiras atualizadas.')
      setAbrindo(null)
      recarregar()
    },
    onError: aoFalhar,
  })

  return (
    <section>
      <h1 className="adm-titulo">Concursos</h1>
      <p className="adm-sub">
        Fichas sem conferência há mais de 60 dias, da mais antiga para a mais recente. Salário
        e vaga errados geram reclamação e perda de confiança — esta fila é o que impede o
        catálogo de envelhecer em silêncio.
      </p>

      <QueryBoundary
        query={fila}
        empty="Nenhuma ficha pendente de revisão. Todas foram conferidas nos últimos 60 dias."
      >
        {(pendentes) => (
          <div className="adm-tabela-wrap">
            <table className="adm-tabela">
              <thead>
                <tr>
                  <th>Concurso</th>
                  <th>Última verificação</th>
                  <th>Situação</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {pendentes.map((c) => (
                  <Linha
                    key={c.id}
                    ficha={c}
                    carreiras={carreiras.data ?? []}
                    abrindoCarreiras={abrindo === c.id}
                    onAbrirCarreiras={() => setAbrindo(abrindo === c.id ? null : c.id)}
                    onVerificar={(sourceUrl) => verificar.mutate({ id: c.id, sourceUrl })}
                    onPublicar={() => publicar.mutate(c.id)}
                    onSalvarCarreiras={(careerIds) =>
                      definirCarreiras.mutate({ id: c.id, careerIds })
                    }
                    ocupado={verificar.isPending || publicar.isPending || definirCarreiras.isPending}
                  />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </QueryBoundary>
    </section>
  )
}

function Linha({
  ficha,
  carreiras,
  abrindoCarreiras,
  onAbrirCarreiras,
  onVerificar,
  onPublicar,
  onSalvarCarreiras,
  ocupado,
}: {
  ficha: Pendente
  carreiras: Carreira[]
  abrindoCarreiras: boolean
  onAbrirCarreiras: () => void
  onVerificar: (sourceUrl: string) => void
  onPublicar: () => void
  onSalvarCarreiras: (ids: string[]) => void
  ocupado: boolean
}) {
  const [fonte, setFonte] = useState('')
  const [selecionadas, setSelecionadas] = useState<string[]>([])

  const nuncaVerificada = ficha.verifiedAt === null
  const acaoPequena = { padding: '3px 11px', fontSize: '0.78rem' }

  return (
    <>
      <tr>
        <td>
          <b>{ficha.name}</b>
          <br />
          <span style={{ color: 'var(--bb-text-dim)', fontSize: '0.8rem' }}>{ficha.slug}</span>
        </td>
        <td style={{ whiteSpace: 'nowrap' }}>
          {nuncaVerificada ? (
            <span style={{ color: 'var(--bb-danger)' }}>Nunca verificada</span>
          ) : (
            <>
              {dataHoraBR(ficha.verifiedAt)}
              <br />
              <span style={{ color: 'var(--bb-danger)', fontSize: '0.8rem' }}>
                há {ficha.daysSinceVerification} dias
              </span>
            </>
          )}
        </td>
        <td>
          <span className={`adm-status ${ficha.published ? 'adm-status--ok' : 'adm-status--atencao'}`}>
            {ficha.published ? 'PUBLICADA' : 'RASCUNHO'}
          </span>
        </td>
        <td style={{ whiteSpace: 'nowrap' }}>
          <Button ghost style={acaoPequena} disabled={ocupado} onClick={onAbrirCarreiras}>
            Carreiras
          </Button>{' '}
          {!ficha.published && (
            <Button
              ghost
              style={acaoPequena}
              disabled={ocupado || nuncaVerificada}
              title={
                nuncaVerificada
                  ? 'Publicar exige ter conferido a ficha e registrado a fonte oficial'
                  : undefined
              }
              onClick={onPublicar}
            >
              Publicar
            </Button>
          )}
        </td>
      </tr>

      <tr>
        <td colSpan={4} style={{ paddingTop: 0 }}>
          {/*
            Registrar verificação exige o link. "Verificado" sem dizer contra o
            quê não prova nada, e a seção 11 pede o link justamente para o aluno
            poder conferir sozinho.
          */}
          <div style={{ display: 'flex', gap: 'var(--bb-s2)', flexWrap: 'wrap', alignItems: 'center' }}>
            <input
              value={fonte}
              onChange={(e) => setFonte(e.target.value)}
              placeholder="https://… link da fonte oficial conferida"
              aria-label={`Fonte oficial de ${ficha.name}`}
              style={{
                flex: '1 1 320px',
                minWidth: 0,
                background: 'var(--bb-surface)',
                border: '1px solid var(--bb-border)',
                borderRadius: 'var(--bb-r1)',
                color: 'var(--bb-text)',
                font: 'inherit',
                fontSize: '0.86rem',
                padding: '7px 11px',
              }}
            />
            <Button
              style={acaoPequena}
              disabled={ocupado || fonte.trim().length === 0}
              onClick={() => {
                onVerificar(fonte.trim())
                setFonte('')
              }}
            >
              Registrar verificação
            </Button>
          </div>

          {abrindoCarreiras && (
            <div style={{ marginTop: 'var(--bb-s3)' }}>
              <p style={{ margin: '0 0 6px', fontSize: '0.82rem', color: 'var(--bb-text-dim)' }}>
                Um concurso pode estar em mais de uma carreira — é o que faz o cargo aparecer
                para quem estuda a outra.
              </p>
              <div style={{ display: 'flex', gap: 'var(--bb-s3)', flexWrap: 'wrap' }}>
                {carreiras.map((c) => (
                  <label key={c.id} style={{ display: 'flex', gap: 6, alignItems: 'center', fontSize: '0.86rem' }}>
                    <input
                      type="checkbox"
                      checked={selecionadas.includes(c.id)}
                      onChange={(e) =>
                        setSelecionadas((atual) =>
                          e.target.checked ? [...atual, c.id] : atual.filter((x) => x !== c.id),
                        )
                      }
                    />
                    {c.name}
                  </label>
                ))}
              </div>
              <Button
                style={{ ...acaoPequena, marginTop: 'var(--bb-s2)' }}
                disabled={ocupado || selecionadas.length === 0}
                onClick={() => onSalvarCarreiras(selecionadas)}
              >
                Salvar carreiras
              </Button>
            </div>
          )}
        </td>
      </tr>
    </>
  )
}
