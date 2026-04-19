"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { doc, getDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useLanguage } from "@/lib/language";
import { Button } from "@/components/ui/button";
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

export default function ChangelogDetailPage() {
  const params = useParams();
  const id = params.id as string;
  const { t, tObj } = useLanguage();
  const [entry, setEntry] = useState<ChangelogEntry | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    async function fetchEntry() {
      try {
        const docRef = doc(db, "changelog", id);
        const docSnap = await getDoc(docRef);
        if (docSnap.exists()) {
          setEntry({ id: docSnap.id, ...docSnap.data() } as ChangelogEntry);
        } else {
          setNotFound(true);
        }
      } catch (error) {
        console.error("Failed to fetch changelog entry:", error);
        setNotFound(true);
      } finally {
        setLoading(false);
      }
    }
    if (id) {
      fetchEntry();
    }
  }, [id]);

  if (loading) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6 sm:py-24">
        <div className="animate-pulse">
          <div className="h-4 w-24 rounded bg-muted" />
          <div className="mt-6 h-8 w-96 rounded bg-muted" />
          <div className="mt-4 h-4 w-48 rounded bg-muted" />
          <div className="mt-8 space-y-3">
            <div className="h-4 w-full rounded bg-muted" />
            <div className="h-4 w-full rounded bg-muted" />
            <div className="h-4 w-3/4 rounded bg-muted" />
          </div>
        </div>
      </div>
    );
  }

  if (notFound || !entry) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-16 text-center sm:px-6 sm:py-24">
        <h1 className="text-2xl font-bold">
          {t("Entry not found", "未找到条目")}
        </h1>
        <p className="mt-3 text-muted-foreground">
          {t(
            "The changelog entry you're looking for doesn't exist.",
            "您查找的更新日志条目不存在。"
          )}
        </p>
        <Link href="/changelog" className="mt-6 inline-block">
          <Button variant="outline">
            &larr; {t("Back to Changelog", "返回更新日志")}
          </Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6 sm:py-24">
      <Link href="/changelog">
        <Button variant="ghost" size="sm" className="mb-8">
          &larr; {t("Back to Changelog", "返回更新日志")}
        </Button>
      </Link>

      <div className="flex flex-wrap items-center gap-3">
        <span className="rounded-md bg-primary/10 px-2.5 py-0.5 text-sm font-medium text-primary">
          v{entry.version}
        </span>
        <span className="text-sm text-muted-foreground">{entry.date}</span>
        {entry.tags?.map((tag) => (
          <span
            key={tag}
            className="rounded-md bg-muted px-2 py-0.5 text-xs text-muted-foreground"
          >
            {tag}
          </span>
        ))}
      </div>

      <h1 className="mt-4 text-3xl font-bold tracking-tight sm:text-4xl">
        {tObj(entry.title)}
      </h1>

      <article className="mt-8 prose prose-neutral dark:prose-invert max-w-none prose-headings:font-semibold prose-a:text-primary prose-code:rounded prose-code:bg-muted prose-code:px-1.5 prose-code:py-0.5 prose-code:text-sm prose-pre:bg-muted prose-pre:border prose-pre:border-border">
        <ReactMarkdown>{tObj(entry.content)}</ReactMarkdown>
      </article>
    </div>
  );
}
