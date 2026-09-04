import { useQuery, type UseQueryResult } from '@tanstack/react-query'
import { useDeferredValue, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api/http'
import { Button, EmptyState, Skeleton } from '../../ui/basics'
import { dataBR, moeda } from '../../ui/format'
import { IconeCheck, IconeLupa } from '../../ui/icons'
import './concursos.css'

interface Carreira {
  id: string
  name: string
  slug: string
  description: string | null
  position: number
}

interface Orgao {
  id: string
  name: string
  acronym: string
  sphere: string
  state: string | null
}

interface Resumo {
  id: string
  name: string
  slug: string
  board: string | null
  status: string
  vacancies: number | null
  salaryCents: number | null
  registrationEnd: string | null
  examDate: string | null
  registrationOpen: boolean
  verifiedAt: string | null
  verificationStale: boolean
}

interface Pagina {
  items: Resumo[]
  page: number
  totalPages: number
  totalItems: number
}

interface Resultado {
  kind: string
  id: string
  slug: string
  title: string
  subtitle: string | null
  status: string | null
  salaryCents: number | null
  educationLevel: string | null
  relevance: number
}

const STATUS: Record<string, string> = {
  EXPECTED: 'Previsto',
  AUTHORIZED: 'Autorizado',
  NOTICE_PUBLISHED: 'Edital publicado',
  REGISTRATION_OPEN: 'Inscrições abertas',
  REGISTRATION_CLOSED: 'Inscrições encerradas',
  EXAM_TAKEN: 'Prova aplicada',
  CLOSED: 'Encerrado',
}

const ESCOLARIDADE: Record<string, string> = {
  FUNDAMENTAL: 'Fundamental',
  MEDIO: 'Médio',
  TECNICO: 'Técnico',
  SUPERIOR: 'Superior',
  POS: 'Pós-graduação',
}

export function ConcursosPage() {
  const [termo, setTermo] = useState('')
  const [carreira, setCarreira] = useState('')
  const [orgao, setOrgao] = useState('')
  const [status, setStatus] = useState('')
  const [escolaridade, setEscolaridade] = useState('')
  const [page, setPage] = useState(0)

  // A busca dispara a cada tecla; `useDeferredValue` evita uma requisição por
  // caractere sem esconder o que a pessoa acabou de digitar.
  const termoAtivo = useDeferredValue(termo.trim())
  const buscando = termoAtivo.length >= 2

  const carreiras = useQuery({
    queryKey: ['carreiras'],
    queryFn: () => api<Carreira[]>('/contests/careers'),
    staleTime: 5 * 60 * 1000,
  })

  const orgaos = useQuery({
    queryKey: ['orgaos'],
    queryFn: () => api<Orgao[]>('/contests/agencies'),
    staleTime: 5 * 60 * 1000,
  })

  const filtros = new URLSearchParams()
  if (carreira) filtros.set('career', carreira)
  if (orgao) filtros.set('agency', orgao)
  if (status) filtros.set('status', status)
  if (escolaridade) filtros.set('education_level', escolaridade)

  const lista = useQuery({
    queryKey: ['concursos', carreira, orgao, status, escolaridade, page],
    queryFn: () => api<Pagina>(`/contests?${filtros}&page=${page}&size=20`),
    enabled: !buscando,
    placeholderData: (anterior) => anterior,
  })

  const busca = useQuery({
    queryKey: ['busca-concursos', termoAtivo, carreira, orgao, status, escolaridade],
    queryFn: () =>
      api<Resultado[]>(
        `/contests/search?q=${encodeURIComponent(termoAtivo)}&${filtros}&limit=30`,
      ),
    enabled: buscando,
  })

  function limpar() {
    setTermo('')
    setCarreira('')
    setOrgao('')
    setStatus('')
    setEscolaridade('')
    setPage(0)
  }

  const temFiltro = Boolean(termo || carreira || orgao || status || escolaridade)

  return (
    <section>
      <header className="cc-cabecalho">
        <h1>Concursos</h1>
        <p>
          Salário, vagas e prazos conferidos na fonte oficial. Cada ficha mostra a data da
          última verificação — e avisa quando ela envelheceu.
        </p>
      </header>

      <div className="cc-busca">
        <IconeLupa />
        <input
          type="search"
          value={termo}
          onChange={(e) => {
            setTermo(e.target.value)
            setPage(0)
          }}
          placeholder="Banco do Brasil, escriturário, Cesgranrio…"
          aria-label="Buscar concursos e cursos"
        />
        {temFiltro && (
          <button type="button" className="cc-limpar" onClick={limpar}>
            Limpar
          </button>
        )}
      </div>

      <div className="cc-filtros">
        <Filtro id="f-carreira" rotulo="Carreira" valor={carreira} onChange={(v) => { setCarreira(v); setPage(0) }}>
          <option value="">Todas</option>
          {(carreiras.data ?? []).map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </Filtro>

        <Filtro id="f-orgao" rotulo="Órgão" valor={orgao} onChange={(v) => { setOrgao(v); setPage(0) }}>
          <option value="">Todos</option>
          {(orgaos.data ?? []).map((o) => (
            <option key={o.id} value={o.id}>{o.acronym} — {o.name}</option>
          ))}
        </Filtro>

        <Filtro id="f-status" rotulo="Situação" valor={status} onChange={(v) => { setStatus(v); setPage(0) }}>
          <option value="">Todas</option>
          {Object.entries(STATUS).map(([v, r]) => (
            <option key={v} value={v}>{r}</option>
          ))}
        </Filtro>

        <Filtro id="f-escolaridade" rotulo="Escolaridade" valor={escolaridade} onChange={(v) => { setEscolaridade(v); setPage(0) }}>
          <option value="">Todas</option>
          {Object.entries(ESCOLARIDADE).map(([v, r]) => (
            <option key={v} value={v}>{r}</option>
          ))}
        </Filtro>
      </div>

      {buscando ? <ResultadosDaBusca query={busca} termo={termoAtivo} /> : <Catalogo query={lista} onPage={setPage} />}
    </section>
  )
}

function Filtro({
  id,
  rotulo,
  valor,
  onChange,
  children,
}: {
  id: string
  rotulo: string
  valor: string
  onChange: (v: string) => void
  children: ReactNode
}) {
  return (
    <div className="cc-filtro">
      <label htmlFor={id}>{rotulo}</label>
      <select id={id} value={valor} onChange={(e) => onChange(e.target.value)}>
        {children}
      </select>
    </div>
  )
}

function Catalogo({
  query,
  onPage,
}: {
  query: UseQueryResult<Pagina>
  onPage: (p: number) => void
}) {
  if (query.isPending) return <Skeleton height={220} />
  if (query.isError) return <EmptyState>Não foi possível carregar os concursos agora.</EmptyState>

  const pagina = query.data
  if (!pagina || pagina.items.length === 0) {
    return <EmptyState>Nenhum concurso com esses filtros.</EmptyState>
  }

  return (
    <>
      <p className="cc-resumo">
        {pagina.totalItems} concurso{pagina.totalItems === 1 ? '' : 's'} publicado
        {pagina.totalItems === 1 ? '' : 's'}
      </p>

      <ul className="cc-lista">
        {pagina.items.map((c) => (
          <li key={c.id}>
            <Link to={`/concursos/${c.slug}`} className="cc-item">
              <div className="cc-item-topo">
                <h2>{c.name}</h2>
                <span
                  className={`cc-etiqueta cc-etiqueta--${c.registrationOpen ? 'aberta' : 'fechada'}`}
                >
                  {c.registrationOpen ? 'Inscrições abertas' : STATUS[c.status] ?? c.status}
                </span>
              </div>

              <div className="cc-meta">
                {c.board && <span>Banca <b>{c.board}</b></span>}
                {c.vacancies !== null && <span><b>{c.vacancies}</b> vagas</span>}
                {c.salaryCents !== null && <span>Salário <b>{moeda(c.salaryCents)}</b></span>}
                {c.registrationEnd && <span>Inscrições até <b>{dataBR(c.registrationEnd)}</b></span>}
                <SeloDeVerificacao verifiedAt={c.verifiedAt} defasado={c.verificationStale} />
              </div>
            </Link>
          </li>
        ))}
      </ul>

      {pagina.totalPages > 1 && (
        <div className="adm-paginacao" style={{ marginTop: 'var(--bb-s4)' }}>
          <Button ghost disabled={pagina.page === 0} onClick={() => onPage(pagina.page - 1)}>
            ‹ Anterior
          </Button>
          <span>Página {pagina.page + 1} de {pagina.totalPages}</span>
          <Button
            ghost
            disabled={pagina.page + 1 >= pagina.totalPages}
            onClick={() => onPage(pagina.page + 1)}
          >
            Próxima ›
          </Button>
        </div>
      )}
    </>
  )
}

function ResultadosDaBusca({
  query,
  termo,
}: {
  query: UseQueryResult<Resultado[]>
  termo: string
}) {
  if (query.isPending) return <Skeleton height={180} />
  if (query.isError) return <EmptyState>A busca falhou. Tente de novo.</EmptyState>

  const itens = query.data ?? []
  if (itens.length === 0) {
    return (
      <EmptyState>
        Nada encontrado para “{termo}”. A busca aceita erro de digitação e falta de acento —
        se não achou, provavelmente ainda não está publicado.
      </EmptyState>
    )
  }

  return (
    <>
      <p className="cc-resumo">
        {itens.length} resultado{itens.length === 1 ? '' : 's'} para “{termo}”
      </p>
      <ul className="cc-lista">
        {itens.map((r) => (
          <li key={`${r.kind}-${r.id}`}>
            {/* A busca é unificada: concurso vai para a ficha, curso vai para o
                catálogo do aluno. Um link só para os dois levaria a 404. */}
            <Link
              to={r.kind === 'contest' ? `/concursos/${r.slug}` : `/curso/${r.id}`}
              className="cc-item"
            >
              <div className="cc-item-topo">
                <h2>{r.title}</h2>
                <span className="cc-tipo">{r.kind === 'contest' ? 'Concurso' : 'Curso'}</span>
              </div>
              <div className="cc-meta">
                {r.subtitle && <span>{r.subtitle}</span>}
                {r.salaryCents !== null && <span>Salário <b>{moeda(r.salaryCents)}</b></span>}
                {r.educationLevel && <span>{ESCOLARIDADE[r.educationLevel] ?? r.educationLevel}</span>}
              </div>
            </Link>
          </li>
        ))}
      </ul>
    </>
  )
}

export function SeloDeVerificacao({
  verifiedAt,
  defasado,
}: {
  verifiedAt: string | null
  defasado: boolean
}) {
  if (!verifiedAt) {
    return <span className="cc-selo cc-selo--defasado">Ainda não verificado</span>
  }
  return (
    <span className={`cc-selo ${defasado ? 'cc-selo--defasado' : ''}`.trim()}>
      <IconeCheck size={13} />
      Verificado em {dataBR(verifiedAt)}
    </span>
  )
}
