import { Link, Outlet } from 'react-router-dom'
import './auth.css'

/** Moldura premium comum a login, esqueci-senha e definir-senha. */
export function AuthLayout() {
  return (
    <div className="auth">
      <aside className="auth-panel">
        <Link to="/" className="auth-logo">
          Beto <em>Banco</em>
        </Link>

        <blockquote className="auth-quote">
          <span className="marca" aria-hidden="true">
            “
          </span>
          Desistir dos seus sonhos não é uma opção. Sua aprovação começa aqui, com um passo
          de cada vez.
        </blockquote>

        <div className="auth-panel-rodape">
          <img src="/images/professor/prof-betao-signature-white.png" alt="" aria-hidden="true" />
          <span>Prof. Beto Fernandes</span>
        </div>
      </aside>

      <main className="auth-main">
        <div className="auth-card">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
