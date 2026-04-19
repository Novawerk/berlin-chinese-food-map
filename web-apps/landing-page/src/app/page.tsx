"use client";

import { useLanguage } from "@/lib/language";
import { Button } from "@/components/ui/button";

const features = [
  {
    icon: "🗺️",
    title: { en: "Interactive Map", zh: "交互式地图" },
    desc: {
      en: "Explore Chinese restaurants across all Berlin districts with cuisine-colored pins. Tap to preview, tap again for full details.",
      zh: "通过按菜系着色的标记探索柏林各区中餐厅。点击预览，再点进入详情。",
    },
  },
  {
    icon: "🔍",
    title: { en: "Trilingual Search", zh: "三语搜索" },
    desc: {
      en: "Search in Chinese, English, or German. Stack filters by cuisine type and district to find exactly what you're craving.",
      zh: "支持中文、英文、德文搜索。叠加菜系和城区筛选，精准找到你想吃的。",
    },
  },
  {
    icon: "📋",
    title: { en: "Restaurant Profiles", zh: "餐厅画像" },
    desc: {
      en: "Detailed restaurant profiles with photos, contact info, price range, and multilingual descriptions.",
      zh: "详尽的餐厅画像，包含照片、联系方式、价格区间及多语言描述。",
    },
  },
  {
    icon: "❤️",
    title: { en: "Favorites & Visits", zh: "收藏与足迹" },
    desc: {
      en: "Save favorites and mark visited restaurants locally. Privacy-first — no personal data collected.",
      zh: "本地收藏和标记已去过的餐厅。隐私优先——不收集任何个人数据。",
    },
  },
  {
    icon: "🌐",
    title: { en: "Bilingual UI", zh: "双语界面" },
    desc: {
      en: "Full English and Chinese interface. Restaurant names also support German. Switch language anytime.",
      zh: "完整中英双语界面。餐厅名称还支持德文。随时切换语言。",
    },
  },
  {
    icon: "🌙",
    title: { en: "Dark Mode", zh: "深色模式" },
    desc: {
      en: "Light, dark, or system-default themes. Material Design 3 Expressive theming throughout.",
      zh: "浅色、深色或跟随系统主题。全局采用 Material Design 3 Expressive 主题。",
    },
  },
];

const roadmapPhases = [
  {
    phase: 1,
    title: { en: "Kick-off", zh: "项目启动" },
    desc: {
      en: "Data handoff from physical map to structured digital schema. Visual direction alignment, cuisine category standards, and interaction design finalization.",
      zh: "将纸质地图数据转化为结构化数字 Schema。对齐视觉方向、统一菜系分类标准、敲定交互逻辑。",
    },
    tags: {
      en: ["Data Handoff", "Visual Direction", "Schema Design"],
      zh: ["数据交接", "视觉方向", "Schema 设计"],
    },
    status: "current" as const as "done" | "current" | "upcoming",
  },
  {
    phase: 2,
    title: { en: "MVP Development", zh: "MVP 核心开发" },
    subtitle: { en: "Week 1", zh: "第 1 周" },
    desc: {
      en: "Polished map UI with district filtering and cuisine-colored pins. Restaurant profiles, trilingual search, privacy-first favorites & visit tracking. Internal beta via TestFlight and APK.",
      zh: "精致地图 UI，支持按行政区筛选及菜系色彩标注。餐厅画像、三语搜索、隐私优先的收藏与足迹。通过 TestFlight 和 APK 进行内部测试。",
    },
    tags: {
      en: ["Map UI", "Search", "Favorites", "Internal Beta"],
      zh: ["地图 UI", "搜索", "收藏", "内部测试"],
    },
    status: "upcoming" as const,
  },
  {
    phase: 3,
    title: { en: "Beta & Launch Prep", zh: "内测与发布筹备" },
    subtitle: { en: "Week 2", zh: "第 2 周" },
    desc: {
      en: "Community beta via WeChat and Xiaohongshu channels. App Store Optimization (ASO) with multilingual descriptions, screenshots, and keywords. GTM coordination with partners.",
      zh: "通过微信、小红书等社区渠道进行封闭测试。准备应用商店多语言描述、截图及关键词 (ASO)。与合作伙伴协同市场预热 (GTM)。",
    },
    tags: {
      en: ["Community Beta", "ASO", "GTM"],
      zh: ["社区内测", "ASO 优化", "市场预热"],
    },
    status: "upcoming" as const,
  },
  {
    phase: 4,
    title: { en: "Launch & Growth", zh: "正式发布与增长" },
    desc: {
      en: "Official submission to App Store and Google Play. Continuous iteration based on user feedback. Community content submission (UGC) pipeline and curated collections.",
      zh: "正式提交 App Store 和 Play Store。根据用户反馈持续迭代。建立社区内容提交 (UGC) 机制，推出编辑精选及主题美食路线。",
    },
    tags: {
      en: ["App Store", "Google Play", "UGC", "Curated Content"],
      zh: ["App Store", "Google Play", "社区提交", "精品策划"],
    },
    status: "upcoming" as const,
  },
];

