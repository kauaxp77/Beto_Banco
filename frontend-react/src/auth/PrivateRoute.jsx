import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

/**
 * Portão das rotas autenticadas.
 *
 * Enquanto a sessão está sendo recuperada não dá para decidir nada: redirecionar
 * nesse instante expulsaria para /login quem só recarregou a página estando
 * logado. Por isso o estado de carregamento tem um retorno próprio, em vez de
 * cair no `!session`.
 */
export const PrivateRoute = () => {
    const { session, loading } = useAuth();

    if (loading) {
        return (
            <div className="conteiner" style={{ paddingBlock: 'var(--esp-7)' }} aria-busy="true">
                <span className="apenas-leitor">Verificando sua sessão</span>
                <div className="esqueleto esqueleto--titulo" />
                <div className="esqueleto esqueleto--bloco" style={{ marginTop: 'var(--esp-5)' }} />
            </div>
        );
    }

    if (!session) {
        return <Navigate to="/login" replace />;
    }

    return <Outlet />;
};
