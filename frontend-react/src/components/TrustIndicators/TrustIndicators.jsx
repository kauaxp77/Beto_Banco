import React from 'react';
import { Users, BookOpen, Target, Award } from 'lucide-react';
import './TrustIndicators.css';

const TrustIndicators = () => {
    const indicators = [
        {
            icon: <Users size={32} />,
            title: "Milhares de Alunos",
            description: "Preparação validada por quem já alcançou o objetivo."
        },
        {
            icon: <BookOpen size={32} />,
            title: "Cursos Direcionados",
            description: "Conteúdo focado no que realmente cai nas provas bancárias."
        },
        {
            icon: <Target size={32} />,
            title: "Simulados Práticos",
            description: "Treinamento intensivo com questões atualizadas."
        },
        {
            icon: <Award size={32} />,
            title: "Metodologia Comprovada",
            description: "Estudo organizado passo a passo para sua aprovação."
        }
    ];

    return (
        <section className="trust-section">
            <div className="container">
                <div className="trust-grid">
                    {indicators.map((item, index) => (
                        <div key={index} className="trust-card">
                            <div className="trust-icon">{item.icon}</div>
                            <h3 className="trust-title">{item.title}</h3>
                            <p className="trust-description">{item.description}</p>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
};

export default TrustIndicators;
