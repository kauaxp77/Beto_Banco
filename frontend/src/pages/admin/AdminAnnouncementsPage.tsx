import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { api } from '../../api/http'
import { Button, Input } from '../../ui/basics'
import { nomeAmigavel } from '../../ui/format'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { useToast } from '../../ui/Toast'
import type { CourseAdminRow } from './AdminCoursesPage'
import { dataCurta } from './AdminLayout'

interface AnnouncementRow {
  id: string
  courseId: string | null
  courseTitle: string | null
  title: string
  body: string
  createdAt: string
}

export function AdminAnnouncementsPage() {
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()
  const [cursoId, setCursoId] = useState('')
  const [titulo, setTitulo] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [enviarEmail, setEnviarEmail] = useState(false)

  const query = useQuery({
    queryKey: ['admin-anuncios'],
    queryFn: () => api<AnnouncementRow[]>('/admin/courses/announcements'),
  })
  const cursos = useQuery({
    queryKey: ['admin-cursos'],
    queryFn: () => api<CourseAdminRow[]>('/admin/courses'),
  })

  const criar = useMutation({
    mutationFn: () =>
      api('/admin/courses/announcements', {
        method: 'POST',
        body: JSON.stringify({
          courseId: cursoId || null,
          title: titulo.trim(),
          body: mensagem.trim(),
          sendEmail: enviarEmail && cursoId !== '',
        }),
      }),
    onSuccess: () => {
      toast(
        enviarEmail && cursoId !== ''
          ? 'Aviso publicado e e-mails enfileirados para a turma.'
          : 'Aviso publicado na área do aluno.',
      )
      setTitulo('')
      setMensagem('')
      setEnviarEmail(false)
      void queryClient.invalidateQueries({ queryKey: ['admin-anuncios'] })
    },
    onError: () => toastErro('Não foi possível publicar. Tente de novo.'),
  })

  const remover = useMutation({
    mutationFn: (id: string) =>
      api(`/admin/courses/announcements/${id}`, { method: 'DELETE' }),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['admin-anuncios'] }),
    onError: () => toastErro('Não foi possível remover. Tente de novo.'),
  })

  function submeter(e: FormEvent) {
    e.preventDefault()
    criar.mutate()
  }

  return (
    <section>
      <h1 className="adm-titulo">Anúncios</h1>
      <p className="adm-sub">
        Avisos que aparecem na área do aluno. Anúncio de curso pode também ir por e-mail para
        todos que compraram.
      </p>

      <form className="adm-form" onSubmit={submeter}>
        <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>Novo aviso</h2>
        <div className="bb-field">
          <label htmlFor="anuncio-curso">Para quem</label>
          <select
            id="anuncio-curso"
            value={cursoId}
            onChange={(e) => {
              setCursoId(e.target.value)
              if (e.target.value === '') setEnviarEmail(false)
            }}
            style={{
              background: 'var(--bb-surface-2)',
              border: '1px solid var(--bb-border)',
              borderRadius: 'var(--bb-r1)',
              color: 'var(--bb-text)',
              padding: '9px 12px',
              width: '100%',
            }}
          >
            <option value="">Todos os alunos (geral)</option>
            {(cursos.data ?? []).map((c) => (
              <option key={c.id} value={c.id}>
                Turma: {nomeAmigavel(c.title)}
              </option>
            ))}
          </select>
        </div>
        <Input
          label="Título"
          value={titulo}
          onChange={(e) => setTitulo(e.target.value)}
          maxLength={200}
          required
        />
        <div className="bb-field">
          <label htmlFor="anuncio-corpo">Mensagem</label>
          <textarea
            id="anuncio-corpo"
            value={mensagem}
            onChange={(e) => setMensagem(e.target.value)}
            rows={4}
            maxLength={8000}
            required
            style={{
              width: '100%',
              background: 'var(--bb-surface-2)',
              border: '1px solid var(--bb-border)',
              borderRadius: 'var(--bb-r1)',
              color: 'var(--bb-text)',
              padding: '10px 12px',
              fontFamily: 'inherit',
            }}
          />
        </div>
        <label
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            fontSize: '0.88rem',
            color: cursoId === '' ? 'var(--bb-text-dim)' : 'var(--bb-text)',
            marginBottom: 'var(--bb-s3)',
          }}
        >
          <input
            type="checkbox"
            checked={enviarEmail}
            disabled={cursoId === ''}
            onChange={(e) => setEnviarEmail(e.target.checked)}
          />
          Enviar também por e-mail para a turma
          {cursoId === '' && ' (escolha uma turma para habilitar)'}
        </label>
        <Button type="submit" disabled={!titulo.trim() || !mensagem.trim() || criar.isPending}>
          Publicar aviso
        </Button>
      </form>

      <QueryBoundary query={query} empty="Nenhum aviso publicado ainda.">
        {(avisos) => (
          <div className="adm-tabela-wrap">
            <table className="adm-tabela">
              <thead>
                <tr>
                  <th>Quando</th>
                  <th>Para</th>
                  <th>Título</th>
                  <th>Mensagem</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {avisos.map((a) => (
                  <tr key={a.id}>
                    <td style={{ whiteSpace: 'nowrap' }}>{dataCurta(a.createdAt)}</td>
                    <td>{a.courseTitle ? nomeAmigavel(a.courseTitle) : 'Geral'}</td>
                    <td>{a.title}</td>
                    <td style={{ maxWidth: 320, color: 'var(--bb-text-dim)' }}>{a.body}</td>
                    <td>
                      <Button
                        ghost
                        style={{ padding: '2px 10px', fontSize: '0.78rem' }}
                        disabled={remover.isPending}
                        onClick={() => remover.mutate(a.id)}
                      >
                        Remover
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </QueryBoundary>
    </section>
  )
}
