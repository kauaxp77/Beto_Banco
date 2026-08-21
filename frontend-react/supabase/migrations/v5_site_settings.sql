-- ==========================================
-- APROVAÇÃO PASSO A PASSO - MIGRATION V5 (CMS)
-- ==========================================

-- 1. Create table for dynamic site settings
CREATE TABLE public.site_settings (
    setting_key TEXT PRIMARY KEY,
    setting_value TEXT NOT NULL,
    description TEXT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 2. Configure RLS
ALTER TABLE public.site_settings ENABLE ROW LEVEL SECURITY;

-- Everyone can read settings (for Landing Page render)
CREATE POLICY "Public Read Access on Site Settings" 
ON public.site_settings FOR SELECT TO public USING (true);

-- Only Admins can modify settings
CREATE POLICY "Admin All Access on Site Settings" 
ON public.site_settings FOR ALL TO authenticated 
USING (
  EXISTS (
    SELECT 1 FROM public.profiles WHERE profiles.id = auth.uid() AND profiles.role = 'ADMIN'
  )
);

-- 3. Insert Initial Seeds (Fallbacks)
INSERT INTO public.site_settings (setting_key, setting_value, description) VALUES
('hero_headline', 'Sua aprovação no banco está mais próxima.', 'Título principal na capa do site'),
('hero_subheadline', 'A preparação definitiva para você dominar os editais bancários do Brasil.', 'Subtítulo da capa do site'),
('checkout_link', 'https://pay.kiwify.com.br/xxxxx', 'Link global para botões de compra (Quero Ser Aprovado)'),
('whatsapp_number', '5511999999999', 'Número do WhatsApp sem formatação (Apenas números e código país)');
