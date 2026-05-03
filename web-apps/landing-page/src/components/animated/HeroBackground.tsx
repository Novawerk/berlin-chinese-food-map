"use client";

import Image from "next/image";

export function HeroBackground() {
  return (
    <div className="pointer-events-none absolute inset-0 -z-10 overflow-hidden grain">
      {/* Brand-red blob */}
      <div className="animate-blob-a absolute -top-40 -left-32 h-[36rem] w-[36rem] rounded-full bg-brand/25 blur-3xl dark:bg-brand/30" />
      {/* Cream blob */}
      <div className="animate-blob-b absolute -right-40 top-32 h-[30rem] w-[30rem] rounded-full bg-amber-200/40 blur-3xl dark:bg-amber-500/15" />
      {/* Soft top vignette */}
      <div className="absolute inset-0 bg-gradient-to-b from-background/40 via-transparent to-background" />

      {/* Floating wok illustrations */}
      <div className="animate-wok-float absolute -left-10 top-1/3 hidden opacity-30 sm:block">
        <Image
          src="/logo.png"
          alt=""
          width={220}
          height={220}
          aria-hidden
          className="h-44 w-44 rounded-3xl"
        />
      </div>
      <div className="animate-wok-float-rev absolute -right-12 bottom-12 hidden opacity-25 lg:block">
        <Image
          src="/logo.png"
          alt=""
          width={300}
          height={300}
          aria-hidden
          className="h-56 w-56 rounded-3xl"
        />
      </div>
    </div>
  );
}