const pocItems = [
  {
    title: { en: "Mobile App", zh: "移动端应用" },
    desc: {
      en: "Cross-platform (iOS/Android) map & search, custom markers, Google Maps compliance",
      zh: "跨平台（iOS/Android）地图与搜索、自定义 Marker、谷歌地图合规性检查",
    },
  },
  {
    title: { en: "Data Pipeline", zh: "数据流水线" },
    desc: {
      en: "YAML → Firestore automated sync & deployment via GitHub",
      zh: "通过 GitHub 实现 YAML → Firestore 的自动同步与部署",
    },
  },
  {
    title: { en: "Landing Page", zh: "项目落地页" },
    desc: {
      en: "Bilingual UI framework with feature showcase",
      zh: "具备双语 UI 框架及功能展示",
    },
  },
  {
    title: { en: "Admin Panel", zh: "管理后台" },
    desc: {
      en: "Full CRUD management for restaurant data",
      zh: "餐厅数据增删改查 (CRUD) 全套管理功能",
    },
  },
];

export default function HomePage() {
  const { lang, t, tObj } = useLanguage();

  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 -z-10 bg-gradient-to-b from-primary/5 via-transparent to-transparent" />
        <div className="mx-auto max-w-6xl px-4 py-24 text-center sm:px-6 sm:py-32 lg:py-40">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-border bg-muted/50 px-4 py-1.5 text-sm text-muted-foreground">
            <span>🍜</span>
            <span>{t("Community-driven digital food guide", "社区驱动的数字美食指南")}</span>
          </div>

          <h1 className="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
            Berlin Chinese Food Map
          </h1>
          <p className="mt-2 text-2xl font-medium text-muted-foreground sm:text-3xl lg:text-4xl">
            柏林中餐地图
          </p>

          <p className="mx-auto mt-6 max-w-2xl text-lg text-muted-foreground sm:text-xl">
            {t(
              "A non-profit, community-driven guide to Chinese restaurants in Berlin. Bilingual, privacy-first, open source. Available on Android and iOS.",
              "非营利的社区驱动柏林中餐馆指南。双语支持、隐私优先、完全开源。支持 Android 和 iOS。"
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

      {/* POC Status */}
      <section className="border-t border-border bg-muted/20">
        <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6 sm:py-28">
          <div className="text-center">
            <div className="mb-4 inline-flex items-center gap-2 rounded-full bg-green-500/10 px-4 py-1.5 text-sm font-medium text-green-600 dark:text-green-400">
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
              >
                <polyline points="20 6 9 17 4 12" />
              </svg>
              <span>{t("POC Complete", "POC 已完成")}</span>
            </div>
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
              {t("Technical Foundation Validated", "技术架构已就绪")}
            </h2>
            <p className="mt-3 text-lg text-muted-foreground">
              {t(
                "All four pillars have been validated on staging",
                "四个核心方向已在 Staging 环境全部通过验证"
              )}
            </p>
          </div>

          <div className="mt-14 grid gap-4 sm:grid-cols-2">
            {pocItems.map((item) => (
              <div
                key={item.title.en}
                className="flex items-start gap-4 rounded-xl border border-green-500/20 bg-green-500/5 p-5"
              >
                <div className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-green-500/20 text-green-600 dark:text-green-400">
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                </div>
                <div>
                  <h3 className="font-semibold">{tObj(item.title)}</h3>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {tObj(item.desc)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="border-t border-border">
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
      <section id="roadmap" className="border-t border-border bg-muted/20">
        <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6 sm:py-28">
          <div className="text-center">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
              {t("Roadmap", "实施路线图")}
            </h2>
            <p className="mt-3 text-lg text-muted-foreground">
              {t(
                "From POC to production — 4 phases to launch",
                "从 POC 到产品 — 4 个阶段走向上线"
              )}
            </p>
          </div>

          <div className="relative mt-14">
            {/* Timeline line */}
            <div className="absolute left-[23px] top-0 hidden h-full w-px bg-border sm:block" />

            <div className="space-y-8">
              {roadmapPhases.map((item) => (
                <div key={item.phase} className="relative flex gap-6">
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
                        `P${item.phase}`
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
                        {t(`Phase ${item.phase}`, `第 ${item.phase} 阶段`)}
                      </span>
                      <h3 className="text-lg font-semibold">
                        {t(`Phase ${item.phase}`, `第 ${item.phase} 阶段`)}
                        <span className="mx-2 text-muted-foreground">—</span>
                        {tObj(item.title)}
                        {item.subtitle && (
                          <span className="ml-2 text-sm font-normal text-muted-foreground">
                            ({tObj(item.subtitle)})
                          </span>
                        )}
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
                      {(lang === "zh" ? item.tags.zh : item.tags.en).map((tag: string) => (
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
