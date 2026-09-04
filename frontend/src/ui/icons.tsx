/**
 * Ícones em SVG inline.
 *
 * Sem biblioteca de ícones de propósito: são poucos, e uma dependência inteira
 * para oito traços custa mais no bundle do que o desenho custa aqui. Todos
 * herdam `currentColor`, então mudam de cor com o texto ao redor.
 *
 * `aria-hidden` em todos: um ícone ao lado de um rótulo que já diz a mesma
 * coisa vira ruído no leitor de tela.
 */
import type { SVGProps } from 'react'

type Props = SVGProps<SVGSVGElement> & { size?: number }

function Base({ size = 18, children, ...props }: Props) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      {...props}
    >
      {children}
    </svg>
  )
}

export function IconeEnvelope(props: Props) {
  return (
    <Base {...props}>
      <rect x="2.5" y="4.5" width="19" height="15" rx="2.5" />
      <path d="m3 6.5 8.2 6a1.4 1.4 0 0 0 1.6 0l8.2-6" />
    </Base>
  )
}

export function IconeUsuario(props: Props) {
  return (
    <Base {...props}>
      <circle cx="12" cy="8" r="3.75" />
      <path d="M4.5 20a7.5 7.5 0 0 1 15 0" />
    </Base>
  )
}

export function IconeTelefone(props: Props) {
  return (
    <Base {...props}>
      <path d="M7.5 3.5h-3a1.5 1.5 0 0 0-1.5 1.6C3.5 13 11 20.5 18.9 21a1.5 1.5 0 0 0 1.6-1.5v-3a1.5 1.5 0 0 0-1.2-1.5l-2.6-.5a1.5 1.5 0 0 0-1.5.6l-.8 1.1a12 12 0 0 1-5.1-5.1l1.1-.8a1.5 1.5 0 0 0 .6-1.5L10.5 6a1.5 1.5 0 0 0-1.5-1.2z" />
    </Base>
  )
}

export function IconeCheck(props: Props) {
  return (
    <Base strokeWidth="2.4" {...props}>
      <path d="m4.5 12.5 5 5 10-11" />
    </Base>
  )
}

export function IconeSalvar(props: Props) {
  return (
    <Base {...props}>
      <path d="M4.5 3.5h11l5 5v12a1 1 0 0 1-1 1h-15a1 1 0 0 1-1-1v-16a1 1 0 0 1 1-1z" />
      <path d="M8 3.5v6h7v-6" />
      <rect x="7" y="13.5" width="10" height="7" rx="1" />
    </Base>
  )
}

export function IconeSino(props: Props) {
  return (
    <Base {...props}>
      <path d="M18 9a6 6 0 1 0-12 0c0 5-2 6.5-2 6.5h16S18 14 18 9z" />
      <path d="M13.8 19.5a2 2 0 0 1-3.6 0" />
    </Base>
  )
}

export function IconeSair(props: Props) {
  return (
    <Base {...props}>
      <path d="M14.5 4.5h3a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2h-3" />
      <path d="M10 16.5 14.5 12 10 7.5" />
      <path d="M14.5 12h-10" />
    </Base>
  )
}

export function IconeCamera(props: Props) {
  return (
    <Base size={15} {...props}>
      <path d="M3.5 8.5h3l1.5-2.5h8l1.5 2.5h3a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1h-18a1 1 0 0 1-1-1v-9a1 1 0 0 1 1-1z" />
      <circle cx="12" cy="13.5" r="3.5" />
    </Base>
  )
}

export function IconeLupa(props: Props) {
  return (
    <Base {...props}>
      <circle cx="10.5" cy="10.5" r="6.5" />
      <path d="m15.5 15.5 4.5 4.5" />
    </Base>
  )
}

export function IconeAlerta(props: Props) {
  return (
    <Base {...props}>
      <path d="M12 3.5 22 20.5H2z" />
      <path d="M12 10v4.5" />
      <path d="M12 17.6v.1" />
    </Base>
  )
}

export function IconeLink(props: Props) {
  return (
    <Base size={15} {...props}>
      <path d="M9.5 14.5a4.5 4.5 0 0 0 6.4 0l3.1-3.1a4.5 4.5 0 0 0-6.4-6.4L11 6.6" />
      <path d="M14.5 9.5a4.5 4.5 0 0 0-6.4 0L5 12.6a4.5 4.5 0 0 0 6.4 6.4l1.6-1.6" />
    </Base>
  )
}
