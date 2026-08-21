import React from 'react';
import { ArrowRight, BookOpen } from 'lucide-react';
import './Hero.css';

const Hero = ({ headline, subheadline, checkoutLink }) => {
    return (
        <section className="hero" id="inicio">
            <div className="hero-background">
                <div className="hero-blur-circle primary"></div>
                <div className="hero-blur-circle secondary"></div>
            </div>

            <div className="container hero-container">
                <div className="hero-content">
                    <div className="hero-badge">
                        <span className="badge-dot"></span>
                        Concursos Bancários 2026
                    </div>

                    <h1 className="hero-title">
                        {headline || 'SUA APROVAÇÃO COMEÇA COM UM PASSO DE CADA VEZ.'}
                    </h1>

                    <p className="hero-description">
                        {subheadline || 'Prepare-se para concursos bancários de forma objetiva, com cursos, simulados e materiais desenvolvidos para quem quer estudar com direção e conquistar a aprovação.'}
                    </p>

                    <div className="hero-actions">
                        <a href="/login" className="btn-primary">
                            QUERO ME PREPARAR <ArrowRight size={20} />
                        </a>
                        <a href="/login" className="btn-secondary">
                            VER CURSOS <BookOpen size={20} />
                        </a>
                    </div>

                    <div className="hero-trust">
                        <div className="trust-avatars">
                            {/* Decorative elements representing students */}
                            <div className="avatar"></div>
                            <div className="avatar"></div>
                            <div className="avatar"></div>
                            <div className="avatar"></div>
                        </div>
                        <p>Junte-se a milhares de alunos aprovados.</p>
                    </div>
                </div>

                <div className="hero-visual">
                    <div className="hero-image-wrapper">
                        <div className="glow-effect"></div>
                        <img
                            src="/images/professor/prof-betao.png"
                            alt="Professor Beto Fernandes"
                            className="professor-img"
                        />

                        {/* Floating Elements / Micro-interactions */}
                        <div className="floating-card metric-card">
                            <span className="metric-value">25+</span>
                            <span className="metric-label">Anos de Experiência</span>
                        </div>

                        <div className="floating-card approval-card">
                            <div className="approval-icon">🏆</div>
                            <div>
                                <span className="approval-title">Mentoria Premium</span>
                                <span className="approval-subtitle">Foco Total</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    );
};

export default Hero;
