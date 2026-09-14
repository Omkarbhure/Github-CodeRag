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
  CheckCircle2
} from 'lucide-react';

export function FeaturesSection() {
  const features = [
    {
      icon: Sparkles,
      iconColor: 'text-indigo-600 bg-indigo-50 border-indigo-100',
      title: 'Hybrid Semantic & Keyword Search',
      description:
        'Combines Qdrant 768-dim vector embeddings with PostgreSQL full-text keyword search via min-max weighted score fusion for optimal retrieval.'
    },
    {
      icon: FileCode2,
      iconColor: 'text-purple-600 bg-purple-50 border-purple-100',
      title: 'Grounded Chat & Inline Citations',
      description:
        'Every fact, class, or logic explanation includes strict inline citations [filePath:startLine-endLine] deep-linked into the Monaco Editor.'
    },
    {
      icon: Compass,
      iconColor: 'text-blue-600 bg-blue-50 border-blue-100',
      title: 'Automated Architecture Overview',
      description:
        'Representative sampling across build manifests, documentation, entry points, and directory hierarchies generates commit-cached architectural summaries.'
    },
    {
      icon: Bug,
      iconColor: 'text-rose-600 bg-rose-50 border-rose-100',
      title: 'Stack Trace Bug Investigation',
      description:
        'Parses Java, Python, and JavaScript error traces, fetches exact line ranges from indexed chunks, and proposes concrete code fixes.'
    }
  ];

  return (
    <section id="features" className="py-16 md:py-20 bg-slate-50 border-t border-slate-200/80">
      <div className="mx-auto max-w-6xl px-4 sm:px-6 space-y-12">
        {/* Section Title */}
        <div className="text-center max-w-2xl mx-auto space-y-3">
          <h2 className="text-xs font-bold uppercase tracking-wider text-indigo-600">
            Core Capabilities
          </h2>
          <p className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
            Engineered for Grounded Code Understanding
          </p>
          <p className="text-xs sm:text-sm text-slate-600">
            Built from the ground up with strict code grounding, non-blocking indexing, and deep IDE integration.
          </p>
        </div>

        {/* Feature Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {features.map((feat) => {
            const Icon = feat.icon;
            return (
              <div
                key={feat.title}
                className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm hover:shadow-md transition space-y-3"
              >
                <div className={`flex h-10 w-10 items-center justify-center rounded-xl border ${feat.iconColor}`}>
                  <Icon className="h-5 w-5" />
                </div>
                <h3 className="text-base font-bold text-slate-900">{feat.title}</h3>
                <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">
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
