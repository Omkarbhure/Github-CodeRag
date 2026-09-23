'use client';

import React, { useState } from 'react';
import { CitationChip } from './CitationChip';
import { Check, Copy, Hash, ChevronRight, Terminal, Sparkles, Layers } from 'lucide-react';

interface GroundedMarkdownProps {
  content: string;
  onCitationClick?: (filePath: string, startLine: number, endLine: number) => void;
  activeCitation?: { filePath: string; startLine: number; endLine: number } | null;
  className?: string;
}

export function GroundedMarkdown({
  content,
  onCitationClick,
  activeCitation,
  className = ''
}: GroundedMarkdownProps) {
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);

  const handleCopy = (text: string, index: number) => {
    navigator.clipboard.writeText(text);
    setCopiedIndex(index);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  // Parses inline formatting: bold, inline code, and citations
  const renderInline = (text: string, keyPrefix: string): React.ReactNode[] => {
    // 1. Bracketed citations: [path:1-2] or [path1:1, path2:2-3]
    const bracketRegex = /\[([\w\-./\s,:]+:\d+(?:-\d+)?(?:,[\w\-./\s,:]+:\d+(?:-\d+)?)*)\]/g;
    const tokens: React.ReactNode[] = [];
    bracketRegex.lastIndex = 0;
    let match: RegExpExecArray | null;

    const citationSegments: {
      start: number;
      end: number;
      full: string;
      citations: { path: string; startLine: number; endLine: number }[];
    }[] = [];

    while ((match = bracketRegex.exec(text)) !== null) {
      const inner = match[1];
      const subItems = inner.split(',').map((s) => s.trim());
      const parsedSubs: { path: string; startLine: number; endLine: number }[] = [];
      let allValid = true;

      for (const item of subItems) {
        const itemMatch = item.match(/^([a-zA-Z0-9_\-./]+):(\d+)(?:-(\d+))?$/);
        if (itemMatch) {
          const path = itemMatch[1];
          const startL = parseInt(itemMatch[2], 10);
          const endL = itemMatch[3] ? parseInt(itemMatch[3], 10) : startL;
          parsedSubs.push({ path, startLine: startL, endLine: endL });
        } else {
          allValid = false;
          break;
        }
      }

      if (allValid && parsedSubs.length > 0) {
        citationSegments.push({
          start: match.index,
          end: match.index + match[0].length,
          full: match[0],
          citations: parsedSubs
        });
      }
    }

    let cursor = 0;
    citationSegments.forEach((seg, segIdx) => {
      if (seg.start > cursor) {
        const textBefore = text.substring(cursor, seg.start);
        tokens.push(...renderFormattedText(textBefore, `${keyPrefix}-txt-${segIdx}`));
      }

      // Render citation chips
      seg.citations.forEach((c, cIdx) => {
        const isActive =
          activeCitation?.filePath === c.path &&
          activeCitation?.startLine === c.startLine &&
          activeCitation?.endLine === c.endLine;

        tokens.push(
          <CitationChip
            key={`${keyPrefix}-chip-${segIdx}-${cIdx}-${c.path}-${c.startLine}`}
            filePath={c.path}
            startLine={c.startLine}
            endLine={c.endLine}
            isActive={isActive}
            onClick={onCitationClick || (() => {})}
          />
        );
      });

      cursor = seg.end;
    });

    if (cursor < text.length) {
      tokens.push(...renderFormattedText(text.substring(cursor), `${keyPrefix}-tail`));
    }

    return tokens;
  };

  // Helper to render bold **text** and inline `code` with deep, bold high-contrast styling
  const renderFormattedText = (raw: string, prefix: string): React.ReactNode[] => {
    const nodes: React.ReactNode[] = [];
    const inlineRegex = /(`[^`]+`|\*\*[^*]+\*\*|__[^_]+__)/g;
    let lastIdx = 0;
    let match: RegExpExecArray | null;
    let k = 0;

    while ((match = inlineRegex.exec(raw)) !== null) {
      if (match.index > lastIdx) {
        nodes.push(
          <span key={`${prefix}-p-${k++}`} className="text-slate-900 font-medium">
            {raw.substring(lastIdx, match.index)}
          </span>
        );
      }

      const tok = match[0];
      if (tok.startsWith('`') && tok.endsWith('`')) {
        nodes.push(
          <code
            key={`${prefix}-c-${k++}`}
            className="inline-block mx-1 my-0.5 rounded-md bg-indigo-50/90 border border-indigo-200/80 px-2 py-0.5 font-mono text-[11.5px] font-bold text-indigo-900 shadow-xs"
          >
            {tok.slice(1, -1)}
          </code>
        );
      } else if ((tok.startsWith('**') && tok.endsWith('**')) || (tok.startsWith('__') && tok.endsWith('__'))) {
        nodes.push(
          <strong
            key={`${prefix}-b-${k++}`}
            className="font-extrabold text-slate-950 px-0.5 text-[13px] sm:text-[13.5px] tracking-tight"
          >
            {tok.slice(2, -2)}
          </strong>
        );
      }

      lastIdx = inlineRegex.lastIndex;
    }

    if (lastIdx < raw.length) {
      nodes.push(
        <span key={`${prefix}-end-${k++}`} className="text-slate-900 font-medium">
          {raw.substring(lastIdx)}
        </span>
      );
    }

    return nodes;
  };

  // Split content into blocks: code fences vs markdown paragraphs/lists/headers
  const blocks = content.split(/(```[\s\S]*?```)/g);

  return (
    <div className={`space-y-4 text-[13px] sm:text-sm text-slate-900 leading-relaxed ${className}`}>
      {blocks.map((block, blockIdx) => {
        if (!block) return null;

        // Fenced code block
        if (block.startsWith('```') && block.endsWith('```')) {
          const lines = block.slice(3, -3).trim().split('\n');
          let language = 'code';
          let codeBody = block.slice(3, -3).trim();

          if (lines.length > 0 && /^[a-zA-Z0-9_\-#+.]+$/.test(lines[0].trim())) {
            language = lines[0].trim();
            codeBody = lines.slice(1).join('\n');
          }

          const isCopied = copiedIndex === blockIdx;

          return (
            <div
              key={`block-${blockIdx}`}
              className="my-3.5 overflow-hidden rounded-xl border border-slate-800 bg-slate-950 shadow-md"
            >
              <div className="flex items-center justify-between border-b border-slate-800 bg-slate-900/90 px-3.5 py-1.5 text-[11px] font-mono text-slate-300">
                <div className="flex items-center gap-1.5 font-bold text-slate-200">
                  <Terminal className="h-3.5 w-3.5 text-indigo-400" />
                  <span>{language}</span>
                </div>
                <button
                  type="button"
                  onClick={() => handleCopy(codeBody, blockIdx)}
                  className="flex items-center gap-1 rounded-md px-2.5 py-1 text-[11px] font-bold text-slate-200 transition hover:bg-slate-800 hover:text-white"
                >
                  {isCopied ? (
                    <>
                      <Check className="h-3.5 w-3.5 text-emerald-400" />
                      <span className="text-emerald-400 font-bold">Copied</span>
                    </>
                  ) : (
                    <>
                      <Copy className="h-3.5 w-3.5" />
                      <span>Copy</span>
                    </>
                  )}
                </button>
              </div>
              <pre className="overflow-x-auto p-4 font-mono text-xs font-medium text-slate-100 whitespace-pre leading-relaxed">
                {codeBody}
              </pre>
            </div>
          );
        }

        // Regular Markdown lines (Headings, bullet lists, paragraphs)
        const lines = block.split('\n');
        const elements: React.ReactNode[] = [];
        let i = 0;

        while (i < lines.length) {
          const line = lines[i];
          const trimmed = line.trim();

          if (!trimmed) {
            i++;
            continue;
          }

          // Header 1: # Title
          if (trimmed.startsWith('# ')) {
            elements.push(
              <div key={`h1-${blockIdx}-${i}`} className="mt-6 mb-3 pb-2.5 border-b-2 border-indigo-100">
                <h1 className="text-lg sm:text-xl font-extrabold text-slate-950 flex items-center gap-2.5 tracking-tight">
                  <Hash className="h-5 w-5 text-indigo-600 shrink-0" />
                  <span>{renderInline(trimmed.substring(2), `h1-${blockIdx}-${i}`)}</span>
                </h1>
              </div>
            );
            i++;
            continue;
          }

          // Header 2: ## Title
          if (trimmed.startsWith('## ')) {
            elements.push(
              <div key={`h2-${blockIdx}-${i}`} className="mt-5 mb-3 pb-2 border-b border-slate-200">
                <h2 className="text-base sm:text-lg font-extrabold text-slate-950 flex items-center gap-2 tracking-tight">
                  <Layers className="h-4 w-4 text-indigo-600 shrink-0" />
                  <span>{renderInline(trimmed.substring(3), `h2-${blockIdx}-${i}`)}</span>
                </h2>
              </div>
            );
            i++;
            continue;
          }

          // Header 3: ### Title (e.g. "### 1. High-Level Summary" or "### 4. Data Flow")
          if (trimmed.startsWith('### ')) {
            elements.push(
              <div key={`h3-${blockIdx}-${i}`} className="mt-6 mb-3">
                <div className="inline-flex items-center gap-2 rounded-xl bg-indigo-50 border border-indigo-200/80 px-3.5 py-2 shadow-xs">
                  <Sparkles className="h-4 w-4 text-indigo-600 shrink-0" />
                  <h3 className="text-sm sm:text-base font-extrabold text-indigo-950 tracking-tight">
                    {renderInline(trimmed.substring(4), `h3-${blockIdx}-${i}`)}
                  </h3>
                </div>
              </div>
            );
            i++;
            continue;
          }

          // Header 4: #### Title
          if (trimmed.startsWith('#### ')) {
            elements.push(
              <h4 key={`h4-${blockIdx}-${i}`} className="text-sm font-extrabold text-slate-950 mt-4 mb-2 flex items-center gap-1.5">
                <ChevronRight className="h-4 w-4 text-indigo-500 shrink-0" />
                <span>{renderInline(trimmed.substring(5), `h4-${blockIdx}-${i}`)}</span>
              </h4>
            );
            i++;
            continue;
          }

          // Bullet List item: * or -
          if (/^(\s*)[*-]\s+/.test(line)) {
            const listItems: { indent: number; text: string; lineIdx: number }[] = [];
            while (i < lines.length && /^(\s*)[*-]\s+/.test(lines[i])) {
              const m = lines[i].match(/^(\s*)[*-]\s+(.*)$/);
              if (m) {
                listItems.push({
                  indent: m[1].length,
                  text: m[2],
                  lineIdx: i
                });
              }
              i++;
            }

            elements.push(
              <ul key={`ul-${blockIdx}-${listItems[0].lineIdx}`} className="my-2.5 space-y-2 pl-1">
                {listItems.map((li) => {
                  const isNested = li.indent > 0;
                  return (
                    <li
                      key={`li-${li.lineIdx}`}
                      className={`flex items-start gap-2.5 leading-relaxed ${
                        isNested ? 'pl-6 text-slate-900 text-[12.5px]' : 'text-slate-950 font-medium text-[13px] sm:text-[13.5px]'
                      }`}
                    >
                      <span
                        className={`mt-2 h-2 w-2 rounded-full shrink-0 shadow-xs ${
                          isNested ? 'bg-slate-400' : 'bg-indigo-600 ring-2 ring-indigo-100'
                        }`}
                      />
                      <div className="flex-1 min-w-0 font-medium text-slate-900 leading-normal">
                        {renderInline(li.text, `li-${blockIdx}-${li.lineIdx}`)}
                      </div>
                    </li>
                  );
                })}
              </ul>
            );
            continue;
          }

          // Numbered List item: 1. 2.
          if (/^\d+\.\s+/.test(trimmed)) {
            const numItems: { num: string; text: string; lineIdx: number }[] = [];
            while (i < lines.length && /^\d+\.\s+/.test(lines[i].trim())) {
              const m = lines[i].trim().match(/^(\d+)\.\s+(.*)$/);
              if (m) {
                numItems.push({
                  num: m[1],
                  text: m[2],
                  lineIdx: i
                });
              }
              i++;
            }

            elements.push(
              <ol key={`ol-${blockIdx}-${numItems[0].lineIdx}`} className="my-2.5 space-y-2 pl-1">
                {numItems.map((item) => (
                  <li key={`ol-li-${item.lineIdx}`} className="flex items-start gap-2.5 leading-relaxed">
                    <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-indigo-600 text-[11px] font-extrabold text-white shadow-xs mt-0.5">
                      {item.num}
                    </span>
                    <div className="flex-1 min-w-0 font-medium text-slate-900 text-[13px] sm:text-[13.5px] leading-normal">
                      {renderInline(item.text, `ol-${blockIdx}-${item.lineIdx}`)}
                    </div>
                  </li>
                ))}
              </ol>
            );
            continue;
          }

          // Regular paragraph
          elements.push(
            <p key={`p-${blockIdx}-${i}`} className="leading-relaxed my-2 font-medium text-slate-900 text-[13px] sm:text-[13.5px]">
              {renderInline(trimmed, `p-${blockIdx}-${i}`)}
            </p>
          );
          i++;
        }

        return <div key={`text-block-${blockIdx}`} className="space-y-1.5">{elements}</div>;
      })}
    </div>
  );
}
