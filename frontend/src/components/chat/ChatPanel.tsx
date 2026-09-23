'use client';

import React, { useState, useRef, useEffect } from 'react';
import { Conversation, Message } from '@/types/repository';
import { CitationChip } from './CitationChip';
import { GroundedMarkdown } from './GroundedMarkdown';
import {
  MessageSquare,
  Plus,
  Send,
  Loader2,
  Bot,
  User as UserIcon,
  Sparkles,
  AlertTriangle,
  ChevronDown
} from 'lucide-react';

interface ActiveCitation {
  filePath: string;
  startLine: number;
  endLine: number;
}

interface ChatPanelProps {
  repoId: string;
  conversations: Conversation[];
  activeConversationId: string | null;
  loadingConversations: boolean;
  messages: Message[];
  loadingMessages: boolean;
  isSendingMessage: boolean;
  chatError: string | null;
  activeCitation: ActiveCitation | null;
  onSelectConversation: (convId: string) => void;
  onCreateConversation: () => void;
  onSendMessage: (content: string) => void;
  onCitationClick: (filePath: string, startLine: number, endLine: number) => void;
}

export function ChatPanel({
  repoId,
  conversations,
  activeConversationId,
  loadingConversations,
  messages,
  loadingMessages,
  isSendingMessage,
  chatError,
  activeCitation,
  onSelectConversation,
  onCreateConversation,
  onSendMessage,
  onCitationClick
}: ChatPanelProps) {
  const [inputPrompt, setInputPrompt] = useState('');
  const [isThreadDropdownOpen, setIsThreadDropdownOpen] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const handleSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const prompt = inputPrompt.trim();
    if (!prompt || isSendingMessage) return;
    onSendMessage(prompt);
    setInputPrompt('');
  };

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isSendingMessage]);

  const activeConv = conversations.find((c) => c.id === activeConversationId);

  return (
    <div className="flex h-full flex-col rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
      {/* Top Thread Bar */}
      <div className="flex items-center justify-between border-b border-slate-200 bg-slate-50/75 px-4 py-2.5">
        <div className="relative flex-1 mr-2">
          <button
            type="button"
            onClick={() => setIsThreadDropdownOpen(!isThreadDropdownOpen)}
            className="flex w-full items-center justify-between gap-2 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-800 shadow-sm transition hover:bg-slate-50"
          >
            <div className="flex items-center gap-2 truncate">
              <MessageSquare className="h-3.5 w-3.5 text-indigo-600 shrink-0" />
              <span className="truncate">{activeConv?.title || 'Current Conversation'}</span>
            </div>
            <ChevronDown className="h-3.5 w-3.5 text-slate-400 shrink-0" />
          </button>

          {/* Thread Switcher Dropdown */}
          {isThreadDropdownOpen && (
            <div className="absolute left-0 top-full z-30 mt-1 max-h-60 w-full overflow-y-auto rounded-xl border border-slate-200 bg-white p-1.5 shadow-lg">
              {loadingConversations ? (
                <div className="py-4 text-center text-xs text-slate-400">Loading threads...</div>
              ) : conversations.length === 0 ? (
                <div className="py-4 text-center text-xs text-slate-400">No conversations yet</div>
              ) : (
                conversations.map((conv) => (
                  <button
                    key={conv.id}
                    type="button"
                    onClick={() => {
                      onSelectConversation(conv.id);
                      setIsThreadDropdownOpen(false);
                    }}
                    className={`w-full rounded-lg px-3 py-2 text-left text-xs transition flex flex-col ${
                      conv.id === activeConversationId
                        ? 'bg-indigo-50 text-indigo-900 font-semibold'
                        : 'text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    <span className="truncate">{conv.title || 'Untitled Thread'}</span>
                    <span className="text-[10px] text-slate-400 font-normal mt-0.5">
                      {new Date(conv.updatedAt || conv.createdAt).toLocaleDateString(undefined, {
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                      })}
                    </span>
                  </button>
                ))
              )}
            </div>
          )}
        </div>

        <button
          type="button"
          onClick={onCreateConversation}
          className="flex items-center gap-1 rounded-xl bg-indigo-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm transition hover:bg-indigo-500 shrink-0"
        >
          <Plus className="h-3.5 w-3.5" />
          <span>New</span>
        </button>
      </div>

      {/* Messages Stream */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {loadingMessages ? (
          <div className="flex h-full flex-col items-center justify-center gap-2 text-xs text-slate-400">
            <Loader2 className="h-6 w-6 animate-spin text-indigo-600" />
            <span>Loading message history...</span>
          </div>
        ) : messages.length === 0 ? (
          <div className="flex h-full flex-col items-center justify-center text-center max-w-sm mx-auto py-8 space-y-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-600 shadow-inner">
              <Sparkles className="h-5 w-5" />
            </div>
            <div>
              <h4 className="text-xs font-semibold text-slate-900">Ask anything about this repo</h4>
              <p className="mt-1 text-[11px] text-slate-500">
                Ask a question to retrieve code chunks with exact line citations.
              </p>
            </div>

            <div className="w-full space-y-1.5 pt-2">
              {[
                'How is authentication and JWT token handling implemented?',
                'Explain the main repository import flow and zip extraction.',
                'Where is vector embedding and Qdrant search handled?',
                'What file filtering rules and size limits are applied?'
              ].map((prompt, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => onSendMessage(prompt)}
                  className="w-full rounded-xl border border-slate-200 p-2 text-left text-xs text-slate-700 hover:border-indigo-300 hover:bg-indigo-50/60 hover:text-indigo-900 transition flex items-center justify-between"
                >
                  <span className="truncate">{prompt}</span>
                  <Sparkles className="h-3 w-3 text-indigo-500 shrink-0 ml-1" />
                </button>
              ))}
            </div>
          </div>
        ) : (
          <>
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`flex gap-2.5 ${msg.role === 'USER' ? 'justify-end' : 'justify-start'}`}
              >
                {msg.role === 'ASSISTANT' && (
                  <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-indigo-600 text-white shadow-sm mt-0.5">
                    <Bot className="h-3.5 w-3.5" />
                  </div>
                )}

                <div
                  className={`max-w-[88%] rounded-2xl p-3.5 shadow-sm ${
                    msg.role === 'USER'
                      ? 'bg-indigo-600 text-white rounded-tr-none'
                      : 'bg-slate-50 border border-slate-200/80 text-slate-900 rounded-tl-none'
                  }`}
                >
                  {msg.role === 'USER' ? (
                    <p className="text-xs whitespace-pre-wrap">{msg.content}</p>
                  ) : (
                    <GroundedMarkdown
                      content={msg.content}
                      activeCitation={activeCitation}
                      onCitationClick={onCitationClick}
                    />
                  )}
                </div>

                {msg.role === 'USER' && (
                  <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-slate-200 text-slate-700 shadow-sm mt-0.5">
                    <UserIcon className="h-3.5 w-3.5" />
                  </div>
                )}
              </div>
            ))}

            {isSendingMessage && (
              <div className="flex gap-2.5 justify-start">
                <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-indigo-600 text-white shadow-sm mt-0.5">
                  <Bot className="h-3.5 w-3.5" />
                </div>
                <div className="rounded-2xl rounded-tl-none bg-slate-50 border border-slate-200/80 p-3.5 shadow-sm flex items-center gap-2.5">
                  <Loader2 className="h-4 w-4 animate-spin text-indigo-600" />
                  <span className="text-xs text-slate-500 font-medium">
                    Retrieving code & generating grounded answer...
                  </span>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </>
        )}
      </div>

      {/* Chat Error Banner */}
      {chatError && (
        <div className="mx-3 mb-2 flex items-center gap-2 rounded-xl bg-rose-50 p-2.5 text-xs text-rose-700 border border-rose-200">
          <AlertTriangle className="h-4 w-4 shrink-0 text-rose-600" />
          <span>{chatError}</span>
        </div>
      )}

      {/* Input Box */}
      <div className="border-t border-slate-200 p-3 bg-white">
        <form onSubmit={handleSubmit} className="flex items-center gap-2">
          <input
            type="text"
            value={inputPrompt}
            onChange={(e) => setInputPrompt(e.target.value)}
            placeholder="Ask a question about this repository's code..."
            disabled={isSendingMessage}
            className="flex-1 rounded-xl border border-slate-200 bg-slate-50/60 px-3.5 py-2 text-xs placeholder-slate-400 focus:border-indigo-600 focus:bg-white focus:outline-none focus:ring-1 focus:ring-indigo-600 disabled:opacity-50"
          />
          <button
            type="submit"
            disabled={isSendingMessage || !inputPrompt.trim()}
            className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-600 text-white shadow-sm transition hover:bg-indigo-500 disabled:opacity-40 shrink-0"
          >
            {isSendingMessage ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Send className="h-3.5 w-3.5" />
            )}
          </button>
        </form>
        <div className="mt-1.5 flex items-center justify-between text-[10px] text-slate-400 px-1">
          <span>Grounded Gemini 1.5 + Qdrant</span>
          <span>Click citations to view code</span>
        </div>
      </div>
    </div>
  );
}
