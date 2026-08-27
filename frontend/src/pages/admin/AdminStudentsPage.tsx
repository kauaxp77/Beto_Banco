import { useQuery } from '@tanstack/react-query'
import { useDeferredValue, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiPage } from '../../api/http'
import { Input } from '../../ui/basics'
import { Paginacao } from '../../ui/Paginacao'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { dataCurta, StatusPill } from './AdminLayout'

interface StudentRow {
  id: string
  email: string
  fullName: string
  status: string
  createdAt: string
}

export function AdminStudentsPage() {
  const [busca, setBusca] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const buscaAtiva = useDeferredValue(busca)

  const query = useQuery({
    queryKey: ['admin-alunos', buscaAtiva, status, page],
    queryFn: () =>
      apiPage<StudentRow>(
        `/admin/students?search=${encodeURIComponent(buscaAtiva)}&status=${status}&page=${page}`,
      ),
    placeholderData: (anterior) => anterior,
  })

  return (
    <section>
      <h1 className="adm-titulo">Alunos</h1>
      <p className="adm-sub">Busque por e-mail ou nome; clique para ver o detalhe.</p>

      <div className="adm-filtros">
        <Input
          label="Buscar aluno"
          value={busca}
          onChange={(e) => {
            setBusca(e.target.value)
            setPage(0)
          }}
          placeholder="e-mail ou nome"
        />
        <div className="bb-field">
          <label htmlFor="filtro-status">Status</label>
          <select
            id="filtro-status"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value)
              setPage(0)
            }}
          >
            <option value="">Todos</option>
            <option value="ACTIVE">Ativos</option>
            <option value="BLOCKED">Bloqueados</option>
          </select>
        </div>
      </div>

      <QueryBoundary query={query} empty="Nenhum aluno encontrado com esses filtros.">
        {(pagina) =>
          pagina.data.length === 0 ? (
            <p className="bb-state">Nenhum aluno encontrado com esses filtros.</p>
          ) : (
            <>
              <div className="adm-tabela-wrap">
                <table className="adm-tabela">
                  <thead>
                    <tr>
                      <th>E-mail</th>
                      <th>Nome</th>
                      <th>Status</th>
                      <th>Cadastro</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pagina.data.map((a) => (
                      <tr key={a.id}>
                        <td>
                          <Link to={`/admin/alunos/${a.id}`}>{a.email}</Link>
                        </td>
                        <td>{a.fullName}</td>
                        <td>
                          <StatusPill valor={a.status} />
                        </td>
                        <td>{dataCurta(a.createdAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Paginacao meta={pagina.pagination} onPage={setPage} />
            </>
          )
        }
      </QueryBoundary>
    </section>
  )
}
