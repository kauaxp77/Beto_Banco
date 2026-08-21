import React from 'react';
import './CoursesSection.css';

const courses = [
    {
        id: 1,
        category: "Pacote Completo",
        title: "Caixa Econômica Federal",
        description: "Preparação completa com videoaulas, PDFs e questões focadas no edital.",
        svgLogo: "/images/bancos/caixa.png",
        price: "R$ 497,00",
        promo: "12x R$ 49,70",
        highlight: true,
    },
    {
        id: 2,
        category: "Conhecimentos Específicos",
        title: "Banco do Brasil (Agente Comercial)",
        description: "Aprofundamento focado nos conhecimentos específicos do último edital.",
        svgLogo: "/images/bancos/bb.png",
        price: "R$ 347,00",
        promo: "12x R$ 34,70"
    },
    {
        id: 3,
        category: "Reta Final",
        title: "BNDES 2026",
        description: "Direcionamento estratégico para a reta final de preparação.",
        svgLogo: "/images/bancos/bndes.png",
        price: "R$ 297,00",
        promo: "12x R$ 29,70"
    }
];

const CoursesSection = () => {
    return (
        <section className="courses-section" id="cursos">
            <div className="container">
                <div className="section-header text-center">
                    <h2 className="section-title">ENCONTRE O MATERIAL CERTO PARA SUA <span className="text-gold">PREPARAÇÃO</span></h2>
                    <p className="section-subtitle">
                        Cursos estruturados passo a passo para quem busca a aprovação definitiva nos melhores concursos bancários do país.
                    </p>
                </div>

                <div className="courses-grid">
                    {courses.map(course => (
                        <div key={course.id} className={`course-card ${course.highlight ? 'highlight' : ''}`}>
                            <div className="course-image-wrapper">
                                <div className="course-overlay"></div>
                                <img src={course.svgLogo} alt={course.title} className="course-svg-logo" />
                                <div className="course-category">{course.category}</div>
                            </div>

                            <div className="course-content">
                                <h3 className="course-title">{course.title}</h3>
                                <p className="course-description">{course.description}</p>

                                <div className="course-pricing">
                                    <div className="price-tag">
                                        <span className="price-full">De {course.price} por</span>
                                        <span className="price-promo">{course.promo}</span>
                                    </div>
                                </div>

                                <a href={`#curso-${course.id}`} className="btn-course">
                                    VER CURSO
                                </a>
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
};

export default CoursesSection;
