'use client';

import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { useRouter, useParams } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { apiFetch } from '@/lib/api';
import {
  RepositoryDetail,
  RepositoryFile,
  CodeChunk,
  SearchResult,
  PageResponse,
  ArchitectureOverview,
  BugInvestigationResponse
} from '@/types/repository';
import { GroundedMarkdown } from '@/components/chat/GroundedMarkdown';
import {
  ArrowLeft,
  GitBranch,
  GitCommit,
  HardDrive,
  FileCode2,
  AlertTriangle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  ExternalLink,
  Loader2,
  FileText,
  Search,
  Layers,
  Code2,
  Sparkles,
  ChevronDown,
  ChevronUp,
  Bot,
  MessageSquare,
  ArrowRight,
  Compass,
  Bug,
  RefreshCw,
  Cpu,
  Boxes,
  Terminal,
  Check,
  Share2,
  SlidersHorizontal,
  Database
} from 'lucide-react';

export default function RepositoryDetailPage() {
  const params = useParams();
  const repoId = params.id as string;
  const { user, loading: authLoading, isAuthenticated } = useAuth();
  const router = useRouter();

  const [repository, setRepository] = useState<RepositoryDetail | null>(null);
  const [loadingRepo, setLoadingRepo] = useState(true);

  // Tab State: 'architecture' | 'bug' | 'search' | 'chunks' | 'files'
  const [activeTab, setActiveTab] = useState<'architecture' | 'bug' | 'search' | 'chunks' | 'files'>('architecture');

  // Files tab state
  const [filesData, setFilesData] = useState<PageResponse<RepositoryFile> | null>(null);
  const [loadingFiles, setLoadingFiles] = useState(true);
  const [filterSkipped, setFilterSkipped] = useState<string>('all');
  const [filePage, setFilePage] = useState(0);
  const [fileSearchQuery, setFileSearchQuery] = useState('');

  // Chunks tab state
  const [chunksData, setChunksData] = useState<PageResponse<CodeChunk> | null>(null);
  const [loadingChunks, setLoadingChunks] = useState(false);
  const [chunkPage, setChunkPage] = useState(0);
  const [chunkSearchQuery, setChunkSearchQuery] = useState('');
  const [expandedChunkId, setExpandedChunkId] = useState<string | null>(null);

  // Search tab state
  const [semanticQuery, setSemanticQuery] = useState('');
  const [topK, setTopK] = useState(10);
  const [searchResults, setSearchResults] = useState<SearchResult[] | null>(null);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  // Architecture Overview tab state
  const [architectureData, setArchitectureData] = useState<ArchitectureOverview | null>(null);
  const [loadingArchitecture, setLoadingArchitecture] = useState(false);
  const [architectureError, setArchitectureError] = useState<string | null>(null);

  // Bug Investigation tab state
  const [bugInput, setBugInput] = useState('');
  const [bugResponse, setBugResponse] = useState<BugInvestigationResponse | null>(null);
  const [loadingBug, setLoadingBug] = useState(false);
  const [bugError, setBugError] = useState<string | null>(null);
  const [selectedBugFile, setSelectedBugFile] = useState<string | null>(null);
  const [loadingFileContent, setLoadingFileContent] = useState(false);
  const [repoFilesList, setRepoFilesList] = useState<RepositoryFile[]>([]);
  const [fileFilterInput, setFileFilterInput] = useState('');
  const [fileSelectorOpen, setFileSelectorOpen] = useState(false);

  const filteredRepoFiles = useMemo(() => {
    if (!fileFilterInput.trim()) return repoFilesList;
    const term = fileFilterInput.toLowerCase();
    return repoFilesList.filter((f) => f.filePath.toLowerCase().includes(term));
  }, [repoFilesList, fileFilterInput]);

  const handleSelectBugFile = async (filePath: string) => {
    if (selectedBugFile === filePath) {
      setSelectedBugFile(null);
      setBugInput('');
      setFileSelectorOpen(false);
      return;
    }

    setSelectedBugFile(filePath);
    setFileSelectorOpen(false);
    setFileFilterInput('');
    setLoadingFileContent(true);

    try {
      const apiBase = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';
      const res = await fetch(`${apiBase}/api/repositories/${repoId}/files/content?path=${encodeURIComponent(filePath)}`, {
        credentials: 'include'
      });
      if (res.ok) {
        const text = await res.text();
        setBugInput(text);
      }
    } catch (err) {
      console.error('Failed to load file content:', err);
    } finally {
      setLoadingFileContent(false);
    }
  };

  const handleClearSelectedFile = () => {
    setSelectedBugFile(null);
    setBugInput('');
  };

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push('/login');
    }
  }, [isAuthenticated, authLoading, router]);

  const fetchRepository = useCallback(async () => {
    try {
      const data = await apiFetch<RepositoryDetail>(`/api/repositories/${repoId}`);
      setRepository(data);
    } catch (err: any) {
      console.error('Failed to fetch repo detail:', err);
    } finally {
      setLoadingRepo(false);
    }
  }, [repoId]);

  const fetchAllRepoFiles = useCallback(async () => {
    try {
      const data = await apiFetch<PageResponse<RepositoryFile>>(`/api/repositories/${repoId}/files?size=100&skipped=false`);
      if (data && data.content) {
        setRepoFilesList(data.content);
      }
    } catch (err) {
      console.error('Failed to fetch repo files for selector:', err);
    }
  }, [repoId]);

  const fetchFiles = useCallback(async () => {
    setLoadingFiles(true);
    try {
      let url = `/api/repositories/${repoId}/files?page=${filePage}&size=25`;
      if (filterSkipped === 'kept') {
        url += '&skipped=false';
      } else if (filterSkipped === 'skipped') {
        url += '&skipped=true';
      }

      const data = await apiFetch<PageResponse<RepositoryFile>>(url);
      setFilesData(data);
    } catch (err: any) {
      console.error('Failed to fetch files:', err);
    } finally {
      setLoadingFiles(false);
    }
  }, [repoId, filePage, filterSkipped]);

  const fetchChunks = useCallback(async () => {
    setLoadingChunks(true);
    try {
      const url = `/api/repositories/${repoId}/chunks?page=${chunkPage}&size=25&includeContent=true`;
      const data = await apiFetch<PageResponse<CodeChunk>>(url);
      setChunksData(data);
    } catch (err: any) {
      console.error('Failed to fetch chunks:', err);
    } finally {
      setLoadingChunks(false);
    }
  }, [repoId, chunkPage]);

  const fetchArchitecture = useCallback(async (force = false) => {
    setLoadingArchitecture(true);
    setArchitectureError(null);
    try {
      const data = await apiFetch<ArchitectureOverview>(
        `/api/repositories/${repoId}/architecture-overview?force=${force}`,
        { method: 'POST' }
      );
      setArchitectureData(data);
    } catch (err: any) {
      console.error('Failed to fetch architecture overview:', err);
      setArchitectureError(err.message || 'Could not generate architecture overview.');
    } finally {
      setLoadingArchitecture(false);
    }
  }, [repoId]);

  const handleSearch = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!semanticQuery.trim()) return;

    setIsSearching(true);
    setSearchError(null);
    try {
      const results = await apiFetch<SearchResult[]>(`/api/repositories/${repoId}/search`, {
        method: 'POST',
        body: JSON.stringify({
          query: semanticQuery.trim(),
          topK: Number(topK)
        })
      });
      setSearchResults(results);
    } catch (err: any) {
      console.error('Search failed:', err);
      setSearchError(err.message || 'Hybrid search failed. Ensure Qdrant and GEMINI_API_KEY are active.');
    } finally {
      setIsSearching(false);
    }
  };

  const handleInvestigateBug = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!bugInput.trim()) return;

    setLoadingBug(true);
    setBugError(null);
    try {
      const res = await apiFetch<BugInvestigationResponse>(`/api/repositories/${repoId}/investigate-bug`, {
        method: 'POST',
        body: JSON.stringify({
          errorText: bugInput.trim(),
          targetFiles: selectedBugFile ? [selectedBugFile] : []
        })
      });
      setBugResponse(res);
    } catch (err: any) {
      console.error('Bug investigation failed:', err);
      setBugError(err.message || 'Bug investigation failed.');
    } finally {
      setLoadingBug(false);
    }
  };

  useEffect(() => {
    if (isAuthenticated && repoId) {
      fetchRepository();
      fetchAllRepoFiles();
    }
  }, [isAuthenticated, repoId, fetchRepository, fetchAllRepoFiles]);

  // Polling when repository is in non-terminal status
  useEffect(() => {
    if (!repository) return;
    const isActive =
      repository.status === 'PENDING' ||
      repository.status === 'DOWNLOADING' ||
      repository.status === 'SCANNING' ||
      repository.status === 'CHUNKING' ||
      repository.status === 'EMBEDDING';

    if (isActive) {
      const interval = setInterval(() => {
        fetchRepository();
        if (activeTab === 'files') fetchFiles();
        if (activeTab === 'chunks') fetchChunks();
      }, 2500);
      return () => clearInterval(interval);
    }
  }, [repository, activeTab, fetchRepository, fetchFiles, fetchChunks]);

  useEffect(() => {
    if (isAuthenticated && repoId && activeTab === 'files') {
      fetchFiles();
    }
  }, [isAuthenticated, repoId, activeTab, fetchFiles]);

  useEffect(() => {
    if (isAuthenticated && repoId && activeTab === 'chunks') {
      fetchChunks();
    }
  }, [isAuthenticated, repoId, activeTab, fetchChunks]);

  useEffect(() => {
    if (isAuthenticated && repoId && activeTab === 'architecture' && !architectureData) {
      fetchArchitecture(false);
    }
  }, [isAuthenticated, repoId, activeTab, architectureData, fetchArchitecture]);



  if (authLoading || loadingRepo || !repository) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#07090e]">
        <div className="flex flex-col items-center gap-3">
          <Loader2 className="h-8 w-8 animate-spin text-indigo-400" />
          <p className="text-sm font-mono text-slate-400">Loading repository details...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#07090e] text-slate-100 pb-16">
      {/* Header */}
      <header className="border-b border-white/[0.08] bg-[#07090e]/80 backdrop-blur-xl sticky top-0 z-10 shadow-sm">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-3.5 sm:px-6">
          <div className="flex items-center gap-3 min-w-0">
            <Link
              href="/dashboard"
              className="rounded-xl border border-white/[0.08] bg-white/[0.03] p-2 text-slate-300 hover:bg-white/[0.08] hover:text-white transition shadow-sm shrink-0"
              title="Back to Dashboard"
            >
              <ArrowLeft className="h-4 w-4" />
            </Link>
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="text-base font-bold text-white truncate max-w-[200px] sm:max-w-md">{repository.fullName}</h1>
                <span className="rounded-md bg-indigo-950/60 px-2 py-0.5 text-[10px] font-mono font-bold text-indigo-300 border border-indigo-500/30 shrink-0">
                  {repository.status}
                </span>
              </div>
              <p className="text-xs font-mono text-slate-400 hidden sm:block">Developer Intelligence, File Inventory &amp; Hybrid Search</p>
            </div>
          </div>

          <div className="flex items-center gap-2 sm:gap-3 shrink-0">
            {repository.status === 'COMPLETED' && (
              <Link
                href={`/repositories/${repoId}/chat`}
                className="flex items-center gap-1.5 rounded-xl bg-indigo-600 px-3 sm:px-4 py-2 text-xs font-semibold text-white shadow-glow transition hover:bg-indigo-500"
              >
                <Bot className="h-4 w-4" />
                <span className="hidden xs:inline sm:inline">Open Chat + Monaco</span>
                <span className="inline xs:hidden sm:hidden">Chat</span>
              </Link>
            )}

            <a
              href={repository.url}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1.5 rounded-xl border border-white/[0.08] bg-white/[0.03] px-3 py-2 text-xs font-semibold text-slate-300 shadow-sm transition hover:bg-white/[0.08] hover:text-white"
            >
              <span>GitHub</span>
              <ExternalLink className="h-3.5 w-3.5" />
            </a>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="mx-auto max-w-6xl px-4 py-6 sm:py-8 sm:px-6 space-y-6">
        {/* Repo Meta Overview Card */}
        <section className="rounded-2xl border border-white/[0.08] bg-[#0c101d] p-4 sm:p-6 shadow-xl">
          <div className="grid grid-cols-2 sm:grid-cols-5 gap-4 divide-y sm:divide-y-0 sm:divide-x divide-white/[0.06] text-xs">
            <div className="space-y-1">
              <span className="text-slate-400 font-medium">Default Branch</span>
              <div className="flex items-center gap-1.5 font-semibold text-white">
                <GitBranch className="h-4 w-4 text-indigo-400 shrink-0" />
                <span className="truncate">{repository.defaultBranch || 'main'}</span>
              </div>
            </div>

            <div className="space-y-1 pt-3 sm:pt-0 sm:pl-4">
              <span className="text-slate-400 font-medium">Latest Commit</span>
              <div className="flex items-center gap-1.5 font-mono font-semibold text-slate-200">
                <GitCommit className="h-4 w-4 text-indigo-400 shrink-0" />
                <span>{repository.latestCommitSha?.substring(0, 7) || 'N/A'}</span>
              </div>
            </div>

            <div className="space-y-1 pt-3 sm:pt-0 sm:pl-4">
              <span className="text-slate-400 font-medium">Files (Kept / Total)</span>
              <div className="flex items-center gap-1.5 font-semibold text-white">
                <FileCode2 className="h-4 w-4 text-indigo-400 shrink-0" />
                <span>{repository.keptFiles} / {repository.totalFiles}</span>
              </div>
              {repository.lowValueSkippedCount ? (
                <span className="text-[10px] text-amber-400 font-medium block">
                  ({repository.lowValueSkippedCount} low-value skipped)
                </span>
              ) : null}
            </div>

            <div className="space-y-1 pt-3 sm:pt-0 sm:pl-4">
              <span className="text-slate-400 font-medium">Chunks &amp; Vectors</span>
              <div className="flex items-center gap-1.5 font-semibold text-indigo-400">
                <Layers className="h-4 w-4 text-indigo-400 shrink-0" />
                <span>{repository.totalChunks ?? 0} Chunks</span>
              </div>
              <span className="text-[10px] text-emerald-400 font-medium block">
                ({repository.embeddedChunkCount ?? 0} Qdrant vectors)
              </span>
            </div>

            <div className="space-y-1 pt-3 sm:pt-0 sm:pl-4 col-span-2 sm:col-span-1">
              <span className="text-slate-400 font-medium">Repository Size</span>
              <div className="flex items-center gap-1.5 font-semibold text-white">
                <HardDrive className="h-4 w-4 text-indigo-400 shrink-0" />
                <span>{(repository.sizeKb / 1024).toFixed(2)} MB</span>
              </div>
            </div>
          </div>
        </section>

        {/* Hero banner for Split View Chat */}
        {repository.status === 'COMPLETED' && (
          <div className="rounded-2xl border border-indigo-500/30 bg-gradient-to-r from-indigo-950/40 via-purple-950/30 to-indigo-950/40 p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-xl">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-600 text-white shadow-glow shrink-0">
                <Bot className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-white">Interactive Split-View RAG Workspace</h3>
                <p className="text-xs text-slate-400">
                  Ask questions with inline grounded citations and view highlighted source code in Monaco Editor.
                </p>
              </div>
            </div>

            <Link
              href={`/repositories/${repoId}/chat`}
              className="flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white shadow-glow hover:bg-indigo-500 transition shrink-0 w-full sm:w-auto justify-center"
            >
              <span>Launch Split-View Chat</span>
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        )}

        {/* Tab Selector */}
        <div className="flex items-center gap-1 sm:gap-2 border-b border-white/[0.08] overflow-x-auto no-scrollbar scroll-smooth">
          <button
            onClick={() => setActiveTab('architecture')}
            className={`flex items-center gap-2 border-b-2 px-3 sm:px-4 py-2.5 text-xs font-semibold transition whitespace-nowrap shrink-0 ${
              activeTab === 'architecture'
                ? 'border-indigo-500 text-indigo-400'
                : 'border-transparent text-slate-400 hover:text-white'
            }`}
          >
            <Compass className="h-4 w-4 text-purple-400" />
            <span>Architecture Overview</span>
          </button>
          <button
            onClick={() => setActiveTab('bug')}
            className={`flex items-center gap-2 border-b-2 px-3 sm:px-4 py-2.5 text-xs font-semibold transition whitespace-nowrap shrink-0 ${
              activeTab === 'bug'
                ? 'border-rose-500 text-rose-400'
                : 'border-transparent text-slate-400 hover:text-white'
            }`}
          >
            <Bug className="h-4 w-4 text-rose-400" />
            <span>Bug Investigation</span>
          </button>
          <button
            onClick={() => setActiveTab('search')}
            className={`flex items-center gap-2 border-b-2 px-3 sm:px-4 py-2.5 text-xs font-semibold transition whitespace-nowrap shrink-0 ${
              activeTab === 'search'
                ? 'border-indigo-500 text-indigo-400'
                : 'border-transparent text-slate-400 hover:text-white'
            }`}
          >
            <Sparkles className="h-4 w-4 text-indigo-400" />
            <span>Hybrid Search</span>
          </button>
          <button
            onClick={() => setActiveTab('chunks')}
            className={`flex items-center gap-2 border-b-2 px-3 sm:px-4 py-2.5 text-xs font-semibold transition whitespace-nowrap shrink-0 ${
              activeTab === 'chunks'
                ? 'border-indigo-500 text-indigo-400'
                : 'border-transparent text-slate-400 hover:text-white'
            }`}
          >
            <SlidersHorizontal className="h-4 w-4 text-slate-400" />
            <span>Advanced / Indexing Stats ({repository.totalChunks ?? 0})</span>
          </button>
        </div>

        {/* Advanced / Indexing Stats View */}
        {activeTab === 'chunks' && (
          <div className="space-y-6">
            {/* Indexing Diagnostics Cards */}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <div className="rounded-2xl border border-white/[0.08] bg-[#0c101d] p-4 shadow-xl">
                <div className="flex items-center gap-2 text-slate-400 mb-1">
                  <Layers className="h-4 w-4 text-indigo-400" />
                  <span className="text-xs font-semibold">Total Code Chunks</span>
                </div>
                <div className="text-xl font-bold text-white">
                  {chunksData?.totalElements ?? repository.totalChunks ?? 0}
                </div>
                <div className="text-[10px] text-slate-400 mt-1">Indexed in Vector & Keyword Stores</div>
              </div>

              <div className="rounded-2xl border border-white/[0.08] bg-[#0c101d] p-4 shadow-xl">
                <div className="flex items-center gap-2 text-slate-400 mb-1">
                  <Code2 className="h-4 w-4 text-purple-400" />
                  <span className="text-xs font-semibold">Chunk Window</span>
                </div>
                <div className="text-xl font-bold text-white">120 Lines</div>
                <div className="text-[10px] text-slate-400 mt-1">20-line sliding window overlap</div>
              </div>

              <div className="rounded-2xl border border-white/[0.08] bg-[#0c101d] p-4 shadow-xl">
                <div className="flex items-center gap-2 text-slate-400 mb-1">
                  <Cpu className="h-4 w-4 text-emerald-400" />
                  <span className="text-xs font-semibold">Vector Embedding</span>
                </div>
                <div className="text-xl font-bold text-white">768-dim</div>
                <div className="text-[10px] text-slate-400 mt-1">Gemini text-embedding-004</div>
              </div>

              <div className="rounded-2xl border border-white/[0.08] bg-[#0c101d] p-4 shadow-xl">
                <div className="flex items-center gap-2 text-slate-400 mb-1">
                  <AlertTriangle className="h-4 w-4 text-amber-400" />
                  <span className="text-xs font-semibold">Low-Value Filtered</span>
                </div>
                <div className="text-xl font-bold text-white">
                  {repository.lowValueSkippedCount ?? 0}
                </div>
                <div className="text-[10px] text-slate-400 mt-1">Minified & generated files skipped</div>
              </div>
            </div>

            {/* Chunks Explorer Section */}
            <section className="rounded-2xl border border-white/[0.08] bg-[#0c101d] shadow-xl overflow-hidden">
              <div className="border-b border-white/[0.08] p-4 bg-[#090d18] flex flex-col sm:flex-row items-center justify-between gap-4">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold text-white">
                    Chunk Inspector & Raw Segments ({chunksData?.totalElements ?? repository.totalChunks ?? 0})
                  </span>
                </div>
                <div className="relative w-full sm:w-64">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
                  <input
                    type="text"
                    placeholder="Filter chunk paths..."
                    value={chunkSearchQuery}
                    onChange={(e) => setChunkSearchQuery(e.target.value)}
                    className="w-full rounded-xl border border-white/[0.08] bg-black/40 pl-9 pr-3 py-1.5 text-xs text-white placeholder-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  />
                </div>
              </div>

            {loadingChunks ? (
              <div className="flex py-16 items-center justify-center">
                <Loader2 className="h-6 w-6 animate-spin text-indigo-400" />
              </div>
            ) : !chunksData || chunksData.content.length === 0 ? (
              <div className="p-8 text-center text-xs text-slate-400">No code chunks found.</div>
            ) : (
              <div className="divide-y divide-white/[0.06]">
                {chunksData.content
                  .filter((c) => !chunkSearchQuery || c.filePath.toLowerCase().includes(chunkSearchQuery.toLowerCase()))
                  .map((chunk) => {
                    const isExpanded = expandedChunkId === chunk.id;
                    return (
                      <div key={chunk.id} className="p-4 transition hover:bg-white/[0.02]">
                        <div className="flex items-center justify-between gap-4">
                          <div className="flex items-center gap-2 min-w-0">
                            <span className="rounded bg-indigo-950/60 px-2 py-0.5 font-mono text-[10px] font-bold text-indigo-300 border border-indigo-500/30">
                              #{chunk.chunkIndex}
                            </span>
                            <span className="font-mono text-xs font-semibold text-slate-200 truncate">
                              {chunk.filePath}
                            </span>
                            <span className="rounded-md bg-white/[0.05] px-2 py-0.5 text-[10px] font-mono text-slate-400 border border-white/[0.05]">
                              Lines {chunk.startLine}&ndash;{chunk.endLine}
                            </span>
                          </div>

                          <button
                            onClick={() => setExpandedChunkId(isExpanded ? null : chunk.id)}
                            className="flex items-center gap-1 text-xs font-semibold text-indigo-400 hover:text-indigo-300 transition"
                          >
                            <span>{isExpanded ? 'Hide Code' : 'View Code'}</span>
                            {isExpanded ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
                          </button>
                        </div>

                        {isExpanded && chunk.content && (
                          <div className="mt-3 rounded-xl bg-[#070a12] border border-white/[0.08] p-3.5 font-mono text-[11px] text-slate-200 overflow-x-auto shadow-inner">
                            <pre className="whitespace-pre overflow-x-auto">{chunk.content}</pre>
                          </div>
                        )}
                      </div>
                    );
                  })}

                {/* Pagination */}
                <div className="flex items-center justify-between border-t border-white/[0.08] px-4 py-3 bg-[#090d18]">
                  <span className="text-xs text-slate-400 font-mono">
                    Page {chunksData.page + 1} of {chunksData.totalPages || 1} ({chunksData.totalElements} chunks)
                  </span>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setChunkPage((p) => Math.max(0, p - 1))}
                      disabled={chunksData.first}
                      className="rounded-lg border border-white/[0.08] bg-white/[0.03] p-1 text-slate-300 hover:bg-white/[0.08] hover:text-white disabled:opacity-40"
                    >
                      <ChevronLeft className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => setChunkPage((p) => p + 1)}
                      disabled={chunksData.last}
                      className="rounded-lg border border-white/[0.08] bg-white/[0.03] p-1 text-slate-300 hover:bg-white/[0.08] hover:text-white disabled:opacity-40"
                    >
                      <ChevronRight className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            )}
          </section>
        </div>
      )}

        {/* Semantic Search View */}
        {activeTab === 'search' && (
          <section className="space-y-6">
            <div className="rounded-2xl border border-white/[0.08] bg-[#0c101d] p-6 shadow-xl space-y-4">
              <div className="space-y-1">
                <h2 className="text-sm font-bold text-white flex items-center gap-2">
                  <Sparkles className="h-4 w-4 text-indigo-400" />
                  <span>Hybrid Semantic & Keyword Code Search</span>
                </h2>
                <p className="text-xs text-slate-400">
                  Search across functions, classes, and logic using natural language or exact keywords.
                </p>
              </div>

              <form onSubmit={handleSearch} className="space-y-3">
                <div className="flex flex-col sm:flex-row items-center gap-3">
                  <div className="relative flex-1 w-full">
                    <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                    <input
                      type="text"
                      placeholder="e.g. 'How does authentication work?', 'JWT validation logic', 'Zip Slip security'"
                      value={semanticQuery}
                      onChange={(e) => setSemanticQuery(e.target.value)}
                      className="w-full rounded-xl border border-white/[0.08] bg-black/40 pl-10 pr-4 py-2 text-xs text-white placeholder-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 shadow-sm"
                    />
                  </div>

                  <div className="flex items-center gap-2 w-full sm:w-auto">
                    <select
                      value={topK}
                      onChange={(e) => setTopK(Number(e.target.value))}
                      className="rounded-xl border border-white/[0.08] bg-[#0d1322] px-3 py-2 text-xs text-slate-200 shadow-sm focus:border-indigo-500 focus:outline-none"
                    >
                      <option value={5}>Top 5 chunks</option>
                      <option value={10}>Top 10 chunks</option>
                      <option value={20}>Top 20 chunks</option>
                    </select>

                    <button
                      type="submit"
                      disabled={isSearching || !semanticQuery.trim()}
                      className="flex items-center justify-center gap-1.5 rounded-xl bg-indigo-600 px-5 py-2 text-xs font-semibold text-white shadow-glow transition hover:bg-indigo-500 disabled:opacity-50 shrink-0"
                    >
                      {isSearching ? (
                        <>
                          <Loader2 className="h-3.5 w-3.5 animate-spin" />
                          <span>Searching...</span>
                        </>
                      ) : (
                        <>
                          <Search className="h-3.5 w-3.5" />
                          <span>Search</span>
                        </>
                      )}
                    </button>
                  </div>
                </div>

                {/* Suggestions */}
                <div className="flex flex-wrap items-center gap-2 pt-1 text-[11px] text-slate-400">
                  <span className="font-medium text-slate-300">Try searching:</span>
                  {['JWT authentication', 'Zip extraction security', 'Low value code detector', 'Gemini embeddings'].map((pill) => (
                    <button
                      key={pill}
                      type="button"
                      onClick={() => { setSemanticQuery(pill); }}
                      className="rounded-lg bg-white/[0.04] border border-white/[0.08] px-2.5 py-0.5 font-mono text-slate-300 hover:bg-white/[0.08] hover:text-white transition"
                    >
                      {pill}
                    </button>
                  ))}
                </div>
              </form>

              {searchError && (
                <div className="rounded-xl border border-rose-500/30 bg-rose-950/40 p-3 text-xs text-rose-300">
                  {searchError}
                </div>
              )}
            </div>

            {/* Results Display */}
            {searchResults && (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-xs font-semibold text-white">
                    Ranked Results ({searchResults.length} chunks retrieved)
                  </h3>
                </div>

                {searchResults.length === 0 ? (
                  <div className="rounded-2xl border border-white/[0.08] bg-[#0c101d] p-8 text-center">
                    <p className="text-xs text-slate-400">No matching code chunks found for this query.</p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {searchResults.map((result, idx) => (
                      <div
                        key={result.id || idx}
                        className="rounded-2xl border border-white/[0.08] bg-[#0c101d] p-5 shadow-xl space-y-3 transition hover:border-indigo-500/40"
                      >
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-white/[0.06] pb-3">
                          <div className="flex items-center gap-2">
                            <span className="flex h-5 w-5 items-center justify-center rounded-full bg-indigo-950/80 border border-indigo-500/40 text-[10px] font-bold text-indigo-300">
                              #{idx + 1}
                            </span>
                            <span className="font-mono text-xs font-bold text-white">
                              {result.filePath}
                            </span>
                            <span className="inline-flex items-center rounded-md bg-white/[0.05] px-2 py-0.5 text-[11px] font-mono text-slate-300 border border-white/[0.05]">
                              L{result.startLine} &ndash; L{result.endLine}
                            </span>
                          </div>

                          <div className="flex items-center gap-2 text-xs">
                            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-950/60 px-2.5 py-0.5 font-semibold text-emerald-300 border border-emerald-500/30">
                              <Sparkles className="h-3 w-3 text-emerald-400" />
                              {(result.score * 100).toFixed(1)}% match
                            </span>
                          </div>
                        </div>

                        {/* Code snippet */}
                        <div className="rounded-xl bg-[#070a12] border border-white/[0.08] p-3.5 font-mono text-[11px] text-slate-200 overflow-x-auto max-h-72">
                          <pre className="whitespace-pre overflow-x-auto">{result.content}</pre>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </section>
        )}

        {/* Architecture Overview View */}
        {activeTab === 'architecture' && (
          <section className="space-y-6">
            <div className="rounded-2xl border border-white/[0.08] bg-[#0c101d] p-6 shadow-xl space-y-5">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/[0.06] pb-4">
                <div className="space-y-1">
                  <h2 className="text-sm font-bold text-white flex items-center gap-2">
                    <Compass className="h-4 w-4 text-purple-400" />
                    <span>Repository Architecture Overview</span>
                  </h2>
                  <p className="text-xs text-slate-400">
                    High-level structural synthesis generated from representative manifests, entry points, and directory sampling.
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => fetchArchitecture(true)}
                    disabled={loadingArchitecture}
                    className="flex items-center gap-1.5 rounded-xl border border-white/[0.08] bg-white/[0.03] px-3 py-1.5 text-xs font-semibold text-slate-300 shadow-sm hover:bg-white/[0.08] hover:text-white transition disabled:opacity-50"
                  >
                    <RefreshCw className={`h-3.5 w-3.5 text-purple-400 ${loadingArchitecture ? 'animate-spin' : ''}`} />
                    <span>Regenerate</span>
                  </button>
                </div>
              </div>

              {loadingArchitecture ? (
                <div className="flex flex-col items-center justify-center py-16 gap-3 text-slate-400">
                  <Loader2 className="h-8 w-8 animate-spin text-purple-400" />
                  <p className="text-xs font-mono">Analyzing codebase architecture and module hierarchy...</p>
                </div>
              ) : architectureError ? (
                <div className="rounded-xl border border-rose-500/30 bg-rose-950/40 p-4 text-xs text-rose-300">
                  {architectureError}
                </div>
              ) : architectureData ? (
                <div className="space-y-6">
                  {/* Tech Stack Pills */}
                  {architectureData.technologies && architectureData.technologies.length > 0 && (
                    <div className="space-y-2">
                      <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5 font-mono">
                        <Cpu className="h-3.5 w-3.5 text-purple-400" />
                        <span>Detected Technologies &amp; Tools</span>
                      </h4>
                      <div className="flex flex-wrap gap-2">
                        {architectureData.technologies.map((tech) => (
                          <span
                            key={tech}
                            className="rounded-lg bg-purple-950/50 border border-purple-500/30 px-2.5 py-1 text-xs font-semibold text-purple-300 font-mono"
                          >
                            {tech}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Modules Pills */}
                  {architectureData.modules && architectureData.modules.length > 0 && (
                    <div className="space-y-2">
                      <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5 font-mono">
                        <Boxes className="h-3.5 w-3.5 text-purple-400" />
                        <span>Core Modules &amp; Top Directories</span>
                      </h4>
                      <div className="flex flex-wrap gap-2">
                        {architectureData.modules.map((mod) => (
                          <span
                            key={mod}
                            className="rounded-lg bg-white/[0.03] border border-white/[0.08] px-2.5 py-1 text-xs font-mono text-slate-300"
                          >
                            {mod}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Overview Text */}
                  <div className="rounded-2xl border border-white/[0.08] bg-[#070a12] p-7 shadow-xl">
                    <GroundedMarkdown
                      content={architectureData.overviewText}
                      onCitationClick={(fp, start, end) => {
                        router.push(`/repositories/${repoId}/chat?file=${encodeURIComponent(fp)}&startLine=${start}&endLine=${end}`);
                      }}
                    />
                  </div>
                </div>
              ) : null}
            </div>
          </section>
        )}

        {/* Bug Investigation View */}
        {activeTab === 'bug' && (
          <section className="space-y-6">
            <div className="rounded-2xl border border-white/[0.08] bg-[#0c101d] p-6 shadow-xl space-y-4">
              <div className="space-y-1">
                <h2 className="text-sm font-bold text-white flex items-center gap-2">
                  <Bug className="h-4 w-4 text-rose-400" />
                  <span>Bug &amp; Stack Trace Investigator</span>
                </h2>
                <p className="text-xs text-slate-400">
                  Paste an exception stack trace or error log to pinpoint the responsible source files and lines with grounded fix recommendations.
                </p>
              </div>

              <form onSubmit={handleInvestigateBug} className="space-y-4">
                {/* Single Repository File Selector */}
                <div className="space-y-2 rounded-xl border border-white/[0.08] bg-black/20 p-3.5">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-semibold text-slate-200 flex items-center gap-1.5">
                      <FileCode2 className="h-3.5 w-3.5 text-rose-400" />
                      <span>Select Target File to Inspect (1 File at a time)</span>
                      <span className="text-[10px] font-normal text-slate-400 hidden sm:inline">
                        &mdash; Loads file source code directly into the editor
                      </span>
                    </label>
                    {selectedBugFile && (
                      <button
                        type="button"
                        onClick={handleClearSelectedFile}
                        className="text-[11px] font-medium text-rose-400 hover:text-rose-300 transition"
                      >
                        Clear selected file
                      </button>
                    )}
                  </div>

                  {/* Selected File Active Badge */}
                  {selectedBugFile && (
                    <div className="flex items-center justify-between rounded-lg bg-rose-950/60 border border-rose-500/30 px-3 py-1.5 text-xs">
                      <div className="flex items-center gap-2 truncate">
                        <FileCode2 className="h-3.5 w-3.5 text-rose-400 shrink-0" />
                        <span className="font-mono font-bold text-rose-200 truncate text-[11px]">
                          {selectedBugFile}
                        </span>
                      </div>
                      <button
                        type="button"
                        onClick={handleClearSelectedFile}
                        className="text-xs font-semibold text-rose-400 hover:text-rose-300 flex items-center gap-1 shrink-0 ml-2"
                      >
                        <span>Remove</span>
                        <span className="font-bold text-sm leading-none">&times;</span>
                      </button>
                    </div>
                  )}

                  {/* Searchable file picker input & popover list */}
                  <div className="relative">
                    <div className="relative">
                      <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-400" />
                      <input
                        type="text"
                        placeholder="Search & choose a file from this repository to load code..."
                        value={fileFilterInput}
                        onChange={(e) => {
                          setFileFilterInput(e.target.value);
                          setFileSelectorOpen(true);
                        }}
                        onFocus={() => setFileSelectorOpen(true)}
                        className="w-full rounded-xl border border-white/[0.08] bg-black/40 pl-9 pr-8 py-1.5 text-xs text-white placeholder:text-slate-400 focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500 shadow-sm"
                      />
                      {fileFilterInput && (
                        <button
                          type="button"
                          onClick={() => {
                            setFileFilterInput('');
                            setFileSelectorOpen(false);
                          }}
                          className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white text-xs font-bold"
                        >
                          &times;
                        </button>
                      )}
                    </div>

                    {fileSelectorOpen && (
                      <>
                        <div
                          className="fixed inset-0 z-20"
                          onClick={() => setFileSelectorOpen(false)}
                        />
                        <div className="absolute z-30 mt-1 max-h-56 w-full overflow-y-auto rounded-xl border border-white/[0.1] bg-[#0d1322] p-1.5 shadow-2xl divide-y divide-white/[0.06]">
                          {filteredRepoFiles.length === 0 ? (
                            <div className="p-3 text-center text-xs text-slate-400 font-mono">
                              No matching repository files found.
                            </div>
                          ) : (
                            filteredRepoFiles.map((file) => {
                              const isSelected = selectedBugFile === file.filePath;
                              return (
                                <button
                                  key={file.id || file.filePath}
                                  type="button"
                                  onClick={() => handleSelectBugFile(file.filePath)}
                                  className={`flex w-full items-center justify-between px-3 py-2 text-left text-xs transition rounded-lg ${
                                    isSelected
                                      ? 'bg-rose-950/60 text-rose-200 font-semibold border border-rose-500/30'
                                      : 'text-slate-300 hover:bg-white/[0.05]'
                                  }`}
                                >
                                  <div className="flex items-center gap-2 truncate pr-2">
                                    <FileCode2
                                      className={`h-3.5 w-3.5 shrink-0 ${
                                        isSelected ? 'text-rose-400' : 'text-slate-400'
                                      }`}
                                    />
                                    <span className="font-mono truncate text-[11px]">{file.filePath}</span>
                                  </div>
                                  <div className="flex items-center gap-2 shrink-0">
                                    {file.language && (
                                      <span className="rounded bg-white/[0.05] px-1.5 py-0.5 text-[10px] font-mono text-slate-400">
                                        {file.language}
                                      </span>
                                    )}
                                    <span
                                      className={`text-[11px] font-bold ${
                                        isSelected ? 'text-rose-400' : 'text-slate-400'
                                      }`}
                                    >
                                      {isSelected ? '✓ Selected' : 'Select'}
                                    </span>
                                  </div>
                                </button>
                              );
                            })
                          )}
                        </div>
                      </>
                    )}
                  </div>
                </div>

                <div className="space-y-2 relative">
                  {loadingFileContent && (
                    <div className="absolute inset-0 z-10 flex items-center justify-center rounded-xl bg-black/60 backdrop-blur-xs">
                      <div className="flex items-center gap-2 text-xs font-semibold text-rose-400 font-mono">
                        <Loader2 className="h-4 w-4 animate-spin" />
                        <span>Loading file source code...</span>
                      </div>
                    </div>
                  )}

                  <textarea
                    rows={8}
                    placeholder={
                      selectedBugFile
                        ? `Loaded source code for ${selectedBugFile}. You can add error notes, stack trace, or click Analyze.`
                        : `Paste stack trace or error description, or select a file above to load its code...\n\njava.lang.NullPointerException: Cannot invoke method\n    at com.example.coderag.service.AuthService.validateToken(AuthService.java:42)\n    at com.example.coderag.controller.AuthController.login(AuthController.java:25)`
                    }
                    value={bugInput}
                    onChange={(e) => setBugInput(e.target.value)}
                    className="w-full rounded-xl border border-white/[0.08] bg-black/40 p-3.5 font-mono text-xs text-white placeholder-slate-400 focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500 shadow-sm"
                  />
                </div>

                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="flex flex-wrap items-center gap-2 text-[11px] text-slate-400">
                    <span className="font-medium text-slate-300">Sample Traces:</span>
                    <button
                      type="button"
                      onClick={() => setBugInput("java.lang.NullPointerException: Cannot invoke method\n    at com.example.coderag.security.JwtService.validateToken(JwtService.java:42)\n    at com.example.coderag.controller.AuthController.login(AuthController.java:30)")}
                      className="rounded-lg bg-white/[0.04] border border-white/[0.08] px-2.5 py-0.5 font-mono text-slate-300 hover:bg-white/[0.08] hover:text-white transition"
                    >
                      Java NPE
                    </button>
                    <button
                      type="button"
                      onClick={() => setBugInput("TypeError: Cannot read property 'map' of undefined\n    at RepositoryList (src/app/dashboard/page.tsx:45:18)")}
                      className="rounded-lg bg-white/[0.04] border border-white/[0.08] px-2.5 py-0.5 font-mono text-slate-300 hover:bg-white/[0.08] hover:text-white transition"
                    >
                      React / Next.js TypeError
                    </button>
                  </div>

                  <button
                    type="submit"
                    disabled={loadingBug || !bugInput.trim()}
                    className="flex items-center justify-center gap-1.5 rounded-xl bg-rose-600 px-5 py-2 text-xs font-semibold text-white shadow-glow transition hover:bg-rose-500 disabled:opacity-50 shrink-0"
                  >
                    {loadingBug ? (
                      <>
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                        <span>Investigating...</span>
                      </>
                    ) : (
                      <>
                        <Bug className="h-3.5 w-3.5" />
                        <span>Analyze & Locate Bug</span>
                      </>
                    )}
                  </button>
                </div>
              </form>

              {bugError && (
                <div className="rounded-xl border border-rose-500/30 bg-rose-950/40 p-3 text-xs text-rose-300">
                  {bugError}
                </div>
              )}
            </div>

            {/* Investigation Output */}
            {bugResponse && (
              <div className="space-y-5">
                <div className="rounded-2xl border border-white/[0.08] bg-[#0c101d] p-6 shadow-xl space-y-4">
                  <div className="flex items-center justify-between border-b border-white/[0.06] pb-3">
                    <h3 className="text-xs font-bold text-white flex items-center gap-2">
                      <Terminal className="h-4 w-4 text-rose-400" />
                      <span>Diagnostic Analysis &amp; Root Cause</span>
                    </h3>

                    {bugResponse.identifiedFiles && bugResponse.identifiedFiles.length > 0 && (
                      <div className="flex items-center gap-1.5">
                        <span className="text-[11px] text-slate-400 font-medium font-mono">Direct Signals:</span>
                        {bugResponse.identifiedFiles.map((f) => (
                          <span key={f} className="rounded bg-rose-950/60 border border-rose-500/30 px-2 py-0.5 font-mono text-[10px] font-bold text-rose-300">
                            {f}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>

                  <div className="rounded-2xl border border-white/[0.08] bg-[#070a12] p-7 shadow-xl">
                    <GroundedMarkdown
                      content={bugResponse.analysis}
                      onCitationClick={(fp, start, end) => {
                        router.push(`/repositories/${repoId}/chat?file=${encodeURIComponent(fp)}&startLine=${start}&endLine=${end}`);
                      }}
                    />
                  </div>
                </div>

                {/* Relevant Chunks Inspected */}
                {bugResponse.relevantChunks && bugResponse.relevantChunks.length > 0 && (
                  <div className="space-y-3">
                    <h4 className="text-xs font-bold text-slate-200">
                      Inspected Relevant Code Chunks ({bugResponse.relevantChunks.length})
                    </h4>
                    <div className="space-y-3">
                      {bugResponse.relevantChunks.map((chunk, idx) => (
                        <div
                          key={chunk.id || idx}
                          className="rounded-2xl border border-white/[0.08] bg-[#0c101d] p-4 shadow-xl space-y-2.5"
                        >
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                              <FileCode2 className="h-4 w-4 text-indigo-400" />
                              <span className="font-mono text-xs font-bold text-white">{chunk.filePath}</span>
                              <span className="rounded bg-white/[0.05] px-2 py-0.5 text-[10px] font-mono text-slate-300 border border-white/[0.05]">
                                L{chunk.startLine}&ndash;L{chunk.endLine}
                              </span>
                            </div>

                            <Link
                              href={`/repositories/${repoId}/chat?file=${encodeURIComponent(chunk.filePath)}&startLine=${chunk.startLine}&endLine=${chunk.endLine}`}
                              className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 flex items-center gap-1 transition"
                            >
                              <span>Open in Monaco</span>
                              <ExternalLink className="h-3 w-3" />
                            </Link>
                          </div>

                          <div className="rounded-xl bg-[#070a12] border border-white/[0.08] p-3.5 font-mono text-[11px] text-slate-200 overflow-x-auto max-h-60">
                            <pre className="whitespace-pre overflow-x-auto">{chunk.content}</pre>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </section>
        )}
      </main>
    </div>
  );
}
