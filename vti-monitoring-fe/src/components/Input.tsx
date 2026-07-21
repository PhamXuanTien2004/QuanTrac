import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  icon?: React.ReactNode;
  error?: string;
}

export const Input: React.FC<InputProps> = ({ label, icon, error, ...props }) => {
  return (
    <div className="mb-4">
      <label className="block text-sm font-medium text-slate-400 mb-1.5">{label}</label>
      <div className="relative rounded-lg shadow-sm">
        {icon && (
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
            {icon}
          </div>
        )}
        <input
          {...props}
          className={`block w-full bg-slate-950 border rounded-lg py-2.5 ${icon ? 'pl-10' : 'pl-3'} pr-3 text-slate-200 placeholder-slate-600 focus:outline-none focus:ring-2 focus:ring-cyan-500/50 transition-all duration-200 ${
            error ? 'border-red-500 focus:border-red-500' : 'border-slate-800 focus:border-cyan-500'
          }`}
        />
      </div>
      {error && <p className="mt-1 text-xs text-red-500">{error}</p>}
    </div>
  );
};