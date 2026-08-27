/**
 * Nomes de produto chegam de gateways externos como slug gritado
 * ("MENTORIA_PROTOCOLO_BB___COMBO_2_EM_1"). O banco guarda o original —
 * esta funcao existe so para a exibicao ficar legivel.
 */

/** Siglas que permanecem em caixa alta mesmo depois de humanizar. */
const SIGLAS = new Set([
  'BB', 'BNB', 'BRB', 'CEF', 'TI', 'BACEN', 'BNDES', 'BASA', 'BANRISUL',
  'D3', 'PDF', 'CPA', 'EAD',
])

/** Conectores que ficam em minusculo no meio do nome. */
const MINUSCULAS = new Set([
  'e', 'de', 'da', 'do', 'das', 'dos', 'em', 'com', 'para',
  'a', 'o', 'na', 'no', 'nas', 'nos',
])

export function nomeAmigavel(bruto: string | null | undefined): string {
  if (!bruto) return ''
  const texto = bruto.replace(/_+/g, ' ').replace(/\s+/g, ' ').trim()
  if (texto === '') return ''

  return texto
    .split(' ')
    .map((palavra, i) => {
      // Pontuacao colada ("BB," / "(BNB)") nao pode esconder a sigla.
      const nucleo = palavra.replace(/[^\p{L}\p{N}]/gu, '')
      if (SIGLAS.has(nucleo.toUpperCase())) return palavra.toUpperCase()
      const minuscula = palavra.toLowerCase()
      if (i > 0 && MINUSCULAS.has(nucleo.toLowerCase())) return minuscula
      return minuscula.charAt(0).toUpperCase() + minuscula.slice(1)
    })
    .join(' ')
}
