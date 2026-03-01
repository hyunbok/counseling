'use client';

import { ButtonHTMLAttributes } from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
}

const variantClasses: Record<ButtonVariant, string> = {
  primary:
    'bg-(--color-primary) text-white rounded-lg px-4 py-2 hover:bg-(--color-primary-hover) dark:bg-(--color-primary-dark) dark:hover:bg-(--color-primary-dark-hover) transition-colors',
  secondary:
    'bg-(--color-bg-base) border border-gray-300 text-(--color-text-secondary) rounded-lg px-4 py-2 hover:bg-(--color-bg-surface) dark:bg-(--color-bg-surface-dark) dark:border-gray-600 dark:text-(--color-text-secondary-dark) dark:hover:bg-(--color-bg-elevated-dark) transition-colors',
  ghost:
    'text-(--color-text-tertiary) hover:text-(--color-text-secondary) hover:bg-(--color-bg-surface) rounded-lg px-3 py-2 dark:text-(--color-text-tertiary-dark) dark:hover:text-(--color-text-secondary-dark) dark:hover:bg-(--color-bg-elevated-dark) transition-colors',
  danger:
    'bg-red-600 text-white rounded-lg px-4 py-2 hover:bg-red-700 dark:bg-red-500 dark:hover:bg-red-400 transition-colors',
};

export const Button = ({ variant = 'primary', className = '', children, ...props }: ButtonProps) => {
  return (
    <button
      className={`inline-flex items-center justify-center font-medium focus:outline-none focus:ring-2 focus:ring-(--color-primary) focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed ${variantClasses[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
};
