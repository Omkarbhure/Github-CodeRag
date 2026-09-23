'use client';

import React from 'react';
import Link from 'next/link';
import { BrandLogo } from '@/components/common/BrandLogo';
import { Github, Terminal } from 'lucide-react';

export function LandingFooter() {
  return (
    <footer className="border-t border-white/[0.08] bg-[#05070a] py-12 text-xs text-slate-400">
      <div className="mx-auto max-w-6xl px-4 sm:px-6 space-y-8">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-6">
          <BrandLogo size="sm" subtitle="Codebase Intelligence Engine" />

          <div className="flex items-center gap-6 font-medium text-slate-300">
            <Link href="/login" className="hover:text-white transition">Sign In</Link>
            <Link href="/signup" className="hover:text-white transition">Create Account</Link>
            <Link href="/dashboard" className="hover:text-white transition">Dashboard</Link>
            <a
              href="https://github.com"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1 hover:text-white transition"
            >
              <Github className="h-3.5 w-3.5" />
              <span>GitHub</span>
            </a>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 border-t border-white/[0.06] pt-6 text-[11px] text-slate-400 font-mono">
          <p>&copy; {new Date().getFullYear()} GitHub CodeRAG. Production-grade codebase RAG.</p>
          <p className="flex items-center gap-2">
            <span className="flex h-1.5 w-1.5 rounded-full bg-emerald-400" />
            <span>Spring Boot 3 • Next.js 14 • Qdrant 768-dim • Gemini</span>
          </p>
        </div>
      </div>
    </footer>
  );
}
