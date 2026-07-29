import { useState, useEffect } from 'react';
import { Calendar, Clock as ClockIcon } from 'lucide-react';

export default function Clock() {
  const [time, setTime] = useState(new Date());

  useEffect(() => {
    const timer = setInterval(() => {
      setTime(new Date());
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  const formatDate = (date: Date) => {
    const options: Intl.DateTimeFormatOptions = { 
      weekday: 'long', 
      year: 'numeric', 
      month: '2-digit', 
      day: '2-digit' 
    };
    return date.toLocaleDateString('vi-VN', options);
  };

  const formatTime = (date: Date) => {
    return date.toLocaleTimeString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '16px', color: 'var(--text-secondary)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
        <Calendar size={16} />
        <span style={{ fontSize: '0.85rem', fontWeight: 500 }}>{formatDate(time)}</span>
      </div>
      <div style={{ width: '1px', height: '14px', backgroundColor: 'var(--border-color)' }}></div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--primary-color)' }}>
        <ClockIcon size={16} />
        <span style={{ fontSize: '1.1rem', fontWeight: 700, fontFamily: 'monospace' }}>{formatTime(time)}</span>
      </div>
    </div>
  );
}
