import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { api, ApiError } from '../api/http'
import { Link } from 'react-router-dom'
import { useSession } from '../auth/session'
import {
  IconeCamera,
  IconeCheck,
  IconeEnvelope,
  IconeSalvar,
  IconeTelefone,
  IconeUsuario,
} from '../ui/icons'
import { QueryBoundary } from '../ui/QueryBoundary'
import { useToast } from '../ui/Toast'
import './perfil.css'

interface StudentResponse {
  id: string
  email: string
  fullName: string
  phone: string | null
}

/** Do papel técnico para o que a pessoa lê na tela. */
const NIVEIS: Array<{ role: string; nivel: string; curto: string }> = [
  { role: 'ROLE_ADMIN', nivel: 'Administrador', curto: 'Admin' },
  { role: 'ROLE_INSTRUCTOR', nivel: 'Instrutor', curto: 'Prof' },
  { role: 'ROLE_CORRECTOR', nivel: 'Corretor', curto: 'Corretor' },
]

export function ProfilePage() {
  const query = useQuery({
    queryKey: ['perfil'],
    queryFn: () => api<StudentResponse>('/students/me'),
  })

  return (
    <section className="pf">
      <header className="pf-cabecalho">
        <h1>Meu perfil</h1>
        <p>Seus dados de cadastro e como falamos com você.</p>
      </header>

      <QueryBoundary query={query}>{(perfil) => <Cartao perfil={perfil} />}</QueryBoundary>
    </section>
  )
}

