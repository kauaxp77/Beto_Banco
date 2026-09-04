import type { CSSProperties } from 'react'
import './marca.css'

/**
 * A marca da plataforma, em um só lugar.
 *
 * Existe como componente, e não como arquivo de imagem único, por um motivo
 * prático: a assinatura "APROVAÇÃO · passo a passo" é texto, e um SVG externo
 * carregado por {@code <img>} não enxerga as fontes da página — cairia numa
 * fonte do sistema e a marca mudaria de cara. Aqui o símbolo é imagem e o
 * lettering é texto real, com a tipografia do produto.
 *
 * Trocar o símbolo por outro arquivo é substituir
 * {@code public/marca/simbolo.svg}; nenhum componente precisa mudar.
 */

export type VarianteDaMarca =
  /** Só o monograma. Barra estreita, avatar, favicon impresso. */
  | 'simbolo'
  /** Monograma + nome. O uso padrão dentro do produto. */
  | 'compacta'
  /** Monograma + nome + assinatura. Abertura, login, certificado. */
  | 'completa'

export function Marca({
  variante = 'compacta',
  tamanho = 34,
  className,
}: {
  variante?: VarianteDaMarca
  /** Altura do monograma em pixels. O texto acompanha em proporção. */
  tamanho?: number
  className?: string
}) {
  const classes = ['bb-marca', `bb-marca--${variante}`, className ?? ''].join(' ').trim()

  return (
    <span className={classes} style={{ '--bb-marca-h': `${tamanho}px` } as CSSProperties}>
      <img className="bb-marca-simbolo" src="/marca/simbolo.svg" alt="" aria-hidden="true" />

      {variante !== 'simbolo' && (
        <span className="bb-marca-texto">
          <span className="bb-marca-nome">
            APRO<Confere />AÇÃO
          </span>
          {variante === 'completa' && <span className="bb-marca-assinatura">passo a passo</span>}
        </span>
      )}

      {/* O nome acessível fica aqui, uma vez só: o símbolo é decorativo e o
          lettering acima está quebrado em pedaços que um leitor de tela
          soletraria como "APRO... AÇÃO". */}
      <span className="bb-marca-leitor">Aprovação passo a passo</span>
    </span>
  )
}

/**
 * O "V" de APROVAÇÃO é um visto de correção — a ideia inteira da marca.
 * Braço curto em dourado, braço longo na cor do texto: o mesmo gesto de quem
 * corrige uma prova.
 */
function Confere() {
  return (
    // Traçado, e não polígono preenchido: a espessura acompanha o tamanho da
    // fonte sem redesenhar o caminho, e as pontas arredondadas casam com o
    // peso da tipografia ao lado.
    <svg
      className="bb-marca-visto"
      viewBox="0 0 46 44"
      fill="none"
      strokeWidth="9.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d="M5 24 L19 38" stroke="var(--bb-gold, #ffc300)" />
      <path d="M19 38 L41 5" stroke="currentColor" />
    </svg>
  )
}
