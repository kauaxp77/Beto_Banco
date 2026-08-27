import { useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api, ApiError } from '../api/http'
import { Button, Input } from '../ui/basics'
import { useToast } from '../ui/Toast'

/** Atende primeiro acesso E recuperacao: o purpose vive no token (spec 8.4). */
export function DefinePasswordPage() {
  const { token } = useParams<{ token: string }>()
  const navigate = useNavigate()
  const { toast } = useToast()
  const [senha, setSenha] = useState('')
  const [confirmar, setConfirmar] = useState('')
  const [erro, setErro] = useState<string | undefined>()
  const [enviando, setEnviando] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setErro(undefined)
    if (senha !== confirmar) {
      setErro('As senhas não conferem')
      return
    }
    setEnviando(true)
    try {
      await api('/auth/reset-password', {
        method: 'POST',
        body: JSON.stringify({ token, password: senha }),
      })
      toast('Senha definida! Agora é só entrar.')
      navigate('/login', { replace: true })
    } catch (err) {
      setErro(err instanceof ApiError ? err.message : 'Erro inesperado. Tente de novo.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <>
      <h1>Definir senha</h1>
      <p className="auth-sub">Escolha uma senha forte para acessar a plataforma.</p>
      <form onSubmit={onSubmit} noValidate>
        <Input
          label="Nova senha"
          type="password"
          autoComplete="new-password"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          required
        />
        <Input
          label="Confirmar nova senha"
          type="password"
          autoComplete="new-password"
          value={confirmar}
          onChange={(e) => setConfirmar(e.target.value)}
          error={erro}
          required
        />
        <Button type="submit" disabled={enviando}>
          {enviando ? 'Salvando…' : 'Definir senha'}
        </Button>
      </form>
    </>
  )
}
