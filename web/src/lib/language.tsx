"use client";

import { createContext, useContext, useState, useCallback, type ReactNode } from "react";

type Language = "en" | "zh";

interface LanguageContextType {
  lang: Language;
  toggleLang: () => void;
  t: (en: string, zh: string) => string;
  tObj: (obj: { en: string; zh: string }) => string;
}

const LanguageContext = createContext<LanguageContextType | null>(null);

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [lang, setLang] = useState<Language>("en");

  const toggleLang = useCallback(() => {
    setLang((prev) => (prev === "en" ? "zh" : "en"));
  }, []);

  const t = useCallback(
    (en: string, zh: string) => (lang === "en" ? en : zh),
    [lang]
  );

  const tObj = useCallback(
    (obj: { en: string; zh: string }) => obj[lang],
    [lang]
  );

  return (
    <LanguageContext.Provider value={{ lang, toggleLang, t, tObj }}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage() {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error("useLanguage must be used within a LanguageProvider");
  }
  return context;
}
