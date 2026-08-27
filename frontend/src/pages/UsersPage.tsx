import { useState, useEffect } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { useStationStore } from '../store/useStationStore';
import { UserPlus, Search, Filter, Phone, RadioTower, RotateCw, Edit2, Trash2, Mail } from 'lucide-react';
import api from '../services/api';

interface UserItem {
  id: string;
  username: string;
  fullName: string;
  phone: string;
  email?: string;
  stationId: string;
  role: string;
  status: string;
  notificationMethod?: string;
}

export default function UsersPage() {
  const { user } = useAuthStore();
  const { stations, fetchStations } = useStationStore();
  const userRole = user?.role || 'ROLE_STAFF';
  const isAdmin = userRole === 'ROLE_ADMIN';
  const isManager = userRole === 'ROLE_MANAGER';

  const [users, setUsers] = useState<UserItem[]>([]);
  const [isLoadingUsers, setIsLoadingUsers] = useState(false);

  const fetchUsers = async () => {
    setIsLoadingUsers(true);
    try {
      const endpoint = (isManager && user?.stationId)
        ? `/users/station/${user.stationId}`
        : '/users';
      const response = await api.get(endpoint);
      let userList: UserItem[] = [];
      if (Array.isArray(response.data?.data)) {
        userList = response.data.data;
      } else if (Array.isArray(response.data)) {
        userList = response.data;
      }
      userList.sort((a, b) => {
         const nameA = a.fullName || a.username || '';
         const nameB = b.fullName || b.username || '';
         return nameA.localeCompare(nameB, 'vi');
      });
      setUsers(userList);
    } catch (err: any) {
      console.error('Could not load users from backend DB:', err);
    } finally {
      setIsLoadingUsers(false);
    }
  };

  useEffect(() => {
    fetchStations();
    fetchUsers();
  }, [fetchStations]);

  const [searchQuery, setSearchQuery] = useState('');
  const [filterRole, setFilterRole] = useState('ALL');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [editUserId, setEditUserId] = useState<string | null>(null);

  // Lấy stationId thực tế từ DB (đề phòng token bị thiếu station_id)
  const dbCurrentUser = users.find(u => u.username === user?.username);
  const currentUserStationId = dbCurrentUser?.stationId || user?.stationId;

  const [formData, setFormData] = useState({
    username: '',
    password: '',
    firstName: '',
    lastName: '',
    phone: '',
    email: '',
    stationId: currentUserStationId || (stations[0]?.id || ''),
    role: isManager ? 'Staff' : 'Manager',
  });

  useEffect(() => {
    if (stations.length > 0 && !formData.stationId) {
      setFormData(prev => ({ ...prev, stationId: stations[0].id }));
    }
  }, [stations]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  const [isOtpModalOpen, setIsOtpModalOpen] = useState(false);
  const [otpValue, setOtpValue] = useState('');
  const [pendingUserPayload, setPendingUserPayload] = useState<any>(null);

  const getStationName = (stId: string) => {
    if (!stId) return 'Tất cả các Trạm (Global Admin)';
    const found = stations.find((s) => s.id === stId);
    return found ? found.name : stId;
  };



  // Manager chỉ được nhìn thấy Staff ở trạm của mình
  const displayedUsers = users.filter((u) => {
    if (!isAdmin) {
      if (currentUserStationId && u.stationId !== currentUserStationId) return false;
      if (u.role === 'ROLE_ADMIN' || u.role === 'Admin') return false;
    }

    if (filterRole !== 'ALL') {
      const uRole = (u.role || '').toUpperCase();
      const targetRole = filterRole.toUpperCase();
      if (!uRole.includes(targetRole.replace('ROLE_', ''))) return false;
    }

    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      return (
        (u.username && u.username.toLowerCase().includes(q)) ||
        (u.fullName && u.fullName.toLowerCase().includes(q)) ||
        (u.phone && u.phone.includes(q)) ||
        (u.stationId && u.stationId.toLowerCase().includes(q)) ||
        (getStationName(u.stationId).toLowerCase().includes(q))
      );
    }
    return true;
  });

  // Phân trang
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;
  
  // Logic phân trang hiển thị
  const totalPages = Math.ceil(displayedUsers.length / itemsPerPage);
  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentUsers = displayedUsers.slice(indexOfFirstItem, indexOfLastItem);

  useEffect(() => {
    // Reset về trang 1 khi filter thay đổi
    setCurrentPage(1);
  }, [filterRole, searchQuery]);

  const handleDeleteUser = async (id: string, username: string) => {
    if (window.confirm(`Bạn có chắc chắn muốn xóa người dùng "${username}" không? Hành động này sẽ vô hiệu hóa tài khoản.`)) {
      try {
        await api.delete(`/auth/users/${id}`);
        setMessage({ text: 'Xóa người dùng thành công.', type: 'success' });
        setTimeout(() => {
          fetchUsers();
          setMessage(null);
        }, 1000);
      } catch (err: any) {
        console.error('Lỗi khi xóa người dùng:', err);
        setMessage({ text: 'Có lỗi xảy ra khi xóa người dùng!', type: 'error' });
      }
    }
  };


  const resetForm = () => {
    setFormData({
      username: '',
      password: '',
      firstName: '',
      lastName: '',
      phone: '',
      email: '',
      stationId: currentUserStationId || (stations[0]?.id || ''),
      role: isManager ? 'Staff' : 'Manager',
      notificationMethod: 'ALL'
    });
    setIsEditMode(false);
    setEditUserId(null);
    setIsModalOpen(false); // Đóng Modal khi bấm Hủy
  };

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setMessage(null);

    const selectedRole = isManager ? 'Staff' : formData.role;
    const isCreatingAdmin = selectedRole === 'Admin';
    const finalStationId = isCreatingAdmin
      ? ''
      : isManager
        ? currentUserStationId || (stations[0]?.id || '')
        : formData.stationId;

    const selectedStation = stations.find((s) => s.id === finalStationId);
    const finalStationName = isCreatingAdmin
      ? ''
      : selectedStation
        ? selectedStation.name
        : getStationName(finalStationId);

    try {
      const payload = {
        username: formData.username,
        password: formData.password,
        firstName: formData.firstName,
        lastName: formData.lastName,
        phone: formData.phone,
        email: formData.email,
        stationId: finalStationId,
        stationName: finalStationName,
        roles: [selectedRole],
      };

      if (isEditMode && editUserId) {
        await api.put(`/auth/users/${editUserId}`, payload);
        setMessage({ text: 'Cập nhật tài khoản thành công!', type: 'success' });
        setIsModalOpen(false);
        setTimeout(() => {
          fetchUsers();
        }, 1000);
      } else {
        const response = await api.post('/auth/register', payload);

        if (response.data?.data?.status === 'PENDING' || response.data?.status === 'PENDING') {
          setPendingUserPayload({ ...payload, roleStr: `ROLE_${selectedRole.toUpperCase()}` });
          setIsOtpModalOpen(true);
          setMessage({ text: 'Vui lòng kiểm tra Email để lấy mã xác nhận OTP.', type: 'success' });
        } else {
          // Fallback for immediate active
          const newUser: UserItem = {
            id: Date.now().toString(),
            username: formData.username,
            fullName: `${formData.lastName} ${formData.firstName}`,
            phone: formData.phone,
            stationId: finalStationId,
            role: `ROLE_${selectedRole.toUpperCase()}`,
            status: 'ACTIVE',
          };

          setUsers([newUser, ...users]);
          setMessage({ text: 'Tạo tài khoản mới thành công! Đã gửi thông tin tới CSDL.', type: 'success' });
          setIsModalOpen(false);
          setTimeout(() => {
            fetchUsers();
          }, 1500);
          resetForm();
        }
      }
    } catch (err: any) {
      const errorMsg =
        err.response?.data?.message ||
        err.response?.data?.errorMessage ||
        err.message ||
        'Tạo tài khoản thất bại! Vui lòng kiểm tra lại thông tin.';
      setMessage({ text: errorMsg, type: 'error' });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!otpValue) {
      setMessage({ text: 'Vui lòng nhập mã OTP', type: 'error' });
      return;
    }
    setIsSubmitting(true);
    setMessage(null);
    try {
      await api.post('/auth/verify-otp', {
        email: pendingUserPayload.email,
        otp: otpValue
      });
      
      const newUser: UserItem = {
        id: Date.now().toString(),
        username: pendingUserPayload.username,
        fullName: `${pendingUserPayload.lastName} ${pendingUserPayload.firstName}`,
        phone: pendingUserPayload.phone,
        stationId: pendingUserPayload.stationId,
        role: pendingUserPayload.roleStr,
        status: 'ACTIVE',
      };

      setUsers([newUser, ...users]);
      setMessage({ text: 'Xác thực thành công! Tài khoản đã được tạo.', type: 'success' });
      setIsOtpModalOpen(false);
      setIsModalOpen(false);
      setOtpValue('');
      setPendingUserPayload(null);
      setTimeout(() => {
        fetchUsers();
      }, 1500);
      resetForm();
    } catch (err: any) {
      setMessage({ text: err.response?.data?.message || err.response?.data?.errorMessage || 'Mã OTP không chính xác!', type: 'error' });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 600 }}>
            {isAdmin ? 'Quản lý Nguời dùng Hệ thống (Manager & Staff)' : 'Quản lý Nhân viên Trạm (Staff)'}
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '4px' }}>
            {isAdmin
              ? 'Tạo, phân quyền và gán trạm hoạt động cho các Manager và Staff toàn hệ thống'
              : 'Tạo và giám sát các tài khoản Staff thuộc trạm phụ trách'}
          </p>
        </div>

        <div style={{ display: 'flex', gap: '12px' }}>
          <button
            onClick={() => fetchUsers()}
            disabled={isLoadingUsers}
            className="hover-lift"
            style={{
              display: 'flex', alignItems: 'center', gap: '8px',
              backgroundColor: 'rgba(255, 255, 255, 0.05)', color: 'white',
              border: '1px solid var(--border-color)',
              padding: '10px 16px', borderRadius: 'var(--radius-md)', fontWeight: 500
            }}
          >
            <RotateCw size={18} className={isLoadingUsers ? 'animate-spin' : ''} /> {isLoadingUsers ? 'Đang tải...' : 'Làm mới'}
          </button>

          <button
            onClick={() => setIsModalOpen(true)}
            className="hover-lift"
            style={{
              display: 'flex', alignItems: 'center', gap: '8px',
              backgroundColor: 'var(--primary-color)', color: 'white',
              padding: '10px 20px', borderRadius: 'var(--radius-md)', fontWeight: 500
            }}
          >
            <UserPlus size={18} /> {isAdmin ? 'Tạo Người dùng Mới' : 'Tạo Staff Mới'}
          </button>
        </div>
      </div>

      {/* Message notification */}
      {message && (
        <div style={{
          padding: '12px 16px', borderRadius: 'var(--radius-md)',
          backgroundColor: message.type === 'success' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)',
          color: message.type === 'success' ? 'var(--success)' : 'var(--danger)',
          border: `1px solid ${message.type === 'success' ? 'rgba(16, 185, 129, 0.2)' : 'rgba(239, 68, 68, 0.2)'}`
        }}>
          {message.text}
        </div>
      )}

      {/* Filter and Search */}
      <div className="glass-panel" style={{ padding: '16px', display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
        <div style={{ position: 'relative', flex: 1, minWidth: '240px' }}>
          <Search size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            type="text"
            placeholder="Tìm theo tên, username, SĐT, mã trạm..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{
              width: '100%', padding: '10px 12px 10px 40px', borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white'
            }}
          />
        </div>

        {isAdmin && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Filter size={18} style={{ color: 'var(--text-muted)' }} />
            <select
              value={filterRole}
              onChange={(e) => setFilterRole(e.target.value)}
              style={{
                padding: '10px 16px', borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white'
              }}
            >
              <option value="ALL">Tất cả Vai trò</option>
              <option value="ROLE_MANAGER">Chỉ Manager</option>
              <option value="ROLE_STAFF">Chỉ Staff</option>
            </select>
          </div>
        )}
      </div>

      {/* Users Table */}
      <div className="glass-panel" style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border-glass)', backgroundColor: 'rgba(255, 255, 255, 0.02)' }}>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Họ và Tên</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Username</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Vai trò</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Trạm Phụ trách</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Email</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500 }}>Trạng thái</th>
              <th style={{ padding: '16px 24px', color: 'var(--text-secondary)', fontWeight: 500, textAlign: 'right' }}>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {isLoadingUsers ? (
              <tr>
                <td colSpan={7} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  Đang tải danh sách người dùng thực tế từ CSDL...
                </td>
              </tr>
            ) : currentUsers.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  Chưa có dữ liệu người dùng trong CSDL.
                </td>
              </tr>
            ) : (
              currentUsers.map((u) => (
                <tr key={u.id} style={{ borderBottom: '1px solid var(--border-glass)' }} className="hover-row">
                  <td style={{ padding: '16px 24px', fontWeight: 500 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                      <div style={{
                        width: '36px', height: '36px', borderRadius: '50%',
                        backgroundColor: (u.role && u.role.includes('ADMIN')) ? 'rgba(239, 68, 68, 0.15)' : (u.role && u.role.includes('MANAGER')) ? 'rgba(245, 158, 11, 0.15)' : 'rgba(59, 130, 246, 0.15)',
                        color: (u.role && u.role.includes('ADMIN')) ? '#ef4444' : (u.role && u.role.includes('MANAGER')) ? '#f59e0b' : '#3b82f6',
                        display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 600
                      }}>
                        {(u.fullName || u.username || 'U').charAt(0).toUpperCase()}
                      </div>
                      {u.fullName || u.username}
                    </div>
                  </td>
                  <td style={{ padding: '16px 24px', color: 'var(--text-secondary)' }}>{u.username}</td>
                  <td style={{ padding: '16px 24px' }}>
                    <span style={{
                      padding: '4px 12px', borderRadius: '16px', fontSize: '0.75rem', fontWeight: 600,
                      backgroundColor: (u.role && u.role.includes('ADMIN')) ? 'rgba(239, 68, 68, 0.1)' : (u.role && u.role.includes('MANAGER')) ? 'rgba(245, 158, 11, 0.1)' : 'rgba(59, 130, 246, 0.1)',
                      color: (u.role && u.role.includes('ADMIN')) ? '#ef4444' : (u.role && u.role.includes('MANAGER')) ? '#f59e0b' : '#3b82f6'
                    }}>
                      {(u.role && u.role.includes('ADMIN')) ? 'Admin' : (u.role && u.role.includes('MANAGER')) ? 'Manager' : 'Staff'}
                    </span>
                  </td>
                  <td style={{ padding: '16px 24px', color: 'var(--text-secondary)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <RadioTower size={16} /> {getStationName(u.stationId)}
                    </div>
                  </td>
                  <td style={{ padding: '16px 24px', color: 'var(--text-secondary)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <Mail size={14} /> {u.email || 'N/A'}
                    </div>
                  </td>
                  <td style={{ padding: '16px 24px' }}>
                    <span style={{
                      padding: '4px 10px', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 500,
                      backgroundColor: 'rgba(16, 185, 129, 0.1)', color: 'var(--success)'
                    }}>
                      {u.status}
                    </span>
                  </td>
                  <td style={{ padding: '16px 24px', textAlign: 'right' }}>
                    <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
                      <button
                        onClick={() => {
                          setIsEditMode(true);
                          setEditUserId(u.id);
                          
                          // Phân tích Vai trò (Role)
                          let parsedRole = 'Staff';
                          if (u.role) {
                              const upperRole = u.role.toUpperCase();
                              if (upperRole.includes('ADMIN')) parsedRole = 'Admin';
                              else if (upperRole.includes('MANAGER')) parsedRole = 'Manager';
                          }

                          // Phân tích Họ Tên (nếu API có firstName/lastName thì lấy, không thì tách từ fullName)
                          const anyU = u as any;
                          let fName = anyU.firstName || '';
                          let lName = anyU.lastName || '';
                          if (!fName && !lName && u.fullName) {
                              const parts = u.fullName.trim().split(' ');
                              fName = parts.pop() || '';
                              lName = parts.join(' ');
                          }

                          setFormData({
                            username: u.username || '',
                            password: '', // Khi edit để trống mật khẩu
                            firstName: fName,
                            lastName: lName,
                            phone: u.phone || '',
                            email: u.email || '',
                            stationId: u.stationId || '',
                            role: parsedRole,
                            notificationMethod: u.notificationMethod || 'ALL'
                          });
                          setIsModalOpen(true);
                        }}
                        style={{ color: 'var(--text-muted)', transition: 'color 0.2s', padding: '6px', background: 'transparent', border: 'none', cursor: 'pointer' }}
                        onMouseEnter={(e) => e.currentTarget.style.color = 'var(--primary-color)'}
                        onMouseLeave={(e) => e.currentTarget.style.color = 'var(--text-muted)'}
                        title="Chỉnh sửa"
                      >
                        <Edit2 size={18} />
                      </button>
                      <button
                        onClick={() => handleDeleteUser(u.id, u.username)}
                        style={{ color: 'var(--text-muted)', transition: 'color 0.2s', padding: '6px', background: 'transparent', border: 'none', cursor: 'pointer' }}
                        onMouseEnter={(e) => e.currentTarget.style.color = 'var(--danger)'}
                        onMouseLeave={(e) => e.currentTarget.style.color = 'var(--text-muted)'}
                        title="Xóa người dùng"
                      >
                        <Trash2 size={18} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Phân trang */}
        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', padding: '16px 24px', gap: '16px', borderTop: '1px solid var(--border-glass)' }}>
            <span style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
              Trang {currentPage} / {totalPages}
            </span>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                disabled={currentPage === 1}
                style={{
                  padding: '6px 12px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: currentPage === 1 ? 'rgba(255,255,255,0.05)' : 'var(--primary-color)',
                  color: currentPage === 1 ? 'var(--text-muted)' : 'white',
                  border: 'none',
                  cursor: currentPage === 1 ? 'not-allowed' : 'pointer'
                }}
              >
                Trước
              </button>
              <button
                onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                disabled={currentPage === totalPages}
                style={{
                  padding: '6px 12px',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: currentPage === totalPages ? 'rgba(255,255,255,0.05)' : 'var(--primary-color)',
                  color: currentPage === totalPages ? 'var(--text-muted)' : 'white',
                  border: 'none',
                  cursor: currentPage === totalPages ? 'not-allowed' : 'pointer'
                }}
              >
                Sau
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Modal Create User */}
      {isModalOpen && (
        <div style={{
          position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh',
          backgroundColor: 'rgba(0, 0, 0, 0.6)', backdropFilter: 'blur(4px)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100
        }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: '520px', padding: '32px', backgroundColor: 'var(--bg-surface)' }}>
            <h3 style={{ fontSize: '1.25rem', marginBottom: '20px', fontWeight: 600 }}>
              {isEditMode 
                ? 'Cập nhật Thông tin Tài khoản' 
                : (isAdmin ? 'Tạo Tài khoản Người dùng Mới' : 'Tạo Tài khoản Staff Mới')}
            </h3>

            <form onSubmit={handleCreateUser} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div style={{ display: 'flex', gap: '16px' }}>
                <div style={{ flex: 1 }}>
                  <label style={{ display: 'block', marginBottom: '6px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Họ (Last Name)</label>
                  <input required value={formData.lastName} onChange={e => setFormData({ ...formData, lastName: e.target.value })}
                    style={{ width: '100%', padding: '10px 14px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }}
                    placeholder="Nguyễn" />
                </div>
                <div style={{ flex: 1 }}>
                  <label style={{ display: 'block', marginBottom: '6px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Tên (First Name)</label>
                  <input required value={formData.firstName} onChange={e => setFormData({ ...formData, firstName: e.target.value })}
                    style={{ width: '100%', padding: '10px 14px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }}
                    placeholder="Văn A" />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '6px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Tên Đăng nhập (Username)</label>
                <input required value={formData.username} onChange={e => setFormData({ ...formData, username: e.target.value })}
                  disabled={isEditMode}
                  style={{ width: '100%', padding: '10px 14px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: isEditMode ? 'rgba(0,0,0,0.4)' : 'rgba(0,0,0,0.2)', color: isEditMode ? 'var(--text-muted)' : 'white' }}
                  placeholder="staff_tram1" />
              </div>

              {!isEditMode && (
                <div>
                  <label style={{ display: 'block', marginBottom: '6px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Mật khẩu (Password)</label>
                  <input type="password" required value={formData.password} onChange={e => setFormData({ ...formData, password: e.target.value })}
                    style={{ width: '100%', padding: '10px 14px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }}
                    placeholder="••••••••" />
                </div>
              )}

              <div style={{ display: 'flex', gap: '16px' }}>
                <div style={{ flex: 1 }}>
                  <label style={{ display: 'block', marginBottom: '6px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Số Điện thoại</label>
                  <input required value={formData.phone} onChange={e => setFormData({ ...formData, phone: e.target.value })}
                    style={{ width: '100%', padding: '10px 14px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }}
                    placeholder="0912345678" />
                </div>
                <div style={{ flex: 1 }}>
                  <label style={{ display: 'block', marginBottom: '6px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Email</label>
                  <input type="email" required value={formData.email} onChange={e => setFormData({ ...formData, email: e.target.value })}
                    style={{ width: '100%', padding: '10px 14px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }}
                    placeholder="user@example.com" />
                </div>
              </div>

              <div style={{ display: 'flex', gap: '16px' }}>
                {isAdmin && (
                  <div style={{ flex: 1 }}>
                    <label style={{ display: 'block', marginBottom: '6px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Vai trò (Role)</label>
                    <select value={formData.role} onChange={e => setFormData({ ...formData, role: e.target.value })}
                      style={{ width: '100%', padding: '10px 14px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }}>
                      <option value="Admin">Admin (Quản trị Hệ thống)</option>
                      <option value="Manager">Manager (Quản lý Trạm)</option>
                      <option value="Staff">Staff (Nhân viên Trạm)</option>
                    </select>
                  </div>
                )}

                <div style={{ flex: 1 }}>
                  <label style={{ display: 'block', marginBottom: '6px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Trạm Phân công (Station)</label>
                  {formData.role === 'Admin' ? (
                    <input disabled value="Tất cả các Trạm (Global Admin)"
                      style={{ width: '100%', padding: '10px 14px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(255,255,255,0.05)', color: 'var(--text-muted)' }} />
                  ) : isManager ? (
                    <input disabled value={getStationName(currentUserStationId || '')}
                      style={{ width: '100%', padding: '10px 14px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(255,255,255,0.05)', color: 'var(--text-muted)' }} />
                  ) : stations.length > 0 ? (
                    <select required value={formData.stationId} onChange={e => setFormData({ ...formData, stationId: e.target.value })}
                      style={{ width: '100%', padding: '10px 14px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }}>
                      {stations.map(st => (
                        <option key={st.id} value={st.id}>
                          {st.name}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <input required value={formData.stationId} onChange={e => setFormData({ ...formData, stationId: e.target.value })}
                      style={{ width: '100%', padding: '10px 14px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }}
                      placeholder="Nhập tên Trạm ..." />
                  )}
                </div>
              </div>

              <div style={{ display: 'flex', gap: '16px' }}>
                <div style={{ flex: 1 }}>
                  <label style={{ display: 'block', marginBottom: '6px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>Phương thức Cảnh báo</label>
                  <select required value={formData.notificationMethod} onChange={e => setFormData({ ...formData, notificationMethod: e.target.value })}
                    style={{ width: '100%', padding: '10px 14px', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'rgba(0,0,0,0.2)', color: 'white' }}>
                    <option value="ALL">Tất cả (Email & SMS)</option>
                    <option value="EMAIL">Chỉ Email</option>
                    <option value="SMS">Chỉ SMS</option>
                    <option value="NONE">Không nhận</option>
                  </select>
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
                <button type="button" onClick={resetForm} style={{ padding: '10px 20px', color: 'var(--text-secondary)', background: 'transparent', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)' }}>
                  Hủy
                </button>
                <button type="submit" disabled={isSubmitting} style={{ padding: '10px 20px', backgroundColor: 'var(--primary-color)', color: 'white', borderRadius: 'var(--radius-md)', fontWeight: 500, border: 'none' }}>
                  {isSubmitting ? 'Đang xử lý...' : isEditMode ? 'Lưu Thay đổi' : 'Tạo Tài Khoản'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* OTP Verification Modal */}
      {isOtpModalOpen && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 9999,
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <div className="glass-panel" style={{ width: '400px', padding: '24px' }}>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 600, color: 'white', marginBottom: '16px' }}>
              Xác thực OTP
            </h2>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '16px', fontSize: '0.875rem' }}>
              Một mã xác nhận (OTP) đã được gửi đến email <strong>{pendingUserPayload?.email}</strong>. Vui lòng kiểm tra và nhập mã vào bên dưới để hoàn tất đăng ký.
            </p>

            {message && (
              <div style={{
                padding: '12px', borderRadius: '4px', marginBottom: '16px',
                backgroundColor: message.type === 'success' ? 'rgba(34, 197, 94, 0.1)' : 'rgba(239, 68, 68, 0.1)',
                color: message.type === 'success' ? 'var(--success-color)' : 'var(--danger-color)',
                border: `1px solid ${message.type === 'success' ? 'rgba(34, 197, 94, 0.2)' : 'rgba(239, 68, 68, 0.2)'}`
              }}>
                {message.text}
              </div>
            )}

            <form onSubmit={handleVerifyOtp} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '4px' }}>
                  Mã OTP (6 chữ số)
                </label>
                <input
                  type="text"
                  maxLength={6}
                  value={otpValue}
                  onChange={(e) => setOtpValue(e.target.value)}
                  style={{
                    width: '100%', padding: '10px 16px', borderRadius: 'var(--radius-md)',
                    border: '1px solid rgba(255,255,255,0.1)', backgroundColor: 'rgba(0,0,0,0.2)',
                    color: 'white', fontSize: '1.2rem', textAlign: 'center', letterSpacing: '0.2em'
                  }}
                  required
                />
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '16px' }}>
                <button
                  type="button"
                  onClick={() => setIsOtpModalOpen(false)}
                  style={{
                    flex: 1, padding: '10px', borderRadius: 'var(--radius-md)',
                    backgroundColor: 'rgba(255,255,255,0.05)', color: 'white', border: 'none', cursor: 'pointer'
                  }}
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  style={{
                    flex: 1, padding: '10px', borderRadius: 'var(--radius-md)',
                    backgroundColor: 'var(--primary-color)', color: 'white', border: 'none',
                    fontWeight: 600, cursor: isSubmitting ? 'not-allowed' : 'pointer', opacity: isSubmitting ? 0.7 : 1
                  }}
                >
                  {isSubmitting ? 'Đang xác thực...' : 'Xác thực'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <style>{`
        .hover-row:hover { background-color: rgba(255,255,255,0.02); }
      `}</style>
    </div>
  );
}
