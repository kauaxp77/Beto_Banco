import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, type FormEvent } from 'react'
import { api, ApiError } from '../api/http'
import { Button, Card, Input } from '../ui/basics'
import { QueryBoundary } from '../ui/QueryBoundary'
import { useToast } from '../ui/Toast'

interface StudentResponse {
  id: string
  email: string
  fullName: string
  phone: string | null
}

export function ProfilePage() {
  const query = useQuery({
    queryKey: ['perfil'],
    queryFn: () => api<StudentResponse>('/students/me'),
  })

  return (
    <section>
      <h1>Meu perfil</h1>
      <QueryBoundary query={query}>{(perfil) => <ProfileForm perfil={perfil} />}</QueryBoundary>
    </section>
  )
}

function ProfileForm({ perfil }: { perfil: StudentResponse }) {
  const { toast, toastErro } = useToast()
  const queryClient = useQueryClient()
  const [nome, setNome] = useState(perfil.fullName)
  const [telefone, setTelefone] = useState(perfil.phone ?? '')
  // Erros de validacao por campo, na chave que a API usa no fieldError.
  const [erros, setErros] = useState<Record<string, string>>({})

  useEffect(() => {
    setNome(perfil.fullName)
    setTelefone(perfil.phone ?? '')
  }, [perfil])

  const salvar = useMutation({
    mutationFn: () =>
      api<StudentResponse>('/students/me', {
        method: 'PUT',
        body: JSON.stringify({ fullName: nome, phone: telefone || null }),
      }),
    onSuccess: (atualizado) => {
      queryClient.setQueryData(['perfil'], atualizado)
      toast('Perfil atualizado!')
    },
    onError: (err) => {
      if (err instanceof ApiError && err.fieldErrors?.length) {
        setErros(Object.fromEntries(err.fieldErrors.map((f) => [f.field, f.message])))
      } else if (err instanceof ApiError) {
        toastErro(err.message)
      } else {
        toastErro('Erro inesperado ao salvar.')
      }
    },
  })

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    setErros({})
    salvar.mutate()
  }

  return (
    <Card>
      <form onSubmit={onSubmit} noValidate>
        <Input label="E-mail" type="email" value={perfil.email} readOnly />
        <Input
          label="Nome"
          value={nome}
          onChange={(e) => setNome(e.target.value)}
          error={erros.fullName}
          required
        />
        <Input
          label="Telefone"
          type="tel"
          autoComplete="tel"
          value={telefone}
          onChange={(e) => setTelefone(e.target.value)}
          error={erros.phone}
        />
        <Button type="submit" disabled={salvar.isPending}>
          {salvar.isPending ? 'Salvando…' : 'Salvar'}
        </Button>
      </form>
    </Card>
  )
}
