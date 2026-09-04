import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { api } from '../api/http'
import { QueryBoundary } from '../ui/QueryBoundary'
import { dataBR } from '../ui/format'
import './privacidade.css'

interface Documento {
  type: string
  version: string
  body: string
  effectiveFrom: string
}

const TITULO: Record<string, string> = {
  TERMS_OF_USE: 'Termos de uso',
  PRIVACY_POLICY: 'Política de privacidade',
  COOKIE_POLICY: 'Política de cookies',
}

/**
 * Texto legal vigente. Documento Mestre V4.0, seção 22.
 *
 * Rota pública: o aceite acontece no checkout, antes de existir conta. Termo de
 * uso atrás de login não informa ninguém.
 */
export function LegalPage() {
  const { tipo } = useParams()

  const query = useQuery({
    queryKey: ['legal', tipo],
    queryFn: () => api<Documento>(`/legal/${tipo}`),
    enabled: Boolean(tipo),
  })

  return (
    <section className="lg">
      <QueryBoundary query={query}>
        {(doc) => (
          <>
            <h1>{TITULO[doc.type] ?? doc.type}</h1>
            <p className="lg-versao">
              Versão {doc.version} · em vigor desde {dataBR(doc.effectiveFrom)}
            </p>
            {/* O corpo vem como texto puro do banco e é renderizado como texto:
                interpretar HTML aqui abriria injeção a partir de um documento
                que o admin edita. `white-space: pre-wrap` preserva os
                parágrafos. */}
            <div className="lg-corpo">{doc.body}</div>
          </>
        )}
      </QueryBoundary>
    </section>
  )
}
