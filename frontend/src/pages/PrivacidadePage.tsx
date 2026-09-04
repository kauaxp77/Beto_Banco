import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../api/http'
import { useSession } from '../auth/session'
import { Button, Card, Skeleton } from '../ui/basics'
import { dataHoraBR } from '../ui/format'
import { useToast } from '../ui/Toast'
import './privacidade.css'

interface Consentimentos {
  atual: Record<string, boolean>
  historico: Array<{
    purpose: string
    granted: boolean
    acceptedText: string | null
    recordedAt: string
  }>
}

interface Pedido {
  type: string
  status: string
  createdAt: string
}

/**
 * As finalidades que a API aceita, com o texto que a pessoa realmente lê.
 *
 * Esse texto vai junto no registro do consentimento: a LGPD exige provar não só
 * que houve aceite, mas o que estava escrito quando ele aconteceu. Mudar a
 * frase aqui muda o que passa a ser registrado — o que já foi aceito continua
 * guardado com a frase antiga.
 */
const FINALIDADES: Array<{ codigo: string; titulo: string; texto: string }> = [
  {
    codigo: 'MARKETING_EMAIL',
    titulo: 'E-mails sobre cursos e concursos',
    texto: 'Autorizo o envio de e-mails com novidades de cursos, editais e promoções.',
  },
  {
    codigo: 'MARKETING_WHATSAPP',
    titulo: 'Mensagens no WhatsApp',
    texto: 'Autorizo o contato por WhatsApp com avisos de edital, turmas e promoções.',
  },
  {
    codigo: 'COOKIE_ANALYTICS',
    titulo: 'Medição de uso da plataforma',
    texto: 'Autorizo cookies que medem como a plataforma é usada, para melhorá-la.',
  },
  {
    codigo: 'COOKIE_MARKETING',
    titulo: 'Cookies de publicidade',
    texto: 'Autorizo cookies usados para personalizar anúncios fora da plataforma.',
  },
]

const TIPO_DE_PEDIDO: Record<string, string> = {
  EXPORT: 'Exportação de dados',
  DELETE: 'Exclusão da conta',
  CORRECTION: 'Correção de dados',
}

/** Portal do titular. Documento Mestre V4.0, seção 22. */
export function PrivacidadePage() {
  const consentimentos = useQuery({
    queryKey: ['meus-consentimentos'],
    queryFn: () => api<Consentimentos>('/me/privacy/consents'),
  })

  const pedidos = useQuery({
    queryKey: ['meus-pedidos-lgpd'],
    queryFn: () => api<Pedido[]>('/me/privacy/requests'),
  })

  return (
    <section className="pv">
      <header className="pv-cabecalho">
        <h1>Privacidade</h1>
        <p>
          Seus dados são seus. Aqui você escolhe o que autoriza, leva tudo embora em um
          arquivo, ou encerra a conta — sem pedir por e-mail e sem esperar resposta de
          ninguém.
        </p>
      </header>

      <h2 className="pv-secao">O que você autoriza</h2>
      {consentimentos.isPending ? (
        <Skeleton height={220} />
      ) : (
        <Consentimentos dados={consentimentos.data} />
      )}

      <h2 className="pv-secao">Seus dados</h2>
      <Exportacao />

      <h2 className="pv-secao">Histórico de pedidos</h2>
      {pedidos.isPending ? (
        <Skeleton height={80} />
      ) : (pedidos.data ?? []).length === 0 ? (
        <p className="pv-vazio">Você ainda não fez nenhum pedido sobre seus dados.</p>
      ) : (
        <ul className="pv-pedidos">
          {(pedidos.data ?? []).map((p, i) => (
            <li key={`${p.type}-${i}`}>
              <b>{TIPO_DE_PEDIDO[p.type] ?? p.type}</b>
              <span>{p.status}</span>
              <span>{dataHoraBR(p.createdAt)}</span>
            </li>
          ))}
        </ul>
      )}

      <h2 className="pv-secao">Documentos</h2>
      <p className="pv-links">
        <Link to="/legal/terms-of-use">Termos de uso</Link>
        <Link to="/legal/privacy-policy">Política de privacidade</Link>
        <Link to="/legal/cookie-policy">Política de cookies</Link>
      </p>

      <h2 className="pv-secao pv-secao--perigo">Encerrar a conta</h2>
      <Exclusao />
    </section>
  )
}

