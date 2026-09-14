'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { apiFetch } from '@/lib/api';
import { RepositorySummary, IndexingStatus } from '@/types/repository';
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
  Layers
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
          <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700 ring-1 ring-emerald-600/20">
            <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
            COMPLETED
          </span>
        );
      case 'EMBEDDING':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-purple-50 px-2.5 py-1 text-xs font-semibold text-purple-700 ring-1 ring-purple-600/20">
            <Sparkles className="h-3.5 w-3.5 animate-pulse text-purple-600" />
            EMBEDDING VECTORS
          </span>
        );
      case 'CHUNKING':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-indigo-50 px-2.5 py-1 text-xs font-semibold text-indigo-700 ring-1 ring-indigo-600/20">
            <Layers className="h-3.5 w-3.5 animate-pulse text-indigo-600" />
            CHUNKING CODE
          </span>
        );
      case 'SCANNING':
      case 'DOWNLOADING':
      case 'PENDING':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-blue-50 px-2.5 py-1 text-xs font-semibold text-blue-700 ring-1 ring-blue-600/20">
            <Loader2 className="h-3.5 w-3.5 animate-spin text-blue-600" />
            {status}
          </span>
        );
      case 'REJECTED_TOO_LARGE':
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-800 ring-1 ring-amber-600/20">
            <AlertTriangle className="h-3.5 w-3.5 text-amber-600" />
            TOO LARGE (&gt;100MB)
          </span>
        );
      case 'FAILED':
      default:
        return (
          <span className="inline-flex items-center gap-1.5 rounded-full bg-rose-50 px-2.5 py-1 text-xs font-semibold text-rose-700 ring-1 ring-rose-600/20">
            <XCircle className="h-3.5 w-3.5 text-rose-600" />
            FAILED
          </span>
        );
    }
  };

  if (loading || !user) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50">
        <div className="flex flex-col items-center gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-indigo-600 border-t-transparent" />
          <p className="text-sm font-medium text-slate-500">Loading your session...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Header */}
      <header className="sticky top-0 z-10 border-b border-slate-200 bg-white/80 backdrop-blur-md">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4 sm:px-6">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-indigo-600 font-bold text-white shadow-sm">
              CR
            </div>
            <div>
              <h1 className="text-base font-semibold text-slate-900">GitHub CodeRAG</h1>
              <p className="text-xs text-slate-500">AI Code Assistant + Monaco Code Viewer</p>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <div className="hidden sm:flex items-center gap-2 text-xs text-slate-600">
              <span className="h-2 w-2 rounded-full bg-emerald-500" />
              <span>{user.email || user.githubUsername}</span>
            </div>
            <button
              onClick={handleLogout}
              className="flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50 hover:text-red-600 focus:outline-none focus:ring-2 focus:ring-slate-900 focus:ring-offset-2"
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
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-center gap-2 text-slate-900 font-semibold text-base">
            <FolderGit2 className="h-5 w-5 text-indigo-600" />
            <h2>Import Public GitHub Repository</h2>
          </div>
          <p className="mt-1 text-xs text-slate-500">
            Provide a public GitHub URL (e.g. <code className="rounded bg-slate-100 px-1 py-0.5 font-mono text-slate-800">https://github.com/owner/repository</code>). Repo cap: 100MB; individual file cap: 10MB.
          </p>

          {importError && (
            <div className="mt-4 flex items-center gap-2 rounded-lg bg-red-50 p-3.5 text-xs text-red-700 ring-1 ring-red-200">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{importError}</span>
            </div>
          )}

          {importSuccess && (
            <div className="mt-4 flex items-center gap-2 rounded-lg bg-emerald-50 p-3.5 text-xs text-emerald-700 ring-1 ring-emerald-200">
              <CheckCircle2 className="h-4 w-4 shrink-0" />
              <span>{importSuccess}</span>
            </div>
          )}

          <form onSubmit={handleImport} className="mt-4 flex flex-col sm:flex-row gap-3">
            <div className="relative flex-1">
              <Github className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                required
                value={githubUrl}
                onChange={(e) => setGithubUrl(e.target.value)}
                placeholder="https://github.com/owner/repository"
                className="w-full rounded-lg border border-slate-200 py-2.5 pl-9 pr-3 text-sm placeholder-slate-400 focus:border-indigo-600 focus:outline-none focus:ring-1 focus:ring-indigo-600"
              />
            </div>

            <button
              type="submit"
              disabled={importing}
              className="flex items-center justify-center gap-2 rounded-lg bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2 disabled:opacity-50"
            >
              {importing ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span>Importing & Indexing...</span>
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
            <h2 className="text-base font-semibold text-slate-900">Your Repositories</h2>
            <button
              onClick={fetchRepositories}
              className="flex items-center gap-1.5 text-xs font-medium text-slate-600 hover:text-slate-900 transition"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              <span>Refresh</span>
            </button>
          </div>

          {loadingRepos ? (
            <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center">
              <Loader2 className="mx-auto h-6 w-6 animate-spin text-slate-400" />
              <p className="mt-2 text-xs text-slate-500">Loading repositories...</p>
            </div>
          ) : repositories.length === 0 ? (
            <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center">
              <FolderGit2 className="mx-auto h-10 w-10 text-slate-300" />
              <h3 className="mt-2 text-sm font-semibold text-slate-900">No repositories yet</h3>
              <p className="mt-1 text-xs text-slate-500">
                Paste a public GitHub URL above to import your first code repository.
              </p>
            </div>
          ) : (
            <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
              <div className="divide-y divide-slate-200">
                {repositories.map((repo) => (
                  <div
                    key={repo.id}
                    className="flex flex-col sm:flex-row sm:items-center justify-between p-5 gap-4 hover:bg-slate-50/80 transition"
                  >
                    <div className="space-y-1.5">
                      <div className="flex items-center gap-2">
                        <Link
                          href={repo.status === 'COMPLETED' ? `/repositories/${repo.id}/chat` : `/repositories/${repo.id}`}
                          className="font-semibold text-sm text-slate-900 hover:text-indigo-600 transition"
                        >
                          {repo.fullName}
                        </Link>
                        {renderStatusBadge(repo.status)}
                      </div>

                      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500">
                        {repo.defaultBranch && (
                          <span className="flex items-center gap-1">
                            <GitBranch className="h-3.5 w-3.5 text-slate-400" />
                            {repo.defaultBranch}
                          </span>
                        )}
                        {repo.latestCommitSha && (
                          <span className="flex items-center gap-1 font-mono">
                            <GitCommit className="h-3.5 w-3.5 text-slate-400" />
                            {repo.latestCommitSha.substring(0, 7)}
                          </span>
                        )}
                        <span className="flex items-center gap-1">
                          <HardDrive className="h-3.5 w-3.5 text-slate-400" />
                          {formatSize(repo.sizeKb)}
                        </span>
                        <span className="flex items-center gap-1">
                          <FileCode2 className="h-3.5 w-3.5 text-slate-400" />
                          {repo.totalFiles} files {repo.skippedFiles > 0 ? `(${repo.skippedFiles} skipped)` : ''}
                        </span>
                      </div>

                      {repo.errorMessage && (
                        <p className="text-xs text-red-600">{repo.errorMessage}</p>
                      )}
                    </div>

                    <div className="flex items-center gap-2.5">
                      {repo.status === 'COMPLETED' ? (
                        <>
                          <Link
                            href={`/repositories/${repo.id}/chat`}
                            className="flex items-center gap-1.5 rounded-xl bg-indigo-600 px-3.5 py-1.5 text-xs font-semibold text-white shadow-sm transition hover:bg-indigo-500"
                          >
                            <Bot className="h-3.5 w-3.5" />
                            <span>Open Chat</span>
                          </Link>
                          <Link
                            href={`/repositories/${repo.id}`}
                            className="flex items-center gap-1 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50 hover:text-indigo-600"
                          >
                            <span>Overview</span>
                          </Link>
                        </>
                      ) : (
                        <Link
                          href={`/repositories/${repo.id}`}
                          className="flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50 hover:text-indigo-600"
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
