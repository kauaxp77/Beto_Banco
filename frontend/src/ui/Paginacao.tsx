import type { PaginationMeta } from '../api/http'
import { Button } from './basics'

export function Paginacao({
  meta,
  onPage,
}: {
  meta: PaginationMeta | undefined
  onPage: (page: number) => void
}) {
  if (!meta || meta.totalPages <= 1) return null
  return (
    <div className="adm-paginacao">
      <Button ghost disabled={meta.page === 0} onClick={() => onPage(meta.page - 1)}>
        ‹ Anterior
      </Button>
      <span>
        Página {meta.page + 1} de {meta.totalPages} · {meta.totalElements} registros
      </span>
      <Button ghost disabled={meta.page + 1 >= meta.totalPages} onClick={() => onPage(meta.page + 1)}>
        Próxima ›
      </Button>
    </div>
  )
}