function Consentimentos({ dados }: { dados: Consentimentos | undefined }) {
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()
  const [verHistorico, setVerHistorico] = useState(false)

  const registrar = useMutation({
    mutationFn: ({ codigo, conceder, texto }: { codigo: string; conceder: boolean; texto: string }) =>
      api('/me/privacy/consents', {
        method: 'POST',
        body: JSON.stringify({ purpose: codigo, granted: conceder, acceptedText: texto }),
      }),
    onSuccess: () => {
      toast('Preferência registrada.')
      void queryClient.invalidateQueries({ queryKey: ['meus-consentimentos'] })
    },
    onError: (err) =>
      toastErro(err instanceof ApiError ? err.message : 'Não foi possível registrar.'),
  })

  const revogarTudo = useMutation({
    mutationFn: () => api<{ revogados: number }>('/me/privacy/consents', { method: 'DELETE' }),
    onSuccess: (r) => {
      toast(`${r.revogados} autorizaç${r.revogados === 1 ? 'ão revogada' : 'ões revogadas'}.`)
      void queryClient.invalidateQueries({ queryKey: ['meus-consentimentos'] })
    },
    onError: (err) =>
      toastErro(err instanceof ApiError ? err.message : 'Não foi possível revogar.'),
  })

  const atual = dados?.atual ?? {}
  const historico = dados?.historico ?? []

  return (
    <Card>
      <ul className="pv-finalidades">
        {FINALIDADES.map((f) => (
          <li key={f.codigo}>
            <div>
              <b>{f.titulo}</b>
              <span>{f.texto}</span>
            </div>
            <label className="pv-interruptor">
              <input
                type="checkbox"
                checked={atual[f.codigo] ?? false}
                disabled={registrar.isPending}
                onChange={(e) =>
                  registrar.mutate({ codigo: f.codigo, conceder: e.target.checked, texto: f.texto })
                }
              />
              <span aria-hidden="true" />
              <span className="pv-interruptor-rotulo">
                {atual[f.codigo] ? 'Autorizado' : 'Não autorizado'}
              </span>
            </label>
          </li>
        ))}
      </ul>

      <div className="pv-acoes">
        <Button ghost disabled={revogarTudo.isPending} onClick={() => revogarTudo.mutate()}>
          Revogar todas
        </Button>
        <Button ghost onClick={() => setVerHistorico(!verHistorico)}>
          {verHistorico ? 'Fechar histórico' : `Histórico (${historico.length})`}
        </Button>
      </div>

      {verHistorico && (
        <>
          {/*
            O histórico é somente-leitura e não some quando a pessoa revoga:
            a prova de consentimento é justamente o registro do que foi aceito,
            quando, e com qual texto na tela.
          */}
          <p className="pv-nota">
            Cada mudança fica registrada com data, hora e o texto que estava na tela. Revogar
            não apaga o histórico — ele é a prova de que a autorização existiu.
          </p>
          <ul className="pv-historico">
            {historico.map((h, i) => (
              <li key={`${h.recordedAt}-${i}`}>
                <span className={h.granted ? 'pv-sim' : 'pv-nao'}>
                  {h.granted ? 'Autorizou' : 'Revogou'}
                </span>
                <b>{FINALIDADES.find((f) => f.codigo === h.purpose)?.titulo ?? h.purpose}</b>
                <span className="pv-quando">{dataHoraBR(h.recordedAt)}</span>
              </li>
            ))}
          </ul>
        </>
      )}
    </Card>
  )
}

