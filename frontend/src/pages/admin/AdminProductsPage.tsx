import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { api, ApiError } from '../../api/http'
import { Button, Input } from '../../ui/basics'
import { nomeAmigavel } from '../../ui/format'
import { QueryBoundary } from '../../ui/QueryBoundary'
import { useToast } from '../../ui/Toast'
import { brl, StatusPill } from './AdminLayout'

interface ProductRow {
  id: string
  sku: string
  name: string
  description: string | null
  priceCents: number
  currency: string
  active: boolean
}

interface FormState {
  id: string | null
  sku: string
  name: string
  description: string
  preco: string
  active: boolean
}

const FORM_VAZIO: FormState = { id: null, sku: '', name: '', description: '', preco: '', active: true }

/** "497,00" | "1.234,56" | "497" -> cents. NaN vira -1 para falhar na API. */
function paraCents(preco: string): number {
  const normalizado = preco.trim().replace(/\./g, '').replace(',', '.')
  const valor = Number.parseFloat(normalizado)
  return Number.isFinite(valor) ? Math.round(valor * 100) : -1
}

export function AdminProductsPage() {
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()
  const [form, setForm] = useState<FormState | null>(null)
  const [erro, setErro] = useState<string | undefined>()

  const query = useQuery({
    queryKey: ['admin-produtos'],
    queryFn: () => api<ProductRow[]>('/admin/products'),
  })

  const salvar = useMutation({
    mutationFn: (f: FormState) => {
      const corpo = {
        name: f.name.trim(),
        description: f.description.trim() || null,
        priceCents: paraCents(f.preco),
      }
      return f.id
        ? api(`/admin/products/${f.id}`, {
            method: 'PUT',
            body: JSON.stringify({ ...corpo, active: f.active }),
          })
        : api('/admin/products', {
            method: 'POST',
            body: JSON.stringify({ ...corpo, sku: f.sku.trim() }),
          })
    },
    onSuccess: () => {
      toast('Produto salvo.')
      setForm(null)
      setErro(undefined)
      void queryClient.invalidateQueries({ queryKey: ['admin-produtos'] })
    },
    onError: (err) => {
      if (err instanceof ApiError) setErro(err.fieldErrors?.[0]?.message ?? err.message)
      else toastErro('Erro inesperado ao salvar.')
    },
  })

  function submeter(e: FormEvent) {
    e.preventDefault()
    if (form) salvar.mutate(form)
  }

  return (
    <section>
      <h1 className="adm-titulo">Produtos</h1>
      <p className="adm-sub">
        O catálogo à venda. Não existe excluir: desativar tira da vitrine sem apagar o
        histórico de pagamentos.
      </p>

      {form === null ? (
        <p>
          <Button onClick={() => setForm(FORM_VAZIO)}>Novo produto</Button>
        </p>
      ) : (
        <form className="adm-form" onSubmit={submeter}>
          <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>
            {form.id ? `Editar ${form.sku}` : 'Novo produto'}
          </h2>
          {form.id === null && (
            <Input
              label="SKU"
              value={form.sku}
              onChange={(e) => setForm({ ...form, sku: e.target.value })}
              required
            />
          )}
          <Input
            label="Nome"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            error={erro}
            required
          />
          <Input
            label="Descrição"
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
          <Input
            label="Preço (R$)"
            inputMode="decimal"
            placeholder="497,00"
            value={form.preco}
            onChange={(e) => setForm({ ...form, preco: e.target.value })}
            required
          />
          {form.id !== null && (
            <div className="bb-field">
              <label htmlFor="produto-ativo">À venda</label>
              <select
                id="produto-ativo"
                value={form.active ? 'sim' : 'nao'}
                onChange={(e) => setForm({ ...form, active: e.target.value === 'sim' })}
              >
                <option value="sim">Sim — visível na vitrine</option>
                <option value="nao">Não — fora da vitrine</option>
              </select>
            </div>
          )}
          <div className="adm-acoes">
            <Button type="submit" disabled={salvar.isPending}>
              Salvar
            </Button>
            <Button ghost onClick={() => setForm(null)}>
              Cancelar
            </Button>
          </div>
        </form>
      )}

      <QueryBoundary query={query} empty="Nenhum produto cadastrado ainda.">
        {(produtos) => (
          <div className="adm-tabela-wrap">
            <table className="adm-tabela">
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Nome</th>
                  <th>Preço</th>
                  <th>Vitrine</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {produtos.map((p) => (
                  <tr key={p.id}>
                    <td>{p.sku}</td>
                    <td>{nomeAmigavel(p.name)}</td>
                    <td>{brl.format(p.priceCents / 100)}</td>
                    <td>
                      <StatusPill valor={p.active ? 'ACTIVE' : 'INACTIVE'} />
                    </td>
                    <td>
                      <Button
                        ghost
                        style={{ padding: '4px 12px', fontSize: '0.8rem' }}
                        onClick={() =>
                          setForm({
                            id: p.id,
                            sku: p.sku,
                            name: p.name,
                            description: p.description ?? '',
                            preco: (p.priceCents / 100).toFixed(2).replace('.', ','),
                            active: p.active,
                          })
                        }
                      >
                        Editar
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
