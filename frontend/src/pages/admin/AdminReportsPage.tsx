import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../../api/http'
import { Button } from '../../ui/basics'
import { nomeAmigavel } from '../../ui/format'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { StatusPill } from './AdminLayout'

interface CourseReport {
  id: string
  title: string
  published: boolean
  students: number
  started: number
  totalLessons: number
  avgCompletionPct: number
}

interface LessonLine {
  id: string
  title: string
  moduleTitle: string
  completions: number
  completionPct: number
  helpful: number
  notHelpful: number
  comments: number
}

interface LessonReport {
  courseId: string
  courseTitle: string
  students: number
  lessons: LessonLine[]
}

/** Barra inline: percentual legivel de relance, numero exato ao lado. */
function Barra({ pct }: { pct: number }) {
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8, minWidth: 140 }}>
      <span
        aria-hidden="true"
        style={{
          flex: 1,
          height: 6,
          borderRadius: 999,
          background: 'var(--bb-surface-2)',
          overflow: 'hidden',
        }}
      >
        <span
          style={{
            display: 'block',
            height: '100%',
            width: `${pct}%`,
            background:
              pct < 30 ? 'var(--bb-danger)' : pct < 60 ? 'var(--bb-gold)' : 'var(--bb-success)',
          }}
        />
      </span>
      <span style={{ fontVariantNumeric: 'tabular-nums', fontSize: '0.82rem', width: 38 }}>
        {pct}%
      </span>
    </span>
  )
}

function RelatorioDoCurso({ courseId, onVoltar }: { courseId: string; onVoltar: () => void }) {
  const query = useQuery({
    queryKey: ['admin-relatorio-curso', courseId],
    queryFn: () => api<LessonReport>(`/admin/courses/${courseId}/reports`),
  })

  return (
    <QueryBoundary query={query}>
      {(r) => (
        <>
          <p>
            <Button ghost onClick={onVoltar}>
              ← Todos os cursos
            </Button>
          </p>
          <h2 style={{ fontSize: '1.1rem' }}>
            {nomeAmigavel(r.courseTitle)} · {r.students} aluno{r.students === 1 ? '' : 's'}
          </h2>
          <p className="adm-sub">
            Conclusão por aula: quedas bruscas entre aulas vizinhas indicam onde a turma abandona.
          </p>
          <div className="adm-tabela-wrap">
            <table className="adm-tabela">
              <thead>
                <tr>
                  <th>Aula</th>
                  <th>Módulo</th>
                  <th>Conclusão</th>
                  <th>👍</th>
                  <th>👎</th>
                  <th>Comentários</th>
                </tr>
              </thead>
              <tbody>
                {r.lessons.map((l) => (
                  <tr key={l.id}>
                    <td>{l.title}</td>
                    <td style={{ color: 'var(--bb-text-dim)', fontSize: '0.82rem' }}>
                      {l.moduleTitle}
                    </td>
                    <td>
                      <Barra pct={l.completionPct} />
                    </td>
                    <td>{l.helpful}</td>
                    <td>{l.notHelpful}</td>
                    <td>{l.comments}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </QueryBoundary>
  )
}

export function AdminReportsPage() {
  const [cursoAberto, setCursoAberto] = useState<string | null>(null)

  const query = useQuery({
    queryKey: ['admin-relatorios'],
    queryFn: () => api<CourseReport[]>('/admin/courses/reports'),
  })

  return (
    <section>
      <h1 className="adm-titulo">Relatórios</h1>
      <p className="adm-sub">Engajamento real da turma, curso a curso.</p>

      {cursoAberto ? (
        <RelatorioDoCurso courseId={cursoAberto} onVoltar={() => setCursoAberto(null)} />
      ) : (
        <QueryBoundary query={query} empty="Nenhum curso para reportar ainda.">
          {(cursos) => (
            <div className="adm-tabela-wrap">
              <table className="adm-tabela">
                <thead>
                  <tr>
                    <th>Curso</th>
                    <th>Status</th>
                    <th>Alunos</th>
                    <th>Iniciaram</th>
                    <th>Aulas</th>
                    <th>Conclusão média</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {cursos.map((c) => (
                    <tr key={c.id}>
                      <td>{nomeAmigavel(c.title)}</td>
                      <td>
                        <StatusPill valor={c.published ? 'ACTIVE' : 'PENDING'} />
                      </td>
                      <td>{c.students}</td>
                      <td>{c.started}</td>
                      <td>{c.totalLessons}</td>
                      <td>
                        <Barra pct={c.avgCompletionPct} />
                      </td>
                      <td>
                        <Button
                          ghost
                          style={{ padding: '2px 10px', fontSize: '0.78rem' }}
                          onClick={() => setCursoAberto(c.id)}
                        >
                          Por aula
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </QueryBoundary>
      )}
    </section>
  )
}
