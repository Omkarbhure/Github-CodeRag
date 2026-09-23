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
      className={`inline-flex items-center gap-1.5 mx-1 my-0.5 px-2.5 py-0.5 rounded-md font-mono text-xs font-semibold tracking-tight transition shadow-sm border align-baseline cursor-pointer ${
        isActive
          ? 'bg-indigo-600 text-white border-indigo-700 ring-2 ring-indigo-300'
          : 'bg-indigo-50 hover:bg-indigo-100 text-indigo-700 hover:text-indigo-900 border-indigo-200'
      }`}
    >
      <FileCode2 className={`h-3.5 w-3.5 shrink-0 ${isActive ? 'text-white' : 'text-indigo-600'}`} />
      <span>{displayLabel}</span>
    </button>
  );
}
