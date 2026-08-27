import { describe, expect, it } from 'vitest'
import { playerDe } from './video'

describe('playerDe', () => {
  it('converte youtube watch para embed nocookie', () => {
    expect(playerDe('https://www.youtube.com/watch?v=dQw4w9WgXcQ')).toEqual({
      tipo: 'iframe',
      src: 'https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ',
    })
  })

  it('converte youtu.be e shorts', () => {
    expect(playerDe('https://youtu.be/dQw4w9WgXcQ')?.src).toContain('/embed/dQw4w9WgXcQ')
    expect(playerDe('https://www.youtube.com/shorts/dQw4w9WgXcQ')?.src).toContain(
      '/embed/dQw4w9WgXcQ',
    )
  })

  it('converte vimeo para player', () => {
    expect(playerDe('https://vimeo.com/123456789')).toEqual({
      tipo: 'iframe',
      src: 'https://player.vimeo.com/video/123456789',
    })
  })

  it('mp4 direto vira video nativo', () => {
    expect(playerDe('https://cdn.exemplo.com/aula.mp4')).toEqual({
      tipo: 'video',
      src: 'https://cdn.exemplo.com/aula.mp4',
    })
  })

  it('url invalida ou vazia devolve null', () => {
    expect(playerDe(null)).toBeNull()
    expect(playerDe('')).toBeNull()
    expect(playerDe('nao-e-url')).toBeNull()
    expect(playerDe('https://exemplo.com/pagina')).toBeNull()
  })
})
