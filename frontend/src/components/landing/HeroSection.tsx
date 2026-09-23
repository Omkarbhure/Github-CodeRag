'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { GITHUB_AUTH_URL } from '@/lib/api';
import {
  Sparkles,
  ArrowRight,
  Github,
  Terminal,
  FileCode2,
  CheckCircle2,
  Layers,
  Search,
  Bot,
  Flame,
  Code2
} from 'lucide-react';

export function HeroSection() {
  const { isAuthenticated, loading } = useAuth();

  // Interactive Live Hero Demo State
  const [activeQueryIndex, setActiveQueryIndex] = useState(0);

  const sampleQueries = [
    {
      question: "Where is the JWT session cookie validated?",
      answer: "The stateless JWT filter extracts the cookie from the incoming request and validates its signature before populating the SecurityContext.",
      citation: "JwtAuthenticationFilter.java:38-47",
      file: "backend/.../security/JwtAuthenticationFilter.java",
      highlightLines: [38, 39, 40, 41, 42, 43, 44, 45, 46, 47],
      codeSnippet: `35: @Override
36: protected void doFilterInternal(HttpServletRequest request, ...) {
37:     Optional<String> tokenOpt = cookieService.extractTokenFromCookie(request);
38:     if (tokenOpt.isPresent()) {
39:         String token = tokenOpt.get();
40:         if (jwtService.validateToken(token)) {
41:             String username = jwtService.extractUsername(token);
42:             UserPrincipal principal = userDetailsService.loadUserByUsername(username);
43:             UsernamePasswordAuthenticationToken auth = new ...(principal);
44:             SecurityContextHolder.getContext().setAuthentication(auth);
45:         }
46:     }
47:     filterChain.doFilter(request, response);
48: }`,
    },
    {
      question: "How does the hybrid search combine vector and keyword scores?",
      answer: "Hybrid search executes Qdrant vector similarity and PostgreSQL full-text search in parallel via CompletableFuture, then applies min-max score fusion.",
      citation: "HybridSearchService.java:54-62",
      file: "backend/.../service/HybridSearchService.java",
      highlightLines: [54, 55, 56, 57, 58, 59, 60, 61, 62],
      codeSnippet: `50: // Weighted linear score combination
51: double finalScore = (vectorWeight * normVectorScore) + (keywordWeight * normKeywordScore);
52: 
53: return SearchResultDto.builder()
54:         .chunkId(chunk.getId())
55:         .filePath(chunk.getFilePath())
56:         .startLine(chunk.getStartLine())
57:         .endLine(chunk.getEndLine())
58:         .content(chunk.getContent())
59:         .vectorScore(vectorScore)
60:         .keywordScore(keywordScore)
61:         .finalScore(finalScore)
62:         .build();`,
    },
    {
      question: "How are low-value minified files detected during chunking?",
      answer: "The LowValueDetectorService evaluates max line length (>1000), average line length (>300), and auto-generated annotations to prevent polluting vector space.",
      citation: "LowValueDetectorService.java:22-31",
      file: "backend/.../service/LowValueDetectorService.java",
      highlightLines: [22, 23, 24, 25, 26, 27, 28, 29, 30, 31],
      codeSnippet: `20: public boolean isMinified(String content) {
21:     String[] lines = content.split("\\r?\\n");
22:     if (lines.length == 0) return false;
23:     int maxLineLen = Arrays.stream(lines).mapToInt(String::length).max().orElse(0);
24:     double avgLineLen = Arrays.stream(lines).mapToInt(String::length).average().orElse(0);
25:     
26:     if (maxLineLen > 1000 || (lines.length > 5 && avgLineLen > 300)) {
27:         log.debug("Skipping minified file with maxLine={}, avgLine={}", maxLineLen, avgLineLen);
28:         return true;
29:     }
30:     return false;
31: }`,
    },
  ];

  const currentSample = sampleQueries[activeQueryIndex];

  return (
    <section className="relative overflow-hidden pt-12 pb-20 md:pt-20 md:pb-28">
      {/* Background Dot Grid */}
      <div className="absolute inset-0 bg-dot-grid pointer-events-none -z-10" />

      {/* Radial Ambient Glow */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[350px] bg-gradient-to-r from-indigo-600/20 via-purple-600/20 to-blue-600/10 rounded-full blur-3xl pointer-events-none -z-10" />

      <div className="mx-auto max-w-6xl px-4 sm:px-6 space-y-10">
        {/* Top Header Content */}
        <div className="text-center space-y-5 max-w-3xl mx-auto">

          {/* Headline */}
          <h1 className="text-3xl font-extrabold tracking-tight text-white sm:text-5xl md:text-6xl leading-[1.1]">
            AI Code Intelligence with{' '}
            <span className="bg-gradient-to-r from-indigo-400 via-purple-400 to-violet-400 bg-clip-text text-transparent">
              Grounded Citations
            </span>
          </h1>

          {/* Subtitle */}
          <p className="mx-auto max-w-2xl text-sm sm:text-base text-slate-400 leading-relaxed">
            Instant RAG over repositories with hybrid vector + full-text search. Inspect verified source code in Monaco Editor, synthesize architecture overviews, and diagnose stack trace bugs.
          </p>

          {/* CTA Buttons */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
            {!loading && isAuthenticated ? (
              <Link
                href="/dashboard"
                className="flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl bg-indigo-600 px-6 py-3 text-sm font-semibold text-white shadow-glow transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-[#07090e]"
              >
                <span>Go to Dashboard</span>
                <ArrowRight className="h-4 w-4" />
              </Link>
            ) : (
              <>
                <Link
                  href="/signup"
                  className="flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl bg-indigo-600 px-6 py-3 text-sm font-semibold text-white shadow-glow transition hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 focus:ring-offset-[#07090e]"
                >
                  <span>Get Started Free</span>
                  <ArrowRight className="h-4 w-4" />
                </Link>
                <a
                  href={GITHUB_AUTH_URL}
                  className="flex w-full sm:w-auto items-center justify-center gap-2 rounded-xl border border-white/[0.12] bg-white/[0.04] px-5 py-3 text-sm font-semibold text-slate-200 shadow-sm transition hover:bg-white/[0.08] hover:text-white focus:outline-none focus:ring-2 focus:ring-slate-400 focus:ring-offset-2 focus:ring-offset-[#07090e]"
                >
                  <Github className="h-4 w-4" />
                  <span>Continue with GitHub</span>
                </a>
              </>
            )}
          </div>
        </div>

        {/* Live Interactive Hero IDE Workbench Preview */}
        <div className="rounded-2xl border border-white/[0.1] bg-[#0c101d] shadow-2xl overflow-hidden">
          {/* Top Window Bar */}
          <div className="flex items-center justify-between border-b border-white/[0.08] bg-[#090d18] px-4 py-3">
            <div className="flex items-center gap-2">
              <div className="h-3 w-3 rounded-full bg-rose-500/80" />
              <div className="h-3 w-3 rounded-full bg-amber-500/80" />
              <div className="h-3 w-3 rounded-full bg-emerald-500/80" />
              <span className="ml-2 font-mono text-xs text-slate-400">repopilot-workspace / split-view-grounding</span>
            </div>
            <div className="flex items-center gap-2 text-xs font-mono text-slate-400">
              <span className="inline-flex items-center gap-1 text-emerald-400">
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
                Live Demo
              </span>
            </div>
          </div>

          {/* Interactive Question Selector Pills */}
          <div className="flex flex-wrap items-center gap-2 border-b border-white/[0.08] bg-[#080c16]/70 p-3">
            <span className="text-xs font-mono text-slate-400 px-2 flex items-center gap-1.5">
              <Terminal className="h-3.5 w-3.5 text-indigo-400" />
              Select Query:
            </span>
            {sampleQueries.map((item, idx) => (
              <button
                key={idx}
                onClick={() => setActiveQueryIndex(idx)}
                className={`rounded-lg px-3 py-1.5 text-xs font-medium transition ${
                  activeQueryIndex === idx
                    ? 'bg-indigo-600/30 text-indigo-300 border border-indigo-500/40 shadow-sm'
                    : 'bg-white/[0.03] text-slate-400 border border-white/[0.05] hover:bg-white/[0.07] hover:text-slate-200'
                }`}
              >
                {item.question}
              </button>
            ))}
          </div>

          {/* Split-View Display */}
          <div className="grid grid-cols-1 lg:grid-cols-12 min-h-[340px]">
            {/* Left: Chat Answer & Grounded Citation */}
            <div className="lg:col-span-5 p-5 border-b lg:border-b-0 lg:border-r border-white/[0.08] space-y-4 flex flex-col justify-between bg-[#0b0f1c]/50">
              <div className="space-y-3">
                <div className="flex items-center gap-2 text-xs font-semibold text-slate-400">
                  <div className="flex h-5 w-5 items-center justify-center rounded-md bg-indigo-600 text-white text-[10px]">
                    <Bot className="h-3 w-3" />
                  </div>
                  <span>Assistant Answer</span>
                </div>
                <p className="text-xs sm:text-sm text-slate-200 leading-relaxed font-sans">
                  {currentSample.answer}
                </p>
                <div className="pt-2">
                  <span className="text-[11px] font-mono text-slate-400 uppercase tracking-wider block mb-1.5">
                    Grounded Source Citation:
                  </span>
                  <div className="inline-flex items-center gap-1.5 rounded-lg border border-indigo-500/30 bg-indigo-950/40 px-3 py-1.5 text-xs font-mono text-indigo-300">
                    <FileCode2 className="h-3.5 w-3.5 text-indigo-400" />
                    <span>[{currentSample.citation}]</span>
                  </div>
                </div>
              </div>

              <div className="rounded-xl border border-white/[0.06] bg-white/[0.02] p-3 text-[11px] text-slate-400 font-mono flex items-center justify-between">
                <span>Vector match score: 0.941</span>
                <span className="text-emerald-400">100% Grounded</span>
              </div>
            </div>

            {/* Right: Monaco-Style Code Viewer Pane */}
            <div className="lg:col-span-7 flex flex-col bg-[#070a12]">
              {/* File tab */}
              <div className="flex items-center justify-between border-b border-white/[0.08] bg-[#090d18] px-4 py-2">
                <div className="flex items-center gap-2 font-mono text-xs text-indigo-300">
                  <FileCode2 className="h-3.5 w-3.5 text-indigo-400" />
                  <span>{currentSample.file}</span>
                </div>
                <span className="font-mono text-[11px] text-slate-400">Java / Read-Only</span>
              </div>

              {/* Code Snippet with Highlight */}
              <div className="p-4 font-mono text-xs text-slate-300 overflow-x-auto leading-relaxed flex-1">
                <pre className="text-slate-300 font-mono">
                  {currentSample.codeSnippet.split('\n').map((line, lineIdx) => {
                    const lineNum = parseInt(line.split(':')[0], 10);
                    const isHighlighted = currentSample.highlightLines.includes(lineNum);
                    return (
                      <div
                        key={lineIdx}
                        className={`px-2 py-0.5 rounded transition ${
                          isHighlighted
                            ? 'bg-indigo-950/70 border-l-2 border-indigo-400 text-indigo-200'
                            : 'text-slate-400 hover:text-slate-200'
                        }`}
                      >
                        {line}
                      </div>
                    );
                  })}
                </pre>
              </div>
            </div>
          </div>
        </div>

        {/* Feature Highlights Grid */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 pt-2">
          <div className="glass-card rounded-xl p-3.5 space-y-1">
            <div className="flex items-center gap-2 text-indigo-400">
              <CheckCircle2 className="h-4 w-4" />
              <span className="text-xs font-semibold text-white">Hybrid Retrieval</span>
            </div>
            <p className="text-[11px] text-slate-400">Vector similarity + Postgres full-text ranking</p>
          </div>

          <div className="glass-card rounded-xl p-3.5 space-y-1">
            <div className="flex items-center gap-2 text-indigo-400">
              <CheckCircle2 className="h-4 w-4" />
              <span className="text-xs font-semibold text-white">Monaco Highlighting</span>
            </div>
            <p className="text-[11px] text-slate-400">Direct navigation to exact cited source lines</p>
          </div>

          <div className="glass-card rounded-xl p-3.5 space-y-1">
            <div className="flex items-center gap-2 text-indigo-400">
              <CheckCircle2 className="h-4 w-4" />
              <span className="text-xs font-semibold text-white">Architecture Maps</span>
            </div>
            <p className="text-[11px] text-slate-400">Automated system overview synthesis</p>
          </div>

          <div className="glass-card rounded-xl p-3.5 space-y-1">
            <div className="flex items-center gap-2 text-indigo-400">
              <CheckCircle2 className="h-4 w-4" />
              <span className="text-xs font-semibold text-white">Bug Diagnostics</span>
            </div>
            <p className="text-[11px] text-slate-400">Stack trace parser &amp; root cause fix generator</p>
          </div>
        </div>
      </div>
    </section>
  );
}
