import axios from 'axios';
import React, { useState } from 'react';
import { Mail, Lock, User, Radio } from 'lucide-react';
import { Input } from '../../../components/Input';
import { Button } from '../../../components/Button';

export const RegisterPage: React.FC = () => {
  // Đưa tất cả useState vào BÊN TRONG Component theo đúng quy tắc React
  const [formData, setFormData] = useState({ name: '', email: '', role: 'ROLE_VIEWER', password: '', confirmPassword: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<boolean>(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(false);

    // Kiểm tra mật khẩu khớp nhau
    if (formData.password !== formData.confirmPassword) {
      setError('Mật khẩu xác nhận không trùng khớp.');
      setLoading(false);
      return;
    }

    try {
      // Gửi request POST qua API Gateway (Cổng 8080)
      const response = await axios.post('http://localhost:8480/api/v1/auth/register', {
        name: formData.name,
        email: formData.email,
        role: formData.role,
        password: formData.password
      }, {
        headers: {
          'Content-Type': 'application/json'
        }
      });

      if (response.status === 200 || response.status === 201) {
        setSuccess(true);
        // Reset form sạch sẽ sau khi thành công
        setFormData({ name: '', email: '', role: 'ROLE_VIEWER', password: '', confirmPassword: '' });
      }
    } catch (err: any) {
      if (err.response && err.response.data) {
        setError(err.response.data.message || 'Đăng ký thất bại. Vui lòng kiểm tra lại.');
      } else {
        setError('Không thể kết nối đến hệ thống (Gateway Timeout).');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex bg-slate-950">
      <div className="w-full lg:w-1/2 flex flex-col justify-center px-8 sm:px-16 lg:px-24 py-12">
        <div className="max-w-md w-full mx-auto">
          <h2 className="text-3xl font-bold text-slate-100 tracking-tight mb-2">Đăng Ký Tài Khoản</h2>
          <p className="text-slate-400 text-sm mb-8">Yêu cầu quyền truy cập vào các trạm quan trắc từ xa.</p>

          {/* Khu vực hiển thị thông báo trạng thái trực quan */}
          {error && (
            <div className="mb-4 p-3 bg-red-500/10 border border-red-500/30 rounded-lg text-sm text-red-400 font-mono">
              [ERROR] {error}
            </div>
          )}

          {success && (
            <div className="mb-4 p-3 bg-green-500/10 border border-green-500/30 rounded-lg text-sm text-green-400 font-mono">
              [SUCCESS] Gửi yêu cầu đăng ký thành công! Vui lòng chờ phê duyệt từ Admin trên Keycloak.
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label="Họ và Tên"
              type="text"
              placeholder="Phạm Xuân Tiến"
              icon={<User size={18} />}
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
            />
            <Input
              label="Email Công Vụ"
              type="email"
              placeholder="tien.px@vti.com"
              icon={<Mail size={18} />}
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              required
            />
            
            {/* Vai trò vận hành */}
            <div className="mb-4">
              <label className="block text-sm font-medium text-slate-400 mb-1.5">Vai trò Vận hành Mong muốn</label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
                  <Radio size={18} />
                </div>
                <select
                  value={formData.role}
                  onChange={(e) => setFormData({ ...formData, role: e.target.value })}
                  className="block w-full bg-slate-950 border border-slate-800 rounded-lg py-2.5 pl-10 pr-3 text-slate-300 focus:outline-none focus:ring-2 focus:ring-cyan-500/50"
                >
                  <option value="ROLE_VIEWER">ROLE_VIEWER (Chỉ xem báo cáo)</option>
                  <option value="ROLE_OPERATOR">ROLE_OPERATOR (Quản lý thiết bị/Cảnh báo)</option>
                  <option value="ROLE_ADMIN">ROLE_ADMIN (Toàn quyền hệ thống)</option>
                </select>
              </div>
            </div>

            <Input
              label="Mật Khẩu"
              type="password"
              placeholder="••••••••"
              icon={<Lock size={18} />}
              value={formData.password}
              onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              required
            />
            <Input
              label="Xác Nhận Mật Khẩu"
              type="password"
              placeholder="••••••••"
              icon={<Lock size={18} />}
              value={formData.confirmPassword}
              onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
              required
            />

            <Button type="submit" isLoading={loading}>Gửi Yêu Cầu Phê Duyệt</Button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-500">
            Đã có tài khoản?{' '}
            <a href="/login" className="text-cyan-400 font-medium hover:underline">Quay lại đăng nhập</a>
          </p>
        </div>
      </div>

      {/* Cột phải đồ họa */}
      <div className="hidden lg:flex lg:w-1/2 bg-slate-900 border-l border-slate-800 items-center justify-center p-12 relative overflow-hidden">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(6,182,212,0.05)_0%,transparent_70%)]" />
        <div className="max-w-md text-center z-10 text-slate-400 font-mono text-sm">
          <p className="text-cyan-400 font-bold text-lg mb-4">[ PHÂN QUYỀN HỆ THỐNG - RBAC ]</p>
          <p className="text-left">Mọi tài khoản sau khi đăng ký sẽ được đẩy vào nhóm lưu trữ tạm thời của Keycloak IAM và cần được phê duyệt thủ công bởi Quản trị viên để kích hoạt Token hợp lệ.</p>
        </div>
      </div>
    </div>
  );
};