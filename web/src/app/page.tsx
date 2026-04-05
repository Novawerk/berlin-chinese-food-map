"use client";

import { useLanguage } from "@/lib/language";
import { Button } from "@/components/ui/button";

const features = [
  {
    icon: "🗺️",
    title: { en: "Map Mode", zh: "地图模式" },
    desc: {
      en: "Explore Chinese restaurants across Berlin with an interactive Google Map. Pins mark every restaurant, tap to preview details instantly.",
      zh: "通过交互式 Google 地图探索柏林各地的中餐厅。标记标注每家餐厅，点击即可预览详情。",
    },
  },
  {
    icon: "📋",
    title: { en: "List Mode", zh: "列表模式" },
    desc: {
      en: "Browse all restaurants in a clean, scrollable list. Sort by visit count or name, filter by cuisine type with quick-tap chips.",
      zh: "在简洁的可滚动列表中浏览所有餐厅。按到访次数或名称排序，通过标签快速筛选菜系。",
    },
  },
  {
    icon: "🔍",
    title: { en: "Smart Search", zh: "智能搜索" },
    desc: {
      en: "Search in Chinese, English, or German. Stack filters by cuisine type and district to find exactly what you're craving.",
      zh: "支持中文、英文、德文搜索。叠加菜系和城区筛选，精准找到你想吃的。",
    },
  },
  {
    icon: "✅",
    title: { en: "Visit Tracking", zh: "到访记录" },
    desc: {
      en: "Mark restaurants as visited and save your favorites. Build your personal Chinese food diary across Berlin.",
      zh: "标记已去过的餐厅，收藏你的最爱。建立属于你的柏林中餐探索日记。",
    },
  },
  {
    icon: "🌙",
    title: { en: "Dark Mode", zh: "深色模式" },
    desc: {
      en: "Switch between light, dark, or system-default themes. Material Design 3 Expressive theming throughout.",
      zh: "在浅色、深色或跟随系统之间切换。全局采用 Material Design 3 Expressive 主题。",
    },
  },
  {
    icon: "🌐",
    title: { en: "Bilingual", zh: "双语支持" },
    desc: {
      en: "Full English and Chinese UI. Restaurant names also support German. Switch language anytime in settings.",
      zh: "完整中英双语界面。餐厅名称还支持德文。随时在设置中切换语言。",
    },
  },
];

