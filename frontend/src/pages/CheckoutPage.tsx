import { useMutation, useQuery } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api, ApiError } from '../api/http'
import { Button, Skeleton } from '../ui/basics'
import { nomeAmigavel } from '../ui/format'
import { moeda } from '../ui/format'
import { IconeCheck } from '../ui/icons'
import './checkout.css'

interface Produto {
  id: string
  sku: string
  name: string
  description: string | null
  priceCents: number
  currency: string
}

interface Pedido {
  orderId: string
  checkoutUrl: string
  amountCents: number
}

/**
 * Início da compra. Documento Mestre Premium V3.0, seção 8.
 *
 * Público: quem compra ainda não tem conta — ela nasce quando o pagamento é
 * aprovado, e o aluno recebe um link para definir a senha. Exigir login aqui
 * inverteria o funil.
 *
 * Nada de acesso acontece nesta tela. Ela abre o pedido e leva o comprador para
 * o ambiente da InfinitePay; o acesso vem do webhook, depois de o provedor
 * confirmar que o pedido foi pago.
 */
export function CheckoutPage() {
  const [params] = useSearchParams()
  const produtoNaUrl = params.get('produto')

  const produtos = useQuery({
    queryKey: ['produtos-publicos'],
    queryFn: () => api<Produto[]>('/products'),
  })

  const [escolhido, setEscolhido] = useState<string | null>(produtoNaUrl)

  if (produtos.isPending) return <Skeleton height={260} />
  if (produtos.isError) {
    return <p className="ck-erro">Não foi possível carregar os cursos à venda agora.</p>
  }

  const lista = produtos.data ?? []
  const produto = lista.find((p) => p.id === escolhido) ?? null

  return (
    <section className="ck">
      <header className="ck-cabecalho">
        <h1>Garanta seu acesso</h1>
        <p>
          O pagamento é processado pela InfinitePay. Assim que ele for confirmado, você recebe
          um e-mail para criar sua senha e o acesso é liberado na hora.
        </p>
      </header>

      {!produto ? (
        <ul className="ck-produtos">
          {lista.map((p) => (
            <li key={p.id}>
              <button type="button" className="ck-produto" onClick={() => setEscolhido(p.id)}>
                <span className="ck-produto-nome">{nomeAmigavel(p.name)}</span>
                {p.description && <span className="ck-produto-desc">{p.description}</span>}
                <span className="ck-produto-preco">{moeda(p.priceCents)}</span>
              </button>
            </li>
          ))}
        </ul>
      ) : (
        <Formulario produto={produto} onTrocar={() => setEscolhido(null)} />
      )}

      <p className="ck-legal">
        Ao continuar você concorda com os <Link to="/legal/terms-of-use">termos de uso</Link> e
        com a <Link to="/legal/privacy-policy">política de privacidade</Link>.
      </p>
    </section>
  )
}

function Formulario({ produto, onTrocar }: { produto: Produto; onTrocar: () => void }) {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [whatsapp, setWhatsapp] = useState('')
  const [erros, setErros] = useState<Record<string, string>>({})
  const [falha, setFalha] = useState<string | null>(null)

  const abrir = useMutation({
    mutationFn: () =>
      api<Pedido>('/checkout', {
        method: 'POST',
        body: JSON.stringify({ productId: produto.id, name, email, whatsapp: whatsapp || null }),
      }),
    onSuccess: (pedido) => {
      // Sai da plataforma: o cartão é digitado no ambiente da InfinitePay, e
      // dado de cartão nunca passa por aqui (seção 21).
      window.location.href = pedido.checkoutUrl
    },
    onError: (err) => {
      if (err instanceof ApiError && err.fieldErrors?.length) {
        setErros(Object.fromEntries(err.fieldErrors.map((f) => [f.field, f.message])))
      } else if (err instanceof ApiError) {
        setFalha(err.message)
      } else {
        setFalha('Não foi possível abrir o pagamento agora. Tente novamente em instantes.')
      }
    },
  })

  function enviar(e: FormEvent) {
    e.preventDefault()
    setErros({})
    setFalha(null)
    abrir.mutate()
  }

  return (
    <div className="ck-cartao">
      <div className="ck-resumo">
        <span className="ck-resumo-rotulo">Você está comprando</span>
        <h2>{nomeAmigavel(produto.name)}</h2>
        {produto.description && <p>{produto.description}</p>}
        <p className="ck-preco">{moeda(produto.priceCents)}</p>
        <button type="button" className="ck-trocar" onClick={onTrocar}>
          Escolher outro curso
        </button>

        <ul className="ck-garantias">
          <li><IconeCheck size={14} /> Acesso liberado assim que o pagamento é confirmado</li>
          <li><IconeCheck size={14} /> Pix, cartão e parcelamento pela InfinitePay</li>
          <li><IconeCheck size={14} /> Seus dados de cartão não passam pela plataforma</li>
        </ul>
      </div>

      <form className="ck-form" onSubmit={enviar} noValidate>
        <div className="ck-campo">
          <label htmlFor="ck-nome">Nome completo</label>
          <input
            id="ck-nome"
            value={name}
            onChange={(e) => setName(e.target.value)}
            autoComplete="name"
            required
          />
          <span className="ck-erro-campo">{erros.name}</span>
        </div>

        <div className="ck-campo">
          <label htmlFor="ck-email">E-mail</label>
          <input
            id="ck-email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
            required
          />
          {/* O acesso vai para este e-mail: se ele estiver errado, o aluno paga
              e não recebe nada. Dizer isso aqui custa menos que o suporte. */}
          <span className="ck-dica">É para este endereço que o acesso será enviado.</span>
          <span className="ck-erro-campo">{erros.email}</span>
        </div>

        <div className="ck-campo">
          <label htmlFor="ck-whatsapp">WhatsApp (opcional)</label>
          <input
            id="ck-whatsapp"
            type="tel"
            value={whatsapp}
            onChange={(e) => setWhatsapp(e.target.value)}
            placeholder="(61) 99999-0000"
            autoComplete="tel"
          />
          <span className="ck-erro-campo">{erros.whatsapp}</span>
        </div>

        {falha && <p className="ck-erro">{falha}</p>}

        <Button type="submit" className="ck-botao" disabled={abrir.isPending}>
          {abrir.isPending ? 'Abrindo pagamento…' : `Ir para o pagamento · ${moeda(produto.priceCents)}`}
        </Button>
      </form>
    </div>
  )
}
