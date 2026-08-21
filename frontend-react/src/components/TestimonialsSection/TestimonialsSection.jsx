import React from 'react';
import { Quote } from 'lucide-react';
import './TestimonialsSection.css';

const TestimonialsSection = () => {
    // Respecting the rule: "Nunca inventar depoimentos."
    // Left placeholders to show the UI layout correctly.
    const placeholders = [
        {
            id: 1,
            name: "Nome do Aluno",
            role: "Aprovado no Banco do Brasil",
            text: "Espaço reservado para o depoimento real do aluno. O design foi preparado para receber textos variados mantendo o aspecto premium.",
        },
        {
            id: 2,
            name: "Nome do Aluno",
            role: "Aprovada na Caixa",
            text: "Espaço reservado para o depoimento real do aluno. O design foi preparado para receber textos variados mantendo o aspecto premium.",
        },
        {
            id: 3,
            name: "Nome do Aluno",
            role: "Aprovado no BNDES",
            text: "Espaço reservado para o depoimento real do aluno. O design foi preparado para receber textos variados mantendo o aspecto premium.",
        }
    ];

    return (
        <section className="testimonials-section" id="depoimentos">
            <div className="container">
                <div className="section-header text-center">
                    <h2 className="section-title">
                        QUEM ESTUDA COM A GENTE, <span className="text-gold">AVANÇA PASSO A PASSO</span>
                    </h2>
                    <p className="section-subtitle">
                        Conheça as histórias de sucesso de quem decidiu se preparar com direção e hoje colhe os resultados da aprovação.
                    </p>
                </div>

                <div className="testimonials-grid">
                    {placeholders.map((testimonial) => (
                        <div key={testimonial.id} className="testimonial-card">
                            <Quote className="quote-icon" size={32} />
                            <p className="testimonial-text">"{testimonial.text}"</p>

                            <div className="testimonial-author">
                                <div className="author-avatar">
                                    {testimonial.name.charAt(0)}
                                </div>
                                <div className="author-info">
                                    <span className="author-name">{testimonial.name}</span>
                                    <span className="author-role">{testimonial.role}</span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
};

export default TestimonialsSection;
