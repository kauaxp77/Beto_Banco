import React, { useState, useEffect } from 'react';
import { supabase } from '../../api/supabase';
import { Save, RefreshCw } from 'lucide-react';
import toast from 'react-hot-toast';
import './LandingPageCMS.css';

const LandingPageCMS = () => {
    const [settings, setSettings] = useState({});
    const [originalSettings, setOriginalSettings] = useState({});
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        fetchSettings();
    }, []);

    const fetchSettings = async () => {
        setLoading(true);
        const { data, error } = await supabase
            .from('site_settings')
            .select('*');

        if (data) {
            const settingsMap = {};
            data.forEach(item => {
                settingsMap[item.setting_key] = {
                    value: item.setting_value,
                    description: item.description
                };
            });
            setSettings(settingsMap);
            setOriginalSettings(settingsMap);
        }
        setLoading(false);
    };

    const handleValueChange = (key, newValue) => {
        setSettings(prev => ({
            ...prev,
            [key]: {
                ...prev[key],
                value: newValue
            }
        }));
    };

    const handleSave = async (key) => {
        setSaving(true);
        const { error } = await supabase
            .from('site_settings')
            .update({
                setting_value: settings[key].value,
                updated_at: new Date()
            })
            .eq('setting_key', key);

        if (error) {
            toast.error('Erro ao salvar: ' + error.message);
        } else {
            // Update original to match saved
            setOriginalSettings(prev => ({
                ...prev,
                [key]: { ...settings[key] }
            }));
            toast.success('Salvo com sucesso!');
        }
        setSaving(false);
    };

    const isChanged = (key) => {
        return settings[key]?.value !== originalSettings[key]?.value;
    };

    if (loading) return <div className="cms-loading"><RefreshCw className="spin" /> Carregando CMS... (Verifique se executou a v5_site_settings.sql)</div>;

    const renderKeyLabel = (key) => {
        const labels = {
            hero_headline: "Headline Principal (Capa)",
            hero_subheadline: "Subtítulo de Apoio",
            checkout_link: "Link GERAL de Compra",
            whatsapp_number: "WhatsApp Oficial de Contato"
        };
        return labels[key] || key;
    };

    return (
        <div className="lp-cms">
            <div className="cms-header">
                <div>
                    <h3>Landing Page CMS</h3>
                    <p>Altere os textos globais e links de vendas da Landing Page em tempo real.</p>
                </div>
            </div>

            {Object.keys(settings).length === 0 ? (
                <div className="cms-empty">
                    <p>Nenhuma configuração localizada. Você precisa rodar o script SQL `v5_site_settings.sql` primeiro.</p>
                </div>
            ) : (
                <div className="cms-grid">
                    {Object.keys(settings).sort().map(key => (
                        <div key={key} className={`cms-card ${isChanged(key) ? 'has-changes' : ''}`}>
                            <div className="cms-card-header">
                                <span className="cms-key-badge">{key}</span>
                                <h4>{renderKeyLabel(key)}</h4>
                                <p className="cms-desc">{settings[key].description}</p>
                            </div>

                            <div className="cms-card-body">
                                {key.includes('headline') ? (
                                    <textarea
                                        rows="3"
                                        value={settings[key].value}
                                        onChange={(e) => handleValueChange(key, e.target.value)}
                                        className="cms-input"
                                    />
                                ) : (
                                    <input
                                        type="text"
                                        value={settings[key].value}
                                        onChange={(e) => handleValueChange(key, e.target.value)}
                                        className="cms-input"
                                    />
                                )}
                            </div>

                            <div className="cms-card-footer">
                                {isChanged(key) && <span className="unsaved-badge">Não salvo</span>}
                                <button
                                    className={`btn-save ${isChanged(key) ? 'active' : ''}`}
                                    onClick={() => handleSave(key)}
                                    disabled={!isChanged(key) || saving}
                                >
                                    <Save size={16} /> Salvar Alteração
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default LandingPageCMS;
