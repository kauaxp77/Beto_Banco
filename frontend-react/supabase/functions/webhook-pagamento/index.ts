// supabase/functions/webhook-pagamento/index.ts
// Funcão TypeScript Edge disparada sempre que a Hotmart ou Kiwify confirmam um pagamento

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

serve(async (req) => {
    // Configuro cabeçalhos (CORS) caso precise de requests browser, 
    // embora webhooks geralmente sejam server-to-server.
    const headers = { 'Content-Type': 'application/json' };

    try {
        // 1. Validar Método
        if (req.method !== 'POST') {
            return new Response(JSON.stringify({ error: 'Method Not Allowed' }), { status: 405, headers })
        }

        // 2. Extrair o Payload (o corpo enviado pelo webhook)
        const payload = await req.json()
        console.log("Recebendo payload do Gateway de Pagamento:", payload)

        // Ajustar essas variáveis conforme o payload exato da Hotmart/Kiwify
        const eventType = payload?.event || payload?.status;

        // Ignora se não for evento de Compra Aprovada
        if (eventType !== 'COMPRA_APROVADA' && eventType !== 'approved') {
            return new Response(JSON.stringify({ message: 'Evento ignorado. Não é aprovação.' }), { status: 200, headers });
        }

        const { email, full_name } = payload?.data || payload?.buyer || {};

        if (!email) {
            throw new Error("E-mail não fornecido pelo gateway.");
        }

        // 3. Conexão com Supabase usando SERVICE_ROLE_KEY (Permite bypass RLS para criar usuarios no Auth)
        const supabaseAdmin = createClient(
            Deno.env.get('SUPABASE_URL') ?? '',
            Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
        )

        // 4. Cadastrar Usuário Administrativamente (Auth)
        // Gera uma senha temporária (ex: os 6 primeros dígitos do CPF, ou string aleatória)
        const randomPassword = Math.random().toString(36).slice(-8)

        const { data: newUser, error: authError } = await supabaseAdmin.auth.admin.createUser({
            email: email,
            password: randomPassword,
            email_confirm: true, // Já aprovado
            user_metadata: { full_name: full_name }
        });

        if (authError) {
            // Se o usuario ja existir, apenas ignora
            if (authError.message.includes('User already registered')) {
                return new Response(JSON.stringify({ message: 'Aluno já existe na base. Acesso liberado/renovado.' }), { status: 200, headers });
            }
            throw authError;
        }

        /**
         * 5. Enviar Email (Passo Futuro Opcional usando Resend ou SendGrid APIs)
         * Como a conta foi criada, mandamos o e-mail:
         * "Olá fullname, seu acesso ao Aprovação Passo a Passo foi liberado!
         * Seu email: email
         * Senha provisória: randomPassword ..."
         */

        return new Response(
            JSON.stringify({ message: "Aluno registrado com sucesso via Webhook!", email, success: true }),
            { status: 200, headers }
        )

    } catch (err) {
        console.error("Webhook Error:", err.message)
        return new Response(JSON.stringify({ error: err.message }), { status: 400, headers })
    }
})
