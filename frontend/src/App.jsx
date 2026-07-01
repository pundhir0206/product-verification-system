import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import Sidebar from './components/Sidebar';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import UploadPage from './pages/UploadPage';
import ValidatePage from './pages/ValidatePage';
import ReportPage from './pages/ReportPage';
import UserManagementPage from './pages/UserManagementPage';

function AppShell({ children }) {
  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main">{children}</main>
    </div>
  );
}

export default function App() {
  const { token } = useSelector((state) => state.auth);

  return (
    <Routes>
      <Route path="/login" element={token ? <Navigate to="/validate" replace /> : <LoginPage />} />

      <Route
        path="/upload"
        element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <AppShell><UploadPage /></AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/validate"
        element={
          <ProtectedRoute allowedRoles={['ADMIN', 'OPERATOR']}>
            <AppShell><ValidatePage /></AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/reports"
        element={
          <ProtectedRoute allowedRoles={['ADMIN', 'OPERATOR']}>
            <AppShell><ReportPage /></AppShell>
          </ProtectedRoute>
        }
      />
      <Route
        path="/users"
        element={
          <ProtectedRoute allowedRoles={['ADMIN']}>
            <AppShell><UserManagementPage /></AppShell>
          </ProtectedRoute>
        }
      />

      <Route path="*" element={<Navigate to={token ? '/validate' : '/login'} replace />} />
    </Routes>
  );
}
