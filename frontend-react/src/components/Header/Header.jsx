import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Menu, X, User } from 'lucide-react';
import './Header.css';

const Header = ({ whatsapp }) => {
    const [isScrolled, setIsScrolled] = useState(false);
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
    const navigate = useNavigate();
    const whatsappUrl = `https://wa.me/${whatsapp || '556199999999'}`;

    useEffect(() => {
        const handleScroll = () => {
            setIsScrolled(window.scrollY > 50);
        };

        window.addEventListener('scroll', handleScroll);
        return () => window.removeEventListener('scroll', handleScroll);
    }, []);

    const navLinks = [
        { name: 'Início', href: '#inicio' },
        { name: 'Cursos', href: '/bancos.html' },
        { name: 'Simulados', href: '#simulados' },
        { name: 'Quem é o Professor', href: '#professor' },
        { name: 'Depoimentos', href: '#depoimentos' },
    ];

    const handleLogoClick = (e) => {
        e.preventDefault();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    return (
        <header className={`header ${isScrolled ? 'scrolled' : ''}`}>
            <div className="container header-container">
                <a href="#inicio" className="logo" onClick={handleLogoClick} style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <img src="/logo-beto-banco.jpg" alt="Beto Banco" style={{ height: '40px', borderRadius: '8px' }} />
                    <span style={{ color: 'var(--gold)', fontWeight: 'bold', fontSize: '20px' }}>Beto Banco</span>
                </a>

                {/* Desktop Navigation */}
                <nav className="header-nav-desktop">
                    <ul className="header-nav-list">
                        {navLinks.map((link) => (
                            <li key={link.name}>
                                <a href={link.href} className="header-nav-link">{link.name}</a>
                            </li>
                        ))}
                    </ul>
                </nav>

                <div className="header-actions">
                    <button onClick={() => navigate('/login')} className="btn-login" style={{ display: 'flex', alignItems: 'center', gap: '8px', background: 'transparent', border: '1px solid var(--gold)', color: 'var(--gold)', padding: '8px 16px', borderRadius: '50px', cursor: 'pointer', fontWeight: 'bold' }}>
                        <User size={16} /> Entrar
                    </button>
                    <a href={whatsappUrl} className="btn-header">FALE CONOSCO</a>

                    <button
                        className="mobile-menu-toggle"
                        onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                        aria-label="Abrir menu"
                    >
                        {isMobileMenuOpen ? <X size={28} /> : <Menu size={28} />}
                    </button>
                </div>
            </div>

            {/* Mobile Navigation */}
            <div className={`mobile-menu ${isMobileMenuOpen ? 'open' : ''}`}>
                <ul className="mobile-nav-list">
                    {navLinks.map((link) => (
                        <li key={link.name}>
                            <a
                                href={link.href}
                                className="mobile-nav-link"
                                onClick={() => setIsMobileMenuOpen(false)}
                            >
                                {link.name}
                            </a>
                        </li>
                    ))}
                    <li>
                        <button
                            className="btn-primary mobile-cta"
                            onClick={() => { navigate('/login'); setIsMobileMenuOpen(false); }}
                            style={{ width: '100%', marginBottom: '12px', background: 'transparent', border: '1px solid var(--gold)', color: 'var(--gold)' }}
                        >
                            ENTRAR LÁ ÁREA DO ALUNO
                        </button>
                    </li>
                    <li>
                        <a
                            href={whatsappUrl}
                            className="btn-primary mobile-cta"
                            onClick={() => setIsMobileMenuOpen(false)}
                        >
                            FALE CONOSCO
                        </a>
                    </li>
                </ul>
            </div>
        </header>
    );
};

export default Header;
