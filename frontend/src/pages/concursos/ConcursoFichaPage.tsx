import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { api } from '../../api/http'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { dataBR, moeda } from '../../ui/format'
import { IconeAlerta, IconeLink } from '../../ui/icons'
import { SeloDeVerificacao } from './ConcursosPage'
import './concursos.css'

interface Ficha {
  id: string
  name: string
  slug: string
  board: string | null
  status: string
  vacancies: number | null
  reserveList: number | null
  salaryCents: number | null
  educationLevel: string | null
  weeklyHours: number | null
  benefits: string | null
  registrationStart: string | null
  registrationEnd: string | null
  registrationFeeCents: number | null
  examDate: string | null
  registrationOpen: boolean
  officialPdfUrl: string | null
  sourceUrl: string | null
  verifiedAt: string | null
  verificationStale: boolean
  daysSinceVerification: number
  careers: string[]
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
  FUNDAMENTAL: 'Ensino fundamental',
  MEDIO: 'Ensino médio',
  TECNICO: 'Técnico',
  SUPERIOR: 'Ensino superior',
  POS: 'Pós-graduação',
}

export function ConcursoFichaPage() {
  const { slug } = useParams()

  const query = useQuery({
    queryKey: ['concurso', slug],
    queryFn: () => api<Ficha>(`/contests/${slug}`),
    enabled: Boolean(slug),
  })

  return (
    <section>
      <Link to="/concursos" className="cc-voltar">
        ← Todos os concursos
      </Link>
      <QueryBoundary query={query}>{(ficha) => <Conteudo ficha={ficha} />}</QueryBoundary>
    </section>
  )
}

function Conteudo({ ficha }: { ficha: Ficha }) {
  return (
    <article className="cc-ficha">
      <h1>{ficha.name}</h1>

      {ficha.careers.length > 0 && (
        <div className="cc-carreiras">
          {/* Seção 07: um concurso pertence a mais de uma carreira, e mostrar
              todas é o que faz o cargo aparecer para quem estuda a outra. */}
          {ficha.careers.map((c) => (
            <span key={c} className="cc-carreira">{c}</span>
          ))}
        </div>
      )}

      {ficha.verificationStale && (
        <p className="cc-aviso">
          <IconeAlerta />
          <span>
            Esta ficha não é conferida há {ficha.daysSinceVerification} dias. Salário e vagas
            podem ter mudado — confirme no edital oficial antes de decidir a inscrição.
          </span>
        </p>
      )}

      <div className="cc-dados">
        <Dado rotulo="Situação" valor={STATUS[ficha.status] ?? ficha.status} />
        <Dado rotulo="Banca" valor={ficha.board ?? '—'} />
        <Dado rotulo="Vagas" valor={ficha.vacancies?.toString() ?? '—'} />
        <Dado rotulo="Cadastro reserva" valor={ficha.reserveList?.toString() ?? '—'} />
        <Dado rotulo="Salário" valor={moeda(ficha.salaryCents)} />
        <Dado
          rotulo="Escolaridade"
          valor={ficha.educationLevel ? ESCOLARIDADE[ficha.educationLevel] ?? ficha.educationLevel : '—'}
        />
        <Dado rotulo="Jornada" valor={ficha.weeklyHours ? `${ficha.weeklyHours}h semanais` : '—'} />
        <Dado rotulo="Taxa de inscrição" valor={moeda(ficha.registrationFeeCents)} />
        <Dado rotulo="Inscrições" valor={periodo(ficha.registrationStart, ficha.registrationEnd)} />
        <Dado rotulo="Prova" valor={dataBR(ficha.examDate)} />
      </div>

      {ficha.benefits && <p className="cc-beneficios">{ficha.benefits}</p>}

      {/*
        Seção 11: a ficha nunca é a autoridade — ela aponta para quem é. A data
        de verificação e o link da fonte saem juntos de propósito: um sem o
        outro pede confiança sem dar como conferir.
      */}
      <footer className="cc-fonte">
        <SeloDeVerificacao verifiedAt={ficha.verifiedAt} defasado={ficha.verificationStale} />
        {ficha.sourceUrl && (
          <a href={ficha.sourceUrl} target="_blank" rel="noreferrer noopener">
            <IconeLink /> Fonte oficial
          </a>
        )}
        {ficha.officialPdfUrl && (
          <a href={ficha.officialPdfUrl} target="_blank" rel="noreferrer noopener">
            <IconeLink /> Edital em PDF
          </a>
        )}
      </footer>
    </article>
  )
}

function Dado({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <div className="cc-dado">
      <span className="rotulo">{rotulo}</span>
      <span className="valor">{valor}</span>
    </div>
  )
}

function periodo(inicio: string | null, fim: string | null): string {
  if (!inicio && !fim) return '—'
  if (inicio && fim) return `${dataBR(inicio)} a ${dataBR(fim)}`
  return dataBR(inicio ?? fim)
}
