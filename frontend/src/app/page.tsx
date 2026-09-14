import React from 'react';
import { LandingNavbar } from '@/components/landing/LandingNavbar';
import { HeroSection } from '@/components/landing/HeroSection';
import { DemoShowcaseSection } from '@/components/landing/DemoShowcaseSection';
import { FeaturesSection } from '@/components/landing/FeaturesSection';
import { LandingFooter } from '@/components/landing/LandingFooter';

export default function HomePage() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 flex flex-col justify-between">
      <div>
        <LandingNavbar />
        <main>
          <HeroSection />
          <DemoShowcaseSection />
          <FeaturesSection />
        </main>
      </div>
      <LandingFooter />
    </div>
  );
}
