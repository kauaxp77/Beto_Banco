import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export const PrivateRoute = ({ allowedRoles }) => {
    const { session, user } = useAuth();

    // For now, since we don't have roles fetched easily yet, just check if logged in.
    if (!session) {
        return <Navigate to="/login" replace />;
    }

    return <Outlet />;
};
