'use client';

import React, { useState } from 'react';
import {
  Bot,
  FileCode2,
  ExternalLink,
  Sparkles,
  Layers,
  Compass,
  Bug,
  Code2,
  Eye,
  CheckCircle2
} from 'lucide-react';

export function DemoShowcaseSection() {
  const [activeTab, setActiveTab] = useState<'chat' | 'architecture' | 'bug'>('chat');

  return (
    <section id="demo" className="py-12 md:py-16 bg-slate-900 text-white relative overflow-hidden">
      <div className="mx-auto max-w-6xl px-4 sm:px-6 space-y-8">
        {/* Section Header */}
        <div className="text-center max-w-2xl mx-auto space-y-2">
          <div className="inline-flex items-center gap-1.5 rounded-full bg-indigo-500/10 border border-indigo-500/20 px-3 py-1 text-xs font-semibold text-indigo-400">
            <Eye className="h-3.5 w-3.5" />
            <span>Interactive Workspace Preview</span>
          </div>
          <h2 className="text-2xl sm:text-3xl font-bold tracking-tight text-white">
            Designed for Developers, Grounded in Real Source
          </h2>
          <p className="text-xs sm:text-sm text-slate-400">
            Experience the split-view RAG assistant with live Monaco editor code highlighting, automated architecture overviews, and targeted bug investigation.
          </p>
        </div>

        {/* Tab Switcher */}
        <div className="flex justify-center">
          <div className="inline-flex items-center rounded-xl bg-slate-800/80 p-1 border border-slate-700">
            <button
              onClick={() => setActiveTab('chat')}
              className={`flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold transition ${
                activeTab === 'chat'
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <Bot className="h-3.5 w-3.5" />
              <span>Split-View Chat + Monaco</span>
            </button>
            <button
              onClick={() => setActiveTab('architecture')}
              className={`flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold transition ${
                activeTab === 'architecture'
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <Compass className="h-3.5 w-3.5" />
              <span>Architecture Overview</span>
            </button>
            <button
              onClick={() => setActiveTab('bug')}
              className={`flex items-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold transition ${
                activeTab === 'bug'
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <Bug className="h-3.5 w-3.5" />
              <span>Bug Investigation</span>
            </button>
          </div>
        </div>

        {/* Active Mockup Container */}
        <div className="rounded-2xl border border-slate-800 bg-slate-950 p-4 sm:p-6 shadow-2xl">
          {activeTab === 'chat' && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
              {/* Left Pane: Chat Mockup */}
              <div className="lg:col-span-5 rounded-xl border border-slate-800 bg-slate-900/90 p-4 space-y-4 flex flex-col justify-between">
                <div className="space-y-3">
                  <div className="flex items-center justify-between border-b border-slate-800 pb-2">
                    <div className="flex items-center gap-2 text-xs font-semibold text-slate-200">
                      <Bot className="h-4 w-4 text-indigo-400" />
                      <span>RAG Assistant</span>
                    </div>
                    <span className="rounded bg-indigo-500/20 px-2 py-0.5 text-[10px] font-mono text-indigo-300">
                      Hybrid Retrieval (Top 8)
                    </span>
                  </div>

                  {/* User Question */}
                  <div className="flex justify-end">
                    <div className="rounded-xl bg-indigo-600 px-3.5 py-2 text-xs text-white max-w-[85%] shadow-sm">
                      How does user authentication and JWT validation work on incoming requests?
                    </div>
                  </div>

                  {/* Assistant Answer with Citation Chips */}
                  <div className="flex justify-start">
                    <div className="rounded-xl bg-slate-800 border border-slate-700/80 p-3.5 text-xs text-slate-200 space-y-2 max-w-[95%]">
                      <p className="leading-relaxed">
                        Requests are filtered through <span className="inline-flex items-center gap-1 rounded bg-indigo-500/20 px-1.5 py-0.5 font-mono text-[10px] text-indigo-300 border border-indigo-500/30 cursor-pointer">JwtAuthenticationFilter.java:35-62</span>, which extracts the HttpOnly JWT token from cookies.
                      </p>
                      <p className="leading-relaxed">
                        If valid, user claims are loaded via <span className="inline-flex items-center gap-1 rounded bg-indigo-500/20 px-1.5 py-0.5 font-mono text-[10px] text-indigo-300 border border-indigo-500/30 cursor-pointer">CustomUserDetailsService.java:18-32</span> and registered in the Spring Security context.
                      </p>
                    </div>
                  </div>
                </div>

                {/* Screenshot Placeholder Note */}
                <div className="rounded-lg border border-dashed border-slate-700 bg-slate-950/60 p-2.5 text-center text-[10px] text-slate-400">
                  📷 Placeholder: <code className="text-slate-300 font-mono">/screenshots/chat-demo.png</code>
                </div>
              </div>

              {/* Right Pane: Monaco Editor Mockup */}
              <div className="lg:col-span-7 rounded-xl border border-slate-800 bg-slate-900/90 p-4 space-y-3 font-mono text-xs">
                <div className="flex items-center justify-between border-b border-slate-800 pb-2">
                  <div className="flex items-center gap-2 text-slate-300 font-medium">
                    <FileCode2 className="h-4 w-4 text-indigo-400" />
                    <span>src/security/JwtAuthenticationFilter.java</span>
                  </div>
                  <span className="rounded bg-emerald-500/10 px-2 py-0.5 text-[10px] text-emerald-400 border border-emerald-500/20">
                    Lines 35&ndash;62 Active
                  </span>
                </div>

                {/* Code Window with Highlighted Lines */}
                <div className="rounded-lg bg-slate-950 p-4 space-y-1 overflow-x-auto text-[11px] leading-relaxed">
                  <div className="text-slate-600">34 |   @Override</div>
                  <div className="bg-indigo-950/80 border-l-2 border-indigo-500 px-2 py-0.5 text-indigo-200">
                    35 |   protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) &#123;
                  </div>
                  <div className="bg-indigo-950/80 border-l-2 border-indigo-500 px-2 py-0.5 text-indigo-200">
                    36 |       String token = cookieService.extractToken(request);
                  </div>
                  <div className="bg-indigo-950/80 border-l-2 border-indigo-500 px-2 py-0.5 text-indigo-200">
                    37 |       if (token != null &amp;&amp; jwtService.validateToken(token)) &#123;
                  </div>
                  <div className="bg-indigo-950/80 border-l-2 border-indigo-500 px-2 py-0.5 text-indigo-200">
                    38 |           UserDetails user = userDetailsService.loadUserByUsername(jwtService.extractEmail(token));
                  </div>
                  <div className="bg-indigo-950/80 border-l-2 border-indigo-500 px-2 py-0.5 text-indigo-200">
                    39 |           SecurityContextHolder.getContext().setAuthentication(auth);
                  </div>
                  <div className="bg-indigo-950/80 border-l-2 border-indigo-500 px-2 py-0.5 text-indigo-200">
                    40 |       &#125;
                  </div>
                  <div className="text-slate-600">41 |       chain.doFilter(request, response);</div>
                  <div className="text-slate-600">42 |   &#125;</div>
                </div>

                {/* Screenshot Placeholder Note */}
                <div className="rounded-lg border border-dashed border-slate-700 bg-slate-950/60 p-2.5 text-center text-[10px] text-slate-400">
                  📷 Placeholder: <code className="text-slate-300 font-mono">/screenshots/monaco-code-viewer.png</code>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'architecture' && (
            <div className="space-y-4">
              <div className="rounded-xl border border-slate-800 bg-slate-900/90 p-5 space-y-4">
                <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800 pb-3">
                  <div>
                    <h3 className="text-sm font-bold text-white flex items-center gap-2">
                      <Compass className="h-4 w-4 text-purple-400" />
                      <span>Synthesized Architecture Overview</span>
                    </h3>
                    <p className="text-xs text-slate-400">Representative sampling across manifests, entry points &amp; module hierarchy</p>
                  </div>
                  <div className="flex gap-1.5">
                    {['Spring Boot 3', 'PostgreSQL', 'Qdrant', 'Next.js 14'].map((t) => (
                      <span key={t} className="rounded bg-purple-500/10 px-2 py-0.5 text-[10px] font-mono text-purple-300 border border-purple-500/20">
                        {t}
                      </span>
                    ))}
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
                  <div className="rounded-xl bg-slate-950 p-4 border border-slate-800 space-y-2">
                    <h4 className="font-semibold text-slate-200">1. Ingestion &amp; Chunking</h4>
                    <p className="text-slate-400 text-[11px] leading-relaxed">
                      Downloads git trees, applies minified/generated low-value filtering, and segments source into 120-line sliding windows.
                    </p>
                  </div>
                  <div className="rounded-xl bg-slate-950 p-4 border border-slate-800 space-y-2">
                    <h4 className="font-semibold text-slate-200">2. Hybrid Vector Search</h4>
                    <p className="text-slate-400 text-[11px] leading-relaxed">
                      Concurrently queries Qdrant 768-dim embeddings &amp; PostgreSQL GIN tsvector full-text index with weighted score fusion.
                    </p>
                  </div>
                  <div className="rounded-xl bg-slate-950 p-4 border border-slate-800 space-y-2">
                    <h4 className="font-semibold text-slate-200">3. Grounded Chat &amp; Monaco</h4>
                    <p className="text-slate-400 text-[11px] leading-relaxed">
                      Gemini 1.5 Flash generates citation-enforced answers deep-linked into the split-view Monaco Editor.
                    </p>
                  </div>
                </div>

                {/* Screenshot Placeholder Note */}
                <div className="rounded-lg border border-dashed border-slate-700 bg-slate-950/60 p-3 text-center text-xs text-slate-400">
                  📷 Placeholder: <code className="text-slate-300 font-mono">/screenshots/architecture-overview.png</code>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'bug' && (
            <div className="space-y-4">
              <div className="rounded-xl border border-slate-800 bg-slate-900/90 p-5 space-y-4">
                <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                  <div>
                    <h3 className="text-sm font-bold text-white flex items-center gap-2">
                      <Bug className="h-4 w-4 text-rose-400" />
                      <span>Automated Stack Trace Root Cause Diagnosis</span>
                    </h3>
                    <p className="text-xs text-slate-400">Targeted line retrieval combined with fix synthesis</p>
                  </div>
                  <span className="rounded bg-rose-500/10 px-2 py-0.5 text-[10px] font-bold text-rose-400 border border-rose-500/20">
                    High Confidence (95%)
                  </span>
                </div>

                <div className="rounded-xl bg-slate-950 p-4 border border-slate-800 text-xs space-y-2">
                  <div className="font-semibold text-rose-300">Suspected Root Cause:</div>
                  <p className="text-slate-300 leading-relaxed text-[11px]">
                    <code className="text-rose-400">NullPointerException</code> at <span className="underline cursor-pointer text-indigo-300">AuthService.java:42</span> caused by unverified OAuth user payload when GitHub email visibility is set to private.
                  </p>
                  <div className="pt-2 font-semibold text-emerald-400">Proposed Code Fix:</div>
                  <pre className="rounded-lg bg-slate-900 p-3 font-mono text-[11px] text-emerald-300 overflow-x-auto">
                    {`String email = gitHubUser.getEmail() != null \n  ? gitHubUser.getEmail() \n  : fetchPrimaryEmailFromGitHubApi(accessToken);`}
                  </pre>
                </div>

                {/* Screenshot Placeholder Note */}
                <div className="rounded-lg border border-dashed border-slate-700 bg-slate-950/60 p-3 text-center text-xs text-slate-400">
                  📷 Placeholder: <code className="text-slate-300 font-mono">/screenshots/bug-investigation.png</code>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
