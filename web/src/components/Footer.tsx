"use client";

import Link from "next/link";
import { useLanguage } from "@/lib/language";

export function Footer() {
  const { t } = useLanguage();

  return (
    <footer className="mt-auto border-t border-border bg-muted/30">
      <div className="mx-auto flex max-w-6xl flex-col items-center gap-4 px-4 py-8 sm:flex-row sm:justify-between sm:px-6">
        <p className="text-sm text-muted-foreground">
          &copy; {new Date().getFullYear()} Novawerk.{" "}
          {t("All rights reserved.", "保留所有权利。")}
        </p>

        <div className="flex items-center gap-4 text-sm text-muted-foreground">
          <Link
            href="/changelog"
            className="transition-colors hover:text-foreground"
          >
            {t("Changelog", "更新日志")}
          </Link>
          <a
            href="https://github.com/nickolasburr/berlin-chinese-food-map"
            target="_blank"
            rel="noopener noreferrer"
            className="transition-colors hover:text-foreground"
          >
            GitHub
          </a>
          <a
            href="https://berlinfoodmap.novawerk.io/admin"
            target="_blank"
            rel="noopener noreferrer"
            className="transition-colors hover:text-foreground"
          >
            Admin
          </a>
          <Link
            href="/privacy"
            className="transition-colors hover:text-foreground"
          >
            {t("Privacy", "隐私")}
          </Link>
        </div>
      </div>
    </footer>
  );
}
