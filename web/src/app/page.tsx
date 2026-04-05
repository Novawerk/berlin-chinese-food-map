"use client";

import { useLanguage } from "@/lib/language";
import { Button } from "@/components/ui/button";

const features = [
  {
    icon: "🗺️",
    title: { en: "Map Mode", zh: "地图模式" },
    desc: {
      en: "Explore Chinese restaurants across Berlin with an interactive map. Pins are color-coded by cuisine type.",
      zh: "通过交互式地图探索柏林各地的中餐厅。标记按菜系类型颜色编码。",
    },
  },
  {
    icon: "📋",
    title: { en: "List Mode", zh: "列表模式" },
    desc: {
      en: "Browse all restaurants in a clean, sortable list. See ratings, distance, and opening hours at a glance.",
      zh: "在简洁、可排序的列表中浏览所有餐厅。一目了然地查看评分、距离和营业时间。",
    },
  },
  {
    icon: "🔍",
    title: { en: "Smart Filters", zh: "智能筛选" },
    desc: {
      en: "Filter by cuisine type, price range, distance, and more. Find exactly what you're craving.",
      zh: "按菜系类型、价格范围、距离等筛选。找到你想吃的。",
    },
  },
  {
    icon: "✅",
    title: { en: "Visit Tracking", zh: "到访记录" },
    desc: {
      en: "Mark restaurants as visited, save your favorites, and build your personal food diary.",
      zh: "标记已去过的餐厅，收藏你的最爱，建立你的美食日记。",
    },
  },
  {
    icon: "📱",
    title: { en: "Offline Support", zh: "离线支持" },
    desc: {
      en: "Access saved restaurants and favorites even without an internet connection.",
      zh: "即使没有网络连接，也可以访问已保存的餐厅和收藏。",
    },
  },
  {
    icon: "🌐",
    title: { en: "Bilingual", zh: "双语支持" },
    desc: {
      en: "Full support for both English and Chinese. Search restaurant names in either language.",
      zh: "完整支持中英双语。可用任意一种语言搜索餐厅名称。",
    },
  },
];

export default function HomePage() {
  const { t, tObj } = useLanguage();

  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 -z-10 bg-gradient-to-b from-primary/5 via-transparent to-transparent" />
        <div className="mx-auto max-w-6xl px-4 py-24 text-center sm:px-6 sm:py-32 lg:py-40">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-border bg-muted/50 px-4 py-1.5 text-sm text-muted-foreground">
            <span>🍜</span>
            <span>{t("Community-driven food guide", "社区驱动的美食指南")}</span>
          </div>

          <h1 className="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
            Berlin Chinese Food Map
          </h1>
          <p className="mt-2 text-2xl font-medium text-muted-foreground sm:text-3xl lg:text-4xl">
            柏林中餐地图
          </p>

          <p className="mx-auto mt-6 max-w-2xl text-lg text-muted-foreground sm:text-xl">
            {t(
              "Discover the best Chinese restaurants in Berlin. Community-curated, always up to date, beautifully designed.",
              "发现柏林最好的中餐厅。由社区策划，始终保持更新，设计精美。"
            )}
          </p>

          <div className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row">
            <a href="#download">
              <Button size="lg" className="h-12 px-8 text-base">
                {t("Download App", "下载应用")}
              </Button>
            </a>
            <a href="#features">
              <Button variant="outline" size="lg" className="h-12 px-8 text-base">
                {t("Learn More", "了解更多")}
              </Button>
            </a>
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="border-t border-border bg-muted/20">
        <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6 sm:py-28">
          <div className="text-center">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
              {t("Features", "功能特色")}
            </h2>
            <p className="mt-3 text-lg text-muted-foreground">
              {t(
                "Everything you need to explore Chinese food in Berlin",
                "探索柏林中餐所需的一切"
              )}
            </p>
          </div>

          <div className="mt-14 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {features.map((feature) => (
              <div
                key={feature.title.en}
                className="group rounded-xl border border-border bg-card p-6 transition-colors hover:bg-accent/50"
              >
                <div className="mb-4 text-3xl">{feature.icon}</div>
                <h3 className="text-lg font-semibold">{tObj(feature.title)}</h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                  {tObj(feature.desc)}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Download */}
      <section id="download" className="border-t border-border">
        <div className="mx-auto max-w-6xl px-4 py-20 text-center sm:px-6 sm:py-28">
          <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
            {t("Get the App", "获取应用")}
          </h2>
          <p className="mt-3 text-lg text-muted-foreground">
            {t(
              "Available soon on iOS and Android",
              "即将登陆 iOS 和 Android"
            )}
          </p>

          <div className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row">
            <a
              href="#"
              className="inline-flex h-14 items-center gap-3 rounded-xl border border-border bg-card px-6 transition-colors hover:bg-accent/50"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="28"
                height="28"
                viewBox="0 0 24 24"
                fill="currentColor"
              >
                <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z" />
              </svg>
              <div className="text-left">
                <div className="text-xs text-muted-foreground">
                  {t("Download on the", "下载于")}
                </div>
                <div className="text-sm font-semibold">App Store</div>
              </div>
            </a>

            <a
              href="#"
              className="inline-flex h-14 items-center gap-3 rounded-xl border border-border bg-card px-6 transition-colors hover:bg-accent/50"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="28"
                height="28"
                viewBox="0 0 24 24"
                fill="currentColor"
              >
                <path d="M3.609 1.814L13.792 12 3.61 22.186a.996.996 0 0 1-.61-.92V2.734a1 1 0 0 1 .609-.92zm10.89 10.893l2.302 2.302-10.937 6.333 8.635-8.635zm3.199-3.199l2.807 1.626a1 1 0 0 1 0 1.732l-2.808 1.626L15.206 12l2.492-2.492zM5.864 2.658L16.802 8.99l-2.303 2.303-8.635-8.635z" />
              </svg>
              <div className="text-left">
                <div className="text-xs text-muted-foreground">
                  {t("Get it on", "下载于")}
                </div>
                <div className="text-sm font-semibold">Google Play</div>
              </div>
            </a>
          </div>

          <p className="mt-6 text-sm text-muted-foreground">
            {t(
              "The app is currently in development. Stay tuned!",
              "应用正在开发中，敬请期待！"
            )}
          </p>
        </div>
      </section>
    </div>
  );
}
