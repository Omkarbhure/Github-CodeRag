'use client';

import React from 'react';
import Link from 'next/link';
import { Github, Heart } from 'lucide-react';

export function LandingFooter() {
  return (
    <footer className="border-t border-slate-200 bg-white py-10 text-xs text-slate-500">
      <div className="mx-auto max-w-6xl px-4 sm:px-6 space-y-6">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-600 font-bold text-white text-xs">
              CR
            </div>
            <span className="font-bold text-slate-800 text-sm">GitHub CodeRAG</span>
          </div>

          <div className="flex items-center gap-6 font-medium">
            <Link href="/login" className="hover:text-indigo-600 transition">Sign In</Link>
            <Link href="/signup" className="hover:text-indigo-600 transition">Create Account</Link>
            <Link href="/dashboard" className="hover:text-indigo-600 transition">Dashboard</Link>
            <a
              href="https://github.com"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1 hover:text-indigo-600 transition"
            >
              <Github className="h-3.5 w-3.5" />
              <span>GitHub</span>
            </a>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-between gap-2 border-t border-slate-100 pt-6 text-[11px] text-slate-400">
          <p>&copy; {new Date().getFullYear()} GitHub CodeRAG. All rights reserved.</p>
          <p className="flex items-center gap-1">
            Built with Spring Boot 3, Next.js 14, Qdrant, and Google Gemini.
          </p>
        </div>
      </div>
    </footer>
  );
}
