'use client';

import React from 'react';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { GITHUB_AUTH_URL } from '@/lib/api';
import {
  Sparkles,
  ArrowRight,
  Github,
  Search,
  Bot,
  FileCode2,
  CheckCircle2,
  Cpu
} from 'lucide-react';

export function HeroSection() {
  const { isAuthenticated, loading } = useAuth();

  return (
    <section className="relative overflow-hidden pt-12 pb-16 md:pt-20 md:pb-24">
      {/* Subtle Background Glow */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[350px] bg-gradient-to-tr from-indigo-500/15 via-purple-500/15 to-blue-500/10 rounded-full blur-3xl -z-10 pointer-events-none" />

      <div className="mx-auto max-w-5xl px-4 sm:px-6 text-center space-y-6">
        {/* Pill Badge */}
        <div className="inline-flex items-center gap-2 rounded-full border border-indigo-200/80 bg-indigo-50/80 px-3.5 py-1 text-xs font-semibold text-indigo-700 shadow-sm backdrop-blur-sm">
          <Sparkles className="h-3.5 w-3.5 text-indigo-600" />
          <span>AI Codebase Intelligence with Grounded Citations</span>
        </div>

        {/* Headline */}
        <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 sm:text-5xl md:text-6xl max-w-4xl mx-auto leading-tight sm:leading-none">
          Ask questions about any GitHub repo and get{' '}
          <span className="bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent">
            grounded, cited answers
          </span>
        </h1>

        {/* Subtitle Pitch */}
        <p className="mx-auto max-w-2xl text-sm sm:text-base text-slate-600 leading-relaxed">
          Index public repositories with hybrid vector and full-text keyword search. Chat with an AI assistant that links directly to verified source lines in Monaco Editor, synthesize architecture overviews, and diagnose bugs from stack traces.
        </p>

        {/* CTA Buttons */}
        <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4">
          {!loading && isAuthenticated ? (
            <Link
              href="/dashboard"
              className="flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl bg-indigo-600 px-6 py-3 text-sm font-semibold text-white shadow-md transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2"
            >
              <span>Go to Dashboard</span>
              <ArrowRight className="h-4 w-4" />
            </Link>
          ) : (
            <>
              <Link
                href="/signup"
                className="flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl bg-indigo-600 px-6 py-3 text-sm font-semibold text-white shadow-md transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2"
              >
                <span>Get Started Free</span>
                <ArrowRight className="h-4 w-4" />
              </Link>
              <a
                href={GITHUB_AUTH_URL}
                className="flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-5 py-3 text-sm font-semibold text-slate-800 shadow-sm transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-slate-900 focus:ring-offset-2"
              >
                <Github className="h-4 w-4" />
                <span>Continue with GitHub</span>
              </a>
            </>
          )}
        </div>

        {/* Feature Highlights Bar */}
        <div className="pt-8 grid grid-cols-2 md:grid-cols-4 gap-3 max-w-3xl mx-auto text-left">
          <div className="flex items-center gap-2 rounded-xl border border-slate-200/80 bg-white/70 p-3 shadow-sm backdrop-blur-sm">
            <CheckCircle2 className="h-4 w-4 text-emerald-500 shrink-0" />
            <span className="text-xs font-semibold text-slate-800">Hybrid Search Fusion</span>
          </div>
          <div className="flex items-center gap-2 rounded-xl border border-slate-200/80 bg-white/70 p-3 shadow-sm backdrop-blur-sm">
            <CheckCircle2 className="h-4 w-4 text-emerald-500 shrink-0" />
            <span className="text-xs font-semibold text-slate-800">Line-Level Citations</span>
          </div>
          <div className="flex items-center gap-2 rounded-xl border border-slate-200/80 bg-white/70 p-3 shadow-sm backdrop-blur-sm">
            <CheckCircle2 className="h-4 w-4 text-emerald-500 shrink-0" />
            <span className="text-xs font-semibold text-slate-800">Monaco Split Viewer</span>
          </div>
          <div className="flex items-center gap-2 rounded-xl border border-slate-200/80 bg-white/70 p-3 shadow-sm backdrop-blur-sm">
            <CheckCircle2 className="h-4 w-4 text-emerald-500 shrink-0" />
            <span className="text-xs font-semibold text-slate-800">Bug Root Cause Fixes</span>
          </div>
        </div>
      </div>
    </section>
  );
}
