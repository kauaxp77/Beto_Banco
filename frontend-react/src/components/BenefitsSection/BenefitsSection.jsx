import React from 'react';
import { Lightbulb, PenTool, Navigation, TrendingUp } from 'lucide-react';
import './BenefitsSection.css';

const BenefitsSection = () => {
    const benefits = [
        {
            icon: <Lightbulb size={32} />,
            title: "Método Exclusivo",
            description: "Estudo organizado e direcionado. Aprenda a estudar com inteligência e maximize seu tempo."
        },
        {
            icon: <PenTool size={32} />,
            title: "Prática Constante",
            description: "Simulados e milhares de questões comentadas para reforçar o conteúdo e treinar para a prova."
        },
        {
            icon: <Navigation size={32} />,
            title: "Direção Clara",
            description: "Foco exclusivo nos conteúdos realmente relevantes para concursos bancários do momento."
        },
        {
            icon: <TrendingUp size={32} />,
            title: "Preparação Evolutiva",
            description: "Estrutura pensada para transformar seu tempo de estudo na sua evolução contínua."
        }
    ];

    return (
        <section className="benefits-section" id="diferenciais">
            <div className="container">
                <div className="benefits-header text-center">
                    <h2 className="section-title">
                        POR QUE ESTUDAR COM O <span className="text-gold">PROF. BETO FERNANDES?</span>
                    </h2>
                    <p className="section-subtitle">
                        Conheça os pilares da metodologia que tem guiado milhares de alunos rumo à aprovação bancária.
                    </p>
                </div>

                <div className="benefits-grid">
                    {benefits.map((item, index) => (
                        <div key={index} className="benefit-card">
                            <div className="benefit-icon-wrapper">
                                {item.icon}
                            </div>
                            <h3 className="benefit-title">{item.title}</h3>
                            <p className="benefit-description">{item.description}</p>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
};

export default BenefitsSection;
