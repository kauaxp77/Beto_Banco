import { createBrowserRouter, Navigate } from 'react-router-dom'
import { RequireAuth } from '../auth/RequireAuth'
import { RequireRole } from '../auth/RequireRole'
import { AppShell } from './AppShell'

// Placeholders das telas: as Tasks 6 e 7 do plano da fase 4a substituem cada
// um pelo componente real. As telas admin chegam na fase 4b.
const EmBreve = ({ nome }: { nome: string }) => <p>{nome} — em construção.</p>

export const router = createBrowserRouter([
  {
    element: <AppShell />,
    children: [
      { path: '/', element: <Navigate to="/dashboard" replace /> },
      { path: '/login', element: <EmBreve nome="Login" /> },
      { path: '/esqueci-senha', element: <EmBreve nome="Esqueci a senha" /> },
      { path: '/definir-senha/:token', element: <EmBreve nome="Definir senha" /> },
      {
        path: '/dashboard',
        element: (
          <RequireAuth>
            <EmBreve nome="Dashboard" />
          </RequireAuth>
        ),
      },
      {
        path: '/perfil',
        element: (
          <RequireAuth>
            <EmBreve nome="Perfil" />
          </RequireAuth>
        ),
      },
      {
        path: '/admin/*',
        element: (
          <RequireRole role="ROLE_ADMIN">
            <EmBreve nome="Administração" />
          </RequireRole>
        ),
      },
      { path: '*', element: <p>Página não encontrada.</p> },
    ],
  },
])
