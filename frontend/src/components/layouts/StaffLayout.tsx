import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/useAuthStore';
import { LayoutDashboard, AlertTriangle, FileBarChart, LogOut, Cpu } from 'lucide-react';
import styles from './StaffLayout.module.css';

export default function StaffLayout() {
  const navigate = useNavigate();
  const { logout } = useAuthStore();

  const navItems = [
    { path: '/dashboard', label: 'Tổng quan', icon: LayoutDashboard },
    { path: '/sensors', label: 'Sensors', icon: Cpu },
    { path: '/alerts', label: 'Cảnh báo', icon: AlertTriangle },
    { path: '/reports', label: 'Báo cáo', icon: FileBarChart },
  ];

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className={styles.layout}>
      {/* Top minimal header */}
      <header className={styles.topbar}>
        <div className={styles.brand}>QT-Monitor (Kiosk)</div>
        <button onClick={handleLogout} className={styles.logoutBtn} title="Đăng xuất">
          <LogOut size={16} /> Thoát
        </button>
      </header>

      {/* Main Fullscreen Content */}
      <main className={styles.content}>
        <Outlet />
      </main>

      {/* Floating Bottom Navigation */}
      <nav className={styles.bottomNav}>
        <div className={styles.navContainer}>
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
                <Icon size={22} className={styles.icon} />
                <span className={styles.label}>{item.label}</span>
              </NavLink>
            );
          })}
        </div>
      </nav>
    </div>
  );
}
