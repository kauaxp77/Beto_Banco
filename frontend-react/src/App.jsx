import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { PrivateRoute } from './auth/PrivateRoute';
import { Toaster } from 'react-hot-toast';

import LandingPage from './pages/LandingPage';
import LoginPage from './pages/Auth/LoginPage';
import Dashboard from './pages/Aluno/Dashboard';
import SimuladoEngine from './pages/Aluno/SimuladoEngine';
import AdminPanel from './pages/Admin/AdminPanel';

import './App.css';

function App() {
    return (
        <AuthProvider>
            <Router>
                <Routes>
                    {/* Rotas Públicas */}
                    <Route path="/" element={<LandingPage />} />
                    <Route path="/login" element={<LoginPage />} />

                    {/* Rotas Protegidas */}
                    <Route element={<PrivateRoute />}>
                        <Route path="/dashboard" element={<Dashboard />} />
                        <Route path="/simulado" element={<SimuladoEngine />} />
                        <Route path="/admin" element={<AdminPanel />} />
                    </Route>
                </Routes>
                <Toaster
                    position="bottom-right"
                    toastOptions={{
                        style: {
                            background: '#1A1C20',
                            color: '#fff',
                            border: '1px solid #C4A15A'
                        },
                    }}
                />
            </Router>
        </AuthProvider>
    );
}

export default App;
