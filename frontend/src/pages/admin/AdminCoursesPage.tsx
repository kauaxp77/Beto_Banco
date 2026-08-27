import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../../api/http'
import { Button, Input } from '../../ui/basics'
import { nomeAmigavel } from '../../ui/format'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { useToast } from '../../ui/Toast'
import { StatusPill } from './AdminLayout'

export interface CourseAdminRow {
  id: string
  title: string
  slug: string
  description: string | null
  coverUrl: string | null
  published: boolean
  productIds: string[]
  moduleCount: number
  lessonCount: number
}

export function AdminCoursesPage() {
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()
  const [criando, setCriando] = useState(false)
  const [titulo, setTitulo] = useState('')
  const [descricao, setDescricao] = useState('')
  const [capa, setCapa] = useState('')
  const [erro, setErro] = useState<string | undefined>()

  const query = useQuery({
    queryKey: ['admin-cursos'],
    queryFn: () => api<CourseAdminRow[]>('/admin/courses'),
  })

  const criar = useMutation({
    mutationFn: () =>
      api<CourseAdminRow>('/admin/courses', {
        method: 'POST',
        body: JSON.stringify({
          title: titulo.trim(),
          description: descricao.trim() || null,
          coverUrl: capa.trim() || null,
        }),
      }),
    onSuccess: () => {
      toast('Curso criado. Agora adicione módulos e aulas.')
      setCriando(false)
      setTitulo('')
      setDescricao('')
      setCapa('')
      setErro(undefined)
      void queryClient.invalidateQueries({ queryKey: ['admin-cursos'] })
    },
    onError: (err) => {
      if (err instanceof ApiError) setErro(err.message)
      else toastErro('Erro inesperado ao criar o curso.')
    },
  })

  function submeter(e: FormEvent) {
    e.preventDefault()
    criar.mutate()
  }

  return (
    <section>
      <h1 className="adm-titulo">Cursos</h1>
      <p className="adm-sub">
        O conteúdo da área de membros. Um curso só aparece para o aluno quando está publicado
        e vinculado ao produto que ele comprou.
      </p>

      {criando ? (
        <form className="adm-form" onSubmit={submeter}>
          <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>Novo curso</h2>
          <Input
            label="Título"
            value={titulo}
            onChange={(e) => setTitulo(e.target.value)}
            error={erro}
            required
          />
          <Input
            label="Descrição"
            value={descricao}
            onChange={(e) => setDescricao(e.target.value)}
          />
          <Input
            label="URL da capa (opcional)"
            value={capa}
            onChange={(e) => setCapa(e.target.value)}
            placeholder="https://…"
          />
          <div className="adm-acoes">
            <Button type="submit" disabled={criar.isPending}>
              Criar curso
            </Button>
            <Button ghost onClick={() => setCriando(false)}>
              Cancelar
            </Button>
          </div>
        </form>
      ) : (
        <p>
          <Button onClick={() => setCriando(true)}>Novo curso</Button>
        </p>
      )}

      <QueryBoundary query={query} empty="Nenhum curso ainda. Crie o primeiro.">
        {(cursos) => (
          <div className="adm-tabela-wrap">
            <table className="adm-tabela">
              <thead>
                <tr>
                  <th>Curso</th>
                  <th>Status</th>
                  <th>Módulos</th>
                  <th>Aulas</th>
                  <th>Produtos</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {cursos.map((c) => (
                  <tr key={c.id}>
                    <td>{nomeAmigavel(c.title)}</td>
                    <td>
                      <StatusPill valor={c.published ? 'ACTIVE' : 'PENDING'} />
                    </td>
                    <td>{c.moduleCount}</td>
                    <td>{c.lessonCount}</td>
                    <td>{c.productIds.length}</td>
                    <td>
                      <Link to={`/admin/cursos/${c.id}`}>Editar conteúdo</Link>
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