function Cartao({ perfil }: { perfil: StudentResponse }) {
  const { toastErro } = useToast()
  const { user } = useSession()
  const queryClient = useQueryClient()

  const [nome, setNome] = useState(perfil.fullName)
  const [telefone, setTelefone] = useState(perfil.phone ?? '')
  const [erros, setErros] = useState<Record<string, string>>({})
  const [salvo, setSalvo] = useState(false)
  const [avisoAvatar, setAvisoAvatar] = useState(false)

  useEffect(() => {
    setNome(perfil.fullName)
    setTelefone(perfil.phone ?? '')
  }, [perfil])

  const papeis = user?.roles ?? []
  const nivel = useMemo(
    () => NIVEIS.find((n) => papeis.includes(n.role)) ?? { nivel: 'Aluno', curto: 'Aluno' },
    [papeis],
  )

  const alterado = nome !== perfil.fullName || telefone !== (perfil.phone ?? '')

  const salvar = useMutation({
    mutationFn: () =>
      api<StudentResponse>('/students/me', {
        method: 'PUT',
        body: JSON.stringify({ fullName: nome, phone: apenasDigitos(telefone) || null }),
      }),
    onSuccess: (atualizado) => {
      queryClient.setQueryData(['perfil'], atualizado)
      setSalvo(true)
    },
    onError: (err) => {
      if (err instanceof ApiError && err.fieldErrors?.length) {
        setErros(Object.fromEntries(err.fieldErrors.map((f) => [f.field, f.message])))
      } else if (err instanceof ApiError) {
        toastErro(err.message)
      } else {
        toastErro('Erro inesperado ao salvar.')
      }
    },
  })

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    setErros({})
    setSalvo(false)
    salvar.mutate()
  }

  function cancelar() {
    setNome(perfil.fullName)
    setTelefone(perfil.phone ?? '')
    setErros({})
    setSalvo(false)
  }

  return (
    <div className="pf-cartao">
      <aside className="pf-identidade">
        <div className="pf-avatar-area">
          <div className="pf-avatar" aria-hidden="true">
            {iniciais(perfil.fullName)}
          </div>
          {/* Só para quem tem papel além de aluno: um selo "Aluno" em toda
              conta não informa nada e vira ruído sobre a foto. */}
          {nivel.curto !== 'Aluno' && <span className="pf-selo-admin">{nivel.curto}</span>}
        </div>

        <p className="pf-nome">{perfil.fullName}</p>
        <p className="pf-email-secundario">{perfil.email}</p>
        <span className="pf-pilula">Nível: {nivel.nivel}</span>

        <p className="pf-atalho-privacidade">
          <Link to="/privacidade">Privacidade e meus dados</Link>
        </p>

        <div>
          <button
            type="button"
            className="pf-trocar-avatar"
            onClick={() => setAvisoAvatar(true)}
          >
            <IconeCamera />
            Mudar avatar
          </button>
          {avisoAvatar && (
            // Honesto em vez de decorativo: não existe armazenamento de foto
            // no cadastro, e um botão que não faz nada é pior que a ausência
            // dele. As iniciais são o avatar até que haja onde guardar imagem.
            <p className="pf-aviso-avatar">
              O envio de foto ainda não está disponível. Por enquanto o avatar usa as
              iniciais do seu nome.
            </p>
          )}
        </div>
      </aside>

      <form className="pf-formulario" onSubmit={onSubmit} noValidate>
        <h2 className="pf-secao-titulo">Detalhes da conta</h2>

        <div className="pf-campos">
          <div className="pf-campo">
            <label htmlFor="pf-email">E-mail</label>
            <div className="pf-moldura pf-moldura--somente-leitura">
              <IconeEnvelope />
              <input id="pf-email" type="email" value={perfil.email} readOnly />
              {/* O visto confirma que este é o e-mail que autentica a conta —
                  foi por ele que a sessão atual entrou. */}
              <span className="pf-validado" title="E-mail confirmado">
                <IconeCheck size={13} />
              </span>
            </div>
            <span className="pf-dica">
              O e-mail identifica sua conta e não pode ser alterado por aqui.
            </span>
          </div>

          <div className="pf-campo">
            <label htmlFor="pf-nome">Nome completo</label>
            <div className={`pf-moldura ${erros.fullName ? 'pf-moldura--erro' : ''}`.trim()}>
              <IconeUsuario />
              <input
                id="pf-nome"
                value={nome}
                onChange={(e) => setNome(e.target.value)}
                aria-invalid={!!erros.fullName}
                autoComplete="name"
                required
              />
            </div>
            <span className="pf-erro" aria-live="polite">
              {erros.fullName}
            </span>
          </div>

          <div className="pf-campo">
            <label htmlFor="pf-telefone">Telefone</label>
            <div className={`pf-moldura ${erros.phone ? 'pf-moldura--erro' : ''}`.trim()}>
              <IconeTelefone />
              <input
                id="pf-telefone"
                type="tel"
                value={telefone}
                onChange={(e) => setTelefone(e.target.value)}
                /* Sem máscara rígida: a pessoa digita como quiser, e a
                   pontuação é retirada no envio. O placeholder mostra um
                   formato reconhecível em vez da regra crua da API. */
                placeholder="(61) 99999-0000"
                aria-invalid={!!erros.phone}
                autoComplete="tel"
              />
            </div>
            <span className="pf-erro" aria-live="polite">
              {erros.phone}
            </span>
            <span className="pf-dica">
              Pode digitar com parênteses e traço — guardamos só os números.
            </span>
          </div>
        </div>

        <div className="pf-acoes">
          <button
            type="submit"
            className="pf-botao pf-botao--principal"
            disabled={salvar.isPending || !alterado}
          >
            <IconeSalvar />
            {salvar.isPending ? 'Salvando…' : 'Salvar alterações'}
          </button>

          <button
            type="button"
            className="pf-botao pf-botao--secundario"
            onClick={cancelar}
            disabled={salvar.isPending || !alterado}
          >
            Cancelar
          </button>

          {/* aria-live: quem usa leitor de tela ouve a confirmação sem precisar
              procurar por ela na página. */}
          <span aria-live="polite">
            {salvo && !alterado && (
              <span className="pf-status">
                <IconeCheck size={14} />
                Perfil atualizado
              </span>
            )}
            {alterado && !salvar.isPending && (
              <span className="pf-status pf-status--pendente">Alterações não salvas</span>
            )}
          </span>
        </div>
      </form>
    </div>
  )
}

/**
 * A API aceita telefone só em dígitos ({@code ^$|^[0-9]{10,13}$}). Exigir isso
 * de quem digita seria transformar a regra do banco em tarefa do usuário:
 * "(61) 99999-0000" é como as pessoas escrevem número de telefone, e recusar
 * esse formato geraria um erro de validação por motivo nenhum.
 */
function apenasDigitos(valor: string): string {
  return valor.replace(/\D/g, '')
}

/** "Administrador do Laboratorio" -> "AL". Uma só palavra vira uma letra. */
function iniciais(nome: string): string {
  const partes = nome.trim().split(/\s+/).filter(Boolean)
  if (partes.length === 0) return '?'
  if (partes.length === 1) return partes[0].charAt(0).toUpperCase()
  return (partes[0].charAt(0) + partes[partes.length - 1].charAt(0)).toUpperCase()
}
