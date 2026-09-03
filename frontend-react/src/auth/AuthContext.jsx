import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, claimsDoToken, tokenValido, tokens } from '../lib/api';
import { supabase, supabaseConfigurado } from '../api/supabase';

const AuthContext = createContext({});

export const useAuth = () => useContext(AuthContext);

const CHAVE_USUARIO = 'plataforma.usuario';

/**
 * Sessão da plataforma.
 *
 * A fonte de verdade é a API própria (§31: React + Vite · Java 21 + Spring Boot).
 * O Supabase segue aqui apenas como reserva para as telas ainda não migradas —
 * sem configuração dele, nada desse caminho é acionado.
 */
export const AuthProvider = ({ children }) => {
    const [usuario, setUsuario] = useState(null);
    const [loading, setLoading] = useState(true);

    // Restaura a sessão depois de uma recarga de página.
    useEffect(() => {
        let ativo = true;

        const restaurar = async () => {
            const guardado = localStorage.getItem(CHAVE_USUARIO);

            if (tokenValido()) {
                if (ativo) setUsuario(guardado ? JSON.parse(guardado) : perfilDasClaims());
                return;
            }

            // Access expirado, mas com refresh em mãos: renova em silêncio, em vez
            // de mandar para /login quem apenas deixou a aba aberta por mais de
            // 15 minutos (§20: access de 15 min, refresh de 30 dias).
            if (tokens.refresh()) {
                try {
                    await api.renovar();
                    if (ativo) setUsuario(guardado ? JSON.parse(guardado) : perfilDasClaims());
                    return;
                } catch {
                    tokens.limpar();
                    localStorage.removeItem(CHAVE_USUARIO);
                }
            }

            if (supabaseConfigurado) {
                try {
                    const { data } = await supabase.auth.getSession();
                    if (ativo && data?.session) {
                        setUsuario({
                            id: data.session.user.id,
                            nome: data.session.user.user_metadata?.full_name ?? data.session.user.email,
                            email: data.session.user.email,
                            perfis: ['ALUNO'],
                            origem: 'supabase',
                        });
                    }
                } catch {
                    // Provedor legado fora do ar não pode travar a aplicação.
                }
            }
        };

        restaurar().finally(() => {
            if (ativo) setLoading(false);
        });

        return () => {
            ativo = false;
        };
    }, []);

    const entrar = useCallback(async (credenciais) => {
        const resposta = await api.login(credenciais);
        const perfil = { ...resposta.usuario, origem: 'api' };
        localStorage.setItem(CHAVE_USUARIO, JSON.stringify(perfil));
        setUsuario(perfil);
        return perfil;
    }, []);

    const sair = useCallback(async () => {
        await api.logout().catch(() => {});
        if (supabaseConfigurado) await supabase.auth.signOut().catch(() => {});
        localStorage.removeItem(CHAVE_USUARIO);
        setUsuario(null);
    }, []);

    const valor = useMemo(
        () => ({
            usuario,
            // `session` e `user` continuam existindo porque as telas legadas já os liam.
            session: usuario ? { usuario } : null,
            user: usuario,
            loading,
            entrar,
            sair,
            signOut: sair,
            temPerfil: (...perfis) => perfis.some((p) => usuario?.perfis?.includes(p)),
        }),
        [usuario, loading, entrar, sair],
    );

    /*
     * Renderiza sempre. A versão anterior devolvia `{!loading && children}`, o que
     * prendia a aplicação inteira — landing, blog, página de curso — atrás da
     * checagem de sessão, e uma resposta lenta deixava o visitante anônimo diante
     * de tela em branco. Quem espera pela sessão é o PrivateRoute, e só ele.
     */
    return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
};

/** Reserva quando há token válido mas o perfil guardado sumiu do localStorage. */
function perfilDasClaims() {
    const claims = claimsDoToken();
    if (!claims) return null;
    return { id: claims.sub, nome: 'Minha conta', email: '', perfis: claims.perfis ?? [], origem: 'api' };
}
