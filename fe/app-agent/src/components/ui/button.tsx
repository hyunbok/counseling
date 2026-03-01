'use client';

import { ButtonHTMLAttributes } from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
}

const variantClasses: Record<ButtonVariant, string> = {
  primary:
    'bg-indigo-600 text-white rounded-lg px-4 py-2 hover:bg-indigo-700 dark:bg-indigo-500 dark:hover:bg-indigo-400 transition-colors',
  secondary:
    'bg-white border border-gray-300 text-gray-700 rounded-lg px-4 py-2 hover:bg-gray-50 dark:bg-gray-800 dark:border-gray-600 dark:text-gray-200 dark:hover:bg-gray-700 transition-colors',
  ghost:
    'text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg px-3 py-2 dark:text-gray-400 dark:hover:text-gray-200 dark:hover:bg-gray-700 transition-colors',
  danger:
    'bg-red-600 text-white rounded-lg px-4 py-2 hover:bg-red-700 dark:bg-red-500 dark:hover:bg-red-400 transition-colors',
};

export const Button = ({ variant = 'primary', className = '', children, ...props }: ButtonProps) => {
  return (
    <button
      className={`inline-flex items-center justify-center font-medium focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed ${variantClasses[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
};
