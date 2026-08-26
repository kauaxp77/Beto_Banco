import { createClient } from '@supabase/supabase-js';

const supabaseUrl = 'https://bjnplubfqoltaxfboodl.supabase.co';
const supabaseAnonKey = 'sb_publishable_vCOnuETWy1sf_Rap5CyihQ_ggYk0j-Q';

const supabase = createClient(supabaseUrl, supabaseAnonKey);

async function createAccount() {
    const { data, error } = await supabase.auth.signUp({
        email: 'admin@aprovacao.com',
        password: 'senha.admin',
        options: {
            data: {
                full_name: 'Professor Beto Fernandes'
            }
        }
    });

    if (error) {
        console.error('ERRO:', error.message);
    } else {
        console.log('SUCESSO. Confirmado?', data.user?.confirmed_at ? 'SIM' : 'NAO (Precisa desativar Confirm Email no Supabase)');
    }
}

createAccount();
