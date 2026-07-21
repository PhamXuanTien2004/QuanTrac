import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useEffect } from 'react';
import './index.css';

// Layouts
import RoleBasedLayout from './components/layouts/RoleBasedLayout';

// Pages
import DashboardPage from './pages/DashboardPage';
import StationsPage from './pages/StationsPage';
import AlertsPage from './pages/AlertsPage';
import ReportsPage from './pages/ReportsPage';
import NotFoundPage from './pages/NotFoundPage';
import LoginPage from './pages/LoginPage';

import ProtectedRoute from './components/auth/ProtectedRoute';

import UsersPage from './pages/UsersPage';
import SensorsPage from './pages/SensorsPage';
import SensorTypesPage from './pages/SensorTypesPage';
import GatewaysPage from './pages/GatewaysPage';
import ProfilePage from './pages/ProfilePage';

function App() {
  // Theme initialization
  useEffect(() => {
    // Force dark mode by default for that premium feel
    document.documentElement.setAttribute('data-theme', 'dark');
  }, []);

  return (
    <BrowserRouter>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<LoginPage />} />

        {/* Protected Routes */}
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<RoleBasedLayout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="stations" element={<StationsPage />} />
            <Route path="gateways" element={<GatewaysPage />} />
            <Route path="sensors" element={<SensorsPage />} />
            <Route path="sensor-types" element={<SensorTypesPage />} />
            <Route path="users" element={<UsersPage />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="alerts" element={<AlertsPage />} />
            <Route path="reports" element={<ReportsPage />} />
          </Route>
        </Route>
        
        {/* Public Routes */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
