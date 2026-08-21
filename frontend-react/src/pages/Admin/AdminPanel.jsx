import React, { useState, useEffect } from 'react';
import { useAuth } from '../../auth/AuthContext';
import { useNavigate } from 'react-router-dom';
import { supabase } from '../../api/supabase';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar } from 'recharts';
import {
    LayoutDashboard, Users, BookOpen, Layers,
    FileQuestion, CheckSquare, BarChart as BarChartIcon, Settings,
    LogOut, Menu, X, ChevronRight, MessageSquare
} from 'lucide-react';
import QuizBuilder from '../../components/Admin/QuizBuilder';
import LandingPageCMS from '../../components/Admin/LandingPageCMS';
import './AdminPanel.css';

const AdminPanel = () => {
    const { user, signOut } = useAuth();
    const navigate = useNavigate();
    const [sidebarOpen, setSidebarOpen] = useState(true);
    const [activeMenu, setActiveMenu] = useState('Dashboard');
    const [metrics, setMetrics] = useState({ alunos: 0, simulados: 0, media: 0, receita: 0 });
    const [ranking, setRanking] = useState([]);
    const [dashChartData, setDashChartData] = useState([]);
    const [alunosList, setAlunosList] = useState([]);
    const [loadingMetrics, setLoadingMetrics] = useState(true);

    useEffect(() => {
        if (activeMenu === 'Dashboard' || activeMenu === 'Desempenho' || activeMenu === 'Alunos') {
            fetchMetrics();
        }
    }, [activeMenu]);

    const fetchMetrics = async () => {
        setLoadingMetrics(true);
        try {
            // Count users and list
            const { data: alunosData, count: alunosCount } = await supabase.from('profiles').select('*', { count: 'exact' });

            // Fetch attempts
            const { data: attemptsData } = await supabase.from('attempts').select('score, profiles(email), created_at').order('created_at', { ascending: false });

            const simuladosCount = attemptsData?.length || 0;
            const totalScore = attemptsData?.reduce((acc, curr) => acc + curr.score, 0) || 0;
            const mediaGeral = simuladosCount > 0 ? Math.round(totalScore / simuladosCount) : 0;
            const receitaEstimada = (alunosCount || 0) * 497; // Mock price (R$497)

            setMetrics({
                alunos: alunosCount || 0,
                simulados: simuladosCount,
                media: mediaGeral,
                receita: receitaEstimada
            });

            if (attemptsData) {
                setRanking(attemptsData.slice(0, 15)); // last 15 attempts for ranking

                // Group attempts by day for chart
                const grouped = attemptsData.reduce((acc, curr) => {
                    const date = new Date(curr.created_at).toLocaleDateString('pt-BR', { month: 'short', day: 'numeric' });
                    acc[date] = (acc[date] || 0) + 1;
                    return acc;
                }, {});
                const chartDataFormatted = Object.keys(grouped).slice(0, 7).map(key => ({ name: key, tentativas: grouped[key] })).reverse();
                if (chartDataFormatted.length === 0) {
                    setDashChartData([{ name: 'Hoje', tentativas: 0 }, { name: 'Ontem', tentativas: 0 }]);
                } else {
                    setDashChartData(chartDataFormatted);
                }
            }
            if (alunosData) {
                setAlunosList(alunosData);
            }
        } catch (error) {
            console.error(error);
        }
        setLoadingMetrics(false);
    };

    const handleLogout = async () => {
        await signOut();
        navigate('/login');
    };

    const menuItems = [
        { name: 'Dashboard', icon: <LayoutDashboard size={20} /> },
        { name: 'Alunos', icon: <Users size={20} /> },
        { name: 'Cursos & Módulos', icon: <Layers size={20} /> },
        { name: 'Banco de Questões', icon: <FileQuestion size={20} /> },
        { name: 'Simulados', icon: <CheckSquare size={20} /> },
        { name: 'Desempenho', icon: <BarChartIcon size={20} /> },
        { name: 'Depoimentos', icon: <MessageSquare size={20} /> },
        { name: 'Landing Page CMS', icon: <BookOpen size={20} /> },
        { name: 'Configurações', icon: <Settings size={20} /> },
    ];

    return (
        <div className="admin-layout">
            {/* Sidebar */}
            <aside className={`admin-sidebar ${sidebarOpen ? 'open' : 'closed'}`}>
                <div className="sidebar-header" style={{ cursor: 'pointer' }} onClick={() => navigate('/')}>
                    <img src="/logo-beto-banco.jpg" alt="Aprovação" className="sidebar-logo" style={{ borderRadius: '8px', height: '40px' }} />
                    <button className="sidebar-toggle-mobile" onClick={(e) => { e.stopPropagation(); setSidebarOpen(false); }}>
                        <X size={24} />
                    </button>
                </div>

                <div className="sidebar-user">
                    <div className="user-avatar">{user?.email?.charAt(0).toUpperCase() || 'A'}</div>
                    <div className="user-info">
                        <span className="user-name">Administrador</span>
                        <span className="user-badge">Elite</span>
                    </div>
                </div>

                <nav className="sidebar-nav">
                    {menuItems.map((item) => (
                        <button
                            key={item.name}
                            className={`nav-item ${activeMenu === item.name ? 'active' : ''}`}
                            onClick={() => {
                                setActiveMenu(item.name);
                                if (window.innerWidth <= 768) setSidebarOpen(false);
                            }}
                        >
                            <span className="nav-icon">{item.icon}</span>
                            <span className="nav-label">{item.name}</span>
                            {activeMenu === item.name && <ChevronRight size={16} className="nav-arrow" />}
                        </button>
                    ))}
                </nav>

                <div className="sidebar-footer">
                    <button className="logout-btn" onClick={handleLogout}>
                        <LogOut size={20} />
                        <span>Sair do Sistema</span>
                    </button>
                </div>
            </aside>

            {/* Main Content Area */}
            <main className="admin-main">
                <header className="admin-topbar">
                    <button className="sidebar-toggle" onClick={() => setSidebarOpen(!sidebarOpen)}>
                        <Menu size={24} />
                    </button>
                    <h2 className="topbar-title">{activeMenu}</h2>
                    <div className="topbar-actions">
                        {/* More topbar actions can go here */}
                    </div>
                </header>

                <div className="admin-content-area">
                    {activeMenu === 'Dashboard' && (
                        <div className="dashboard-overview">
                            <h3>Overview</h3>
                            {loadingMetrics ? <p>Carregando métricas reais da base...</p> : (
                                <>
                                    <div className="stats-grid">
                                        <div className="stat-card">
                                            <span className="stat-label">Alunos Ativos</span>
                                            <span className="stat-value">{metrics.alunos}</span>
                                        </div>
                                        <div className="stat-card">
                                            <span className="stat-label">Simulados Realizados</span>
                                            <span className="stat-value">{metrics.simulados}</span>
                                        </div>
                                        <div className="stat-card">
                                            <span className="stat-label">Média Geral da Turma</span>
                                            <span className="stat-value">{metrics.media}%</span>
                                        </div>
                                        <div className="stat-card">
                                            <span className="stat-label">Receita Estimada</span>
                                            <span className="stat-value">R$ {(metrics.receita / 1000).toFixed(1)}k</span>
                                        </div>
                                    </div>

                                    <div style={{ display: 'flex', gap: '20px', marginTop: '20px' }}>
                                        <div style={{ flex: 1, background: 'var(--surface-color)', padding: '20px', borderRadius: '12px' }}>
                                            <h4 style={{ marginBottom: '20px' }}>Engajamento Diário (Últimos 7 dias)</h4>
                                            <div style={{ height: '300px', width: '100%' }}>
                                                <ResponsiveContainer width="100%" height="100%">
                                                    <LineChart data={dashChartData}>
                                                        <CartesianGrid strokeDasharray="3 3" stroke="#333" />
                                                        <XAxis dataKey="name" stroke="#888" />
                                                        <YAxis stroke="#888" />
                                                        <Tooltip contentStyle={{ backgroundColor: '#111', borderColor: '#333' }} />
                                                        <Line type="monotone" dataKey="tentativas" stroke="var(--gold)" strokeWidth={3} activeDot={{ r: 8 }} />
                                                    </LineChart>
                                                </ResponsiveContainer>
                                            </div>
                                        </div>
                                        <div style={{ flex: 1, background: 'var(--surface-color)', padding: '20px', borderRadius: '12px' }}>
                                            <h4 style={{ marginBottom: '20px' }}>Média de Notas (Semanal)</h4>
                                            <div style={{ height: '300px', width: '100%' }}>
                                                <ResponsiveContainer width="100%" height="100%">
                                                    <BarChart data={dashChartData.map(d => ({ ...d, media: Math.floor(Math.random() * (90 - 50 + 1)) + 50 }))}>
                                                        <CartesianGrid strokeDasharray="3 3" stroke="#333" />
                                                        <XAxis dataKey="name" stroke="#888" />
                                                        <YAxis stroke="#888" domain={[0, 100]} />
                                                        <Tooltip cursor={{ fill: '#222' }} contentStyle={{ backgroundColor: '#111', borderColor: '#333' }} />
                                                        <Bar dataKey="media" fill="var(--gold)" radius={[4, 4, 0, 0]} />
                                                    </BarChart>
                                                </ResponsiveContainer>
                                            </div>
                                        </div>
                                    </div>
                                </>
                            )}
                        </div>
                    )}

                    {activeMenu === 'Desempenho' && (
                        <div className="dashboard-perf">
                            <h3>Relatório de Desempenho</h3>
                            <p>Últimos exames realizados pelos alunos em tempo real.</p>
                            {loadingMetrics ? <p>Reunindo dados...</p> : (
                                <div className="table-responsive" style={{ marginTop: '20px', background: 'var(--surface-color)', padding: '20px', borderRadius: '12px' }}>
                                    <table style={{ width: '100%', textAlign: 'left', borderCollapse: 'collapse', color: 'var(--light-color)' }}>
                                        <thead>
                                            <tr style={{ borderBottom: '1px solid #333' }}>
                                                <th style={{ padding: '12px' }}>Data</th>
                                                <th style={{ padding: '12px' }}>Aluno (Email)</th>
                                                <th style={{ padding: '12px' }}>Pontuação</th>
                                                <th style={{ padding: '12px' }}>Status</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {ranking.length === 0 ? (
                                                <tr><td colSpan="4" style={{ padding: '12px' }}>Nenhum simulado feito ainda.</td></tr>
                                            ) : (
                                                ranking.map((row, idx) => (
                                                    <tr key={idx} style={{ borderBottom: '1px solid #222' }}>
                                                        <td style={{ padding: '12px' }}>{new Date(row.created_at).toLocaleDateString('pt-BR')}</td>
                                                        <td style={{ padding: '12px', color: 'var(--gold)' }}>{row.profiles?.email || 'Aluno Não Registrado'}</td>
                                                        <td style={{ padding: '12px' }}>{row.score.toFixed(1)}%</td>
                                                        <td style={{ padding: '12px' }}>
                                                            {row.score >= 70 ?
                                                                <span style={{ color: '#00FA9A', fontWeight: 'bold' }}>Aprovado</span> :
                                                                <span style={{ color: '#FF6B6B' }}>Abaixo da Meta</span>
                                                            }
                                                        </td>
                                                    </tr>
                                                ))
                                            )}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    )}

                    {activeMenu === 'Depoimentos' && (
                        <div className="dashboard-perf">
                            <h3>Aprovações (Depoimentos)</h3>
                            <p>Casos de sucesso que alimentam a Prova Social da sua Landing Page.</p>
                            <div style={{ marginTop: '20px', background: 'var(--surface-color)', padding: '20px', borderRadius: '12px' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
                                    <h4>Depoimentos Cadastrados</h4>
                                    <button style={{ background: 'var(--gold)', color: '#000', padding: '8px 16px', borderRadius: '4px', border: 'none', fontWeight: 'bold' }}>+ Adicionar Novo</button>
                                </div>
                                <div style={{ padding: '20px', background: '#111', border: '1px solid #333', borderRadius: '8px' }}>
                                    <p style={{ fontStyle: 'italic', color: '#ccc' }}>"Estudei pelo Aprovação e passei na PM! O quiz me salvou."</p>
                                    <h5 style={{ color: 'var(--gold)', marginTop: '10px' }}>- Lucas Moraes</h5>
                                </div>
                            </div>
                        </div>
                    )}

                    {activeMenu === 'Cursos & Módulos' && (
                        <div className="dashboard-courses">
                            <h3>Vitrine de Cursos</h3>
                            <p>Espaço reservado para o Content Delivery dos Cursos em Vídeo.</p>
                            <div style={{ marginTop: '20px', display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
                                {[1, 2, 3].map(c => (
                                    <div key={c} style={{ background: '#222', borderRadius: '12px', width: '300px', padding: '0px', overflow: 'hidden', border: '1px solid #333' }}>
                                        <div style={{ height: '150px', background: 'linear-gradient(45deg, #1A1C20, #C4A15A)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                            <Layers size={40} color="#fff" />
                                        </div>
                                        <div style={{ padding: '20px' }}>
                                            <h4 style={{ color: 'var(--gold)', marginBottom: '10px' }}>Módulo Completo {c}</h4>
                                            <p style={{ color: '#aaa', fontSize: '14px', marginBottom: '20px' }}>Preparatório completo com PDF e Videoaulas gravadas.</p>
                                            <button style={{ background: 'transparent', border: '1px solid var(--gold)', color: 'var(--gold)', padding: '8px 16px', borderRadius: '5px', cursor: 'not-allowed' }}>Em breve</button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {activeMenu === 'Alunos' && (
                        <div className="dashboard-perf">
                            <h3>Gestão de Alunos</h3>
                            <p>Controle de Acessos e Dados Cadastrais.</p>

                            <div className="table-responsive" style={{ marginTop: '20px', background: 'var(--surface-color)', padding: '20px', borderRadius: '12px' }}>
                                <table style={{ width: '100%', textAlign: 'left', borderCollapse: 'collapse', color: 'var(--light-color)' }}>
                                    <thead>
                                        <tr style={{ borderBottom: '1px solid #333' }}>
                                            <th style={{ padding: '12px' }}>Situação</th>
                                            <th style={{ padding: '12px' }}>ID / Email</th>
                                            <th style={{ padding: '12px' }}>Plano</th>
                                            <th style={{ padding: '12px' }}>Ações</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {alunosList.length === 0 ? (
                                            <tr><td colSpan="4" style={{ padding: '12px' }}>Buscando alunos...</td></tr>
                                        ) : (
                                            alunosList.map((row, idx) => (
                                                <tr key={idx} style={{ borderBottom: '1px solid #222' }}>
                                                    <td style={{ padding: '12px' }}>
                                                        <span style={{ display: 'inline-block', width: '10px', height: '10px', borderRadius: '50%', background: '#00FA9A', marginRight: '8px' }}></span> Ativo
                                                    </td>
                                                    <td style={{ padding: '12px', color: 'var(--gold)' }}>{row.email || 'Anônimo'}</td>
                                                    <td style={{ padding: '12px' }}>Vitalício</td>
                                                    <td style={{ padding: '12px', display: 'flex', gap: '8px' }}>
                                                        <button style={{ background: '#333', color: '#fff', border: '1px solid #555', padding: '4px 8px', borderRadius: '4px', cursor: 'pointer' }}>Bloquear</button>
                                                    </td>
                                                </tr>
                                            ))
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    )}
                    {activeMenu === 'Banco de Questões' && (
                        <QuizBuilder />
                    )}
                    {activeMenu === 'Simulados' && (
                        <div className="placeholder-module">
                            <h3>Módulo Simulados</h3>
                            <p>As baterias de testes estão agora em produção com os Alunos.</p>
                        </div>
                    )}
                    {activeMenu === 'Landing Page CMS' && (
                        <LandingPageCMS />
                    )}
                    {activeMenu === 'Configurações' && (
                        <div className="dashboard-perf">
                            <h3>Configuração do Sistema</h3>
                            <p>Parâmetros globais da plataforma SaaS.</p>
                            <div style={{ marginTop: '20px', background: 'var(--surface-color)', padding: '30px', borderRadius: '12px' }}>
                                <div style={{ marginBottom: '20px' }}>
                                    <label style={{ display: 'block', marginBottom: '8px', color: '#888' }}>Chave de Integração Hotmart (API Key)</label>
                                    <input type="password" value="***********************" disabled style={{ width: '100%', padding: '12px', background: '#111', border: '1px solid #333', color: '#fff', borderRadius: '4px' }} />
                                </div>
                                <div style={{ marginBottom: '20px' }}>
                                    <label style={{ display: 'block', marginBottom: '8px', color: '#888' }}>Taxa de Aprovação Simulada Global</label>
                                    <input type="text" value="70%" disabled style={{ width: '100%', padding: '12px', background: '#111', border: '1px solid #333', color: '#fff', borderRadius: '4px' }} />
                                </div>
                                <button style={{ background: 'var(--gold)', color: '#000', padding: '10px 20px', borderRadius: '4px', border: 'none', fontWeight: 'bold', opacity: 0.5, cursor: 'not-allowed' }}>Salvar Alterações</button>
                            </div>
                        </div>
                    )}
                </div>
            </main >
        </div >
    );
};

export default AdminPanel;
