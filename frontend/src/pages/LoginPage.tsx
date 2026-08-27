import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/http'
import { useSession } from '../auth/session'
import { Button, Card, Input } from '../ui/basics'

export function LoginPage() {
  const { login } = useSession()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<string | undefined>()
  const [enviando, setEnviando] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setErro(undefined)
    setEnviando(true)
    try {
      await login(email, senha)
      const destino = (location.state as { from?: string } | null)?.from ?? '/dashboard'
      navigate(destino, { replace: true })
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro inesperado. Tente de novo.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Card className="bb-auth-card">
      <h1>Entrar</h1>
      <form onSubmit={onSubmit} noValidate>
        <Input
          label="E-mail"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={erro}
          required
        />
        <Input
          label="Senha"
          type="password"
          autoComplete="current-password"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          required
        />
        <Button type="submit" disabled={enviando}>
          {enviando ? 'Entrando…' : 'Entrar'}
        </Button>
      </form>
      <p>
        <Link to="/esqueci-senha">Esqueci minha senha</Link>
      </p>
    </Card>
  )
}
