/**
 * Converte a URL colada pelo professor no formato de embed do player.
 * Aceita YouTube (watch, youtu.be, shorts, embed) e Vimeo; MP4 direto e
 * tratado pela pagina com <video>, e o resto devolve null (sem player).
 */

export type Player =
  | { tipo: 'iframe'; src: string }
  | { tipo: 'video'; src: string }
  | null

export function playerDe(url: string | null | undefined): Player {
  if (!url) return null
  let u: URL
  try {
    u = new URL(url)
  } catch {
    return null
  }

  const host = u.hostname.replace(/^www\./, '')

  if (host === 'youtube.com' || host === 'm.youtube.com' || host === 'youtube-nocookie.com') {
    const id =
      u.searchParams.get('v') ??
      /^\/(?:embed|shorts|live)\/([\w-]{6,})/.exec(u.pathname)?.[1]
    return id ? { tipo: 'iframe', src: `https://www.youtube-nocookie.com/embed/${id}` } : null
  }
  if (host === 'youtu.be') {
    const id = u.pathname.slice(1).split('/')[0]
    return id ? { tipo: 'iframe', src: `https://www.youtube-nocookie.com/embed/${id}` } : null
  }
  if (host === 'vimeo.com' || host === 'player.vimeo.com') {
    const id = /(\d{6,})/.exec(u.pathname)?.[1]
    return id ? { tipo: 'iframe', src: `https://player.vimeo.com/video/${id}` } : null
  }
  if (/\.(mp4|webm|ogg)(\?|$)/i.test(u.pathname)) {
    return { tipo: 'video', src: url }
  }
  return null
}
