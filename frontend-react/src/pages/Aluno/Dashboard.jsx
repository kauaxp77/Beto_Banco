import React, { useEffect, useState } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { supabase } from '../../api/supabase';
import { useNavigate } from 'react-router-dom';
import { Play, TrendingUp, Clock, Award, LogOut } from 'lucide-react';
import './Dashboard.css';

const Dashboard = () => {
    const { user, signOut } = useAuth();
    const navigate = useNavigate();
    const [profile, setProfile] = useState(null);
    const [stats, setStats] = useState({
        totalSimulados: 0,
        averageScore: 0,
        totalTime: 0
    });

    useEffect(() => {
        if (user) {
            fetchUserData();
        }
    }, [user]);

    const fetchUserData = async () => {
        // Obter Perfil
        const { data: pData } = await supabase
            .from('profiles')
            .select('*')
            .eq('id', user.id)
            .single();
        if (pData) setProfile(pData);

        // Obter Histórico de Tentativas para as estatísticas
        const { data: attempts } = await supabase
            .from('attempts')
            .select('*')
            .eq('student_id', user.id);

        if (attempts && attempts.length > 0) {
            const total = attempts.length;
            const avg = attempts.reduce((acc, curr) => acc + Number(curr.score), 0) / total;
            const totalT = attempts.reduce((acc, curr) => acc + Number(curr.time_spent), 0);

            setStats({
                totalSimulados: total,
                averageScore: avg.toFixed(1),
                totalTime: Math.round(totalT / 60) // em minutos
            });
        }
    };

    const handleLogout = async () => {
        await signOut();
        navigate('/login');
    };

    return (
        <div className="student-dashboard">
            <header className="dash-header">
                <div className="d-container">
                    <a href="/" className="dash-logo-link" style={{ display: 'flex', alignItems: 'center', gap: '10px', textDecoration: 'none' }}>
                        <img src="/logo-beto-banco.jpg" alt="Beto Banco" className="dash-logo" style={{ height: '40px', borderRadius: '8px' }} />
                        <span style={{ color: 'var(--gold)', fontWeight: 'bold', fontSize: '20px' }}>Beto Banco</span>
                    </a>
                    <div className="dash-user">
                        <div className="user-text">
                            <span className="dash-greeting">Bem-vindo(a),</span>
                            <span className="dash-name">{profile?.full_name || 'Aluno'}</span>
                        </div>
                        <div className="user-avatar-circle">
                            {profile?.full_name?.charAt(0).toUpperCase() || 'A'}
                        </div>
                        <button onClick={handleLogout} className="dash-logout" title="Sair">
                            <LogOut size={20} />
                        </button>
                    </div>
                </div>
            </header>

            <main className="dash-main d-container">
                <section className="dash-hero">
                    <div className="hero-content">
                        <h1>Sua aprovação no banco está mais próxima.</h1>
                        <p>Pratique com questões reais de concursos anteriores e acompanhe seu desempenho.</p>

                        <div className="hero-actions">
                            <button className="btn-play-simulado" onClick={() => navigate('/simulado')}>
                                <Play fill="currentColor" size={20} />
                                INICIAR SIMULADO GERAL
                            </button>
                        </div>
                    </div>
                    <div className="hero-visual">
                        <img src="/images/professor-lucas.png" alt="Professor" className="hero-prof" />
                    </div>
                </section>

                <section className="dash-stats">
                    <h2>Meu Desempenho Global</h2>
                    <div className="stats-cards">
                        <div className="s-card">
                            <div className="s-icon purple"><Award size={24} /></div>
                            <div className="s-data">
                                <span className="s-val">{stats.totalSimulados}</span>
                                <span className="s-label">Simulados Feitos</span>
                            </div>
                        </div>

                        <div className="s-card">
                            <div className="s-icon green"><TrendingUp size={24} /></div>
                            <div className="s-data">
                                <span className="s-val">{stats.averageScore}%</span>
                                <span className="s-label">Média de Acertos</span>
                            </div>
                        </div>

                        <div className="s-card">
                            <div className="s-icon orange"><Clock size={24} /></div>
                            <div className="s-data">
                                <span className="s-val">{stats.totalTime}m</span>
                                <span className="s-label">Tempo de Prática</span>
                            </div>
                        </div>
                    </div>
                </section>

                <section className="dash-history">
                    <h2>Últimos Resultados</h2>
                    <div className="history-empty">
                        <p>Você ainda não realizou simulados na nova plataforma.</p>
                        <button className="btn-link" onClick={() => navigate('/simulado')}>Fazer primeiro simulado</button>
                    </div>
                </section>
            </main>
        </div>
    );
};

export default Dashboard;
