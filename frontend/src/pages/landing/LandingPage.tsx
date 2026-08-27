import { useQuery } from '@tanstack/react-query'
import { useEffect, useRef, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api/http'
import './landing.css'

/* ------------------------------------------------------------------ */
/* Conteudo real portado do site legado (spec 9.1): o que muda e a    */
/* apresentacao, nunca a promessa.                                    */
/* ------------------------------------------------------------------ */

const PILARES = [
  {
    titulo: 'Método Exclusivo',
    texto:
      'Estudo organizado e direcionado. Aprenda a estudar com inteligência e maximize seu tempo.',
  },
  {
    titulo: 'Prática Constante',
    texto:
      'Simulados e milhares de questões comentadas para reforçar o conteúdo e treinar para a prova.',
  },
  {
    titulo: 'Direção Clara',
    texto:
      'Foco exclusivo nos conteúdos realmente relevantes para concursos bancários do momento.',
  },
  {
    titulo: 'Preparação Evolutiva',
    texto:
      'Estrutura pensada para transformar seu tempo de estudo na sua evolução contínua.',
  },
]

const BANCOS = [
  { arquivo: 'caixa.svg', nome: 'Caixa Econômica Federal' },
  { arquivo: 'bb.svg', nome: 'Banco do Brasil' },
  { arquivo: 'bndes.svg', nome: 'BNDES' },
  { arquivo: 'bacen.svg', nome: 'Banco Central' },
  { arquivo: 'banrisul.svg', nome: 'Banrisul' },
  { arquivo: 'bnb.svg', nome: 'Banco do Nordeste' },
  { arquivo: 'brb.svg', nome: 'BRB' },
  { arquivo: 'basa.svg', nome: 'Banco da Amazônia' },
]

/** Vitrine estatica do legado — usada quando o catalogo da API esta vazio. */
const CURSOS_FALLBACK = [
  {
    id: 'caixa',
    categoria: 'Pacote Completo',
    titulo: 'Caixa Econômica Federal',
    descricao: 'Preparação completa com videoaulas, PDFs e questões focadas no edital.',
    logo: '/images/bancos/caixa.svg',
    preco: 'R$ 497,00',
    destaque: true,
  },
  {
    id: 'bb',
    categoria: 'Conhecimentos Específicos',
    titulo: 'Banco do Brasil (Agente Comercial)',
    descricao: 'Aprofundamento focado nos conhecimentos específicos do último edital.',
    logo: '/images/bancos/bb.svg',
    preco: 'R$ 347,00',
    destaque: false,
  },
  {
    id: 'bndes',
    categoria: 'Reta Final',
    titulo: 'BNDES 2026',
    descricao: 'Direcionamento estratégico para a reta final de preparação.',
    logo: '/images/bancos/bndes.svg',
    preco: 'R$ 297,00',
    destaque: false,
  },
]

interface ProductPublic {
  id: string
  sku: string
  name: string
  description: string | null
  priceCents: number
  currency: string
}

const real = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

/* ------------------------------------------------------------------ */

/** Revela a secao quando ela entra na viewport. */
function Reveal({ children, className = '' }: { children: ReactNode; className?: string }) {
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = ref.current
    if (!el) return
    // Sem IntersectionObserver (browsers antigos, jsdom): mostra direto.
    if (typeof IntersectionObserver === 'undefined') {
      el.classList.add('is-visible')
      return
    }
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-visible')
            observer.unobserve(entry.target)
          }
        }
      },
      { threshold: 0.12 },
    )
    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  return (
    <div ref={ref} className={`lp-reveal ${className}`.trim()}>
      {children}
    </div>
  )
}

const IconeSeta = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M5 12h14m0 0-6-6m6 6-6 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)

const IconeEscudo = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M12 3 5 6v5c0 4.5 3 8.2 7 10 4-1.8 7-5.5 7-10V6l-7-3Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" />
    <path d="m9 12 2 2 4-4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
)

/* ------------------------------------------------------------------ */

