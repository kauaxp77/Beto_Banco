import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/http'
import { Button, Input } from '../ui/basics'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [enviado, setEnviado] = useState(false)
  const [enviando, setEnviando] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setEnviando(true)
    try {
      await api('/auth/forgot-password', {
        method: 'POST',
        body: JSON.stringify({ email }),
      })
    } finally {
      // A confirmacao e IDENTICA exista o e-mail ou nao: qualquer diferenca
      // transformaria a tela num enumerador de clientes (spec 6.6).
      setEnviado(true)
      setEnviando(false)
    }
  }

  if (enviado) {
    return (
      <>
        <h1>Verifique sua caixa de entrada</h1>
        <p className="auth-sub">
          Se o e-mail existir em nossa base, você receberá o link de redefinição.
        </p>
        <p className="auth-links">
          <Link to="/login">Voltar ao login</Link>
        </p>
      </>
    )
  }

  return (
    <>
      <h1>Esqueci minha senha</h1>
      <p className="auth-sub">Informe seu e-mail e enviaremos o link de redefinição.</p>
      <form onSubmit={onSubmit} noValidate>
        <Input
          label="E-mail"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <Button type="submit" disabled={enviando}>
          {enviando ? 'Enviando…' : 'Enviar link'}
        </Button>
      </form>
      <p className="auth-links">
        <Link to="/login">Voltar ao login</Link>
      </p>
    </>
  )
}
