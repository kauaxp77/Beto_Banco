/**
 * Graficos SVG proprios, na mesma filosofia do resto do frontend: zero
 * dependencia, tokens do tema, e so o que o dashboard precisa. Cada grafico
 * recebe dados ja agregados — quem agrega e a tela, aqui so se desenha.
 */

export interface PontoSerie {
  rotulo: string
  valor: number
}

export interface FatiaDonut {
  rotulo: string
  valor: number
  cor: string
}

const OURO = 'var(--bb-gold)'
const TEXTO_FRACO = 'var(--bb-text-dim)'

/** Largura logica dos SVGs; escalam via viewBox + width 100%. */
const L = 560
const A = 220
const MARGEM = { topo: 12, base: 28, esq: 8, dir: 8 }

function escalaY(valores: number[]): (v: number) => number {
  const max = Math.max(...valores, 1)
  const alturaUtil = A - MARGEM.topo - MARGEM.base
  return (v) => A - MARGEM.base - (v / max) * alturaUtil
}

/** Mostra no maximo ~6 rotulos no eixo X para nao virar poeira. */
function passoRotulos(n: number): number {
  return Math.max(1, Math.ceil(n / 6))
}

export function GraficoArea({
  dados,
  formatar = (v: number) => String(v),
  descricao,
}: {
  dados: PontoSerie[]
  formatar?: (v: number) => string
  descricao: string
}) {
  if (dados.length === 0) return <VazioGrafico />

  const y = escalaY(dados.map((d) => d.valor))
  const larguraUtil = L - MARGEM.esq - MARGEM.dir
  const x = (i: number) =>
    MARGEM.esq + (dados.length === 1 ? larguraUtil / 2 : (i / (dados.length - 1)) * larguraUtil)

  const linha = dados.map((d, i) => `${x(i)},${y(d.valor)}`).join(' ')
  const area = `${MARGEM.esq},${A - MARGEM.base} ${linha} ${L - MARGEM.dir},${A - MARGEM.base}`
  const passo = passoRotulos(dados.length)

  return (
    <svg viewBox={`0 0 ${L} ${A}`} role="img" aria-label={descricao} className="bb-grafico">
      <defs>
        <linearGradient id="bb-area-ouro" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#ffc300" stopOpacity="0.35" />
          <stop offset="100%" stopColor="#ffc300" stopOpacity="0.02" />
        </linearGradient>
      </defs>
      <line
        x1={MARGEM.esq} y1={A - MARGEM.base} x2={L - MARGEM.dir} y2={A - MARGEM.base}
        stroke="var(--bb-border)" strokeWidth="1"
      />
      <polygon points={area} fill="url(#bb-area-ouro)" />
      <polyline points={linha} fill="none" stroke={OURO} strokeWidth="2.5" strokeLinejoin="round" />
      {dados.map((d, i) => (
        <g key={d.rotulo}>
          <circle cx={x(i)} cy={y(d.valor)} r="3.5" fill={OURO}>
            <title>{`${d.rotulo}: ${formatar(d.valor)}`}</title>
          </circle>
          {i % passo === 0 && (
            <text x={x(i)} y={A - 8} textAnchor="middle" fontSize="11" fill={TEXTO_FRACO}>
              {d.rotulo}
            </text>
          )}
        </g>
      ))}
    </svg>
  )
}

export function GraficoBarras({
  dados,
  formatar = (v: number) => String(v),
  descricao,
}: {
  dados: PontoSerie[]
  formatar?: (v: number) => string
  descricao: string
}) {
  if (dados.length === 0) return <VazioGrafico />

  const y = escalaY(dados.map((d) => d.valor))
  const larguraUtil = L - MARGEM.esq - MARGEM.dir
  const vao = larguraUtil / dados.length
  const larguraBarra = Math.min(36, vao * 0.6)
  const passo = passoRotulos(dados.length)

  return (
    <svg viewBox={`0 0 ${L} ${A}`} role="img" aria-label={descricao} className="bb-grafico">
      <line
        x1={MARGEM.esq} y1={A - MARGEM.base} x2={L - MARGEM.dir} y2={A - MARGEM.base}
        stroke="var(--bb-border)" strokeWidth="1"
      />
      {dados.map((d, i) => {
        const cx = MARGEM.esq + vao * i + vao / 2
        const topo = y(d.valor)
        return (
          <g key={d.rotulo}>
            <rect
              x={cx - larguraBarra / 2}
              y={topo}
              width={larguraBarra}
              height={Math.max(2, A - MARGEM.base - topo)}
              rx="4"
              fill={OURO}
              opacity="0.85"
            >
              <title>{`${d.rotulo}: ${formatar(d.valor)}`}</title>
            </rect>
            {i % passo === 0 && (
              <text x={cx} y={A - 8} textAnchor="middle" fontSize="11" fill={TEXTO_FRACO}>
                {d.rotulo}
              </text>
            )}
          </g>
        )
      })}
    </svg>
  )
}

export function GraficoDonut({
  dados,
  formatar = (v: number) => String(v),
  descricao,
}: {
  dados: FatiaDonut[]
  formatar?: (v: number) => string
  descricao: string
}) {
  const total = dados.reduce((acc, d) => acc + d.valor, 0)
  if (total === 0) return <VazioGrafico />

  const raio = 80
  const espessura = 26
  const centro = 110
  const circunferencia = 2 * Math.PI * raio
  const fatias = dados.map((d, i) => ({
    ...d,
    fracao: d.valor / total,
    offset: dados.slice(0, i).reduce((acc, ant) => acc + ant.valor / total, 0),
  }))

  return (
    <div className="bb-donut">
      <svg viewBox="0 0 220 220" role="img" aria-label={descricao} className="bb-grafico bb-grafico--donut">
        {fatias.map((d) => {
          const { fracao, offset } = d
          return (
            <circle
              key={d.rotulo}
              cx={centro}
              cy={centro}
              r={raio}
              fill="none"
              stroke={d.cor}
              strokeWidth={espessura}
              strokeDasharray={`${fracao * circunferencia} ${circunferencia}`}
              strokeDashoffset={-offset * circunferencia}
              transform={`rotate(-90 ${centro} ${centro})`}
            >
              <title>{`${d.rotulo}: ${formatar(d.valor)}`}</title>
            </circle>
          )
        })}
        <text x={centro} y={centro - 4} textAnchor="middle" fontSize="26" fill="var(--bb-text)" fontWeight="600">
          {formatar(total)}
        </text>
        <text x={centro} y={centro + 18} textAnchor="middle" fontSize="11" fill={TEXTO_FRACO}>
          total
        </text>
      </svg>
      <ul className="bb-donut-legenda">
        {dados.map((d) => (
          <li key={d.rotulo}>
            <span className="cor" style={{ background: d.cor }} aria-hidden="true" />
            {d.rotulo} — {formatar(d.valor)}
          </li>
        ))}
      </ul>
    </div>
  )
}

function VazioGrafico() {
  return <p className="bb-grafico-vazio">Sem dados suficientes ainda.</p>
}
