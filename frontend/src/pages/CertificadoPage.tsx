import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api/http'
import { Button } from '../ui/basics'
import { nomeAmigavel } from '../ui/format'
import { QueryBoundary } from '../ui/QueryBoundary'
import { Marca } from '../ui/Marca'
import './certificado.css'

interface CertificateData {
  code: string
  studentName: string
  courseId: string
  courseTitle: string | null
  hours: number
  issuedAt: string
}

/**
 * Pagina PUBLICA: o dono imprime/salva em PDF e o link serve de validacao —
 * quem receber o codigo ve exatamente estes dados, direto do banco.
 */
export function CertificadoPage() {
  const { code } = useParams<{ code: string }>()

  const query = useQuery({
    queryKey: ['certificado', code],
    queryFn: () => api<CertificateData>(`/certificates/${code}`),
    enabled: !!code,
  })

  return (
    <div className="cert-moldura">
      <QueryBoundary query={query}>
        {(c) => (
          <>
            <article className="cert-folha" aria-label="Certificado de conclusão">
              <header className="cert-marca">
                <Marca variante="completa" tamanho={54} />
              </header>
              <p className="cert-rotulo">Certificado de conclusão</p>
              <h1 className="cert-nome">{c.studentName}</h1>
              <p className="cert-texto">
                concluiu com êxito o curso
                <strong> {c.courseTitle ? nomeAmigavel(c.courseTitle) : 'Curso'} </strong>
                com carga horária de <strong>{c.hours} hora{c.hours === 1 ? '' : 's'}</strong>,
                em {new Date(c.issuedAt).toLocaleDateString('pt-BR', { dateStyle: 'long' })}.
              </p>
              <footer className="cert-rodape">
                <span className="cert-assinatura">
                  Prof. Beto Fernandes
                  <small>Beto Banco · Preparação para Concursos Bancários</small>
                </span>
                <span className="cert-codigo">
                  Código de verificação
                  <strong>{c.code}</strong>
                  <small>Valide em {window.location.host}/certificado/{c.code}</small>
                </span>
              </footer>
            </article>

            <div className="cert-acoes">
              <Button onClick={() => window.print()}>Imprimir / Salvar PDF</Button>
              <Link to="/dashboard">← Voltar aos meus cursos</Link>
            </div>
          </>
        )}
      </QueryBoundary>
    </div>
  )
}
