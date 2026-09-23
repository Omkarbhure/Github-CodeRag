'use client';

import React from 'react';
import Link from 'next/link';

interface BrandLogoProps {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  showText?: boolean;
  clickable?: boolean;
  subtitle?: string;
  className?: string;
}

export function BrandLogo({
  size = 'md',
  showText = true,
  clickable = true,
  subtitle = 'Codebase Intelligence',
  className = '',
}: BrandLogoProps) {
  const sizeMap = {
    sm: {
      box: 'h-7 w-7 rounded-lg',
      svg: 'h-4 w-4',
      px: 16,
      boxPx: 28,
      title: 'text-xs',
      sub: 'text-[9px]',
    },
    md: {
      box: 'h-9 w-9 rounded-xl',
      svg: 'h-5 w-5',
      px: 20,
      boxPx: 36,
      title: 'text-sm',
      sub: 'text-[10px]',
    },
    lg: {
      box: 'h-11 w-11 rounded-2xl',
      svg: 'h-6 w-6',
      px: 24,
      boxPx: 44,
      title: 'text-base',
      sub: 'text-xs',
    },
    xl: {
      box: 'h-14 w-14 rounded-2xl',
      svg: 'h-7 w-7',
      px: 28,
      boxPx: 56,
      title: 'text-xl',
      sub: 'text-sm',
    },
  };

  const current = sizeMap[size];

  const logoIcon = (
    <div
      style={{ width: current.boxPx, height: current.boxPx, minWidth: current.boxPx, minHeight: current.boxPx }}
      className={`relative flex shrink-0 items-center justify-center bg-gradient-to-tr from-indigo-600 via-violet-600 to-purple-600 text-white shadow-md shadow-indigo-500/25 ring-1 ring-white/20 transition-all duration-300 group-hover:scale-105 group-hover:shadow-indigo-500/40 ${current.box}`}
    >
      {/* Subtle Inner Glow Highlight */}
      <div className="absolute inset-0 rounded-[inherit] bg-gradient-to-b from-white/25 to-transparent pointer-events-none" />

      {/* Modern High-Tech CodeRAG SVG Emblem */}
      <svg
        width={current.px}
        height={current.px}
        viewBox="0 0 24 24"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        style={{ width: current.px, height: current.px }}
        className={`${current.svg} shrink-0 text-white drop-shadow-sm`}
      >
        {/* Left Code Chevron */}
        <path
          d="M7.5 7.5L3.5 12L7.5 16.5"
          stroke="currentColor"
          strokeWidth="2.2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        {/* Right Code Chevron */}
        <path
          d="M16.5 7.5L20.5 12L16.5 16.5"
          stroke="currentColor"
          strokeWidth="2.2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        {/* Central Neural / AI Vector Spark Core */}
        <path
          d="M12 4.5V19.5"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeDasharray="1 3"
          className="opacity-70"
        />
        <circle cx="12" cy="12" r="2.5" fill="currentColor" />
        <circle cx="12" cy="6" r="1.25" fill="currentColor" className="opacity-90" />
        <circle cx="12" cy="18" r="1.25" fill="currentColor" className="opacity-90" />
      </svg>
    </div>
  );

  const logoContent = (
    <div className={`flex items-center gap-2.5 group select-none ${className}`}>
      {logoIcon}
      {showText && (
        <div className="flex flex-col">
          <div className={`font-bold tracking-tight text-white leading-tight ${current.title}`}>
            <span>GitHub </span>
            <span className="bg-gradient-to-r from-indigo-400 via-purple-400 to-violet-400 bg-clip-text text-transparent">
              CodeRAG
            </span>
          </div>
          {subtitle && (
            <span className={`font-medium text-slate-400 leading-none ${current.sub}`}>
              {subtitle}
            </span>
          )}
        </div>
      )}
    </div>
  );

  if (clickable) {
    return (
      <Link href="/" className="inline-flex focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 rounded-xl">
        {logoContent}
      </Link>
    );
  }

  return logoContent;
}
