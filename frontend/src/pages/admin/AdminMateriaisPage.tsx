import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { api, ApiError } from '../../api/http'
import { Button, Input } from '../../ui/basics'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { useToast } from '../../ui/Toast'

interface Material {
  id: string
  slug: string
  title: string
  kind: string
  fileUrl: string
  active: boolean
}

const TIPOS: Record<string, string> = {
  PDF: 'PDF',
  MAPA_MENTAL: 'Mapa mental',
  CRONOGRAMA: 'Cronograma',
  QUESTOES: 'Questões',
}

/**
 * Materiais de captação. Documento Mestre Premium V3.0, seção 11.
 *
 * O material só aparece no formulário público quando está ativo — e é isso que
 * impede a entrega de um arquivo ainda não publicado, que queimaria o contato
 * no instante em que ele acabou de ser dado.
 */
export function AdminMateriaisPage() {
  const query = useQuery({
    queryKey: ['admin-materiais'],
    queryFn: () => api<Material[]>('/admin/lead-magnets'),
  })

  return (
    <section>
      <h1 className="adm-titulo">Materiais de captação</h1>
      <p className="adm-sub">
        Conteúdo que a pessoa recebe em troca do contato. Inativo não aparece no formulário
        público: o link precisa existir antes de o material ir ao ar.
      </p>

      <NovoMaterial />

      <QueryBoundary query={query} empty="Nenhum material cadastrado.">
        {(materiais) => (
          <div className="adm-tabela-wrap">
            <table className="adm-tabela">
              <thead>
                <tr>
                  <th>Material</th>
                  <th>Tipo</th>
                  <th>Arquivo</th>
                  <th>Situação</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {materiais.map((m) => (
                  <LinhaDeMaterial key={m.id} material={m} />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </QueryBoundary>
    </section>
  )
}

function NovoMaterial() {
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()
  const [aberto, setAberto] = useState(false)
  const [slug, setSlug] = useState('')
  const [title, setTitle] = useState('')
  const [kind, setKind] = useState('PDF')
  const [fileUrl, setFileUrl] = useState('')

  const criar = useMutation({
    mutationFn: () =>
      api('/admin/lead-magnets', {
        method: 'POST',
        body: JSON.stringify({ slug, title, kind, fileUrl }),
      }),
    onSuccess: () => {
      toast('Material cadastrado. Ative quando o arquivo estiver publicado.')
      setSlug('')
      setTitle('')
      setFileUrl('')
      setAberto(false)
      void queryClient.invalidateQueries({ queryKey: ['admin-materiais'] })
    },
    onError: (err) =>
      toastErro(err instanceof ApiError ? err.message : 'Não foi possível cadastrar.'),
  })

  function enviar(e: FormEvent) {
    e.preventDefault()
    criar.mutate()
  }

  if (!aberto) {
    return (
      <Button style={{ marginBottom: 'var(--bb-s5)' }} onClick={() => setAberto(true)}>
        Cadastrar material
      </Button>
    )
  }

  return (
    <form className="adm-form" onSubmit={enviar} style={{ marginBottom: 'var(--bb-s5)' }}>
      <Input
        label="Título"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        placeholder="Cronograma de 90 Dias"
        required
      />
      <Input
        label="Identificador na URL"
        value={slug}
        onChange={(e) => setSlug(e.target.value)}
        placeholder="cronograma-90-dias"
        pattern="[a-z0-9-]+"
        required
      />
      <div className="bb-field">
        <label htmlFor="novo-material-tipo">Tipo</label>
        <select
          id="novo-material-tipo"
          value={kind}
          onChange={(e) => setKind(e.target.value)}
        >
          {Object.entries(TIPOS).map(([v, r]) => (
            <option key={v} value={v}>{r}</option>
          ))}
        </select>
      </div>
      <Input
        label="Link do arquivo"
        value={fileUrl}
        onChange={(e) => setFileUrl(e.target.value)}
        placeholder="https://…"
        required
      />
      <div className="adm-acoes">
        <Button type="submit" disabled={criar.isPending}>
          {criar.isPending ? 'Salvando…' : 'Cadastrar'}
        </Button>
        <Button ghost type="button" onClick={() => setAberto(false)}>
          Cancelar
        </Button>
      </div>
    </form>
  )
}

function LinhaDeMaterial({ material }: { material: Material }) {
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()
  const [editando, setEditando] = useState(false)
  const [title, setTitle] = useState(material.title)
  const [fileUrl, setFileUrl] = useState(material.fileUrl)

  const salvar = useMutation({
    mutationFn: (ativo: boolean) =>
      api(`/admin/lead-magnets/${material.id}`, {
        method: 'PUT',
        body: JSON.stringify({ title, fileUrl, active: ativo }),
      }),
    onSuccess: () => {
      toast('Material atualizado.')
      setEditando(false)
      void queryClient.invalidateQueries({ queryKey: ['admin-materiais'] })
    },
    onError: (err) =>
      toastErro(err instanceof ApiError ? err.message : 'Não foi possível atualizar.'),
  })

  const acaoPequena = { padding: '3px 11px', fontSize: '0.78rem' }

  return (
    <>
      <tr>
        <td>
          <b>{material.title}</b>
          <br />
          <span style={{ color: 'var(--bb-text-dim)', fontSize: '0.8rem' }}>{material.slug}</span>
        </td>
        <td>{TIPOS[material.kind] ?? material.kind}</td>
        <td style={{ maxWidth: 260, overflowWrap: 'anywhere' }}>
          <a href={material.fileUrl} target="_blank" rel="noreferrer noopener">
            {material.fileUrl}
          </a>
        </td>
        <td>
          <span className={`adm-status ${material.active ? 'adm-status--ok' : 'adm-status--atencao'}`}>
            {material.active ? 'NO AR' : 'INATIVO'}
          </span>
        </td>
        <td style={{ whiteSpace: 'nowrap' }}>
          <Button ghost style={acaoPequena} onClick={() => setEditando(!editando)}>
            Editar
          </Button>{' '}
          <Button
            ghost
            style={acaoPequena}
            disabled={salvar.isPending}
            onClick={() => salvar.mutate(!material.active)}
          >
            {material.active ? 'Desativar' : 'Ativar'}
          </Button>
        </td>
      </tr>

      {editando && (
        <tr>
          <td colSpan={5}>
            <div style={{ display: 'grid', gap: 'var(--bb-s2)', maxWidth: 520 }}>
              <Input label="Título" value={title} onChange={(e) => setTitle(e.target.value)} />
              <Input
                label="Link do arquivo"
                value={fileUrl}
                onChange={(e) => setFileUrl(e.target.value)}
              />
              <div>
                <Button
                  style={acaoPequena}
                  disabled={salvar.isPending}
                  onClick={() => salvar.mutate(material.active)}
                >
                  Salvar
                </Button>
              </div>
            </div>
          </td>
        </tr>
      )}
    </>
  )
}
