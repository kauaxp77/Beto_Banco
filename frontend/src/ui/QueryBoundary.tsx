import type { UseQueryResult } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { EmptyState, ErrorState, Skeleton } from './basics'

/**
 * Deriva loading, erro, vazio e sucesso do estado da query (spec 9.7):
 * "esqueci o empty state" deixa de ser possivel, porque nenhuma tela
 * implementa esses estados por conta propria.
 */
export function QueryBoundary<T>({
  query,
  empty,
  children,
}: {
  query: UseQueryResult<T>
  empty?: ReactNode
  children: (data: T) => ReactNode
}) {
  if (query.isPending) return <Skeleton />
  if (query.isError) return <ErrorState onRetry={() => void query.refetch()} />

  const data = query.data as T
  if (Array.isArray(data) && data.length === 0) {
    return <EmptyState>{empty ?? 'Nada por aqui ainda.'}</EmptyState>
  }
  return <>{children(data)}</>
}
