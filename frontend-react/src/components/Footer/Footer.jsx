import React from 'react';
import { Mail, ChevronRight } from 'lucide-react';
import './Footer.css';

const Footer = ({ whatsapp }) => {
    const currentYear = new Date().getFullYear();
    const whatsappUrl = `https://wa.me/${whatsapp || '556199999999'}`;

    return (
        <footer className="footer">
            <div className="container">
                <div className="footer-top">
                    <div className="footer-brand">
                        <a href="#inicio" className="footer-logo">
                            <img
                                src="/logo-beto-banco.jpg"
                                alt="Beto Banco"
                                className="footer-logo"
                                style={{ mixBlendMode: 'screen', height: '50px' }}
                            />
                        </a>
                        <p className="footer-bio">
                            Plataforma premium para quem busca aprovação no mercado bancário. Método desenvolvido pelo Professor Beto Fernandes.
                        </p>
                    </div>

                    <div className="footer-links">
                        <h4 className="footer-heading">Navegação</h4>
                        <ul>
                            <li><a href="#inicio"><ChevronRight size={14} className="link-icon" /> Início</a></li>
                            <li><a href="/bancos.html"><ChevronRight size={14} className="link-icon" /> Cursos</a></li>
                            <li><a href="#simulados"><ChevronRight size={14} className="link-icon" /> Simulados</a></li>
                            <li><a href="#professor"><ChevronRight size={14} className="link-icon" /> O Professor</a></li>
                        </ul>
                    </div>

                    <div className="footer-links">
                        <h4 className="footer-heading">Suporte</h4>
                        <ul>
                            <li><a href="#faq"><ChevronRight size={14} className="link-icon" /> Dúvidas Frequentes</a></li>
                            <li><a href="#depoimentos"><ChevronRight size={14} className="link-icon" /> Depoimentos</a></li>
                            <li><a href="#"><ChevronRight size={14} className="link-icon" /> Contato</a></li>
                            <li><a href="#"><ChevronRight size={14} className="link-icon" /> Termos de Uso</a></li>
                        </ul>
                    </div>
                </div>

                <div className="footer-bottom">
                    <div className="copyright">
                        &copy; {new Date().getFullYear()} Aprovação Passo a Passo. Todos os direitos reservados.
                    </div>
                    <div className="developed-by">
                        Professor Beto Fernandes
                    </div>
                </div>
            </div>
        </footer>
    );
};

export default Footer;
