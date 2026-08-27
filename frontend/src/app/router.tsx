import { createBrowserRouter, Navigate } from 'react-router-dom'
import { RequireAuth } from '../auth/RequireAuth'
import { RequireRole } from '../auth/RequireRole'
import { DashboardPage } from '../pages/DashboardPage'
import { DefinePasswordPage } from '../pages/DefinePasswordPage'
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage'
import { LoginPage } from '../pages/LoginPage'
import { ProfilePage } from '../pages/ProfilePage'
import { AppShell } from './AppShell'

// Placeholder restante: as telas admin chegam na fase 4b.
const EmBreve = ({ nome }: { nome: string }) => <p>{nome} — em construção.</p>

export const router = createBrowserRouter([
  {
    element: <AppShell />,
    children: [
      { path: '/', element: <Navigate to="/dashboard" replace /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/esqueci-senha', element: <ForgotPasswordPage /> },
      { path: '/definir-senha/:token', element: <DefinePasswordPage /> },
      {
        path: '/dashboard',
        element: (
          <RequireAuth>
            <DashboardPage />
          </RequireAuth>
        ),
      },
      {
        path: '/perfil',
        element: (
          <RequireAuth>
            <ProfilePage />
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
