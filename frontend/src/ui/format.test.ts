import { describe, expect, it } from 'vitest'
import { nomeAmigavel } from './format'

describe('nomeAmigavel', () => {
  it('troca underscores por espacos e capitaliza', () => {
    expect(nomeAmigavel('MENTORIA_PROTOCOLO_BB___COMBO_2_EM_1')).toBe(
      'Mentoria Protocolo BB Combo 2 em 1',
    )
  })

  it('preserva siglas conhecidas em caixa alta', () => {
    expect(nomeAmigavel('MENTORIA_PROTOCOLO_BB_TI_E_BNB')).toBe('Mentoria Protocolo BB TI e BNB')
  })

  it('mantem conectores em minusculo, exceto no inicio', () => {
    expect(nomeAmigavel('DE_OLHO_NA_PROVA')).toBe('De Olho na Prova')
  })

  it('deixa nomes ja legiveis apresentaveis', () => {
    expect(nomeAmigavel('Mentoria Completa')).toBe('Mentoria Completa')
  })

  it('reconhece sigla mesmo com pontuacao colada', () => {
    expect(nomeAmigavel('Combo 3 em 1 — BB, Caixa e BNB')).toBe(
      'Combo 3 em 1 — BB, Caixa e BNB',
    )
  })

  it('devolve vazio para nulo, indefinido ou so underscores', () => {
    expect(nomeAmigavel(null)).toBe('')
    expect(nomeAmigavel(undefined)).toBe('')
    expect(nomeAmigavel('___')).toBe('')
  })
})