const roadmapWeeks = [
  {
    week: 1,
    title: {
      en: "Data & Design",
      zh: "数据与设计",
    },
    desc: {
      en: "Restaurant data collection and community sourcing. UI/UX design finalization. Core data pipeline setup with Firebase sync.",
      zh: "餐厅数据整理与社区征集。UI/UX 设计定稿。搭建核心数据管线并完成 Firebase 同步。",
    },
    tags: {
      en: ["Data Collection", "Community Sourcing", "UI Design"],
      zh: ["数据整理", "社区征集", "UI 设计"],
    },
    status: "done" as const,
  },
  {
    week: 2,
    title: {
      en: "MVP & Platform",
      zh: "MVP 与平台搭建",
    },
    desc: {
      en: "Mobile app MVP running on both Android and iOS. Admin panel and landing page fully completed. All backend services operational.",
      zh: "移动应用 MVP 在 Android 和 iOS 双端跑通。管理后台和官网着陆页全部完成。所有后端服务上线运行。",
    },
    tags: {
      en: ["Mobile MVP", "Admin Panel", "Landing Page"],
      zh: ["移动端 MVP", "管理后台", "着陆页"],
    },
    status: "current" as const,
  },
  {
    week: 3,
    title: {
      en: "Beta & Marketing",
      zh: "内测与市场推广",
    },
    desc: {
      en: "Internal beta testing and community feedback. Marketing materials, App Store assets, and promotional content preparation.",
      zh: "内测与社区反馈收集。制作市场推广材料、应用商店素材和宣传内容。",
    },
    tags: {
      en: ["Beta Testing", "Feedback", "Marketing Assets"],
      zh: ["内测", "反馈收集", "市场素材"],
    },
    status: "upcoming" as const,
  },
  {
    week: 4,
    title: {
      en: "Launch",
      zh: "正式上线",
    },
    desc: {
      en: "Official launch on both App Store and Google Play. Production deployment, monitoring, and community onboarding.",
      zh: "App Store 和 Google Play 双端正式上线。生产环境部署、监控和社区运营启动。",
    },
    tags: {
      en: ["App Store", "Google Play", "Go Live"],
      zh: ["App Store", "Google Play", "正式发布"],
    },
    status: "upcoming" as const,
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
              "Discover the best Chinese restaurants in Berlin. Community-curated, bilingual, privacy-first. Available on Android and iOS.",
              "发现柏林最好的中餐厅。社区策划、双语支持、隐私优先。支持 Android 和 iOS。"
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

      {/* Roadmap */}
      <section id="roadmap" className="border-t border-border">
        <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6 sm:py-28">
          <div className="text-center">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
              {t("Roadmap", "开发路线图")}
            </h2>
            <p className="mt-3 text-lg text-muted-foreground">
              {t(
                "Phase 1 — From idea to launch in 4 weeks",
                "第一阶段 — 4 周从创意到上线"
              )}
            </p>
          </div>

          <div className="relative mt-14">
            {/* Timeline line */}
            <div className="absolute left-[23px] top-0 hidden h-full w-px bg-border sm:block" />

            <div className="space-y-8">
              {roadmapWeeks.map((item) => (
                <div key={item.week} className="relative flex gap-6">
                  {/* Timeline dot */}
                  <div className="relative z-10 hidden shrink-0 sm:block">
                    <div
                      className={`flex h-12 w-12 items-center justify-center rounded-full border-2 text-sm font-bold ${
                        item.status === "done"
                          ? "border-green-500 bg-green-500/10 text-green-600 dark:text-green-400"
                          : item.status === "current"
                            ? "border-primary bg-primary/10 text-primary"
                            : "border-border bg-muted text-muted-foreground"
                      }`}
                    >
                      {item.status === "done" ? (
                        <svg
                          xmlns="http://www.w3.org/2000/svg"
                          width="20"
                          height="20"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2.5"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        >
                          <polyline points="20 6 9 17 4 12" />
                        </svg>
                      ) : (
                        `W${item.week}`
                      )}
                    </div>
                  </div>

                  {/* Card */}
                  <div
                    className={`flex-1 rounded-xl border p-6 ${
                      item.status === "current"
                        ? "border-primary/50 bg-primary/5"
                        : item.status === "done"
                          ? "border-green-500/30 bg-green-500/5"
                          : "border-border bg-card"
                    }`}
                  >
                    <div className="flex flex-wrap items-center gap-3">
                      <span className="inline-flex items-center rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground sm:hidden">
                        {t(`Week ${item.week}`, `第 ${item.week} 周`)}
                      </span>
                      <h3 className="text-lg font-semibold">
                        {t(`Week ${item.week}`, `第 ${item.week} 周`)}
                        <span className="mx-2 text-muted-foreground">—</span>
                        {tObj(item.title)}
                      </h3>
                      {item.status === "done" && (
                        <span className="inline-flex items-center rounded-full bg-green-500/10 px-2.5 py-0.5 text-xs font-medium text-green-600 dark:text-green-400">
                          {t("Completed", "已完成")}
                        </span>
                      )}
                      {item.status === "current" && (
                        <span className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
                          {t("In Progress", "进行中")}
                        </span>
                      )}
                      {item.status === "upcoming" && (
                        <span className="inline-flex items-center rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground">
                          {t("Upcoming", "即将开始")}
                        </span>
                      )}
                    </div>
                    <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
                      {tObj(item.desc)}
                    </p>
                    <div className="mt-4 flex flex-wrap gap-2">
                      {(tObj(item.tags) as string[]).map((tag: string) => (
                        <span
                          key={tag}
                          className="inline-flex items-center rounded-md border border-border bg-muted/50 px-2 py-0.5 text-xs text-muted-foreground"
                        >
                          {tag}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Download */}
      <section id="download" className="border-t border-border bg-muted/20">
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
