import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/http'
import { Badge, Button, Card, Skeleton } from '../ui/basics'
import { nomeAmigavel } from '../ui/format'
import './cursos.css'

interface CourseSummary {
  id: string
  title: string
  slug: string
  description: string | null
  coverUrl: string | null
  totalLessons: number
  completedLessons: number
  nextLessonId: string | null
}

interface EntitlementResponse {
  entitlementId: string
  productId: string
  sku: string | null
  productName: string | null
  source: string
  grantedAt: string
  expiresAt: string | null
}

interface AnnouncementItem {
  id: string
  courseId: string | null
  courseTitle: string | null
  title: string
  body: string
  createdAt: string
}

interface StudyStats {
  currentStreak: number
  bestStreak: number
  activeDaysLast30: number
  studiedToday: boolean
}

interface TrackCourse {
  id: string
  title: string
  totalLessons: number
  completedLessons: number
}

interface TrackItem {
  productId: string
  title: string
  totalLessons: number
  completedLessons: number
  courses: TrackCourse[]
}

interface CertificateItem {
  code: string
  courseId: string
  courseTitle: string | null
  hours: number
  issuedAt: string
}

interface TestimonialItem {
  id: string
  body: string
  status: string
  createdAt: string
}

function Constancia() {
  const query = useQuery({
    queryKey: ['minhas-stats'],
    queryFn: () => api<StudyStats>('/courses/me/stats'),
  })
  const s = query.data
  if (!s || (s.currentStreak === 0 && s.activeDaysLast30 === 0)) return null

  return (
    <p className="constancia">
      <span aria-hidden="true">🔥</span>{' '}
      {s.currentStreak > 0 ? (
        <>
          <strong>
            {s.currentStreak} dia{s.currentStreak === 1 ? '' : 's'} seguido
            {s.currentStreak === 1 ? '' : 's'}
          </strong>{' '}
          de estudo{!s.studiedToday && ' — estude hoje para manter a sequência!'}
        </>
      ) : (
        'Conclua uma aula hoje para começar uma nova sequência!'
      )}
      <span className="constancia-extra">
        Recorde: {s.bestStreak} · {s.activeDaysLast30} dia
        {s.activeDaysLast30 === 1 ? '' : 's'} ativo{s.activeDaysLast30 === 1 ? '' : 's'} no mês
      </span>
    </p>
  )
}

function Trilhas() {
  const query = useQuery({
    queryKey: ['minhas-trilhas'],
    queryFn: () => api<TrackItem[]>('/courses/me/tracks'),
  })
  const trilhas = query.data ?? []
  if (trilhas.length === 0) return null

  return (
    <section aria-label="Minhas trilhas" style={{ marginBottom: 'var(--bb-s5)' }}>
      <h2 className="secao-titulo">Minhas trilhas</h2>
      {trilhas.map((t) => {
        const pct =
          t.totalLessons === 0 ? 0 : Math.round((t.completedLessons / t.totalLessons) * 100)
        return (
          <details key={t.productId} className="trilha">
            <summary>
              <span className="trilha-titulo">{nomeAmigavel(t.title)}</span>
              <span className="trilha-progresso">
                <span className="barra-progresso" aria-hidden="true">
                  <span style={{ width: `${pct}%` }} />
                </span>
                {pct}% · {t.completedLessons}/{t.totalLessons} aulas
              </span>
            </summary>
            <ul>
              {t.courses.map((c) => (
                <li key={c.id}>
                  <Link to={`/curso/${c.id}`}>{nomeAmigavel(c.title)}</Link>
                  <span className="dim-txt">
                    {c.completedLessons}/{c.totalLessons} aulas
                  </span>
                </li>
              ))}
            </ul>
          </details>
        )
      })}
    </section>
  )
}

function CertificadosEDepoimento() {
  const queryClient = useQueryClient()
  const [texto, setTexto] = useState('')
  const [enviado, setEnviado] = useState(false)

  const certificados = useQuery({
    queryKey: ['meus-certificados'],
    queryFn: () => api<CertificateItem[]>('/courses/me/certificates'),
  })
  const meus = useQuery({
    queryKey: ['meus-depoimentos'],
    queryFn: () => api<TestimonialItem[]>('/courses/me/testimonials'),
  })

  const enviar = useMutation({
    mutationFn: () =>
      api('/courses/testimonials', {
        method: 'POST',
        body: JSON.stringify({ body: texto.trim() }),
      }),
    onSuccess: () => {
      setTexto('')
      setEnviado(true)
      void queryClient.invalidateQueries({ queryKey: ['meus-depoimentos'] })
    },
  })

  function submeter(e: FormEvent) {
    e.preventDefault()
    if (texto.trim()) enviar.mutate()
  }

  const lista = certificados.data ?? []
  const jaEnviou = (meus.data ?? []).length > 0

  return (
    <section style={{ marginTop: 'var(--bb-s6)' }}>
      {lista.length > 0 && (
        <>
          <h2 className="secao-titulo">Meus certificados</h2>
          <ul className="certificados-lista">
            {lista.map((c) => (
              <li key={c.code}>
                <Link to={`/certificado/${c.code}`}>
                  🎓 {c.courseTitle ? nomeAmigavel(c.courseTitle) : 'Curso'}
                </Link>
                <span className="dim-txt">
                  {c.hours}h · emitido em {new Date(c.issuedAt).toLocaleDateString('pt-BR')}
                </span>
              </li>
            ))}
          </ul>
        </>
      )}

      <details className="depoimento-cta">
        <summary>Conte sua experiência — envie um depoimento</summary>
        {enviado || jaEnviou ? (
          <p className="dim-txt" style={{ padding: '0 16px 14px' }}>
            {enviado
              ? 'Obrigado! Seu depoimento foi enviado e aparece no site após aprovação.'
              : 'Você já enviou um depoimento. Obrigado por compartilhar!'}
          </p>
        ) : (
          <form className="comentario-form" style={{ padding: '0 16px 16px' }} onSubmit={submeter}>
            <textarea
              value={texto}
              onChange={(e) => setTexto(e.target.value)}
              placeholder="Como a plataforma ajudou nos seus estudos?"
              rows={3}
              maxLength={2000}
              aria-label="Seu depoimento"
            />
            <Button type="submit" disabled={!texto.trim() || enviar.isPending}>
              Enviar depoimento
            </Button>
          </form>
        )}
      </details>
    </section>
  )
}

