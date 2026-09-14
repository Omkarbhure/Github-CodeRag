'use client';

import React, { useEffect, useRef, useState, useCallback } from 'react';
import dynamic from 'next/dynamic';
import { apiFetch } from '@/lib/api';
import { RelatedFile } from '@/types/repository';
import {
  FileCode2,
  Copy,
  Check,
  AlertCircle,
  Loader2,
  Code2,
  Sparkles,
  Share2,
  ChevronRight,
  X,
  ArrowRight
} from 'lucide-react';

// Dynamic import for Monaco Editor to avoid SSR window issues
const Editor = dynamic(() => import('@monaco-editor/react').then((mod) => mod.Editor), {
  ssr: false,
  loading: () => (
    <div className="flex h-full items-center justify-center bg-slate-900 text-slate-400">
      <Loader2 className="h-6 w-6 animate-spin" />
    </div>
  )
});

interface HighlightRange {
  startLine: number;
  endLine: number;
}

interface CodeViewerPanelProps {
  filePath: string | null;
  content: string | null;
  isLoading: boolean;
  error: string | null;
  highlightRange: HighlightRange | null;
  repositoryId?: string;
  onSelectFile?: (filePath: string, startLine?: number, endLine?: number) => void;
}

export function CodeViewerPanel({
  filePath,
  content,
  isLoading,
  error,
  highlightRange,
  repositoryId,
  onSelectFile
}: CodeViewerPanelProps) {
  const editorRef = useRef<any>(null);
  const monacoRef = useRef<any>(null);
  const decorationsRef = useRef<string[]>([]);
  const [copied, setCopied] = useState(false);

  // Related Files Drawer State
  const [showRelated, setShowRelated] = useState(false);
  const [relatedFiles, setRelatedFiles] = useState<RelatedFile[]>([]);
  const [loadingRelated, setLoadingRelated] = useState(false);
  const [relatedError, setRelatedError] = useState<string | null>(null);

  // Map file extension to Monaco language
  const getLanguage = (path: string | null): string => {
    if (!path) return 'plaintext';
    const ext = path.split('.').pop()?.toLowerCase();
    switch (ext) {
      case 'java':
        return 'java';
      case 'ts':
      case 'tsx':
        return 'typescript';
      case 'js':
      case 'jsx':
        return 'javascript';
      case 'py':
        return 'python';
      case 'go':
        return 'go';
      case 'rs':
        return 'rust';
      case 'cpp':
      case 'cc':
      case 'cxx':
      case 'hpp':
      case 'h':
        return 'cpp';
      case 'c':
        return 'c';
      case 'cs':
        return 'csharp';
      case 'json':
        return 'json';
      case 'yaml':
      case 'yml':
        return 'yaml';
      case 'sql':
        return 'sql';
      case 'md':
      case 'markdown':
        return 'markdown';
      case 'html':
      case 'htm':
        return 'html';
      case 'css':
      case 'scss':
        return 'css';
      case 'xml':
        return 'xml';
      case 'sh':
      case 'bash':
        return 'shell';
      default:
        return 'plaintext';
    }
  };

  const fetchRelatedFiles = useCallback(async (targetPath: string, repoId: string) => {
    setLoadingRelated(true);
    setRelatedError(null);
    try {
      const data = await apiFetch<RelatedFile[]>(
        `/api/repositories/${repoId}/files/related?path=${encodeURIComponent(targetPath)}&topK=8`
      );
      setRelatedFiles(data || []);
    } catch (err: any) {
      console.error('Failed to fetch related files:', err);
      setRelatedError(err.message || 'Could not find related files.');
    } finally {
      setLoadingRelated(false);
    }
  }, []);

  useEffect(() => {
    if (filePath && repositoryId) {
      fetchRelatedFiles(filePath, repositoryId);
    } else {
      setRelatedFiles([]);
    }
  }, [filePath, repositoryId, fetchRelatedFiles]);

  const handleEditorDidMount = (editor: any, monaco: any) => {
    editorRef.current = editor;
    monacoRef.current = monaco;
    applyHighlights();
  };

  const applyHighlights = useCallback(() => {
    const editor = editorRef.current;
    const monaco = monacoRef.current;
    if (!editor || !monaco) return;

    if (!highlightRange || !content) {
      decorationsRef.current = editor.deltaDecorations(decorationsRef.current, []);
      return;
    }

    const { startLine, endLine } = highlightRange;

    // Apply new highlight decoration
    const newDecorations = [
      {
        range: new monaco.Range(startLine, 1, endLine, 1),
        options: {
          isWholeLine: true,
          className: 'monaco-highlight-line',
          glyphMarginClassName: 'monaco-glyph-indicator'
        }
      }
    ];

    decorationsRef.current = editor.deltaDecorations(decorationsRef.current, newDecorations);

    // Scroll to the cited line range
    setTimeout(() => {
      editor.revealLinesInCenter(startLine, endLine);
      editor.setPosition({ lineNumber: startLine, column: 1 });
    }, 50);
  }, [highlightRange, content]);

  // Update highlights whenever highlightRange or content changes
  useEffect(() => {
    applyHighlights();
  }, [applyHighlights]);

  const handleCopy = () => {
    if (!content) return;
    navigator.clipboard.writeText(content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const language = getLanguage(filePath);

  return (
    <div className="flex h-full flex-col rounded-2xl border border-slate-200 bg-slate-900 shadow-sm overflow-hidden relative">
      {/* Viewer Header Bar */}
      <div className="flex items-center justify-between border-b border-slate-800 bg-slate-950/90 px-4 py-3 text-xs text-slate-300">
        <div className="flex items-center gap-2 truncate pr-2">
          <FileCode2 className="h-4 w-4 text-indigo-400 shrink-0" />
          {filePath ? (
            <span className="font-mono font-medium text-slate-100 truncate">{filePath}</span>
          ) : (
            <span className="text-slate-500 italic">No file selected</span>
          )}

          {highlightRange && (
            <span className="inline-flex items-center rounded-md bg-indigo-500/20 px-2 py-0.5 font-mono text-[11px] font-semibold text-indigo-300 border border-indigo-500/30 shrink-0">
              Lines {highlightRange.startLine}&ndash;{highlightRange.endLine}
            </span>
          )}
        </div>

        <div className="flex items-center gap-2 shrink-0">
          {/* Related Files Toggle Button */}
          {filePath && repositoryId && (
            <button
              type="button"
              onClick={() => setShowRelated(!showRelated)}
              className={`flex items-center gap-1.5 rounded-lg border px-2.5 py-1 text-xs font-semibold transition ${
                showRelated
                  ? 'bg-indigo-600 border-indigo-500 text-white'
                  : 'border-slate-800 bg-slate-900 text-slate-300 hover:bg-slate-800 hover:text-white'
              }`}
              title="View files related to this file"
            >
              <Share2 className="h-3.5 w-3.5 text-indigo-400" />
              <span>Related</span>
              {relatedFiles.length > 0 && (
                <span className="rounded-full bg-indigo-500/30 px-1.5 py-0.2 text-[10px] text-indigo-200">
                  {relatedFiles.length}
                </span>
              )}
            </button>
          )}

          {filePath && (
            <span className="rounded bg-slate-800 px-2 py-0.5 text-[10px] uppercase tracking-wider font-semibold text-slate-400">
              {language}
            </span>
          )}

          {content && (
            <button
              type="button"
              onClick={handleCopy}
              className="flex items-center gap-1 rounded-lg border border-slate-800 bg-slate-900 px-2.5 py-1 text-xs font-medium text-slate-300 hover:bg-slate-800 hover:text-white transition"
              title="Copy file contents"
            >
              {copied ? (
                <>
                  <Check className="h-3.5 w-3.5 text-emerald-400" />
                  <span className="text-emerald-400">Copied</span>
                </>
              ) : (
                <>
                  <Copy className="h-3.5 w-3.5" />
                  <span>Copy</span>
                </>
              )}
            </button>
          )}
        </div>
      </div>

      {/* Editor Body */}
      <div className="relative flex-1 min-h-0 bg-slate-900 flex">
        <div className="flex-1 h-full min-w-0">
          {isLoading ? (
            <div className="flex h-full flex-col items-center justify-center gap-3 text-slate-400">
              <Loader2 className="h-8 w-8 animate-spin text-indigo-400" />
              <p className="text-xs font-medium">Fetching file content from repository...</p>
            </div>
          ) : error ? (
            <div className="flex h-full flex-col items-center justify-center gap-3 p-6 text-center text-rose-400">
              <AlertCircle className="h-8 w-8 text-rose-500" />
              <p className="text-xs font-semibold">{error}</p>
              <p className="text-[11px] text-slate-400 max-w-sm">
                The file may have been filtered out during indexing or was omitted.
              </p>
            </div>
          ) : !filePath || !content ? (
            <div className="flex h-full flex-col items-center justify-center gap-3 p-8 text-center text-slate-500">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-800/80 text-slate-400 shadow-inner">
                <Code2 className="h-6 w-6" />
              </div>
              <div>
                <h4 className="text-sm font-semibold text-slate-300">Code Inspector</h4>
                <p className="mt-1 text-xs text-slate-500 max-w-sm">
                  Click any citation badge (e.g. <code className="text-indigo-400 font-mono">AuthService.java:42-58</code>) in the chat response to jump directly to that source file and highlighted line range.
                </p>
              </div>
            </div>
          ) : (
            <Editor
              height="100%"
              language={language}
              value={content}
              theme="vs-dark"
              options={{
                readOnly: true,
                fontSize: 12.5,
                lineNumbers: 'on',
                glyphMargin: true,
                folding: true,
                scrollBeyondLastLine: false,
                automaticLayout: true,
                minimap: { enabled: true, maxColumn: 80 },
                wordWrap: 'off',
                renderLineHighlight: 'all',
                fontFamily: "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace"
              }}
              onMount={handleEditorDidMount}
            />
          )}
        </div>

        {/* Related Files Sidebar Drawer */}
        {showRelated && (
          <div className="w-80 border-l border-slate-800 bg-slate-950/95 flex flex-col h-full z-10 shadow-2xl backdrop-blur-sm">
            <div className="flex items-center justify-between border-b border-slate-800 px-3.5 py-2.5 text-xs text-slate-300 bg-slate-900/60">
              <div className="flex items-center gap-1.5 font-semibold text-slate-100">
                <Sparkles className="h-4 w-4 text-indigo-400" />
                <span>Related Files</span>
              </div>
              <button
                type="button"
                onClick={() => setShowRelated(false)}
                className="rounded p-1 text-slate-400 hover:bg-slate-800 hover:text-white transition"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-3 space-y-2.5 text-xs">
              {loadingRelated ? (
                <div className="flex flex-col items-center justify-center py-10 gap-2 text-slate-400">
                  <Loader2 className="h-5 w-5 animate-spin text-indigo-400" />
                  <span className="text-[11px]">Finding related files...</span>
                </div>
              ) : relatedError ? (
                <div className="rounded-xl border border-rose-900/50 bg-rose-950/20 p-3 text-rose-400 text-center">
                  <p className="text-[11px]">{relatedError}</p>
                </div>
              ) : relatedFiles.length === 0 ? (
                <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-4 text-center text-slate-500">
                  <p className="text-[11px]">No related files discovered in this repository.</p>
                </div>
              ) : (
                relatedFiles.map((rf, idx) => (
                  <div
                    key={rf.filePath || idx}
                    onClick={() => {
                      if (onSelectFile) {
                        onSelectFile(rf.filePath, 1, 1);
                      }
                    }}
                    className="group flex flex-col gap-1.5 rounded-xl border border-slate-800 bg-slate-900/80 p-3 hover:border-indigo-500 hover:bg-slate-800/80 transition cursor-pointer"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex items-center gap-1.5 min-w-0">
                        <FileCode2 className="h-3.5 w-3.5 text-indigo-400 shrink-0" />
                        <span className="font-mono text-[11px] font-bold text-slate-200 truncate group-hover:text-indigo-300">
                          {rf.filePath}
                        </span>
                      </div>
                      <span className="rounded bg-indigo-950 px-1.5 py-0.5 text-[10px] font-bold text-indigo-400 border border-indigo-800/50 shrink-0">
                        {(rf.relevanceScore * 100).toFixed(0)}%
                      </span>
                    </div>

                    <p className="text-[10px] text-slate-400 leading-relaxed line-clamp-2">
                      {rf.reason}
                    </p>
                  </div>
                ))
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
