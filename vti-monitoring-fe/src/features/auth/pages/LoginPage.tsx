import React, { useState } from 'react';
import { Mail, Lock, ShieldAlert } from 'lucide-react';
import { Input } from '../../../components/Input';
import { Button } from '../../../components/Button';

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    // Luồng call Gateway -> Keycloak authentication xử lý ở đây
    setTimeout(() => setLoading(false), 1500); 
  };

  return (
    <div className="min-h-screen flex bg-slate-950">
      {/* Cột trái: Form đăng nhập */}
      <div className="w-full lg:w-1/2 flex flex-col justify-center px-8 sm:px-16 lg:px-24">
        <div className="max-w-md w-full mx-auto">
          <div className="flex items-center gap-2 mb-8">
            <div className="p-2 bg-cyan-500/10 rounded-lg border border-cyan-500/30">
              <ShieldAlert className="h-6 w-6 text-cyan-400" />
            </div>
            <span className="text-xl font-bold tracking-wider text-slate-100">
              VTI TELEMETRY <span className="text-cyan-400 text-xs font-mono px-1.5 py-0.5 bg-cyan-950 border border-cyan-800 rounded">V5</span>
            </span>
          </div>

          <h2 className="text-3xl font-bold text-slate-100 tracking-tight mb-2">Hệ thống Đăng nhập</h2>
          <p className="text-slate-400 text-sm mb-8">Vui lòng nhập tài khoản vận hành để truy cập trung tâm dữ liệu.</p>

          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              label="Email Công Vụ"
              type="email"
              placeholder="name@vti.com"
              icon={<Mail size={18} />}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <Input
              label="Mật Khẩu"
              type="password"
              placeholder="••••••••"
              icon={<Lock size={18} />}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />

            <div className="flex items-center justify-between text-sm pt-2">
              <label className="flex items-center gap-2 cursor-pointer text-slate-400">
                <input type="checkbox" className="accent-cyan-500 rounded bg-slate-950 border-slate-800" />
                Duy trì phiên đăng nhập
              </label>
              <a href="#" className="text-cyan-400 hover:underline">Quên mật khẩu?</a>
            </div>

            <Button type="submit" isLoading={loading}>Xác Thực Hệ Thống</Button>
          </form>

          <p className="mt-8 text-center text-sm text-slate-500">
            Chưa có tài khoản vận hành?{' '}
            <a href="/register" className="text-cyan-400 font-medium hover:underline">Đăng ký tại đây</a>
          </p>
        </div>
      </div>

      {/* Cột phải: Giao diện đồ họa giả lập trạm quan trắc (Chỉ hiện trên Desktop) */}
      <div className="hidden lg:flex lg:w-1/2 bg-slate-900 border-l border-slate-800 items-center justify-center p-12 relative overflow-hidden">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(6,182,212,0.05)_0%,transparent_70%)]" />
        <div className="max-w-md text-center z-10">
          <div className="border border-slate-800 bg-slate-950/80 backdrop-blur p-6 rounded-2xl shadow-2xl text-left font-mono text-xs text-cyan-500 space-y-2">
            <p className="text-slate-500">// KẾT NỐI BROKER THÀNH CÔNG</p>
            <p>&gt; mosquitto -p 1883 --anonymous</p>
            <p className="text-green-400">[INFO] Kafka Topic 'telemetry-normalized' active.</p>
            <p className="text-yellow-400">[WARN] Service Ingestion stateless layer initialized.</p>
            <div className="pt-4 border-t border-slate-900 flex justify-between text-slate-400">
              <span>Bucket: telemetry_raw</span>
              <span className="text-green-400">ONLINE</span>
            </div>
          </div>
          <h3 className="text-xl font-semibold text-slate-200 mt-8 mb-2">Giám sát & Xử lý thời gian thực</h3>
          <p className="text-slate-400 text-sm">Tích hợp Apache Kafka và InfluxDB phục vụ phân tích chuỗi thời gian tần suất cao.</p>
        </div>
      </div>
    </div>
  );
};