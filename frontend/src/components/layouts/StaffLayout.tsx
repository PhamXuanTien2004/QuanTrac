import { Outlet, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/useAuthStore';
import { LayoutDashboard, AlertTriangle, FileBarChart, LogOut, Bell, Activity } from 'lucide-react';
import styles from './ManagerLayout.module.css'; // Reuse Manager's CSS

export default function StaffLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();

  const navItems = [
    { path: '/dashboard', label: 'Tổng quan', icon: LayoutDashboard },
    { path: '/alerts', label: 'Cảnh báo', icon: AlertTriangle },
    { path: '/reports', label: 'Báo cáo', icon: FileBarChart },
  ];

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className={styles.layout}>
      {/* Top Navbar */}
      <header className={styles.header}>
        <div className={styles.logo}>
          <div className={styles.logoIcon}>
            <Activity size={24} />
          </div>
          <h2>QT-Staff</h2>
        </div>
        
        <nav className={styles.nav}>
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) => 
                  `${styles.navItem} ${isActive ? styles.active : ''}`
                }
              >
                <Icon size={18} />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        <div className={styles.headerActions}>
          <button className={styles.iconButton} aria-label="Notifications">
            <Bell size={20} />
          </button>
          
          <div className={styles.userInfo} onClick={() => navigate('/profile')} title="Hồ sơ">
            <span className={styles.userName}>{user?.username || 'Staff'}</span>
            <span className={styles.roleBadge}>Nhân viên Trạm</span>
          </div>

          <button 
            className={styles.logoutButton} 
            onClick={handleLogout}
            title="Đăng xuất"
          >
            <LogOut size={20} />
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <main className={styles.content}>
        <div className={styles.contentWrapper}>
          <Outlet />
        </div>
      </main>
    </div>
  );
}
