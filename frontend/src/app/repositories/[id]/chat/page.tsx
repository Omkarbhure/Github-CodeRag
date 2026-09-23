'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { useRouter, useParams, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { apiFetch } from '@/lib/api';
import {
  RepositoryDetail,
  Conversation,
  Message
} from '@/types/repository';
import { ChatPanel } from '@/components/chat/ChatPanel';
import { CodeViewerPanel } from '@/components/chat/CodeViewerPanel';
import {
  ArrowLeft,
  ExternalLink,
  Loader2,
  AlertTriangle,
  GitCommit,
  GitBranch,
  Bot,
  Code2,
  Layers,
  Sparkles
} from 'lucide-react';

export default function RepositoryChatPage() {
  const params = useParams();
  const searchParams = useSearchParams();
  const repoId = params.id as string;
  const initialFile = searchParams?.get('file');
  const initialStart = searchParams?.get('startLine');
  const initialEnd = searchParams?.get('endLine');

  const { user, loading: authLoading, isAuthenticated } = useAuth();
  const router = useRouter();

  const [repository, setRepository] = useState<RepositoryDetail | null>(null);
  const [loadingRepo, setLoadingRepo] = useState(true);

  // Chat State
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [loadingConversations, setLoadingConversations] = useState(false);
  const [activeConversationId, setActiveConversationId] = useState<string | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [isSendingMessage, setIsSendingMessage] = useState(false);
  const [chatError, setChatError] = useState<string | null>(null);

  // Code Viewer State
  const [activeFilePath, setActiveFilePath] = useState<string | null>(null);
  const [fileContent, setFileContent] = useState<string | null>(null);
  const [loadingFile, setLoadingFile] = useState(false);
  const [fileError, setFileError] = useState<string | null>(null);
  const [highlightRange, setHighlightRange] = useState<{ startLine: number; endLine: number } | null>(null);

  // Mobile View Tab: 'chat' | 'code'
  const [mobileTab, setMobileTab] = useState<'chat' | 'code'>('chat');

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

  const fetchConversations = useCallback(async () => {
    setLoadingConversations(true);
    try {
      const data = await apiFetch<Conversation[]>(`/api/repositories/${repoId}/conversations`);
      setConversations(data);
      if (data.length > 0 && !activeConversationId) {
        setActiveConversationId(data[0].id);
      }
    } catch (err: any) {
      console.error('Failed to fetch conversations:', err);
    } finally {
      setLoadingConversations(false);
    }
  }, [repoId, activeConversationId]);

  const fetchMessages = useCallback(async (conversationId: string) => {
    setLoadingMessages(true);
    setChatError(null);
    try {
      const data = await apiFetch<Message[]>(`/api/conversations/${conversationId}/messages`);
      setMessages(data);
    } catch (err: any) {
      console.error('Failed to fetch messages:', err);
      setChatError(err.message || 'Failed to load conversation messages.');
    } finally {
      setLoadingMessages(false);
    }
  }, []);

  const handleCreateConversation = async () => {
    try {
      const newConv = await apiFetch<Conversation>(`/api/repositories/${repoId}/conversations`, {
        method: 'POST',
        body: JSON.stringify({ title: 'New Conversation' })
      });
      setConversations((prev) => [newConv, ...prev]);
      setActiveConversationId(newConv.id);
      setMessages([]);
    } catch (err: any) {
      console.error('Failed to create conversation:', err);
      setChatError(err.message || 'Could not start new conversation.');
    }
  };

  const handleSendMessage = async (prompt: string) => {
    let targetConvId = activeConversationId;

    if (!targetConvId) {
      try {
        const newConv = await apiFetch<Conversation>(`/api/repositories/${repoId}/conversations`, {
          method: 'POST',
          body: JSON.stringify({ title: prompt.slice(0, 40) })
        });
        setConversations((prev) => [newConv, ...prev]);
        setActiveConversationId(newConv.id);
        targetConvId = newConv.id;
      } catch (err: any) {
        setChatError('Failed to create thread.');
        return;
      }
    }

    const optimisticUserMsg: Message = {
      id: 'temp-' + Date.now(),
      conversationId: targetConvId,
      role: 'USER',
      content: prompt,
      createdAt: new Date().toISOString()
    };

    setMessages((prev) => [...prev, optimisticUserMsg]);
    setIsSendingMessage(true);
    setChatError(null);

    try {
      const assistantMsg = await apiFetch<Message>(`/api/conversations/${targetConvId}/messages`, {
        method: 'POST',
        body: JSON.stringify({ question: prompt, content: prompt })
      });

      setMessages((prev) => [...prev, assistantMsg]);

      // Background refresh of conversation titles
      apiFetch<Conversation[]>(`/api/repositories/${repoId}/conversations`).then((data) => {
        setConversations(data);
      }).catch(() => {});
    } catch (err: any) {
      console.error('Failed to generate answer:', err);
      setChatError(err.message || 'Failed to generate answer. Check Gemini API key & Qdrant connection.');
    } finally {
      setIsSendingMessage(false);
    }
  };

  const handleCitationClick = useCallback(async (filePath: string, startLine: number, endLine: number) => {
    setHighlightRange({ startLine, endLine });
    setMobileTab('code'); // Switch to code view on mobile

    if (activeFilePath === filePath && fileContent) {
      // File already in editor, Monaco will update highlight immediately
      return;
    }

    setActiveFilePath(filePath);
    setLoadingFile(true);
    setFileError(null);

    try {
      const apiBase = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';
      const res = await fetch(`${apiBase}/api/repositories/${repoId}/files/content?path=${encodeURIComponent(filePath)}`, {
        credentials: 'include'
      });

      if (!res.ok) {
        if (res.status === 404) {
          throw new Error(`File ${filePath} not found in repository storage.`);
        }
        throw new Error(`Server returned error ${res.status}`);
      }

      const text = await res.text();
      setFileContent(text);
    } catch (err: any) {
      console.error('Failed to fetch file content:', err);
      setFileError(err.message || 'Could not load file contents.');
      setFileContent(null);
    } finally {
      setLoadingFile(false);
    }
  }, [activeFilePath, fileContent, repoId]);

  useEffect(() => {
    if (isAuthenticated && repoId) {
      fetchRepository();
      fetchConversations();
    }
  }, [isAuthenticated, repoId, fetchRepository, fetchConversations]);

  useEffect(() => {
    if (initialFile) {
      handleCitationClick(initialFile, Number(initialStart) || 1, Number(initialEnd) || 1);
    }
  }, [initialFile, initialStart, initialEnd, handleCitationClick]);

  useEffect(() => {
    if (activeConversationId) {
      fetchMessages(activeConversationId);
    } else {
      setMessages([]);
    }
  }, [activeConversationId, fetchMessages]);

  if (authLoading || loadingRepo || !repository) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50">
        <div className="flex flex-col items-center gap-3">
          <Loader2 className="h-8 w-8 animate-spin text-indigo-600" />
          <p className="text-sm font-medium text-slate-500">Loading CodeRAG workspace...</p>
        </div>
      </div>
    );
  }

  // If repository is actively indexing and has no chunks yet
  if (repository.status !== 'COMPLETED' && (!repository.totalChunks || repository.totalChunks === 0)) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col">
        <header className="border-b border-slate-200 bg-white px-6 py-4 flex items-center justify-between">
          <Link
            href={`/repositories/${repoId}`}
            className="flex items-center gap-2 text-xs font-semibold text-slate-700 hover:text-indigo-600"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Back to Repository</span>
          </Link>
          <span className="font-semibold text-xs text-slate-900">{repository.fullName}</span>
        </header>

        <main className="flex-1 flex items-center justify-center p-6">
          <div className="max-w-md w-full rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm space-y-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-50 text-amber-600 mx-auto">
              <AlertTriangle className="h-6 w-6" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-slate-900">Indexing in Progress</h3>
              <p className="mt-1 text-xs text-slate-500">
                This repository is currently in state <span className="font-semibold text-indigo-600 font-mono">{repository.status}</span>.
                Vector embeddings and chat grounding will be enabled once indexing completes.
              </p>
            </div>

            <div className="pt-2 flex items-center justify-center gap-3">
              <Link
                href="/dashboard"
                className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                Go to Dashboard
              </Link>
              <Link
                href={`/repositories/${repoId}`}
                className="rounded-xl bg-indigo-600 px-4 py-2 text-xs font-semibold text-white hover:bg-indigo-500"
              >
                View Status Details
              </Link>
            </div>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col bg-slate-100 overflow-hidden">
      {/* Top Navbar */}
      <header className="h-14 border-b border-slate-200 bg-white px-4 sm:px-6 flex items-center justify-between shrink-0 z-20">
        <div className="flex items-center gap-3">
          <Link
            href={`/repositories/${repoId}`}
            className="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 hover:text-slate-900 transition"
            title="Overview & Files"
          >
            <ArrowLeft className="h-4 w-4" />
          </Link>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-sm font-semibold text-slate-900 truncate">{repository.fullName}</h1>
              <span className="hidden sm:inline-flex items-center gap-1 rounded-md bg-emerald-50 px-2 py-0.5 text-[10px] font-semibold text-emerald-700 border border-emerald-200">
                <Sparkles className="h-3 w-3 text-emerald-600" />
                Indexed
              </span>
            </div>
          </div>
        </div>

        {/* Mobile View Toggle */}
        <div className="flex md:hidden items-center rounded-lg border border-slate-200 bg-slate-50 p-1 text-xs">
          <button
            type="button"
            onClick={() => setMobileTab('chat')}
            className={`rounded-md px-3 py-1 font-semibold transition ${
              mobileTab === 'chat' ? 'bg-white text-indigo-600 shadow-sm' : 'text-slate-600'
            }`}
          >
            Chat
          </button>
          <button
            type="button"
            onClick={() => setMobileTab('code')}
            className={`rounded-md px-3 py-1 font-semibold transition ${
              mobileTab === 'code' ? 'bg-white text-indigo-600 shadow-sm' : 'text-slate-600'
            }`}
          >
            Code Viewer
          </button>
        </div>

        <div className="flex items-center gap-2">
          <Link
            href={`/repositories/${repoId}`}
            className="hidden sm:flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition"
          >
            <Layers className="h-3.5 w-3.5 text-slate-500" />
            <span>Repo Overview</span>
          </Link>
          <a
            href={repository.url}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 hover:text-indigo-600 transition"
          >
            <span className="hidden sm:inline">GitHub</span>
            <ExternalLink className="h-3.5 w-3.5" />
          </a>
        </div>
      </header>

      {/* Main Split Layout */}
      <main className="flex-1 min-h-0 p-3 sm:p-4">
        <div className="h-full grid grid-cols-1 md:grid-cols-12 gap-3 sm:gap-4">
          {/* Left: Chat Panel (~40-45% width) */}
          <div
            className={`h-full md:col-span-5 ${
              mobileTab === 'chat' ? 'block' : 'hidden md:block'
            }`}
          >
            <ChatPanel
              repoId={repoId}
              conversations={conversations}
              activeConversationId={activeConversationId}
              loadingConversations={loadingConversations}
              messages={messages}
              loadingMessages={loadingMessages}
              isSendingMessage={isSendingMessage}
              chatError={chatError}
              activeCitation={
                activeFilePath && highlightRange
                  ? {
                      filePath: activeFilePath,
                      startLine: highlightRange.startLine,
                      endLine: highlightRange.endLine
                    }
                  : null
              }
              onSelectConversation={setActiveConversationId}
              onCreateConversation={handleCreateConversation}
              onSendMessage={handleSendMessage}
              onCitationClick={handleCitationClick}
            />
          </div>

          {/* Right: Monaco Code Viewer (~55-60% width) */}
          <div
            className={`h-full md:col-span-7 ${
              mobileTab === 'code' ? 'block' : 'hidden md:block'
            }`}
          >
            <CodeViewerPanel
              filePath={activeFilePath}
              content={fileContent}
              isLoading={loadingFile}
              error={fileError}
              highlightRange={highlightRange}
              repositoryId={repoId}
              onSelectFile={(path, start, end) => handleCitationClick(path, start || 1, end || 1)}
            />
          </div>
        </div>
      </main>
    </div>
  );
}
