"use client";

import type { ReactNode } from "react";
import { ThemeProvider } from "@/lib/theme-provider";
import { LanguageProvider } from "@/lib/language";

export function Providers({ children }: { children: ReactNode }) {
  return (
    <ThemeProvider>
      <LanguageProvider>{children}</LanguageProvider>
    </ThemeProvider>
  );
}
