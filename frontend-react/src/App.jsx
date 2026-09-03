import React, { Suspense, lazy, useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, useNavigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './auth/AuthContext';
import { PrivateRoute } from './auth/PrivateRoute';

import BannerCookies from './components/BannerCookies/BannerCookies';
import LimiteDeErro from './components/LimiteDeErro/LimiteDeErro';

import LandingPage from './pages/LandingPage';
import LoginPage from './pages/Auth/LoginPage';

import {
  PaginaNaoEncontrada,
  SemConexao,
  SessaoExpirada,
} from './pages/Excecoes/TelasDeExcecao';

/*
 * Secao 15 -- "Core Web Vitals com LCP < 2,5s", e secao 08: a homepage e a
 * porta de entrada do trafego organico. Dashboard, simulado e admin carregam
 * biblioteca de grafico e sao vistos so depois do login; deixa-los no bundle
 * inicial faria o visitante anonimo baixar centenas de kB que ele nunca usa.
 */
const Dashboard = lazy(() => import('./pages/Aluno/Dashboard'));
const SimuladoEngine = lazy(() => import('./pages/Aluno/SimuladoEngine'));
const AdminPanel = lazy(() => import('./pages/Admin/AdminPanel'));

import './App.css';

/**
 * Secao 20 -- "Logout encerra a sessao no servidor". Quando a renovacao do
 * refresh falha, o cliente da API emite plataforma:sessao-expirada em vez de
 * redirecionar sozinho; a decisao de navegar fica aqui, dentro do Router, e o
 * usuario nao perde um formulario preenchido por um redirect surpresa.
 */
function OuvinteDeSessao() {
  const navegar = useNavigate();

  useEffect(() => {
    const aoExpirar = () => navegar('/sessao-expirada');
    window.addEventListener('plataforma:sessao-expirada', aoExpirar);
    return () => window.removeEventListener('plataforma:sessao-expirada', aoExpirar);
  }, [navegar]);

  return null;
}

/** Secao 06 -- tela de excecao "offline", exibida sobre a rota atual. */
function AvisoDeConexao() {
  const [offline, setOffline] = useState(!navigator.onLine);

  useEffect(() => {
    const online = () => setOffline(false);
    const caiu = () => setOffline(true);
    window.addEventListener('online', online);
    window.addEventListener('offline', caiu);
    return () => {
      window.removeEventListener('online', online);
      window.removeEventListener('offline', caiu);
    };
  }, []);

  if (!offline) return null;
  // Cobre a rota atual em vez de aparecer abaixo dela: sem isso o aluno rola a
  // pagina e encontra dois conteudos concorrentes na mesma tela.
  return (
    <div className="camada-offline" role="status" aria-live="polite">
      <SemConexao />
    </div>
  );
}

/**
 * Estado de carregamento das rotas em lazy. Um esqueleto, nao um spinner: o
 * bloco ja ocupa a altura do conteudo e evita o salto de layout que o Core Web
 * Vitals conta como CLS (secao 15).
 */
function CarregandoRota() {
  return (
    <div className="conteiner" style={{ paddingBlock: 'var(--esp-7)' }} aria-busy="true">
      <span className="apenas-leitor">Carregando a pagina</span>
      <div className="esqueleto esqueleto--titulo" />
      <div className="esqueleto esqueleto--bloco" style={{ marginTop: 'var(--esp-5)' }} />
    </div>
  );
}

function App() {
  return (
    <LimiteDeErro>
      <AuthProvider>
        <Router>
          <OuvinteDeSessao />

          {/* Primeiro elemento focavel da pagina (secao 06, WCAG 2.1 AA). */}
          <a className="pular-para-conteudo" href="#conteudo">
            Pular para o conteudo
          </a>

          <Suspense fallback={<CarregandoRota />}>
            <Routes>
              {/* Publicas */}
              <Route path="/" element={<LandingPage />} />
              <Route path="/login" element={<LoginPage />} />

              {/* Protegidas */}
              <Route element={<PrivateRoute />}>
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/simulado" element={<SimuladoEngine />} />
                <Route path="/admin" element={<AdminPanel />} />
              </Route>

              {/* Telas de excecao (secao 06) */}
              <Route path="/sessao-expirada" element={<SessaoExpirada />} />
              <Route path="/offline" element={<SemConexao />} />
              <Route path="*" element={<PaginaNaoEncontrada />} />
            </Routes>
          </Suspense>

          <AvisoDeConexao />

          {/* Secao 22: o banner fica acima de tudo e nao pode ser dispensado
              sem uma decisao explicita do titular. */}
          <BannerCookies />

          <Toaster
            position="bottom-right"
            toastOptions={{
              // Cores da paleta corrigida da secao 05; o toast herda a mesma
              // superficie de card do resto da interface.
              style: {
                background: 'var(--superficie-card)',
                color: 'var(--texto)',
                border: '1px solid var(--borda)',
                borderLeft: '3px solid var(--dourado)',
                borderRadius: 'var(--raio)',
              },
              success: { style: { borderLeft: '3px solid var(--sucesso)' } },
              error: { style: { borderLeft: '3px solid var(--critico)' } },
            }}
          />
        </Router>
      </AuthProvider>
    </LimiteDeErro>
  );
}

export default App;
