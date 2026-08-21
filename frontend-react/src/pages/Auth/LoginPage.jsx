import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { LogIn, ArrowLeft, Loader2 } from 'lucide-react';
import { supabase } from '../../api/supabase';
import './LoginPage.css';

const LoginPage = () => {
    const navigate = useNavigate();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [fullName, setFullName] = useState('');
    const [isRegistering, setIsRegistering] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    const handleAuth = async (e) => {
        e.preventDefault();
        setError('');
        setSuccessMessage('');
        setLoading(true);

        try {
            if (isRegistering) {
                const { data, error } = await supabase.auth.signUp({
                    email,
                    password,
                    options: {
                        data: {
                            full_name: fullName || 'Novo Usuário'
                        }
                    }
                });
                if (error) throw error;
                setSuccessMessage('Cadastro realizado! (Se o Supabase exigir confirmação, verifique seu e-mail). Se não, mude para Login e entre na conta.');
                setIsRegistering(false);
            } else {
                const { data, error } = await supabase.auth.signInWithPassword({
                    email,
                    password,
                });

                if (error) throw error;

                if (data.session) {
                    // Check user role via profile
                    const { data: profile } = await supabase
                        .from('profiles')
                        .select('role')
                        .eq('id', data.user.id)
                        .single();

                    if (profile?.role === 'ADMIN' || profile?.role === 'SUPER_ADMIN' || profile?.role === 'PROFESSOR') {
                        navigate('/admin');
                    } else {
                        navigate('/dashboard');
                    }
                }
            }
        } catch (err) {
            setError(err.message || 'Erro de autenticação. Verifique os dados e tente novamente.');
        } finally {
            setLoading(false);
        }
    };

    const isDevelopment = import.meta.env.DEV;

    const handleMockSignup = async () => {
        setError('');
        setLoading(true);
        try {
            // Helper to quickly create an account during dev
            const { error: signUpError } = await supabase.auth.signUp({
                email,
                password,
                options: {
                    data: {
                        full_name: fullName || 'Novo Test User'
                    }
                }
            });
            if (signUpError) throw signUpError;
            alert('Cadastro teste executado. Tente logar se Auto Confirm estiver ON.');
        } catch (e) {
            setError(e.message);
        }
        setLoading(false);
    };

    return (
        <div className="login-page">
            <div className="login-card">
                <img src="/logo-beto-banco.jpg" alt="Beto Banco" className="login-logo" style={{ height: '60px', borderRadius: '12px', objectFit: 'contain' }} />

                <h1 className="login-title">Acesse sua Conta</h1>
                <p className="login-subtitle">A plataforma premium para concursos bancários</p>

                {error && <div className="login-error">{error}</div>}
                {successMessage && <div className="login-error" style={{ background: 'rgba(34, 197, 94, 0.1)', color: '#22c55e', borderColor: 'rgba(34, 197, 94, 0.2)' }}>{successMessage}</div>}

                <form className="login-form" onSubmit={handleAuth}>
                    {isRegistering && (
                        <div className="form-group">
                            <label htmlFor="fullName">Nome Completo</label>
                            <input
                                type="text"
                                id="fullName"
                                value={fullName}
                                onChange={(e) => setFullName(e.target.value)}
                                placeholder="Seu nome"
                                required={isRegistering}
                            />
                        </div>
                    )}

                    <div className="form-group">
                        <label htmlFor="email">E-mail de acesso</label>
                        <input
                            type="email"
                            id="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="seu@email.com"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="password">Senha</label>
                        <input
                            type="password"
                            id="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="••••••••"
                            required
                        />
                    </div>

                    {!isRegistering && (
                        <div className="form-options">
                            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                                <input type="checkbox" /> Lembrar de mim
                            </label>
                            <a href="#" className="forgot-password">Esqueceu a senha?</a>
                        </div>
                    )}

                    <button type="submit" className="login-btn" disabled={loading}>
                        {loading ? <Loader2 className="spin" size={20} /> : <LogIn size={20} />}
                        {loading ? 'Processando...' : (isRegistering ? 'CRIAR MINHA CONTA' : 'ENTRAR NA PLATAFORMA')}
                    </button>
                </form>

                <div className="login-demo-note" style={{ textAlign: 'center', marginTop: '24px' }}>
                    {isRegistering ? (
                        <span>Já possui conta? <button type="button" onClick={() => { setIsRegistering(false); setError(''); setSuccessMessage(''); }} style={{ background: 'none', border: 'none', color: 'var(--primary)', cursor: 'pointer', fontWeight: 'bold' }}>Faça Login</button></span>
                    ) : (
                        <span>Não é aluno ainda? <button type="button" onClick={() => { setIsRegistering(true); setError(''); setSuccessMessage(''); }} style={{ background: 'none', border: 'none', color: 'var(--primary)', cursor: 'pointer', fontWeight: 'bold' }}>Cadastre-se</button></span>
                    )}
                </div>

                <Link to="/" className="back-link">
                    <ArrowLeft size={16} /> Voltar para o início
                </Link>
            </div>
        </div>
    );
};

export default LoginPage;