function Avisos() {
  const query = useQuery({
    queryKey: ['meus-anuncios'],
    queryFn: () => api<AnnouncementItem[]>('/courses/announcements'),
  })

  const avisos = query.data ?? []
  if (avisos.length === 0) return null

  return (
    <section className="avisos" aria-label="Avisos do professor">
      <h2>Avisos</h2>
      {avisos.slice(0, 5).map((a) => (
        <details key={a.id} className="aviso">
          <summary>
            <span className="aviso-titulo">{a.title}</span>
            <span className="aviso-meta">
              {a.courseTitle ? nomeAmigavel(a.courseTitle) : 'Geral'} ·{' '}
              {new Date(a.createdAt).toLocaleDateString('pt-BR')}
            </span>
          </summary>
          <p>{a.body}</p>
        </details>
      ))}
    </section>
  )
}

function CursoCard({ curso }: { curso: CourseSummary }) {
  const pct =
    curso.totalLessons === 0
      ? 0
      : Math.round((curso.completedLessons / curso.totalLessons) * 100)
  const comecou = curso.completedLessons > 0

  return (
    <Link to={`/curso/${curso.id}`} style={{ textDecoration: 'none', display: 'block' }}>
      <article className="curso-card">
        <div className="curso-capa">
          {curso.coverUrl ? (
            <img src={curso.coverUrl} alt="" />
          ) : (
            <span className="inicial" aria-hidden="true">
              {curso.title.charAt(0).toUpperCase()}
            </span>
          )}
        </div>
        <div className="curso-corpo">
          <h2>{nomeAmigavel(curso.title)}</h2>
          <div className="curso-progresso">
            <div className="meta">
              <span>
                {curso.completedLessons} de {curso.totalLessons} aulas
              </span>
              <span>{pct}%</span>
            </div>
            <div
              className="barra-progresso"
              role="progressbar"
              aria-valuenow={pct}
              aria-valuemin={0}
              aria-valuemax={100}
              aria-label={`Progresso em ${curso.title}`}
            >
              <span style={{ width: `${pct}%` }} />
            </div>
            <span style={{ color: 'var(--bb-gold)', fontSize: '0.85rem', fontWeight: 600 }}>
              {pct === 100 ? 'Curso concluído ✓' : comecou ? 'Continuar →' : 'Começar →'}
            </span>
          </div>
        </div>
      </article>
    </Link>
  )
}

export function DashboardPage() {
  const cursos = useQuery({
    queryKey: ['meus-cursos'],
    queryFn: () => api<CourseSummary[]>('/courses/me'),
  })

  // Fallback: compras cujo conteudo ainda nao foi montado pelo professor.
  const entitlements = useQuery({
    queryKey: ['meus-entitlements'],
    queryFn: () => api<EntitlementResponse[]>('/students/me/entitlements'),
  })

  if (cursos.isPending) return <Skeleton height={220} />

  const listaCursos = cursos.data ?? []
  const listaProdutos = entitlements.data ?? []

  return (
    <section>
      <h1>Meus cursos</h1>

      <Constancia />
      <Avisos />
      <Trilhas />

      {cursos.isError ? (
        <p className="bb-state">Não foi possível carregar seus cursos agora. Recarregue a página.</p>
      ) : listaCursos.length > 0 ? (
        <div className="cursos-grid">
          {listaCursos.map((c) => (
            <CursoCard key={c.id} curso={c} />
          ))}
        </div>
      ) : listaProdutos.length > 0 ? (
        <>
          <p style={{ color: 'var(--bb-text-dim)' }}>
            Seu acesso está garantido — as aulas destes produtos estão sendo preparadas e
            aparecem aqui assim que forem publicadas.
          </p>
          <div
            style={{
              display: 'grid',
              gap: 'var(--bb-s4)',
              gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
            }}
          >
            {listaProdutos.map((e) => (
              <Card key={e.entitlementId}>
                <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>
                  {nomeAmigavel(e.productName) || 'Produto'}
                </h2>
                {e.sku && <Badge>{e.sku}</Badge>}
                <p style={{ color: 'var(--bb-text-dim)', fontSize: '0.85rem' }}>
                  Liberado em {new Date(e.grantedAt).toLocaleDateString('pt-BR')}
                </p>
              </Card>
            ))}
          </div>
        </>
      ) : (
        <p className="bb-state">
          Você ainda não tem cursos liberados. Assim que um pagamento for confirmado, eles
          aparecem aqui.
        </p>
      )}

      <CertificadosEDepoimento />
    </section>
  )
}
