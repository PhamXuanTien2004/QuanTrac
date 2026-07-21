import { useAuthStore } from '../../store/useAuthStore';
import AdminLayout from './AdminLayout';
import ManagerLayout from './ManagerLayout';
import StaffLayout from './StaffLayout';
import { Navigate } from 'react-router-dom';

export default function RoleBasedLayout() {
  const { user, isAuthenticated } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  const role = user?.role || 'ROLE_STAFF';

  if (role === 'ROLE_ADMIN' || role === 'Admin') {
    return <AdminLayout />;
  }
  
  if (role === 'ROLE_MANAGER') {
    return <ManagerLayout />;
  }

  return <StaffLayout />;
}
