import React from 'react';
import './ProfessorSection.css';

const ProfessorSection = () => {
    return (
        <section className="professor-section" id="professor">
            <div className="container">
                <div className="professor-layout">
                    <div className="professor-visual">
                        <div className="professor-image-frame">
                            <div className="frame-corner top-left"></div>
                            <div className="frame-corner bottom-right"></div>
                            <img
                                src="/images/professor/prof-betao.png"
                                alt="Professor Beto Fernandes"
                                className="img-fluid"
                            />
                            <div className="professor-name-label">PROF. BETO FERNANDES</div>
                        </div>
                    </div>

                    <div className="professor-content">
                        <h2 className="section-title">
                            CONHEÇA QUEM VAI GUIAR SUA <span className="text-gold">APROVAÇÃO</span>
                        </h2>
                        <div className="professor-stats">
                            <div className="stat-item">
                                <span className="stat-number">25+</span>
                                <span className="stat-text">Anos de Experiência</span>
                            </div>
                            <div className="stat-item">
                                <span className="stat-number">+1.000</span>
                                <span className="stat-text">Aprovações Orientadas</span>
                            </div>
                            <div className="stat-item">
                                <span className="stat-number">13</span>
                                <span className="stat-text">Aprovações Pessoais</span>
                            </div>
                        </div>

                        <div className="professor-bio">
                            <p>
                                Professor Beto Fernandes possui mais de duas décadas de experiência em concursos públicos, focando na aprovação estratégica de seus alunos.
                            </p>
                            <p>
                                Com vasta experiência em <strong>Conhecimentos Bancários, Direito Constitucional, Direito Previdenciário e Legislação Educacional</strong>, ele transforma a complexidade dos editais em um passo a passo simples e executável.
                            </p>
                            <blockquote className="professor-quote">
                                "Desistir dos seus sonhos não é uma opção. Sua aprovação começa aqui, com um passo de cada vez."
                            </blockquote>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    );
};

export default ProfessorSection;
