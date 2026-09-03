import React, { createContext, useContext, useEffect, useState } from 'react';
import { supabase, supabaseConfigurado } from '../api/supabase';

const AuthContext = createContext({});

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [session, setSession] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Sem configuração não há sessão a recuperar. Encerrar o carregamento
        // aqui evita que a árvore inteira fique esperando por uma promessa que
        // nunca resolve.
        if (!supabaseConfigurado) {
            setLoading(false);
            return undefined;
        }

        let ativo = true;

        supabase.auth
            .getSession()
            .then(({ data }) => {
                if (!ativo) return;
                setSession(data?.session ?? null);
                setUser(data?.session?.user ?? null);
            })
            .catch(() => {
                // Provedor de auth fora do ar não pode derrubar as páginas
                // públicas: seguimos como visitante anônimo.
            })
            .finally(() => {
                if (ativo) setLoading(false);
            });

        const { data: { subscription } } = supabase.auth.onAuthStateChange((_evento, sessao) => {
            setSession(sessao);
            setUser(sessao?.user ?? null);
            setLoading(false);
        });

        return () => {
            ativo = false;
            subscription.unsubscribe();
        };
    }, []);

    const value = { session, user, loading, signOut: () => supabase.auth.signOut() };

    /*
     * Renderiza sempre. A versão anterior devolvia `{!loading && children}`, o
     * que prendia a aplicação inteira — landing, blog, página de curso — atrás da
     * checagem de sessão. Uma resposta lenta do provedor, ou nenhuma, deixava o
     * visitante diante de uma tela em branco, e é justamente esse visitante
     * anônimo que a §08 e a §15 tratam como a principal fonte de tráfego.
     * Quem precisa esperar pela sessão é o PrivateRoute, e só ele.
     */
    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
