import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, LogIn, Loader2, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../auth/AuthContext';
import { api } from '../../lib/api';
import './LoginPage.css';

/**
 * Documento Mestre V4.0 — §19 (POST /auth/login) e §20 (regras de sessão).
 *
 * Autentica contra a API própria. A versão anterior falava com o Supabase, o que
 * deixou de funcionar assim que a plataforma passou a ter backend próprio (§31,
 * decisão permanente) — e falhava com "Failed to fetch" em qualquer ambiente sem
 * as chaves do Supabase configuradas.
 *
 * Não há cadastro aqui de propósito. Pela §12, a conta nasce na aprovação do
 * pagamento ("4 Webhook → 5 Conta criada → 6 Acesso liberado"); quem ainda não
 * comprou vai para o catálogo, não para um formulário de cadastro.
 */
const LoginPage = () => {
    const navegar = useNavigate();
    const { entrar } = useAuth();

    const [email, setEmail] = useState('');
    const [senha, setSenha] = useState('');
    const [codigoMfa, setCodigoMfa] = useState('');
    const [exigeMfa, setExigeMfa] = useState(false);

    const [carregando, setCarregando] = useState(false);
    const [erro, setErro] = useState('');
    const [aviso, setAviso] = useState('');

    const autenticar = async (e) => {
        e.preventDefault();
        setErro('');
        setAviso('');
        setCarregando(true);

        try {
            const perfil = await entrar({
                email,
                senha,
                codigo_mfa: codigoMfa || undefined,
                dispositivo: navigator.userAgent.slice(0, 120),
            });

            // §20 — permissão acumulativa: quem administra também é aluno, então a
            // checagem vai do perfil mais amplo para o mais restrito.
            const administra = ['ADMIN', 'SUPER_ADMIN', 'PROFESSOR'].some((p) => perfil.perfis?.includes(p));
            navegar(administra ? '/admin' : '/dashboard', { replace: true });
        } catch (falha) {
            tratarFalha(falha);
        } finally {
            setCarregando(false);
        }
    };

    /**
     * O `tipo` do RFC 7807 é a chave estável da resposta (§19). Ler o texto da
     * mensagem para decidir o que fazer quebraria assim que alguém corrigisse uma
     * vírgula no backend.
     */
    const tratarFalha = (falha) => {
        switch (falha.tipo) {
            case 'mfa-obrigatorio':
                setExigeMfa(true);
                setAviso('Esta conta usa segundo fator. Informe o código de 6 dígitos do seu aplicativo autenticador.');
                break;
            case 'mfa-invalido':
                setExigeMfa(true);
                setCodigoMfa('');
                setErro('Código inválido ou expirado. O código muda a cada 30 segundos — tente o atual.');
                break;
            case 'mfa-nao-configurado':
                setErro('Este perfil exige segundo fator, mas ainda não há um configurado. Fale com o suporte.');
                break;
            case 'conta-bloqueada':
                setErro('Conta bloqueada temporariamente por tentativas de login. Tente de novo em 15 minutos.');
                break;
            case 'credenciais-invalidas':
                setErro('E-mail ou senha incorretos.');
                break;
            case 'limite-excedido':
                setErro('Muitas tentativas seguidas. Aguarde um minuto e tente novamente.');
                break;
            default:
                setErro(
                    falha.detalhe ||
                        'Não foi possível entrar agora. Verifique sua conexão e tente novamente.',
                );
        }
    };

    /** §19 — token de uso único, 30 min. A resposta é 202 exista a conta ou não. */
    const recuperarSenha = async () => {
        setErro('');
        setAviso('');
        if (!email) {
            setErro('Informe seu e-mail no campo acima para receber o link de recuperação.');
            return;
        }
        setCarregando(true);
        try {
            await api.recuperarSenha(email);
            setAviso(
                'Se houver uma conta com esse e-mail, o link de recuperação chega em instantes. Ele vale por 30 minutos.',
            );
        } catch {
            setErro('Não foi possível enviar o link agora. Tente novamente em instantes.');
        } finally {
            setCarregando(false);
        }
    };

    return (
        <div className="login-page">
            <main className="login-card" id="conteudo">
                <img
                    src="/logo-beto-banco.jpg"
                    alt="Aprovação Passo a Passo"
                    className="login-logo"
                    style={{ height: '60px', borderRadius: '12px', objectFit: 'contain', mixBlendMode: 'screen' }}
                />

                <h1 className="login-title">Acesse sua conta</h1>
                <p className="login-subtitle">A plataforma de mentoria para concursos bancários</p>

                {/* role="alert" para que o leitor de tela anuncie a falha (§06, WCAG 2.1 AA). */}
                {erro && (
                    <div className="login-error" role="alert">
                        {erro}
                    </div>
                )}
                {aviso && (
                    <div
                        className="login-error"
                        role="status"
                        style={{
                            background: 'var(--sucesso-fundo)',
                            color: 'var(--sucesso)',
                            borderColor: 'var(--sucesso)',
                        }}
                    >
                        {aviso}
                    </div>
                )}

                <form className="login-form" onSubmit={autenticar}>
                    <div className="form-group">
                        <label htmlFor="email">E-mail de acesso</label>
                        <input
                            type="email"
                            id="email"
                            name="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="seu@email.com"
                            autoComplete="username"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="senha">Senha</label>
                        <input
                            type="password"
                            id="senha"
                            name="senha"
                            value={senha}
                            onChange={(e) => setSenha(e.target.value)}
                            placeholder="••••••••"
                            autoComplete="current-password"
                            required
                        />
                    </div>

                    {/* §20 — 2FA obrigatório para Admin e Suporte. O campo só aparece
                        quando a API pede, para não confundir quem não usa. */}
                    {exigeMfa && (
                        <div className="form-group">
                            <label htmlFor="codigoMfa">
                                <ShieldCheck size={14} style={{ verticalAlign: '-2px', marginRight: 6 }} />
                                Código do segundo fator
                            </label>
                            <input
                                type="text"
                                id="codigoMfa"
                                name="codigoMfa"
                                value={codigoMfa}
                                onChange={(e) => setCodigoMfa(e.target.value.replace(/\D/g, '').slice(0, 6))}
                                placeholder="000000"
                                inputMode="numeric"
                                autoComplete="one-time-code"
                                // O navegador não deve guardar código de uso único.
                                maxLength={6}
                                autoFocus
                                required
                                style={{ fontFamily: 'var(--fonte-dados)', letterSpacing: '0.4em' }}
                            />
                        </div>
                    )}

                    <div className="form-options">
                        <span />
                        <button
                            type="button"
                            className="forgot-password"
                            onClick={recuperarSenha}
                            style={{ background: 'none', border: 0, cursor: 'pointer' }}
                        >
                            Esqueceu a senha?
                        </button>
                    </div>

                    <button type="submit" className="login-btn" disabled={carregando} aria-busy={carregando}>
                        {carregando ? <Loader2 className="spin" size={20} /> : <LogIn size={20} />}
                        {carregando ? 'Entrando...' : 'ENTRAR NA PLATAFORMA'}
                    </button>
                </form>

                <p className="login-demo-note" style={{ textAlign: 'center', marginTop: '24px' }}>
                    Ainda não é aluno? <Link to="/#cursos">Conheça os cursos</Link>
                </p>

                <Link to="/" className="back-link">
                    <ArrowLeft size={16} /> Voltar para o início
                </Link>
            </main>
        </div>
    );
};

export default LoginPage;
