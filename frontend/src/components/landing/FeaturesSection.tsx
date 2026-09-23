'use client';

import React from 'react';
import {
  Sparkles,
  Layers,
  FileCode2,
  Compass,
  Bug,
  Cpu,
  Code2,
  Zap,
  ShieldCheck,
  CheckCircle2
} from 'lucide-react';

export function FeaturesSection() {
  const features = [
    {
      icon: Sparkles,
      tag: 'HYBRID FUSION',
      title: 'Hybrid Semantic & Keyword Search',
      description:
        'Combines Qdrant 768-dim vector embeddings with PostgreSQL GIN full-text keyword search via min-max weighted score fusion (0.7 / 0.3) for optimal retrieval.'
    },
    {
      icon: FileCode2,
      tag: 'AST LINE GROUNDING',
      title: 'Grounded Chat & Inline Citations',
      description:
        'Every answer includes strict inline citations [filePath:startLine-endLine] deep-linked into the Monaco Editor with delta line-range decorations.'
    },
    {
      icon: Compass,
      tag: 'COMMIT-CACHED',
      title: 'Automated Architecture Synthesis',
      description:
        'Representative sampling across manifests, documentation, entry points, and directory hierarchies generates commit-cached architectural summaries.'
    },
    {
      icon: Bug,
      tag: 'ROOT CAUSE ANALYSIS',
      title: 'Stack Trace Bug Investigation',
      description:
        'Parses Java, Python, and JavaScript error traces, fetches exact line ranges from indexed chunks, and synthesizes concrete code fixes.'
    }
  ];

  return (
    <section id="features" className="py-16 md:py-24 bg-[#07090e] text-white">
      <div className="mx-auto max-w-6xl px-4 sm:px-6 space-y-12">
        {/* Section Title */}
        <div className="text-center max-w-2xl mx-auto space-y-3">
          <h2 className="text-xs font-mono font-bold uppercase tracking-wider text-indigo-400">
            SYSTEM ARCHITECTURE &amp; CAPABILITIES
          </h2>
          <p className="text-2xl sm:text-4xl font-extrabold tracking-tight text-white">
            Engineered for Grounded Code Understanding
          </p>
          <p className="text-xs sm:text-sm text-slate-400">
            Built from the ground up with strict code grounding, non-blocking asynchronous indexing, and deep Monaco IDE integration.
          </p>
        </div>

        {/* Feature Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {features.map((feat) => {
            const Icon = feat.icon;
            return (
              <div
                key={feat.title}
                className="glass-card rounded-2xl p-6 space-y-4 border border-white/[0.08] hover:border-indigo-500/40"
              >
                <div className="flex items-center justify-between">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
                    <Icon className="h-5 w-5" />
                  </div>
                  <span className="font-mono text-[10px] text-slate-400 tracking-wider">
                    {feat.tag}
                  </span>
                </div>
                <h3 className="text-base font-bold text-white">{feat.title}</h3>
                <p className="text-xs sm:text-sm text-slate-400 leading-relaxed">
                  {feat.description}
                </p>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
