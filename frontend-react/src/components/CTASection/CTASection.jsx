import React from 'react';
import { ArrowRight, Shield } from 'lucide-react';
import './CTASection.css';

const CTASection = ({ checkoutLink, whatsappLink }) => {
    return (
        <section className="cta-section">
            <div className="container">
                <div className="cta-card">
                    <div className="cta-content text-center">
                        <h2 className="cta-title">
                            SEU PRÓXIMO PASSO PODE COMEÇAR <span className="text-gold">AGORA.</span>
                        </h2>
                        <div className="price-tag">
                            <span className="price-small">12x de</span>
                            <span className="price-big">R$ 49,90</span>
                        </div>

                        <a href="/login?redirect=checkout" className="btn-primary-glow btn-large">
                            GARANTIR MINHA VAGA AGORA
                        </a>
                        <span className="guarantee-text">
                            <Shield size={16} /> 7 Dias de Garantia Incondicional
                        </span>
                        <p className="cta-description">
                            Não adie mais o seu futuro. Conheça nossos cursos, simulados e materiais disponíveis e comece a estudar com quem entende de aprovação.
                        </p>
                        <a href="/login" className="btn-primary cta-button">
                            COMEÇAR MINHA PREPARAÇÃO <ArrowRight size={24} />
                        </a>
                    </div>
                </div>
            </div>
        </section>
    );
};

export default CTASection;
