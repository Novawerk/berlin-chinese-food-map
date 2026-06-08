"use client";

import { useEffect, useRef, useState } from "react";

type Props = {
  to: number;
  durationMs?: number;
  suffix?: string;
  prefix?: string;
  formatter?: (n: number) => string;
};

const easeOutQuart = (t: number) => 1 - Math.pow(1 - t, 4);

function isInViewport(el: Element, marginBottom = 80) {
  const r = el.getBoundingClientRect();
  const vh = window.innerHeight || document.documentElement.clientHeight;
  return r.top < vh - marginBottom && r.bottom > 0;
}

export function CountUp({
  to,
  durationMs = 1600,
  suffix = "",
  prefix = "",
  formatter,
}: Props) {
  const ref = useRef<HTMLSpanElement>(null);
  const [display, setDisplay] = useState("0");
  const startedRef = useRef(false);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const el = ref.current;
    if (!el) return;

    const start = () => {
      if (startedRef.current) return;
      startedRef.current = true;
      const t0 = performance.now();
      const interval = setInterval(() => {
        const p = Math.min(1, (performance.now() - t0) / durationMs);
        const v = Math.round(easeOutQuart(p) * to);
        setDisplay(formatter ? formatter(v) : String(v));
        if (p >= 1) clearInterval(interval);
      }, 24);
    };

    if (isInViewport(el)) {
      start();
      return;
    }

    if (!("IntersectionObserver" in window)) {
      start();
      return;
    }

    const io = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            start();
            io.disconnect();
            window.removeEventListener("scroll", onScroll);
            break;
          }
        }
      },
      { rootMargin: "0px 0px -80px 0px" }
    );
    io.observe(el);

    const onScroll = () => {
      if (ref.current && isInViewport(ref.current)) {
        start();
        io.disconnect();
        window.removeEventListener("scroll", onScroll);
      }
    };
    window.addEventListener("scroll", onScroll, { passive: true });

    return () => {
      io.disconnect();
      window.removeEventListener("scroll", onScroll);
    };
  }, [to, durationMs, formatter]);

  return (
    <span ref={ref}>
      {prefix}
      {display}
      {suffix}
    </span>
  );
}
