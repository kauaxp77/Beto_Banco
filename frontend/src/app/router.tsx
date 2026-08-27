import { createBrowserRouter, Navigate } from 'react-router-dom'
import { RequireAuth } from '../auth/RequireAuth'
import { RequireRole } from '../auth/RequireRole'
import { DefinePasswordPage } from '../pages/DefinePasswordPage'
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage'
import { LoginPage } from '../pages/LoginPage'
import { AppShell } from './AppShell'

// Placeholders restantes: a Task 7 traz dashboard/perfil; admin fica na 4b.
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
