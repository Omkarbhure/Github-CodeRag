'use client';

import React from 'react';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { Bot, ArrowRight, LayoutDashboard } from 'lucide-react';

export function LandingNavbar() {
  const { isAuthenticated, user, loading } = useAuth();

  return (
    <header className="sticky top-0 z-50 border-b border-slate-200/80 bg-white/80 backdrop-blur-md">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3.5 sm:px-6">
        {/* Brand Logo */}
        <Link href="/" className="flex items-center gap-2.5 group">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-600 font-bold text-white shadow-sm transition group-hover:bg-indigo-500">
            CR
          </div>
          <div className="flex flex-col">
            <span className="text-sm font-bold text-slate-900 leading-tight">GitHub CodeRAG</span>
            <span className="text-[10px] font-medium text-slate-500">Codebase Intelligence</span>
          </div>
        </Link>


        {/* Actions */}
        <div className="flex items-center gap-3">
          {!loading && isAuthenticated ? (
            <Link
              href="/dashboard"
              className="flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2"
            >
              <LayoutDashboard className="h-3.5 w-3.5" />
              <span>Go to Dashboard</span>
              <ArrowRight className="h-3.5 w-3.5 ml-0.5" />
            </Link>
          ) : (
            <>
              <Link
                href="/login"
                className="rounded-xl px-3.5 py-2 text-xs font-semibold text-slate-700 hover:text-indigo-600 transition"
              >
                Sign In
              </Link>
              <Link
                href="/signup"
                className="flex items-center gap-1.5 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white shadow-sm transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2"
              >
                <span>Get Started</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
