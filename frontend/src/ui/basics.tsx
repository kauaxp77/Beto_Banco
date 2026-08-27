import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode } from 'react'
import { useId } from 'react'
import './ui.css'

export function Button({
  ghost,
  className,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { ghost?: boolean }) {
  const classes = ['bb-button', ghost ? 'bb-button--ghost' : '', className ?? '']
  return <button type="button" {...props} className={classes.join(' ').trim()} />
}

export function Input({
  label,
  error,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string }) {
  const id = useId()
  const errorId = `${id}-erro`
  return (
    <div className="bb-field">
      <label htmlFor={id}>{label}</label>
      <input id={id} aria-invalid={!!error} aria-describedby={error ? errorId : undefined} {...props} />
      {/* aria-live: leitores de tela anunciam o erro quando ele aparece. */}
      <span id={errorId} className="bb-field-error" aria-live="polite">
        {error}
      </span>
    </div>
  )
}

export function Card({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={`bb-card ${className ?? ''}`.trim()}>{children}</div>
}

export function Badge({ children }: { children: ReactNode }) {
  return <span className="bb-badge">{children}</span>
}

export function Skeleton({ height = 96 }: { height?: number }) {
  return <div className="bb-skeleton" style={{ height }} aria-label="Carregando" role="status" />
}

export function EmptyState({ children }: { children: ReactNode }) {
  return <div className="bb-state">{children}</div>
}

export function ErrorState({ onRetry }: { onRetry?: () => void }) {
  return (
    <div className="bb-state" role="alert">
      <p>Algo deu errado ao carregar.</p>
      {onRetry && (
        <Button ghost onClick={onRetry}>
          Tentar de novo
        </Button>
      )}
    </div>
  )
}
