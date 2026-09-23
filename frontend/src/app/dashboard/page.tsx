'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { apiFetch } from '@/lib/api';
import { RepositorySummary, IndexingStatus } from '@/types/repository';
import { BrandLogo } from '@/components/common/BrandLogo';
import {
  LogOut,
  Github,
  Plus,
  FolderGit2,
  FileCode2,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Loader2,
  ArrowRight,
  GitBranch,
  GitCommit,
  HardDrive,
  RefreshCw,
  AlertCircle,
  Bot,
  Sparkles,
  Layers,
  Terminal
} from 'lucide-react';

export default function DashboardPage() {
  const { user, loading, isAuthenticated, logout } = useAuth();
  const router = useRouter();

  const [repositories, setRepositories] = useState<RepositorySummary[]>([]);
  const [loadingRepos, setLoadingRepos] = useState(true);
  const [githubUrl, setGithubUrl] = useState('');
  const [importing, setImporting] = useState(false);
  const [importError, setImportError] = useState<string | null>(null);
  const [importSuccess, setImportSuccess] = useState<string | null>(null);

  useEffect(() => {
    if (!loading && !isAuthenticated) {
      router.push('/login');
    }
  }, [isAuthenticated, loading, router]);

  const fetchRepositories = useCallback(async () => {
    try {
      const data = await apiFetch<RepositorySummary[]>('/api/repositories');
      setRepositories(data);
    } catch (err: any) {
      console.error('Failed to fetch repositories:', err);
    } finally {
      setLoadingRepos(false);
    }
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      fetchRepositories();
    }
  }, [isAuthenticated, fetchRepositories]);

  // Polling for repositories in active status (PENDING, DOWNLOADING, SCANNING, CHUNKING, EMBEDDING)
  useEffect(() => {
    const hasActiveJobs = repositories.some(
      (r) =>
        r.status === 'PENDING' ||
        r.status === 'DOWNLOADING' ||
        r.status === 'SCANNING' ||
        r.status === 'CHUNKING' ||
        r.status === 'EMBEDDING'
    );

    if (hasActiveJobs) {
      const interval = setInterval(fetchRepositories, 3000);
      return () => clearInterval(interval);
    }
  }, [repositories, fetchRepositories]);

  const handleImport = async (e: React.FormEvent) => {
    e.preventDefault();
    setImportError(null);
    setImportSuccess(null);
    setImporting(true);

    try {
      const result = await apiFetch<RepositorySummary>('/api/repositories', {
        method: 'POST',
        body: JSON.stringify({ githubUrl }),
      });

      setGithubUrl('');
      if (result.alreadyIndexed) {
        setImportSuccess(`Repository ${result.fullName} was already indexed at commit ${result.latestCommitSha?.substring(0, 7)}.`);
      } else {
        setImportSuccess(`Repository ${result.fullName} successfully imported (${result.totalFiles} files indexed).`);
      }
      await fetchRepositories();
    } catch (err: any) {
      setImportError(err.message || 'Failed to import repository.');
    } finally {
      setImporting(false);
    }
  };

  const handleLogout = async () => {
    await logout();
    router.push('/');
  };

  const formatSize = (kb: number) => {
    if (kb >= 1024) {
      return `${(kb / 1024).toFixed(2)} MB`;
    }
    return `${kb} KB`;
  };

  const renderStatusBadge = (status: IndexingStatus) => {
    switch (status) {
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-950/60 px-2.5 py-1 text-[11px] font-mono font-medium text-emerald-400 border border-emerald-500/30">
            <CheckCircle2 className="h-3 w-3 text-emerald-400" />
            COMPLETED
          </span>
        );
      case 'EMBEDDING':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-purple-950/60 px-2.5 py-1 text-[11px] font-mono font-medium text-purple-300 border border-purple-500/30">
            <Sparkles className="h-3 w-3 animate-pulse text-purple-400" />
            EMBEDDING VECTORS
          </span>
        );
      case 'CHUNKING':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-indigo-950/60 px-2.5 py-1 text-[11px] font-mono font-medium text-indigo-300 border border-indigo-500/30">
            <Layers className="h-3 w-3 animate-pulse text-indigo-400" />
            CHUNKING CODE
          </span>
        );
      case 'SCANNING':
      case 'DOWNLOADING':
      case 'PENDING':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-blue-950/60 px-2.5 py-1 text-[11px] font-mono font-medium text-blue-300 border border-blue-500/30">
            <Loader2 className="h-3 w-3 animate-spin text-blue-400" />
            {status}
          </span>
        );
      case 'REJECTED_TOO_LARGE':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-950/60 px-2.5 py-1 text-[11px] font-mono font-medium text-amber-300 border border-amber-500/30">
            <AlertTriangle className="h-3 w-3 text-amber-400" />
            TOO LARGE (&gt;100MB)
          </span>
        );
      case 'FAILED':
      default:
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-rose-950/60 px-2.5 py-1 text-[11px] font-mono font-medium text-rose-300 border border-rose-500/30">
            <XCircle className="h-3 w-3 text-rose-400" />
            FAILED
          </span>
        );
    }
  };

  if (loading || !user) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#07090e]">
        <div className="flex flex-col items-center gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-indigo-500 border-t-transparent" />
          <p className="text-xs font-mono text-slate-400">Loading session...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#07090e] text-slate-100">
      {/* Header */}
      <header className="sticky top-0 z-10 border-b border-white/[0.08] bg-[#07090e]/80 backdrop-blur-xl">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-3.5 py-3 sm:px-6">
          <BrandLogo size="sm" subtitle="AI Code Assistant + Monaco Viewer" />

          <div className="flex items-center gap-2 sm:gap-4">
            <div className="hidden sm:flex items-center gap-2 rounded-full border border-white/[0.08] bg-white/[0.03] px-3 py-1 text-xs text-slate-300">
              <span className="h-2 w-2 rounded-full bg-emerald-400" />
              <span className="font-mono text-[11px] text-slate-300">{user.email || user.githubUsername}</span>
            </div>
            <button
              onClick={handleLogout}
              className="flex items-center gap-1.5 sm:gap-2 rounded-lg border border-white/[0.08] bg-white/[0.03] px-2.5 sm:px-3 py-1.5 text-xs font-medium text-slate-300 transition hover:bg-rose-950/40 hover:text-rose-300 hover:border-rose-500/30 focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-2 focus:ring-offset-[#07090e]"
            >
              <LogOut className="h-3.5 w-3.5" />
              <span>Sign Out</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6 space-y-8">
        {/* Import Repo Section */}
        <section className="rounded-2xl border border-white/[0.08] bg-[#0c101d] p-6 shadow-2xl space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-white font-bold text-base">
              <FolderGit2 className="h-5 w-5 text-indigo-400" />
              <h2>Import Public GitHub Repository</h2>
            </div>
            <span className="font-mono text-[10px] text-slate-400 uppercase tracking-wider hidden sm:inline-block">
              Cap: 100MB Repo • 10MB File
            </span>
          </div>
          <p className="text-xs text-slate-400">
            Paste any public GitHub URL to sync repository tree, chunk source files, and generate Gemini embeddings.
          </p>

          {importError && (
            <div className="flex items-center gap-2 rounded-xl bg-rose-950/50 border border-rose-500/30 p-3.5 text-xs text-rose-300">
              <AlertCircle className="h-4 w-4 shrink-0 text-rose-400" />
              <span>{importError}</span>
            </div>
          )}

          {importSuccess && (
            <div className="flex items-center gap-2 rounded-xl bg-emerald-950/50 border border-emerald-500/30 p-3.5 text-xs text-emerald-300">
              <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-400" />
              <span>{importSuccess}</span>
            </div>
          )}

          <form onSubmit={handleImport} className="flex flex-col sm:flex-row gap-3 pt-1">
            <div className="relative flex-1">
              <Github className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                required
                value={githubUrl}
                onChange={(e) => setGithubUrl(e.target.value)}
                placeholder="https://github.com/owner/repository"
                className="w-full rounded-xl border border-white/[0.08] bg-black/40 py-2.5 pl-9 pr-3 text-sm text-white placeholder-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>

            <button
              type="submit"
              disabled={importing}
              className="flex items-center justify-center gap-2 rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white shadow-glow transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-[#07090e] disabled:opacity-50"
            >
              {importing ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span>Importing &amp; Indexing...</span>
                </>
              ) : (
                <>
                  <Plus className="h-4 w-4" />
                  <span>Import Repository</span>
                </>
              )}
            </button>
          </form>
        </section>

        {/* Repositories List */}
        <section className="space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Terminal className="h-4 w-4 text-indigo-400" />
              <h2 className="text-sm font-mono font-bold uppercase tracking-wider text-slate-300">
                Your Repositories ({repositories.length})
              </h2>
            </div>
            <button
              onClick={fetchRepositories}
              className="flex items-center gap-1.5 rounded-lg border border-white/[0.08] bg-white/[0.03] px-3 py-1.5 text-xs font-mono text-slate-300 transition hover:bg-white/[0.08] hover:text-white"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              <span>Refresh</span>
            </button>
          </div>

          {loadingRepos ? (
            <div className="rounded-2xl border border-white/[0.08] bg-[#0c101d] p-12 text-center">
              <Loader2 className="mx-auto h-6 w-6 animate-spin text-slate-400" />
              <p className="mt-2 text-xs font-mono text-slate-400">Loading repositories...</p>
            </div>
          ) : repositories.length === 0 ? (
            <div className="rounded-2xl border border-white/[0.08] bg-[#0c101d] p-12 text-center space-y-3">
              <FolderGit2 className="mx-auto h-10 w-10 text-slate-600" />
              <h3 className="text-sm font-semibold text-white">No repositories indexed yet</h3>
              <p className="text-xs text-slate-400 max-w-sm mx-auto">
                Paste any public GitHub repository URL in the box above to start indexing chunks and chatting with AI.
              </p>
            </div>
          ) : (
            <div className="overflow-hidden rounded-2xl border border-white/[0.08] bg-[#0c101d] shadow-2xl">
              <div className="divide-y divide-white/[0.06]">
                {repositories.map((repo) => (
                  <div
                    key={repo.id}
                    className="flex flex-col sm:flex-row sm:items-center justify-between p-5 gap-4 hover:bg-white/[0.02] transition"
                  >
                    <div className="space-y-2">
                      <div className="flex flex-wrap items-center gap-2.5">
                        <Link
                          href={repo.status === 'COMPLETED' ? `/repositories/${repo.id}/chat` : `/repositories/${repo.id}`}
                          className="font-bold text-sm text-white hover:text-indigo-400 transition"
                        >
                          {repo.fullName}
                        </Link>
                        {renderStatusBadge(repo.status)}
                      </div>

                      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 font-mono text-xs text-slate-400">
                        {repo.defaultBranch && (
                          <span className="flex items-center gap-1 text-slate-300">
                            <GitBranch className="h-3.5 w-3.5 text-indigo-400" />
                            {repo.defaultBranch}
                          </span>
                        )}
                        {repo.latestCommitSha && (
                          <span className="flex items-center gap-1 text-slate-400">
                            <GitCommit className="h-3.5 w-3.5 text-slate-500" />
                            {repo.latestCommitSha.substring(0, 7)}
                          </span>
                        )}
                        <span className="flex items-center gap-1">
                          <HardDrive className="h-3.5 w-3.5 text-slate-500" />
                          {formatSize(repo.sizeKb)}
                        </span>
                        <span className="flex items-center gap-1">
                          <FileCode2 className="h-3.5 w-3.5 text-slate-500" />
                          {repo.totalFiles} files {repo.skippedFiles > 0 ? `(${repo.skippedFiles} skipped)` : ''}
                        </span>
                      </div>

                      {repo.errorMessage && (
                        <p className="text-xs font-mono text-rose-400">{repo.errorMessage}</p>
                      )}
                    </div>

                    <div className="flex items-center gap-2.5">
                      {repo.status === 'COMPLETED' ? (
                        <>
                          <Link
                            href={`/repositories/${repo.id}/chat`}
                            className="flex items-center gap-1.5 rounded-xl bg-indigo-600 px-3.5 py-1.5 text-xs font-semibold text-white shadow-glow transition hover:bg-indigo-500"
                          >
                            <Bot className="h-3.5 w-3.5" />
                            <span>Open Chat</span>
                          </Link>
                          <Link
                            href={`/repositories/${repo.id}`}
                            className="flex items-center gap-1 rounded-xl border border-white/[0.08] bg-white/[0.03] px-3 py-1.5 text-xs font-medium text-slate-300 transition hover:bg-white/[0.08] hover:text-white"
                          >
                            <span>Overview</span>
                          </Link>
                        </>
                      ) : (
                        <Link
                          href={`/repositories/${repo.id}`}
                          className="flex items-center gap-1.5 rounded-xl border border-white/[0.08] bg-white/[0.03] px-3 py-1.5 text-xs font-medium text-slate-300 transition hover:bg-white/[0.08] hover:text-white"
                        >
                          <span>View Status</span>
                          <ArrowRight className="h-3.5 w-3.5" />
                        </Link>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
