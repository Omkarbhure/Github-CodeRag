'use client';

import React from 'react';
import { FileCode2 } from 'lucide-react';

interface CitationChipProps {
  filePath: string;
  startLine: number;
  endLine: number;
  isActive?: boolean;
  onClick: (filePath: string, startLine: number, endLine: number) => void;
}

export function CitationChip({
  filePath,
  startLine,
  endLine,
  isActive = false,
  onClick
}: CitationChipProps) {
  // Extract filename for concise display
  const fileName = filePath.split('/').pop() || filePath;
  const lineLabel = startLine === endLine ? `L${startLine}` : `L${startLine}-${endLine}`;
  const displayLabel = startLine === endLine ? `${fileName}:${startLine}` : `${fileName}:${startLine}-${endLine}`;

  return (
    <button
      type="button"
      onClick={(e) => {
        e.stopPropagation();
        onClick(filePath, startLine, endLine);
      }}
      title={`Open ${filePath} (${lineLabel})`}
      className={`inline-flex items-center gap-1.5 mx-1 my-0.5 px-2.5 py-0.5 rounded-md font-mono text-xs font-medium tracking-tight transition shadow-sm border align-baseline cursor-pointer ${
        isActive
          ? 'bg-indigo-600 text-white border-indigo-400 ring-2 ring-indigo-500/50'
          : 'bg-indigo-950/70 hover:bg-indigo-900/90 text-indigo-300 hover:text-white border-indigo-500/40 hover:border-indigo-400'
      }`}
    >
      <FileCode2 className={`h-3.5 w-3.5 shrink-0 ${isActive ? 'text-white' : 'text-indigo-400'}`} />
      <span>{displayLabel}</span>
    </button>
  );
}
