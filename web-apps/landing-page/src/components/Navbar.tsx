"use client";

import Link from "next/link";
import Image from "next/image";
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
      <div className="mx-auto flex h-20 max-w-6xl items-center justify-between px-4 sm:px-6">
        <Link href="/" className="flex shrink-0 items-center gap-2.5">
          <Image
            src="/logo.png"
            alt=""
            width={40}
            height={40}
            priority
            className="h-9 w-9 rounded-lg sm:h-10 sm:w-10"
          />
          <span className="text-lg font-bold whitespace-nowrap sm:text-xl">
            <span className="hidden sm:inline">
              {t("Berlin Chinese Food", "柏林中餐地图")}
            </span>
            <span className="sm:hidden">
              {t("BCF", "中餐地图")}
            </span>
          </span>
        </Link>

        <div className="flex items-center gap-1 sm:gap-1.5">
          <a href="/#story" className="hidden md:inline-flex">
            <Button variant="ghost" className="text-base">
              {t("Story", "故事")}
            </Button>
          </a>
          <a href="/#features" className="hidden md:inline-flex">
            <Button variant="ghost" className="text-base">
              {t("Features", "功能")}
            </Button>
          </a>
          <a href="/#roadmap" className="hidden md:inline-flex">
            <Button variant="ghost" className="text-base">
              {t("Roadmap", "路线图")}
            </Button>
          </a>
          <a href="/#contribute" className="hidden md:inline-flex">
            <Button variant="ghost" className="text-base">
              {t("Contribute", "贡献")}
            </Button>
          </a>
          <a
            href="https://github.com/Novawerk/berlin-chinese-food-map/issues/new?template=restaurant-submission.yml"
            target="_blank"
            rel="noopener noreferrer"
          >
            <Button
              variant="default"
              className="h-11 gap-1.5 rounded-full px-4 text-base shadow-sm sm:px-5"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden
              >
                <path d="M12 5v14" />
                <path d="M5 12h14" />
              </svg>
              <span className="hidden sm:inline">
                {t("Submit a restaurant", "提交餐厅")}
              </span>
              <span className="sm:hidden">
                {t("Submit", "提交")}
              </span>
            </Button>
          </a>

          <div className="mx-1 h-5 w-px bg-border" />

          <Button variant="outline" className="text-base" onClick={toggleLang}>
            {lang === "en" ? "中文" : "EN"}
          </Button>

          {mounted && (
            <Button
              variant="outline"
              size="icon"
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
