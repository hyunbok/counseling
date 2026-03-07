'use client';

import { useState } from 'react';
import { SidebarLayout } from '@/components/layout/sidebar-layout';
import useAuthStore from '@/stores/auth-store';
import api from '@/lib/api';

export default function ProfilePage() {
  const user = useAuthStore((s) => s.user);
  const updateUser = useAuthStore((s) => s.updateUser);

  // Name editing
  const [isEditingName, setIsEditingName] = useState(false);
  const [editName, setEditName] = useState('');
  const [nameStatus, setNameStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
  const [nameError, setNameError] = useState('');

  const handleStartEditName = () => {
    setEditName(user?.name ?? '');
    setIsEditingName(true);
    setNameStatus('idle');
    setNameError('');
  };

  const handleCancelEditName = () => {
    setIsEditingName(false);
    setNameStatus('idle');
    setNameError('');
  };

  const handleSaveName = async () => {
    const trimmed = editName.trim();
    if (!trimmed) {
      setNameError('이름을 입력해주세요.');
      return;
    }
    if (trimmed === user?.name) {
      setIsEditingName(false);
      return;
    }

    setNameStatus('loading');
    setNameError('');
    try {
      await api.put('/api/auth/name', { name: trimmed });
      updateUser({ name: trimmed });
      setNameStatus('success');
      setIsEditingName(false);
    } catch {
      setNameStatus('error');
      setNameError('이름 변경에 실패했습니다.');
    }
  };

  // Password change
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [pwStatus, setPwStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
  const [pwErrors, setPwErrors] = useState<{ current?: string; new?: string; confirm?: string; general?: string }>({});

  const resetPwStatus = () => {
    if (pwStatus !== 'idle') setPwStatus('idle');
    if (Object.keys(pwErrors).length > 0) setPwErrors({});
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    const errors: typeof pwErrors = {};

    if (!currentPassword.trim()) errors.current = '현재 비밀번호를 입력해주세요.';
    if (!newPassword.trim()) {
      errors.new = '새 비밀번호를 입력해주세요.';
    } else if (newPassword.length < 6) {
      errors.new = '새 비밀번호는 6자 이상이어야 합니다.';
    } else if (currentPassword === newPassword) {
      errors.new = '현재 비밀번호와 다른 비밀번호를 입력해주세요.';
    }
    if (!confirmPassword.trim()) {
      errors.confirm = '새 비밀번호 확인을 입력해주세요.';
    } else if (newPassword && newPassword !== confirmPassword) {
      errors.confirm = '새 비밀번호가 일치하지 않습니다.';
    }

    if (Object.keys(errors).length > 0) {
      setPwErrors(errors);
      return;
    }

    setPwStatus('loading');
    setPwErrors({});
    try {
      await api.put('/api/auth/password', { currentPassword, newPassword });
      setPwStatus('success');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch {
      setPwStatus('error');
      setPwErrors({ current: '현재 비밀번호가 올바르지 않습니다.' });
    }
  };

  return (
    <SidebarLayout>
      <div className="p-6 space-y-6 max-w-2xl">
        <h1 className="text-2xl font-semibold text-(--color-text-primary) dark:text-(--color-text-primary-dark)">
          프로필
        </h1>

        {/* 내 정보 */}
        <div className="rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
          <div className="px-4 py-2.5 bg-gray-50 dark:bg-gray-800/50 border-b border-gray-200 dark:border-gray-700">
            <h2 className="text-xs font-semibold uppercase tracking-wider text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">내 정보</h2>
          </div>
          <div className="divide-y divide-gray-100 dark:divide-gray-700/50">
            {/* 이름 (편집 가능) */}
            <div className="flex items-center justify-between px-4 py-3 text-sm">
              <span className="text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">이름</span>
              {isEditingName ? (
                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') handleSaveName();
                      if (e.key === 'Escape') handleCancelEditName();
                    }}
                    autoFocus
                    className="w-40 rounded-lg border border-gray-300 dark:border-gray-600 bg-(--color-bg-base) dark:bg-(--color-bg-base-dark) px-2 py-1 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                  <button
                    onClick={handleSaveName}
                    disabled={nameStatus === 'loading'}
                    className="rounded-md bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 px-2.5 py-1 text-xs font-medium text-white transition-colors"
                  >
                    {nameStatus === 'loading' ? '저장 중...' : '저장'}
                  </button>
                  <button
                    onClick={handleCancelEditName}
                    className="rounded-md border border-gray-300 dark:border-gray-600 px-2.5 py-1 text-xs font-medium text-(--color-text-secondary) dark:text-(--color-text-secondary-dark) hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
                  >
                    취소
                  </button>
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <span className="text-(--color-text-primary) dark:text-(--color-text-primary-dark) font-medium">{user?.name ?? '-'}</span>
                  <button
                    onClick={handleStartEditName}
                    className="rounded-md px-2 py-0.5 text-xs text-indigo-600 dark:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-900/20 transition-colors"
                  >
                    편집
                  </button>
                </div>
              )}
            </div>
            {nameError && (
              <div className="px-4 py-2">
                <p className="text-xs text-red-600 dark:text-red-400">{nameError}</p>
              </div>
            )}
            {/* 나머지 정보 (읽기 전용) */}
            {[
              ['아이디', user?.username ?? '-'],
              ['역할', user?.role === 'ADMIN' ? '관리자' : user?.role === 'COUNSELOR' ? '상담사' : user?.role ?? '-'],
              ['그룹', user?.groupName ?? '-'],
            ].map(([label, value]) => (
              <div key={label} className="flex items-center justify-between px-4 py-3 text-sm">
                <span className="text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">{label}</span>
                <span className="text-(--color-text-primary) dark:text-(--color-text-primary-dark) font-medium">{value}</span>
              </div>
            ))}
          </div>
        </div>

        {/* 비밀번호 변경 */}
        <div className="rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
          <div className="px-4 py-2.5 bg-gray-50 dark:bg-gray-800/50 border-b border-gray-200 dark:border-gray-700">
            <h2 className="text-xs font-semibold uppercase tracking-wider text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">비밀번호 변경</h2>
          </div>
          <form onSubmit={handleChangePassword} className="px-4 py-4 space-y-4">
            <div className="space-y-1">
              <label htmlFor="currentPassword" className="block text-sm text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">현재 비밀번호</label>
              <input
                id="currentPassword"
                type="password"
                value={currentPassword}
                onChange={(e) => { setCurrentPassword(e.target.value); resetPwStatus(); }}
                autoComplete="current-password"
                className={`w-full rounded-lg border ${pwErrors.current ? 'border-red-400 dark:border-red-500' : 'border-gray-300 dark:border-gray-600'} bg-(--color-bg-base) dark:bg-(--color-bg-base-dark) px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-indigo-500`}
              />
              {pwErrors.current && <p className="text-xs text-red-600 dark:text-red-400">{pwErrors.current}</p>}
            </div>
            <div className="space-y-1">
              <label htmlFor="newPassword" className="block text-sm text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">새 비밀번호</label>
              <input
                id="newPassword"
                type="password"
                value={newPassword}
                onChange={(e) => { setNewPassword(e.target.value); resetPwStatus(); }}
                autoComplete="new-password"
                className={`w-full rounded-lg border ${pwErrors.new ? 'border-red-400 dark:border-red-500' : 'border-gray-300 dark:border-gray-600'} bg-(--color-bg-base) dark:bg-(--color-bg-base-dark) px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-indigo-500`}
              />
              {pwErrors.new ? (
                <p className="text-xs text-red-600 dark:text-red-400">{pwErrors.new}</p>
              ) : (
                <p className="text-xs text-(--color-text-tertiary) dark:text-(--color-text-tertiary-dark)">6자 이상 입력해주세요.</p>
              )}
            </div>
            <div className="space-y-1">
              <label htmlFor="confirmPassword" className="block text-sm text-(--color-text-secondary) dark:text-(--color-text-secondary-dark)">새 비밀번호 확인</label>
              <input
                id="confirmPassword"
                type="password"
                value={confirmPassword}
                onChange={(e) => { setConfirmPassword(e.target.value); resetPwStatus(); }}
                autoComplete="new-password"
                className={`w-full rounded-lg border ${pwErrors.confirm ? 'border-red-400 dark:border-red-500' : 'border-gray-300 dark:border-gray-600'} bg-(--color-bg-base) dark:bg-(--color-bg-base-dark) px-3 py-2 text-sm text-(--color-text-primary) dark:text-(--color-text-primary-dark) focus:outline-none focus:ring-2 focus:ring-indigo-500`}
              />
              {pwErrors.confirm && <p className="text-xs text-red-600 dark:text-red-400">{pwErrors.confirm}</p>}
            </div>

            {pwStatus === 'success' && (
              <p className="text-sm text-green-600 dark:text-green-400">비밀번호가 변경되었습니다.</p>
            )}

            <button
              type="submit"
              disabled={pwStatus === 'loading'}
              className="rounded-lg bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 px-4 py-2 text-sm font-medium text-white transition-colors"
            >
              {pwStatus === 'loading' ? '변경 중...' : '비밀번호 변경'}
            </button>
          </form>
        </div>
      </div>
    </SidebarLayout>
  );
}
