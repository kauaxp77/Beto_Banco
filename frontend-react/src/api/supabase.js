import { createClient } from '@supabase/supabase-js';

/**
 * Cliente Supabase — legado.
 *
 * A V4.0 fixa Java 21 + Spring Boot como backend (§31, decisão permanente); o
 * Supabase segue atendendo as telas ainda não migradas. Ver `src/lib/api.js`
 * para o cliente da API nova.
 *
 * O valor de reserva precisa ser uma URL sintaticamente válida. `createClient`
 * valida a URL e **lança no momento do import** — e um throw aqui derruba o
 * módulo antes de o React montar, deixando a página inteira em branco, inclusive
 * a landing, que não depende de Supabase nenhum. Foi exatamente o que aconteceu
 * ao rodar em contêiner sem `.env.local`.
 */
const url = import.meta.env.VITE_SUPABASE_URL || 'https://indisponivel.supabase.co';
const chaveAnonima = import.meta.env.VITE_SUPABASE_ANON_KEY || 'anon-key-ausente';

/** true quando não há configuração real — as telas legadas usam para não tentar buscar. */
export const supabaseConfigurado = Boolean(
  import.meta.env.VITE_SUPABASE_URL && import.meta.env.VITE_SUPABASE_ANON_KEY,
);

if (!supabaseConfigurado && import.meta.env.DEV) {
  console.warn(
    'VITE_SUPABASE_URL/ANON_KEY ausentes. As telas legadas ficam sem dados; ' +
      'as telas migradas para a API nova continuam funcionando.',
  );
}

export const supabase = createClient(url, chaveAnonima);
