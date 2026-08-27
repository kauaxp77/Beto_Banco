import { createBrowserRouter, Navigate } from 'react-router-dom'
import { RequireAuth } from '../auth/RequireAuth'
import { RequireRole } from '../auth/RequireRole'
import { AdminAuditPage } from '../pages/admin/AdminAuditPage'
import { AdminAnnouncementsPage } from '../pages/admin/AdminAnnouncementsPage'
import { AdminCommentsPage } from '../pages/admin/AdminCommentsPage'
import { AdminCourseContentPage } from '../pages/admin/AdminCourseContentPage'
import { AdminCoursesPage } from '../pages/admin/AdminCoursesPage'
import { AdminDashboardPage } from '../pages/admin/AdminDashboardPage'
import { AdminInvitesPage } from '../pages/admin/AdminInvitesPage'
import { AdminReportsPage } from '../pages/admin/AdminReportsPage'
import { AdminLayout } from '../pages/admin/AdminLayout'
import { AdminPaymentsPage } from '../pages/admin/AdminPaymentsPage'
import { AdminProductsPage } from '../pages/admin/AdminProductsPage'
import { AdminStudentDetailPage } from '../pages/admin/AdminStudentDetailPage'
import { AdminStudentsPage } from '../pages/admin/AdminStudentsPage'
import { AdminTestimonialsPage } from '../pages/admin/AdminTestimonialsPage'
import { AdminWebhooksPage } from '../pages/admin/AdminWebhooksPage'
import { AuthLayout } from '../pages/auth/AuthLayout'
import { CertificadoPage } from '../pages/CertificadoPage'
import { CursoPage } from '../pages/CursoPage'
import { DashboardPage } from '../pages/DashboardPage'
import { DefinePasswordPage } from '../pages/DefinePasswordPage'
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage'
import { LandingPage } from '../pages/landing/LandingPage'
import { LoginPage } from '../pages/LoginPage'
import { ProfilePage } from '../pages/ProfilePage'
import { AppShell } from './AppShell'

export const router = createBrowserRouter([
  // Landing publica: dona da propria moldura, fora do AppShell.
  { path: '/', element: <LandingPage /> },

  // Certificado: publico por natureza (validacao por terceiros) e com
  // moldura propria para a impressao sair limpa.
  { path: '/certificado/:code', element: <CertificadoPage /> },

  // Autenticacao: moldura editorial propria (split-screen).
  {
    element: <AuthLayout />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/esqueci-senha', element: <ForgotPasswordPage /> },
      { path: '/definir-senha/:token', element: <DefinePasswordPage /> },
    ],
  },

  // Area logada.
  {
    element: <AppShell />,
    children: [
      {
        path: '/dashboard',
        element: (
          <RequireAuth>
            <DashboardPage />
          </RequireAuth>
        ),
      },
      {
        path: '/curso/:id',
        element: (
          <RequireAuth>
            <CursoPage />
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
      { path: '*', element: <p>Página não encontrada.</p> },
    ],
  },

  // Painel administrativo: moldura propria com sidebar; o guard e UX,
  // quem nega de verdade e o backend (/admin/** exige ROLE_ADMIN).
  {
    element: (
      <RequireRole role="ROLE_ADMIN">
        <AdminLayout />
      </RequireRole>
    ),
    children: [
      { path: '/admin', element: <Navigate to="/admin/dashboard" replace /> },
      { path: '/admin/dashboard', element: <AdminDashboardPage /> },
      { path: '/admin/alunos', element: <AdminStudentsPage /> },
      { path: '/admin/alunos/:id', element: <AdminStudentDetailPage /> },
      { path: '/admin/pagamentos', element: <AdminPaymentsPage /> },
      { path: '/admin/webhooks', element: <AdminWebhooksPage /> },
      { path: '/admin/produtos', element: <AdminProductsPage /> },
      { path: '/admin/cursos', element: <AdminCoursesPage /> },
      { path: '/admin/cursos/:id', element: <AdminCourseContentPage /> },
      { path: '/admin/comentarios', element: <AdminCommentsPage /> },
      { path: '/admin/relatorios', element: <AdminReportsPage /> },
      { path: '/admin/anuncios', element: <AdminAnnouncementsPage /> },
      { path: '/admin/convites', element: <AdminInvitesPage /> },
      { path: '/admin/depoimentos', element: <AdminTestimonialsPage /> },
      { path: '/admin/auditoria', element: <AdminAuditPage /> },
    ],
  },
])
