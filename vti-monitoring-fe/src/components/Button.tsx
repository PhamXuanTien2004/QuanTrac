import React from 'react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  isLoading?: boolean;
}

export const Button: React.FC<ButtonProps> = ({ children, isLoading, ...props }) => {
  return (
    <button
      {...props}
      disabled={isLoading || props.disabled}
      className="w-full flex justify-center items-center bg-cyan-500 hover:bg-cyan-600 text-slate-950 font-semibold py-2.5 px-4 rounded-lg transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-cyan-500/10 active:scale-[0.98]"
    >
      {isLoading ? (
        <svg className="animate-spin h-5 w-5 text-slate-950" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
        </svg>
      ) : (
        children
      )}
    </button>
  );
};