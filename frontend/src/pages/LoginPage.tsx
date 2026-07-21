import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { Activity, Lock, User } from 'lucide-react';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const { login, isLoading, error, clearError } = useAuthStore();
  const navigate = useNavigate();
  const location = useLocation();

  const from = location.state?.from?.pathname || '/dashboard';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await login(username, password);
      navigate(from, { replace: true });
    } catch (err) {
      // Error is handled in store
    }
  };

  return (
    <div style={{ 
      display: 'flex', height: '100vh', width: '100vw', 
      alignItems: 'center', justifyContent: 'center', 
      background: 'radial-gradient(circle at top right, rgba(59, 130, 246, 0.15), transparent 40%), var(--bg-main)'
    }}>
      <div className="glass-panel" style={{ 
        width: '100%', maxWidth: '400px', padding: '40px', 
        display: 'flex', flexDirection: 'column', alignItems: 'center',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)'
      }}>
        <div style={{ 
          width: '64px', height: '64px', borderRadius: '16px', 
          backgroundColor: 'rgba(59, 130, 246, 0.1)', color: 'var(--primary-color)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '24px'
        }}>
          <Activity size={32} />
        </div>
        
        <h1 style={{ fontSize: '1.5rem', fontWeight: 600, marginBottom: '8px' }}>QuanTrac Pro</h1>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '32px', textAlign: 'center' }}>
          Đăng nhập để truy cập hệ thống quản trị
        </p>

        {error && (
          <div style={{ 
            width: '100%', backgroundColor: 'rgba(239, 68, 68, 0.1)', color: 'var(--danger)', 
            padding: '12px 16px', borderRadius: 'var(--radius-md)', marginBottom: '20px', 
            fontSize: '0.875rem', border: '1px solid rgba(239, 68, 68, 0.2)',
            display: 'flex', justifyContent: 'space-between'
          }}>
            <span>{error}</span>
            <button onClick={clearError} style={{ color: 'var(--danger)', fontWeight: 'bold' }}>×</button>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div style={{ position: 'relative' }}>
            <div style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}>
              <User size={18} />
            </div>
            <input 
              type="text" required placeholder="Tên đăng nhập" 
              value={username} onChange={e => setUsername(e.target.value)}
              style={{ 
                width: '100%', padding: '14px 16px 14px 44px', borderRadius: 'var(--radius-md)', 
                border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white',
                fontSize: '1rem', transition: 'border-color 0.2s'
              }} 
            />
          </div>

          <div style={{ position: 'relative' }}>
            <div style={{ position: 'absolute', left: '16px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }}>
              <Lock size={18} />
            </div>
            <input 
              type="password" required placeholder="Mật khẩu" 
              value={password} onChange={e => setPassword(e.target.value)}
              style={{ 
                width: '100%', padding: '14px 16px 14px 44px', borderRadius: 'var(--radius-md)', 
                border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white',
                fontSize: '1rem', transition: 'border-color 0.2s'
              }} 
            />
          </div>

          <button 
            type="submit" disabled={isLoading} className="hover-lift"
            style={{ 
              width: '100%', padding: '14px', marginTop: '8px',
              backgroundColor: 'var(--primary-color)', color: 'white', 
              borderRadius: 'var(--radius-md)', fontWeight: 600, fontSize: '1rem',
              opacity: isLoading ? 0.7 : 1, cursor: isLoading ? 'not-allowed' : 'pointer'
            }}
          >
            {isLoading ? 'Đang xác thực...' : 'Đăng nhập'}
          </button>
        </form>
      </div>
    </div>
  );
}
