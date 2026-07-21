import { useEffect, useState } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { useStationStore } from '../store/useStationStore';
import api from '../services/api';
import { User, Phone, RadioTower, Shield } from 'lucide-react';

interface UserProfile {
  id: string;
  username: string;
  fullName: string;
  phone: string;
  email?: string;
  stationId: string;
  role: string;
  status: string;
}

export default function ProfilePage() {
  const { user } = useAuthStore();
  const { stations, fetchStations } = useStationStore();
  
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (stations.length === 0) {
      fetchStations();
    }
  }, [stations, fetchStations]);

  useEffect(() => {
    const fetchProfile = async () => {
      if (!user?.username) return;
      
      setIsLoading(true);
      setError(null);
      try {
        const endpoint = user.stationId ? `/users/station/${user.stationId}` : '/users';
        const response = await api.get(endpoint);
        
        let userList: UserProfile[] = [];
        if (Array.isArray(response.data?.data)) {
          userList = response.data.data;
        } else if (Array.isArray(response.data)) {
          userList = response.data;
        }

        const currentProfile = userList.find(u => u.username === user.username);
        
        if (currentProfile) {
          setProfile(currentProfile);
        } else {
          setError('Không tìm thấy thông tin hồ sơ của bạn trên hệ thống.');
        }
      } catch (err: any) {
        setError('Lỗi khi tải thông tin hồ sơ: ' + (err.message || ''));
      } finally {
        setIsLoading(false);
      }
    };

    fetchProfile();
  }, [user]);

  const getStationName = (stationId: string) => {
    if (!stationId) return 'Quản trị viên toàn hệ thống (Không gắn trạm)';
    const station = stations.find(s => s.id === stationId);
    return station ? station.name : 'Unknown Station';
  };

  const getRoleBadgeLabel = (role?: string | null) => {
    if (!role) return 'Nhân viên (Staff)';
    const r = role.toUpperCase();
    if (r.includes('ADMIN')) return 'Admin Hệ Thống';
    if (r.includes('MANAGER')) return 'Quản lý Trạm';
    return 'Nhân viên (Staff)';
  };

  if (isLoading) {
    return (
      <div style={{ padding: '40px', display: 'flex', justifyContent: 'center', color: 'var(--text-muted)' }}>
        Đang tải thông tin hồ sơ...
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: '24px', backgroundColor: 'rgba(239, 68, 68, 0.1)', color: 'var(--danger)', borderRadius: 'var(--radius-md)' }}>
        {error}
      </div>
    );
  }

  if (!profile) return null;

  const roleUpper = profile.role?.toUpperCase() || '';
  const isAdmin = roleUpper.includes('ADMIN');
  const isManager = roleUpper.includes('MANAGER');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px', maxWidth: '800px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Thông tin cá nhân (My Profile)</h2>
      </div>

      <div className="glass-panel" style={{ padding: '32px', display: 'flex', flexDirection: 'column', gap: '32px' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', borderBottom: '1px solid var(--border-glass)', paddingBottom: '24px' }}>
          <h3 style={{ fontSize: '1.5rem', color: 'var(--text-primary)', margin: 0 }}>
            {profile.fullName || profile.username}
          </h3>
          <p style={{ color: 'var(--text-secondary)', margin: 0 }}>
            Tài khoản đăng nhập: <strong>{profile.username}</strong>
          </p>
          <div style={{ marginTop: '8px' }}>
            <span style={{
              padding: '6px 12px',
              borderRadius: '99px',
              fontSize: '0.875rem',
              fontWeight: 600,
              backgroundColor: isAdmin ? 'rgba(239, 68, 68, 0.15)' : isManager ? 'rgba(245, 158, 11, 0.15)' : 'rgba(59, 130, 246, 0.15)',
              color: isAdmin ? '#ef4444' : isManager ? '#f59e0b' : '#3b82f6',
              border: `1px solid ${isAdmin ? 'rgba(239, 68, 68, 0.2)' : isManager ? 'rgba(245, 158, 11, 0.2)' : 'rgba(59, 130, 246, 0.2)'}`,
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px'
            }}>
              <Shield size={16} />
              {getRoleBadgeLabel(profile.role)}
            </span>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '24px' }}>
          
          <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
            <div style={{ padding: '12px', backgroundColor: 'rgba(255, 255, 255, 0.05)', borderRadius: '12px', color: 'var(--text-secondary)' }}>
              <User size={24} />
            </div>
            <div>
              <p style={{ margin: 0, fontSize: '0.875rem', color: 'var(--text-muted)' }}>Họ và tên</p>
              <p style={{ margin: '4px 0 0', fontWeight: 500, color: 'var(--text-primary)', fontSize: '1.1rem' }}>
                {profile.fullName || 'Chưa cập nhật'}
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
            <div style={{ padding: '12px', backgroundColor: 'rgba(255, 255, 255, 0.05)', borderRadius: '12px', color: 'var(--text-secondary)' }}>
              <Phone size={24} />
            </div>
            <div>
              <p style={{ margin: 0, fontSize: '0.875rem', color: 'var(--text-muted)' }}>Số điện thoại</p>
              <p style={{ margin: '4px 0 0', fontWeight: 500, color: 'var(--text-primary)', fontSize: '1.1rem' }}>
                {profile.phone || 'Chưa cập nhật'}
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start', gridColumn: '1 / -1' }}>
            <div style={{ padding: '12px', backgroundColor: 'rgba(255, 255, 255, 0.05)', borderRadius: '12px', color: 'var(--text-secondary)' }}>
              <RadioTower size={24} />
            </div>
            <div>
              <p style={{ margin: 0, fontSize: '0.875rem', color: 'var(--text-muted)' }}>Trạm trực thuộc</p>
              <p style={{ margin: '4px 0 0', fontWeight: 500, color: 'var(--text-primary)', fontSize: '1.1rem' }}>
                {getStationName(profile.stationId)}
              </p>
            </div>
          </div>
          
        </div>
      </div>
    </div>
  );
}
