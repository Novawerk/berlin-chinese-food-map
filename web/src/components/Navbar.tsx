"use client";

import Link from "next/link";
import { useTheme } from "next-themes";
import { useLanguage } from "@/lib/language";
import { Button } from "@/components/ui/button";
import { useEffect, useState } from "react";

export function Navbar() {
  const { lang, toggleLang, t } = useLanguage();
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  return (
    <nav className="sticky top-0 z-50 border-b border-border bg-background/80 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
        <Link href="/" className="flex items-center gap-2">
          <span className="text-xl">🍜</span>
          <span className="text-lg font-bold">
            {t("Berlin Food Map", "柏林中餐地图")}
          </span>
        </Link>

        <div className="flex items-center gap-1 sm:gap-2">
          <Link href="/">
            <Button variant="ghost" size="sm">
              {t("Home", "首页")}
            </Button>
          </Link>
          <a href="/#roadmap">
            <Button variant="ghost" size="sm">
              {t("Roadmap", "路线图")}
            </Button>
          </a>
          <Link href="/changelog">
            <Button variant="ghost" size="sm">
              {t("Changelog", "更新日志")}
            </Button>
          </Link>
          <a
            href="https://github.com/nickolasburr/berlin-chinese-food-map"
            target="_blank"
            rel="noopener noreferrer"
          >
            <Button variant="ghost" size="sm">
              GitHub
            </Button>
          </a>

          <div className="mx-1 h-5 w-px bg-border" />

          <Button variant="outline" size="sm" onClick={toggleLang}>
            {lang === "en" ? "中文" : "EN"}
          </Button>

          {mounted && (
            <Button
              variant="outline"
              size="icon-sm"
              onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
              aria-label="Toggle theme"
            >
              {theme === "dark" ? (
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <circle cx="12" cy="12" r="4" />
                  <path d="M12 2v2" />
                  <path d="M12 20v2" />
                  <path d="m4.93 4.93 1.41 1.41" />
                  <path d="m17.66 17.66 1.41 1.41" />
                  <path d="M2 12h2" />
                  <path d="M20 12h2" />
                  <path d="m6.34 17.66-1.41 1.41" />
                  <path d="m19.07 4.93-1.41 1.41" />
                </svg>
              ) : (
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z" />
                </svg>
              )}
            </Button>
          )}
        </div>
      </div>
    </nav>
  );
}
