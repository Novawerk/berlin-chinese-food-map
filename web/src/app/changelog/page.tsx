"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { collection, query, where, orderBy, getDocs } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useLanguage } from "@/lib/language";
import ReactMarkdown from "react-markdown";

interface ChangelogEntry {
  id: string;
  title: { en: string; zh: string };
  content: { en: string; zh: string };
  version: string;
  date: string;
  tags: string[];
  published: boolean;
}

export default function ChangelogPage() {
  const { t, tObj } = useLanguage();
  const [entries, setEntries] = useState<ChangelogEntry[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchChangelog() {
      try {
        const q = query(
          collection(db, "changelog"),
          where("published", "==", true),
          orderBy("date", "desc")
        );
        const snapshot = await getDocs(q);
        const data = snapshot.docs.map((doc) => ({
          id: doc.id,
          ...doc.data(),
        })) as ChangelogEntry[];
        setEntries(data);
      } catch (error) {
        console.error("Failed to fetch changelog:", error);
      } finally {
        setLoading(false);
      }
    }
    fetchChangelog();
  }, []);

  return (
    <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6 sm:py-24">
      <div className="mb-12">
        <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">
          {t("Changelog", "更新日志")}
        </h1>
        <p className="mt-3 text-lg text-muted-foreground">
          {t(
            "See what's new in Berlin Chinese Food Map",
            "查看柏林中餐地图的最新更新"
          )}
        </p>
      </div>

      {loading ? (
        <div className="space-y-6">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="animate-pulse rounded-xl border border-border p-6"
            >
              <div className="h-5 w-24 rounded bg-muted" />
              <div className="mt-3 h-6 w-64 rounded bg-muted" />
              <div className="mt-4 h-4 w-full rounded bg-muted" />
              <div className="mt-2 h-4 w-3/4 rounded bg-muted" />
            </div>
          ))}
        </div>
      ) : entries.length === 0 ? (
        <div className="rounded-xl border border-border p-12 text-center">
          <p className="text-muted-foreground">
            {t("No changelog entries yet.", "暂无更新日志。")}
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          {entries.map((entry) => (
            <Link
              key={entry.id}
              href={`/changelog/${entry.id}`}
              className="block rounded-xl border border-border bg-card p-6 transition-colors hover:bg-accent/50"
            >
              <div className="flex flex-wrap items-center gap-3">
                <span className="rounded-md bg-primary/10 px-2.5 py-0.5 text-sm font-medium text-primary">
                  v{entry.version}
                </span>
                <span className="text-sm text-muted-foreground">
                  {entry.date}
                </span>
                {entry.tags?.map((tag) => (
                  <span
                    key={tag}
                    className="rounded-md bg-muted px-2 py-0.5 text-xs text-muted-foreground"
                  >
                    {tag}
                  </span>
                ))}
              </div>

              <h2 className="mt-3 text-xl font-semibold">
                {tObj(entry.title)}
              </h2>

              <div className="mt-3 line-clamp-3 text-sm leading-relaxed text-muted-foreground prose prose-sm dark:prose-invert max-w-none">
                <ReactMarkdown>
                  {tObj(entry.content).slice(0, 300)}
                </ReactMarkdown>
              </div>

              <p className="mt-4 text-sm font-medium text-primary">
                {t("Read more", "阅读更多")} &rarr;
              </p>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
