import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100vh' }}>
      <h1 style={{ fontSize: '4rem', color: 'var(--primary-color)', marginBottom: '16px' }}>404</h1>
      <p style={{ fontSize: '1.25rem', color: 'var(--text-secondary)', marginBottom: '24px' }}>
        Trang bạn tìm kiếm không tồn tại.
      </p>
      <Link to="/" className="glass-panel hover-lift" style={{ padding: '12px 24px', color: 'var(--primary-color)', fontWeight: '500' }}>
        Về trang chủ
      </Link>
    </div>
  );
}