function Exportacao() {
  const { toast, toastErro } = useToast()
  const [baixando, setBaixando] = useState(false)

  async function exportar() {
    setBaixando(true)
    try {
      const dados = await api<Record<string, unknown>>('/me/privacy/export')
      // O arquivo é montado no navegador a partir da resposta: o endpoint
      // devolve JSON, não um anexo, e forçar download no servidor exigiria
      // uma rota só para isso.
      const blob = new Blob([JSON.stringify(dados, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `meus-dados-${new Date().toISOString().slice(0, 10)}.json`
      link.click()
      URL.revokeObjectURL(url)
      toast('Arquivo gerado.')
    } catch (err) {
      toastErro(err instanceof ApiError ? err.message : 'Não foi possível exportar seus dados.')
    } finally {
      setBaixando(false)
    }
  }

  return (
    <Card>
      <p style={{ margin: '0 0 var(--bb-s3)', color: 'var(--bb-text-dim)', fontSize: '0.92rem' }}>
        Um arquivo JSON com tudo o que a plataforma guarda sobre você: cadastro, compras,
        progresso nas aulas, comentários, certificados, redações e o histórico de
        consentimentos.
      </p>
      <Button disabled={baixando} onClick={() => void exportar()}>
        {baixando ? 'Gerando…' : 'Baixar meus dados'}
      </Button>
    </Card>
  )
}

function Exclusao() {
  const { toastErro } = useToast()
  const { logout } = useSession()
  const [aberto, setAberto] = useState(false)
  const [confirmacao, setConfirmacao] = useState('')

  const FRASE = 'EXCLUIR MINHA CONTA'

  const excluir = useMutation({
    mutationFn: () =>
      api('/me/privacy/account', {
        method: 'DELETE',
        body: JSON.stringify({ confirmation: confirmacao }),
      }),
    onSuccess: async () => {
      await logout()
      window.location.href = '/'
    },
    onError: (err) =>
      toastErro(err instanceof ApiError ? err.message : 'Não foi possível excluir a conta.'),
  })

  return (
    <Card className="pv-perigo">
      <p style={{ margin: '0 0 var(--bb-s3)', fontSize: '0.92rem', lineHeight: 1.6 }}>
        A exclusão anonimiza seu cadastro: nome, e-mail e telefone deixam de existir e a conta
        para de funcionar. <b>O registro fiscal dos pagamentos é preservado</b> — a lei obriga
        a guardá-lo, e ele fica sem ligação com você.
      </p>
      <p style={{ margin: '0 0 var(--bb-s4)', fontSize: '0.9rem', color: 'var(--bb-danger)' }}>
        Não há como desfazer. Seu progresso, certificados e redações são perdidos.
      </p>

      {!aberto ? (
        <Button ghost onClick={() => setAberto(true)}>
          Quero excluir minha conta
        </Button>
      ) : (
        <div style={{ display: 'grid', gap: 'var(--bb-s3)', maxWidth: 420 }}>
          <label htmlFor="pv-confirmar" style={{ fontSize: '0.88rem' }}>
            Para confirmar, digite <b>{FRASE}</b>
          </label>
          <input
            id="pv-confirmar"
            value={confirmacao}
            onChange={(e) => setConfirmacao(e.target.value)}
            autoComplete="off"
            style={{
              background: 'var(--bb-bg)',
              border: '1px solid var(--bb-border)',
              borderRadius: 10,
              color: 'var(--bb-text)',
              font: 'inherit',
              padding: '10px 12px',
            }}
          />
          <div style={{ display: 'flex', gap: 'var(--bb-s2)' }}>
            <Button
              className="pv-botao-perigo"
              disabled={confirmacao !== FRASE || excluir.isPending}
              onClick={() => excluir.mutate()}
            >
              {excluir.isPending ? 'Excluindo…' : 'Excluir definitivamente'}
            </Button>
            <Button ghost onClick={() => { setAberto(false); setConfirmacao('') }}>
              Cancelar
            </Button>
          </div>
        </div>
      )}
    </Card>
  )
}
