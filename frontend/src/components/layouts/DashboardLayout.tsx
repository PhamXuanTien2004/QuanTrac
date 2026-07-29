import { Outlet, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/useAuthStore';
import { LayoutDashboard, RadioTower, AlertTriangle, FileBarChart, LogOut, Bell, Activity, Users, Cpu, Settings, Server } from 'lucide-react';
import styles from './DashboardLayout.module.css';
import RealtimeClock from '../Clock';

export default function DashboardLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();

  const userRole = user?.role || 'ROLE_STAFF';

  const navItems = [
    { path: '/dashboard', label: 'Tổng quan', icon: LayoutDashboard, roles: ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_STAFF'] },
    { 
      path: '/stations', 
      label: 'Quản lý Trạm', 
      icon: RadioTower, 
      roles: ['ROLE_ADMIN'] 
    },
    { 
      path: '/gateways', 
      label: 'Quản lý Gateway', 
      icon: Server, 
      roles: ['ROLE_ADMIN'] 
    },
    { 
      path: '/sensors', 
      label: 'Quản lý Sensor', 
      icon: Cpu, 
      roles: ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_STAFF'] 
    },
    { 
      path: '/sensor-types', 
      label: 'Loại Sensor', 
      icon: Settings, 
      roles: ['ROLE_ADMIN'] 
    },
    { 
      path: '/users', 
      label: userRole === 'ROLE_ADMIN' ? 'Quản lý Người dùng' : 'Quản lý Staff Trạm', 
      icon: Users, 
      roles: ['ROLE_ADMIN', 'ROLE_MANAGER'] 
    },
    { path: '/alerts', label: 'Cảnh báo', icon: AlertTriangle, roles: ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_STAFF'] },
    { path: '/reports', label: 'Báo cáo', icon: FileBarChart, roles: ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_STAFF'] },
  ];

  const filteredNavItems = navItems.filter((item) => item.roles.includes(userRole));

  const getPageTitle = () => {
    const item = navItems.find((n) => n.path === location.pathname);
    return item ? item.label : 'QuanTrac Pro';
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const getRoleBadgeLabel = (role: string) => {
    switch (role) {
      case 'ROLE_ADMIN':
        return 'Admin';
      case 'ROLE_MANAGER':
        return 'Manager Trạm';
      case 'ROLE_STAFF':
        return 'Staff (Giám sát)';
      default:
        return 'Staff';
    }
  };

  return (
    <div className={styles.layout}>
      {/* Sidebar */}
      <aside className={styles.sidebar}>
        <div className={styles.logo}>
          <div className={styles.logoIcon}>
            <Activity size={24} />
          </div>
          <h2>QuanTrac</h2>
        </div>
        
        <nav className={styles.nav}>
          {filteredNavItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) => 
                  `${styles.navItem} ${isActive ? styles.active : ''}`
                }
              >
                <Icon size={20} />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        <div className={styles.nav} style={{ flex: 0, borderTop: '1px solid var(--border-color)' }}>
          <button 
            className={styles.navItem} 
            style={{ width: '100%', textAlign: 'left' }}
            onClick={handleLogout}
          >
            <LogOut size={20} />
            <span>Đăng xuất</span>
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className={styles.main}>
        {/* Header */}
        <header className={styles.header}>
          <div className={styles.headerTitle}>
            <h1>{getPageTitle()}</h1>
          </div>
          <div className={styles.headerActions}>
            <RealtimeClock />
            <button className={styles.iconButton} aria-label="Notifications">
              <Bell size={20} />
            </button>
            <div 
              style={{ display: 'flex', alignItems: 'center', gap: '12px', cursor: 'pointer' }}
              onClick={() => navigate('/profile')}
              title="Xem thông tin hồ sơ"
            >
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end' }}>
                <span style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-primary)', transition: 'color 0.2s' }}
                      onMouseEnter={(e) => e.currentTarget.style.color = 'var(--primary-color)'}
                      onMouseLeave={(e) => e.currentTarget.style.color = 'var(--text-primary)'}>
                  {user?.username || 'User'}
                </span>
                <span style={{ 
                  fontSize: '0.75rem', 
                  fontWeight: 500, 
                  padding: '2px 8px', 
                  borderRadius: '12px',
                  backgroundColor: userRole === 'ROLE_ADMIN' ? 'rgba(239, 68, 68, 0.2)' : userRole === 'ROLE_MANAGER' ? 'rgba(245, 158, 11, 0.2)' : 'rgba(59, 130, 246, 0.2)',
                  color: userRole === 'ROLE_ADMIN' ? '#ef4444' : userRole === 'ROLE_MANAGER' ? '#f59e0b' : '#3b82f6'
                }}>
                  {getRoleBadgeLabel(userRole)}
                </span>
              </div>
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className={styles.content}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