export function LandingPage() {
  // Catalogo real quando existir; a vitrine do legado como fallback.
  const produtos = useQuery({
    queryKey: ['produtos-publicos'],
    queryFn: () => api<ProductPublic[]>('/products'),
    retry: 1,
  })

  return (
    <div className="lp">
      <header className="lp-header">
        <div className="lp-header-inner">
          <a className="lp-logo" href="#inicio">
            Beto <em>Banco</em>
          </a>
          <nav className="lp-nav" aria-label="Seções">
            <a href="#metodo">Método</a>
            <a href="#professor">Professor</a>
            <a href="#cursos">Cursos</a>
          </nav>
          <Link to="/login" className="lp-btn lp-btn--ghost" style={{ padding: '10px 22px' }}>
            Entrar
          </Link>
        </div>
      </header>

      {/* ---------------- hero ---------------- */}
      <section className="lp-hero" id="inicio">
        <div className="lp-container lp-hero-grid">
          <div>
            <span className="lp-hero-badge">
              <span className="dot" aria-hidden="true" />
              Concursos Bancários 2026
            </span>
            <h1 className="lp-hero-title">
              Sua aprovação começa com <em>um passo de cada vez.</em>
            </h1>
            <p className="lp-hero-sub">
              Prepare-se para concursos bancários de forma objetiva, com cursos, simulados e
              materiais desenvolvidos para quem quer estudar com direção e conquistar a
              aprovação.
            </p>
            <div className="lp-hero-actions">
              <Link to="/login" className="lp-btn lp-btn--gold">
                Quero me preparar <IconeSeta />
              </Link>
              <a href="#cursos" className="lp-btn lp-btn--ghost">
                Ver cursos
              </a>
            </div>
            <p className="lp-hero-note">
              <IconeEscudo /> 7 dias de garantia incondicional em todos os cursos.
            </p>
          </div>

          <Reveal>
            <div className="lp-portrait">
              <img className="foto" src="/images/professor/prof-betao.png" alt="Professor Beto Fernandes" />
              <div className="lp-float lp-float--anos">
                <span className="num">25+</span>
                <span className="label">Anos de experiência</span>
              </div>
              <div className="lp-float lp-float--mentoria">
                <span className="num">+1.000</span>
                <span className="label">Aprovações orientadas</span>
              </div>
            </div>
          </Reveal>
        </div>
      </section>

      {/* ---------------- bancos ---------------- */}
      <div className="lp-banks" aria-label="Bancos cobertos pela preparação">
        <p className="lp-banks-label">Preparação para os principais concursos bancários do país</p>
        <div className="lp-marquee">
          <div className="lp-marquee-track">
            {[...BANCOS, ...BANCOS].map((b, i) => (
              <img key={`${b.arquivo}-${i}`} src={`/images/bancos/${b.arquivo}`} alt={i < BANCOS.length ? b.nome : ''} aria-hidden={i >= BANCOS.length} />
            ))}
          </div>
        </div>
      </div>

      {/* ---------------- numeros ---------------- */}
      <section className="lp-section">
        <div className="lp-container">
          <Reveal>
            <div className="lp-stats">
              <div className="lp-stat">
                <span className="num">25+</span>
                <span className="label">Anos de experiência</span>
              </div>
              <div className="lp-stat">
                <span className="num">+1.000</span>
                <span className="label">Aprovações orientadas</span>
              </div>
              <div className="lp-stat">
                <span className="num">13</span>
                <span className="label">Aprovações pessoais</span>
              </div>
              <div className="lp-stat">
                <span className="num">7 dias</span>
                <span className="label">Garantia incondicional</span>
              </div>
            </div>
          </Reveal>
        </div>
      </section>

      {/* ---------------- metodo ---------------- */}
      <section className="lp-section" id="metodo">
        <div className="lp-container">
          <Reveal>
            <span className="lp-overline">O método</span>
            <h2 className="lp-title">
              Por que estudar com o <em>Prof. Beto Fernandes?</em>
            </h2>
            <p className="lp-subtitle">
              Conheça os pilares da metodologia que tem guiado milhares de alunos rumo à
              aprovação bancária.
            </p>
          </Reveal>
          <div className="lp-pillars">
            {PILARES.map((p, i) => (
              <Reveal key={p.titulo}>
                <article className="lp-pillar">
                  <span className="index" aria-hidden="true">
                    {String(i + 1).padStart(2, '0')}
                  </span>
                  <h3>{p.titulo}</h3>
                  <p>{p.texto}</p>
                </article>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {/* ---------------- professor ---------------- */}
      <section className="lp-section" id="professor">
        <div className="lp-container lp-prof-grid">
          <Reveal>
            <div className="lp-prof-frame">
              <span className="lp-prof-corner tl" aria-hidden="true" />
              <span className="lp-prof-corner br" aria-hidden="true" />
              <img src="/images/professor/prof-betao.png" alt="Professor Beto Fernandes" />
              <img className="assinatura" src="/images/professor/prof-betao-signature-white.png" alt="" aria-hidden="true" />
            </div>
          </Reveal>
          <Reveal>
            <span className="lp-overline">Quem guia você</span>
            <h2 className="lp-title">
              Conheça quem vai guiar a sua <em>aprovação</em>
            </h2>
            <div className="lp-prof-bio">
              <p>
                Professor Beto Fernandes possui mais de duas décadas de experiência em
                concursos públicos, focando na aprovação estratégica de seus alunos.
              </p>
              <p>
                Com vasta experiência em <strong>Conhecimentos Bancários, Direito
                Constitucional, Direito Previdenciário e Legislação Educacional</strong>, ele
                transforma a complexidade dos editais em um passo a passo simples e
                executável.
              </p>
            </div>
            <blockquote className="lp-prof-quote">
              “Desistir dos seus sonhos não é uma opção. Sua aprovação começa aqui, com um
              passo de cada vez.”
              <footer>Prof. Beto Fernandes</footer>
            </blockquote>
          </Reveal>
        </div>
      </section>

      {/* ---------------- cursos ---------------- */}
      <section className="lp-section" id="cursos">
        <div className="lp-container">
          <Reveal>
            <span className="lp-overline">Catálogo</span>
            <h2 className="lp-title">
              Encontre o material certo para a sua <em>preparação</em>
            </h2>
            <p className="lp-subtitle">
              Cursos estruturados passo a passo para quem busca a aprovação definitiva nos
              melhores concursos bancários do país.
            </p>
          </Reveal>

          <div className="lp-courses">
            {produtos.data && produtos.data.length > 0
              ? produtos.data.map((p, i) => (
                  <Reveal key={p.id}>
                    <article className={`lp-course ${i === 0 ? 'lp-course--destaque' : ''}`.trim()}>
                      {i === 0 && <span className="selo">Mais procurado</span>}
                      <span className="categoria">{p.sku}</span>
                      <h3>{p.name}</h3>
                      <p className="descricao">
                        {p.description ?? 'Preparação completa com videoaulas, PDFs e questões focadas no edital.'}
                      </p>
                      <div className="preco">
                        <span className="de">Investimento</span>
                        <span className="por">{real.format(p.priceCents / 100)}</span>
                      </div>
                      <Link to="/login" className="lp-btn lp-btn--gold">
                        Quero este curso <IconeSeta />
                      </Link>
                    </article>
                  </Reveal>
                ))
              : CURSOS_FALLBACK.map((c) => (
                  <Reveal key={c.id}>
                    <article className={`lp-course ${c.destaque ? 'lp-course--destaque' : ''}`.trim()}>
                      {c.destaque && <span className="selo">Mais procurado</span>}
                      <img className="logo" src={c.logo} alt="" aria-hidden="true" />
                      <span className="categoria">{c.categoria}</span>
                      <h3>{c.titulo}</h3>
                      <p className="descricao">{c.descricao}</p>
                      <div className="preco">
                        <span className="de">Investimento</span>
                        <span className="por">{c.preco}</span>
                      </div>
                      <Link to="/login" className="lp-btn lp-btn--gold">
                        Quero este curso <IconeSeta />
                      </Link>
                    </article>
                  </Reveal>
                ))}
          </div>
        </div>
      </section>

      {/* ---------------- CTA final ---------------- */}
      <section className="lp-section">
        <div className="lp-container">
          <Reveal>
            <div className="lp-cta">
              <div className="lp-cta-inner">
                <span className="lp-overline" style={{ justifyContent: 'center' }}>
                  Comece hoje
                </span>
                <h2 className="lp-title">
                  Seu próximo passo pode começar <em>agora.</em>
                </h2>
                <p className="lp-subtitle" style={{ margin: '0 auto var(--bb-s5)' }}>
                  Não adie mais o seu futuro. Conheça os cursos, simulados e materiais
                  disponíveis e comece a estudar com quem entende de aprovação.
                </p>
                <Link to="/login" className="lp-btn lp-btn--gold" style={{ fontSize: '1.05rem' }}>
                  Começar minha preparação <IconeSeta />
                </Link>
                <br />
                <span className="garantia">
                  <IconeEscudo /> 7 dias de garantia incondicional
                </span>
              </div>
            </div>
          </Reveal>
        </div>
      </section>

      <footer className="lp-footer">
        <div className="lp-container lp-footer-grid">
          <a className="lp-logo" href="#inicio">
            Beto <em>Banco</em>
          </a>
          <img className="assinatura" src="/images/professor/prof-betao-signature-white.png" alt="Assinatura do Prof. Beto Fernandes" />
          <p className="copy">
            © {new Date().getFullYear()} Beto Banco · Prof. Beto Fernandes. Todos os direitos
            reservados.
          </p>
        </div>
      </footer>
    </div>
  )
}
