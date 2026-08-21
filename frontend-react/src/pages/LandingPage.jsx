import React from 'react';
import Header from '../components/Header/Header';
import Hero from '../components/Hero/Hero';
import TrustIndicators from '../components/TrustIndicators/TrustIndicators';
import CoursesSection from '../components/CoursesSection/CoursesSection';
import SimuladosSection from '../components/SimuladosSection/SimuladosSection';
import BenefitsSection from '../components/BenefitsSection/BenefitsSection';
import ProfessorSection from '../components/ProfessorSection/ProfessorSection';
import TestimonialsSection from '../components/TestimonialsSection/TestimonialsSection';
import CTASection from '../components/CTASection/CTASection';
import Footer from '../components/Footer/Footer';
import { supabase } from '../api/supabase';

const LandingPage = () => {
    const [settings, setSettings] = React.useState({});

    React.useEffect(() => {
        const loadSettings = async () => {
            const { data } = await supabase.from('site_settings').select('setting_key, setting_value');
            if (data) {
                const sMap = {};
                data.forEach(item => sMap[item.setting_key] = item.setting_value);
                setSettings(sMap);
            }
        };
        loadSettings();
    }, []);

    return (
        <div className="app-wrapper">
            <Header whatsapp={settings.whatsapp_number} />
            <main>
                <Hero
                    headline={settings.hero_headline}
                    subheadline={settings.hero_subheadline}
                    checkoutLink={settings.checkout_link}
                />
                <TrustIndicators />
                <CoursesSection />
                <SimuladosSection />
                <BenefitsSection />
                <ProfessorSection />
                <TestimonialsSection />
                <CTASection checkoutLink={settings.checkout_link} />
            </main>
            <Footer whatsapp={settings.whatsapp_number} />
        </div>
    );
};

export default LandingPage;
