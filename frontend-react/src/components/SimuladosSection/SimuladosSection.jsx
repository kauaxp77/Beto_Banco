import React, { useState } from 'react';
import { PlayCircle, Target, CheckCircle2 } from 'lucide-react';
import './SimuladosSection.css';

const simuladosData = {
    'avancado': [
        {
            id: 1,
            concurso: "Caixa Econômica Federal 2026",
            title: "Simulado Nacional de Diagnóstico",
            questions: 15,
            level: "Avançado",
        }
    ],
    'intermediario': [
        {
            id: 2,
            concurso: "Banco do Brasil",
            title: "Simulado Foco em Conhecimentos Específicos",
            questions: 15,
            level: "Intermediário",
        }
    ],
    'basico': [
        {
            id: 3,
            concurso: "BNDES",
            title: "Simulado Geral - Conhecimentos Básicos",
            questions: 15,
            level: "Básico",
        }
    ]
};

const SimuladosSection = () => {
    const [activeTab, setActiveTab] = useState('avancado');

    const handleTabChange = (tab) => {
        setActiveTab(tab);
    };

    return (
        <section className="simulados-section" id="simulados">
            <div className="container">
                <div className="simulados-layout">
                    <div className="simulados-intro">
                        <h2 className="section-title">
                            TREINE COMO SE FOSSE O <span className="text-gold">DIA DA PROVA</span>
                        </h2>
                        <p className="simulados-description">
                            Metodologia focada na prática. Nossos simulados são desenhados para reproduzir o grau de dificuldade e o estilo de cobrança das principais bancas examinadoras.
                        </p>

                        <ul className="simulados-benefits">
                            <li>
                                <CheckCircle2 className="benefit-icon" size={20} />
                                <span>Questões inéditas baseadas nos últimos editais</span>
                            </li>
                            <li>
                                <CheckCircle2 className="benefit-icon" size={20} />
                                <span>Gabarito comentado pelo Professor Beto Fernandes</span>
                            </li>
                            <li>
                                <CheckCircle2 className="benefit-icon" size={20} />
                                <span>Análise de desempenho e pontos de melhoria</span>
                            </li>
                        </ul>
                    </div>

                    <div className="simulados-panel">
                        {/* Tabs Navigation */}
                        <div className="simulados-tabs">
                            <button
                                className={`simulados-tab-btn ${activeTab === 'avancado' ? 'active' : ''}`}
                                onClick={() => handleTabChange('avancado')}
                            >
                                Nível Avançado
                            </button>
                            <button
                                className={`simulados-tab-btn ${activeTab === 'intermediario' ? 'active' : ''}`}
                                onClick={() => handleTabChange('intermediario')}
                            >
                                Nível Intermediário
                            </button>
                            <button
                                className={`simulados-tab-btn ${activeTab === 'basico' ? 'active' : ''}`}
                                onClick={() => handleTabChange('basico')}
                            >
                                Nível Básico
                            </button>
                        </div>

                        {/* Tabs Content */}
                        <div className="simulados-list">
                            {simuladosData[activeTab].map(simulado => (
                                <div key={simulado.id} className="simulado-card">
                                    <div className="simulado-header">
                                        <span className="simulado-concurso">{simulado.concurso}</span>
                                        <span className={`simulado-level level-${simulado.level.toLowerCase().replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u')}`}>
                                            {simulado.level}
                                        </span>
                                    </div>

                                    <h3 className="simulado-title">{simulado.title}</h3>

                                    <div className="simulado-footer">
                                        <div className="simulado-meta">
                                            <Target size={16} />
                                            <span>{simulado.questions} Questões</span>
                                        </div>

                                        <a href={`/simulado.html?level=${activeTab}&reset=1`} className="simulado-action">
                                            FAZER SIMULADO <PlayCircle size={18} />
                                        </a>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        </section>
    );
};

export default SimuladosSection;
