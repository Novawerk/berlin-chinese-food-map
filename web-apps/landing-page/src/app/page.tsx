"use client";

import Image from "next/image";
import { useLanguage } from "@/lib/language";
import { Button } from "@/components/ui/button";
import { CountUp } from "@/components/animated/CountUp";
import { FadeIn } from "@/components/animated/FadeIn";
import { HeroBackground } from "@/components/animated/HeroBackground";
import { MarqueeTags } from "@/components/animated/MarqueeTags";

const SUBMIT_URL =
  "https://github.com/Novawerk/berlin-chinese-food-map/issues/new?template=restaurant-submission.yml";
const REPO_URL = "https://github.com/Novawerk/berlin-chinese-food-map";

const features = [
  {
    icon: "🗺️",
    title: { en: "Photo-rich Map", zh: "图文地图" },
    desc: {
      en: "Brand-red POI markers with cover photos, names, and tag pills. Google Maps on Android, MapKit on iOS — both feel native.",
      zh: "品牌红色 POI 标记，自带封面图、餐厅名和标签胶囊。Android 用 Google Maps、iOS 用 MapKit，两端都原生体验。",
    },
    span: 2,
    accent: "from-brand/20 to-brand/5",
  },
  {
    icon: "🔍",
    title: { en: "Inline Trilingual Search", zh: "原生三语搜索" },
    desc: {
      en: "Material 3 search bar lives on the map itself — no extra route. Search in Chinese, English, or German.",
      zh: "Material 3 搜索栏直接嵌在地图上，不再跳转独立页面。支持中文、英文、德文搜索。",
    },
    span: 1,
    accent: "from-amber-200/30 to-transparent",
  },
  {
    icon: "🏷️",
    title: { en: "22-Tag Taxonomy", zh: "22 项菜系标签" },
    desc: {
      en: "10 regional cuisines (川/粤/京…) + 12 formats (火锅/烤肉/小吃…). The filter sheet stacks Favourites · Editor's Picks · Open Now toggles with cuisine and format pickers — every selection is independent.",
      zh: "10 个地方菜系（川/粤/京…）+ 12 种业态（火锅/烤肉/小吃…）。筛选面板把 仅看收藏 · 仅看精选 · 仅看营业中 三个开关与菜系、业态选择器堆叠在一起，互不干扰。",
    },
    span: 1,
    accent: "from-emerald-200/30 to-transparent",
  },
  {
    icon: "🕒",
    title: { en: "Open-Now Signals", zh: "营业状态实时显示" },
    desc: {
      en: "Structured opening periods power a real-time \"open now\" badge plus a one-tap filter that hides venues currently closed. Closed restaurants render with a moon icon and faded text.",
      zh: "结构化营业时段驱动实时 \"营业中\" 标记，配合一键筛选隐藏当前已打烊的餐厅；歇业餐厅用月亮图标与淡色显示。",
    },
    span: 1,
    accent: "from-sky-200/30 to-transparent",
  },
  {
    icon: "📋",
    title: { en: "Expressive Detail Sheet", zh: "沉浸式详情卡片" },
    desc: {
      en: "Modal bottom sheet with a swipeable photo pager, opening hours, contact info, and sticky Call / Directions actions — the map stays in view.",
      zh: "底部弹出式卡片：可滑动照片浏览、营业时间、联系方式，底部置顶 \"电话 / 导航\" 按钮。打开详情时地图始终保留。",
    },
    span: 1,
    accent: "from-violet-200/30 to-transparent",
  },
  {
    icon: "🌐",
    title: { en: "Bilingual & Privacy-First", zh: "中英双语 · 隐私优先" },
    desc: {
      en: "Full Chinese / English UI, with German restaurant names. No ads, no third-party tracking SDKs. First-party Firebase Analytics + Crashlytics log only restaurant ids and short route strings — never names, search queries, or GPS coordinates.",
      zh: "完整中英双语界面，餐厅名兼顾德文。无广告、无第三方追踪 SDK；自家 Firebase Analytics + Crashlytics 仅记录餐厅 id 和短路由字符串——不记录餐厅名、搜索关键词、GPS 坐标。",
    },
    span: 2,
    accent: "from-brand/15 to-transparent",
  },
] as const;

const roadmapPhases = [
  {
    phase: 1,
    title: { en: "Kick-off", zh: "项目启动" },
    desc: {
      en: "Data handoff from the printed map to a structured digital schema. Visual direction, cuisine taxonomy, and interaction design all locked in.",
      zh: "将纸质地图数据转化为结构化数字 Schema。视觉方向、菜系分类、交互逻辑全部对齐到位。",
    },
    tags: {
      en: ["Data Handoff", "Visual Direction", "Schema Design"],
      zh: ["数据交接", "视觉方向", "Schema 设计"],
    },
    status: "done",
  },
  {
    phase: 2,
    title: { en: "MVP Development", zh: "MVP 核心开发" },
    desc: {
      en: "Photo-rich map with brand-red markers, inline trilingual search, 22-tag taxonomy and tabbed filters, expressive detail sheet, and structured open-now signals. 200+ restaurants synced via the YAML → Firestore pipeline.",
      zh: "图文地图、品牌红 POI、内嵌三语搜索、22 项菜系标签 + 分 Tab 筛选、沉浸式详情卡片、结构化营业时段。200+ 餐厅通过 YAML → Firestore 流水线同步上线。",
    },
    tags: {
      en: ["Map UI", "Tag Taxonomy", "Open-Now", "Detail Sheet"],
      zh: ["地图 UI", "标签体系", "营业状态", "详情卡片"],
    },
    status: "done",
  },
  {
    phase: 3,
    title: { en: "Beta & Launch Prep", zh: "内测与发布筹备" },
    desc: {
      en: "Release pipeline wired up — Play Store internal track + Xcode Cloud for TestFlight. Community beta via WeChat and Xiaohongshu, GitHub issue form for restaurant submissions, and ASO assets in both languages.",
      zh: "发布流水线已就位：Play Store 内测轨道 + Xcode Cloud 接 TestFlight。通过微信、小红书进行社区内测，GitHub Issue 表单收集餐厅提交，多语言 ASO 素材准备中。",
    },
    tags: {
      en: ["Play Store CI", "Xcode Cloud", "Community Beta", "Submissions"],
      zh: ["Play Store CI", "Xcode Cloud", "社区内测", "餐厅征集"],
    },
    status: "current",
  },
  {
    phase: 4,
    title: { en: "Launch & Growth", zh: "正式发布与增长" },
    desc: {
      en: "Public launch on App Store and Google Play. Continuous iteration on user feedback, broader UGC contribution flow, editorial collections, and curated themed routes.",
      zh: "正式上架 App Store 与 Google Play。根据用户反馈持续迭代，扩展社区 UGC 投稿通路，推出编辑精选与主题美食路线。",
    },
    tags: {
      en: ["App Store", "Google Play", "UGC", "Editorial"],
      zh: ["App Store", "Google Play", "社区提交", "编辑精选"],
    },
    status: "upcoming",
  },
] as const;

const stats = [
  {
    value: 200,
    suffix: "+",
    label: { en: "Restaurants", zh: "家餐厅" },
    sub: { en: "across Berlin's districts", zh: "覆盖柏林全部行政区" },
  },
  {
    value: 22,
    suffix: "",
    label: { en: "Curated tags", zh: "项菜系标签" },
    sub: { en: "10 regional + 12 format", zh: "10 个地方菜 + 12 种业态" },
  },
  {
    value: 3,
    suffix: "",
    label: { en: "Languages", zh: "种语言" },
    sub: { en: "中文 · English · Deutsch", zh: "中文 · English · Deutsch" },
  },
  {
    value: 100,
    suffix: "%",
    label: { en: "Open source", zh: "开源" },
    sub: { en: "code, data & taxonomy", zh: "代码、数据与分类体系" },
  },
];

const contributeWays = [
  {
    label: { en: "The easy way", zh: "最省事" },
    body: {
      en: "Send a CSV, Excel, or Google Sheet — one row per restaurant, columns however you like. We'll clean it up.",
      zh: "把 CSV、Excel 或 Google Sheets 发给我们，每家餐厅一行，列名随你定，剩下的整理工作我们来做。",
    },
    badge: "📊",
  },
  {
    label: { en: "The hands-on way", zh: "上手版" },
    body: {
      en: "Open a pull request on GitHub. Restaurant data lives as YAML in data/restaurants — edit one and the CI pipeline syncs it live.",
      zh: "直接到 GitHub 提 PR。餐厅数据以 YAML 文件存放在 data/restaurants 目录下，改动后 CI 流水线会自动同步上线。",
    },
    badge: "🛠️",
  },
  {
    label: { en: "The casual way", zh: "随手版" },
    body: {
      en: "Drop a message in the WeChat community group, tag us on Xiaohongshu, or open a GitHub issue. All tips land in the review queue.",
      zh: "在微信社区群里留言、在小红书上 @ 我们，或者直接提 GitHub Issue。所有线索都会进入审核队列。",
    },
    badge: "💬",
  },
];

const contributeWhat = [
  {
    en: "A restaurant we don't have yet — name, address, anything you remember.",
    zh: "还没收录的新餐厅——店名、地址，以及你记得的任何细节。",
  },
  {
    en: "Corrections — closed places, renamed shops, wrong opening hours, bad coordinates.",
    zh: "需要更正的信息——已关门、改名、营业时间不对、地图坐标偏差。",
  },
  {
    en: "Cuisine and dish tags — Sichuan, Cantonese, Northeastern home-style; whatever helps the next person find their match.",
    zh: "菜系和招牌菜的标签——川菜、粤菜、东北家常……让下一个人更快找到对口味的店。",
  },
  {
    en: "Photos, a personal note, or a one-line recommendation we can quote.",
    zh: "照片、一段私人笔记，或者一句可以被引用的推荐语。",
  },
];

const contributors = [
  { name: "Haodong Ju", github: "juhaodong" },
];

export default function HomePage() {
  const { lang, t, tObj } = useLanguage();

  return (
    <div>
      {/* ─────────── Hero ─────────── */}
      <section className="relative overflow-hidden">
        <HeroBackground />
        <div className="mx-auto max-w-6xl px-4 py-28 text-center sm:px-6 sm:py-36 lg:py-44">
          <div className="flex flex-col items-center">
            <div
              className="animate-hero-rise mb-8 inline-flex items-center gap-2 rounded-full border border-border bg-background/70 px-5 py-2 text-base text-muted-foreground backdrop-blur-md"
              style={{ animationDelay: "0.05s" }}
            >
              <span className="text-lg">🍜</span>
              <span>
                {t(
                  "Community-driven digital food guide",
                  "社区驱动的数字美食指南"
                )}
              </span>
            </div>

            <h1
              className="animate-hero-rise text-balance text-6xl font-bold leading-[0.95] tracking-tight sm:text-7xl lg:text-[7.5rem]"
              style={{ animationDelay: "0.17s" }}
            >
              <span>Berlin </span>
              <span className="relative inline-block text-brand">
                Chinese
                <svg
                  className="absolute -bottom-2 left-0 w-full text-brand/60"
                  viewBox="0 0 200 12"
                  preserveAspectRatio="none"
                  aria-hidden
                >
                  <path
                    className="animate-underline-draw"
                    d="M2 8 C 50 2, 150 2, 198 8"
                    stroke="currentColor"
                    strokeWidth="3"
                    strokeLinecap="round"
                    fill="none"
                  />
                </svg>
              </span>
              <br className="sm:hidden" />
              <span> Food Map</span>
            </h1>

            <p
              className="animate-hero-rise mt-6 text-3xl font-medium text-muted-foreground sm:text-4xl lg:text-5xl"
              style={{ animationDelay: "0.29s" }}
            >
              柏林中餐地图
            </p>

            <p
              className="animate-hero-rise mx-auto mt-8 max-w-2xl text-xl text-muted-foreground sm:text-2xl"
              style={{ animationDelay: "0.41s" }}
            >
              {t(
                "A non-profit, community-driven guide to 200+ Chinese restaurants in Berlin. Bilingual, privacy-first, open source — coming soon to Android and iOS.",
                "非营利的社区驱动指南，覆盖柏林 200+ 家中餐馆。双语支持、隐私优先、完全开源——即将登陆 Android 与 iOS。"
              )}
            </p>

            <div
              className="animate-hero-rise mt-12 flex flex-col items-center justify-center gap-4 sm:flex-row"
              style={{ animationDelay: "0.53s" }}
            >
              <a href="#download">
                <Button className="h-14 rounded-full px-10 text-lg shadow-lg shadow-brand/20">
                  {t("Download App", "下载应用")}
                </Button>
              </a>
              <a href="#story">
                <Button
                  variant="outline"
                  className="h-14 rounded-full px-10 text-lg"
                >
                  {t("Read our story", "项目故事")}
                </Button>
              </a>
            </div>

            <a
              href="#stats"
              aria-label="Scroll down"
              className="animate-hero-rise mt-20 hidden text-muted-foreground/60 transition-colors hover:text-foreground sm:inline-flex"
              style={{ animationDelay: "0.85s" }}
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="28"
                height="28"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                className="animate-scroll-bob"
              >
                <path d="M12 5v14" />
                <path d="m19 12-7 7-7-7" />
              </svg>
            </a>
          </div>
        </div>
      </section>

      {/* ─────────── Stats ─────────── */}
      <section
        id="stats"
        className="relative border-t border-border bg-muted/30"
      >
        <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6 sm:py-28">
          <div className="grid grid-cols-2 gap-x-6 gap-y-14 sm:grid-cols-4">
            {stats.map((stat, i) => (
              <FadeIn key={stat.label.en} delay={i * 0.08}>
                <div className="text-center">
                  <div className="text-6xl font-bold tracking-tight text-brand sm:text-7xl">
                    <CountUp to={stat.value} suffix={stat.suffix} />
                  </div>
                  <div className="mt-4 text-lg font-semibold sm:text-xl">
                    {tObj(stat.label)}
                  </div>
                  <div className="mt-1.5 text-base text-muted-foreground sm:text-lg">
                    {tObj(stat.sub)}
                  </div>
                </div>
              </FadeIn>
            ))}
          </div>
        </div>

        {/* Marquee tag band */}
        <FadeIn className="border-t border-border bg-background py-3">
          <MarqueeTags />
        </FadeIn>
      </section>

      {/* ─────────── Features (Bento) ─────────── */}
      <section id="features" className="border-t border-border">
        <div className="mx-auto max-w-6xl px-4 py-24 sm:px-6 sm:py-32">
          <FadeIn className="text-center">
            <h2 className="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
              {t("What's inside", "应用一览")}
            </h2>
            <p className="mt-5 text-xl text-muted-foreground sm:text-2xl">
              {t(
                "Six things we obsessed over — so you can stop arguing about beef noodles.",
                "六件我们死磕的事——让你不用再为「哪家牛肉面更对」而吵架。"
              )}
            </p>
          </FadeIn>

          <div className="mt-16 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {features.map((feature, i) => (
              <FadeIn
                key={feature.title.en}
                delay={i * 0.06}
                className={
                  feature.span === 2 ? "lg:col-span-2" : "lg:col-span-1"
                }
              >
                <div className="group relative h-full overflow-hidden rounded-3xl border border-border bg-card p-8 transition-all duration-300 hover:-translate-y-1.5 hover:border-brand/40 hover:shadow-lg">
                  <div
                    className={`pointer-events-none absolute inset-0 bg-gradient-to-br ${feature.accent} opacity-0 transition-opacity duration-500 group-hover:opacity-100`}
                  />
                  <div className="relative">
                    <div className="mb-5 inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-muted/70 text-3xl shadow-sm">
                      {feature.icon}
                    </div>
                    <h3 className="text-2xl font-semibold tracking-tight">
                      {tObj(feature.title)}
                    </h3>
                    <p className="mt-3 text-base leading-relaxed text-muted-foreground sm:text-lg">
                      {tObj(feature.desc)}
                    </p>
                  </div>
                </div>
              </FadeIn>
            ))}
          </div>
        </div>
      </section>

      {/* ─────────── Story ─────────── */}
      <section
        id="story"
        className="relative overflow-hidden border-t border-border bg-muted/30"
      >
        <div className="pointer-events-none absolute -right-24 top-1/2 -translate-y-1/2 opacity-10 sm:opacity-15">
          <Image
            src="/logo.png"
            alt=""
            width={520}
            height={520}
            aria-hidden
            className="h-[24rem] w-[24rem] rounded-[3rem] sm:h-[32rem] sm:w-[32rem]"
          />
        </div>

        <div className="relative mx-auto max-w-3xl px-4 py-24 sm:px-6 sm:py-32">
          <FadeIn>
            <p className="mb-4 text-sm font-semibold uppercase tracking-[0.2em] text-brand">
              {t("Our story", "项目故事")}
            </p>
            <h2 className="text-4xl font-bold tracking-tight sm:text-5xl">
              {t(
                "A love letter to Berlin's noodle shops.",
                "写给柏林小馆子的情书。"
              )}
            </h2>
          </FadeIn>

          <FadeIn delay={0.1} className="mt-10 space-y-6 text-lg leading-relaxed text-foreground/90 sm:text-xl">
            <p>
              {t(
                "Berlin Chinese Food Map started from a simple frustration. Every time a friend visited, we'd end up sending the same handful of restaurant recommendations over WeChat — losing photos, forgetting addresses, and disagreeing about which place really had the best beef noodles.",
                "「柏林中餐地图」始于一个很简单的烦恼。每次有朋友来柏林，我们都要在微信里翻出同一份餐厅推荐——照片丢了、地址忘了，还要为「哪家牛肉面最对味」争上几句。"
              )}
            </p>
            <p>
              {t(
                "We tried a Google Maps list, then a shared spreadsheet, then a Notion doc. None of them captured what mattered: where to actually eat in Berlin if you grew up with Chinese food.",
                "我们试过 Google Maps 收藏夹、共享 Excel、Notion 文档，但没有一个能真正承载我们想说的：在柏林，一个从小吃中餐长大的人，到底该去哪儿吃。"
              )}
            </p>
            <p>
              {t(
                "So we started this project. It's open-source, non-profit, and built around a simple idea — the people who eat at a place should be the ones describing it. Restaurant data lives as plain YAML files in a public GitHub repo, easy for anyone to read, fork, or improve. The app is free, ad-free, and stores nothing about you.",
                "于是有了这个项目。它是开源的、非盈利的，背后只有一个简单的念头——一家店该由真正在那里吃饭的人来描述。所有餐厅数据以 YAML 文件的形式存放在公开的 GitHub 仓库里，谁都可以阅读、Fork 或者改进。App 完全免费、没有广告，也不收集你的任何信息。"
              )}
            </p>
            <p className="text-foreground">
              {t(
                "It's a love letter, really — to the small noodle shops, the family-run Sichuan kitchens, the late-night dumpling spots that don't show up in Google's top picks. If we've helped you find your next favourite, that's the whole point.",
                "说到底它是一封情书——写给那些藏在巷子里的小面馆、家庭经营的川味厨房，以及永远不会出现在 Google 推荐里的深夜饺子店。如果它帮你找到了下一家心头好，那就是这一切的意义。"
              )}
            </p>
          </FadeIn>
        </div>
      </section>

      {/* ─────────── Roadmap ─────────── */}
      <section id="roadmap" className="border-t border-border">
        <div className="mx-auto max-w-6xl px-4 py-24 sm:px-6 sm:py-32">
          <FadeIn className="text-center">
            <p className="mb-4 text-sm font-semibold uppercase tracking-[0.2em] text-brand">
              {t("Where we are", "进度")}
            </p>
            <h2 className="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
              {t("Roadmap", "实施路线图")}
            </h2>
            <p className="mt-5 text-xl text-muted-foreground sm:text-2xl">
              {t(
                "From POC to production — 4 phases to launch",
                "从 POC 到产品 — 4 个阶段走向上线"
              )}
            </p>
          </FadeIn>

          <div className="relative mt-16">
            <div className="absolute left-[27px] top-0 hidden h-full w-px bg-gradient-to-b from-brand via-border to-border sm:block" />

            <div className="space-y-8">
              {roadmapPhases.map((item, i) => (
                <FadeIn key={item.phase} delay={i * 0.1} y={32}>
                  <div className="relative flex gap-7">
                    <div className="relative z-10 hidden shrink-0 sm:block">
                      <div
                        className={`flex h-14 w-14 items-center justify-center rounded-full border-2 text-base font-bold ${
                          item.status === "done"
                            ? "border-brand bg-brand/10 text-brand"
                            : item.status === "current"
                              ? "border-brand bg-brand text-brand-foreground shadow-lg shadow-brand/30"
                              : "border-border bg-muted text-muted-foreground"
                        }`}
                      >
                        {item.status === "done" ? (
                          <svg
                            xmlns="http://www.w3.org/2000/svg"
                            width="22"
                            height="22"
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

                    <div
                      className={`flex-1 rounded-3xl border p-7 transition-all duration-300 hover:-translate-y-0.5 hover:shadow-md ${
                        item.status === "current"
                          ? "border-brand/50 bg-brand/5"
                          : item.status === "done"
                            ? "border-border bg-card"
                            : "border-border bg-card"
                      }`}
                    >
                      <div className="flex flex-wrap items-center gap-3">
                        <span className="inline-flex items-center rounded-full bg-muted px-3 py-1 text-sm font-medium text-muted-foreground sm:hidden">
                          {t(`Phase ${item.phase}`, `第 ${item.phase} 阶段`)}
                        </span>
                        <h3 className="text-2xl font-semibold">
                          {t(`Phase ${item.phase}`, `第 ${item.phase} 阶段`)}
                          <span className="mx-2 text-muted-foreground">—</span>
                          {tObj(item.title)}
                        </h3>
                        {item.status === "done" && (
                          <span className="inline-flex items-center rounded-full bg-brand/10 px-3 py-1 text-sm font-medium text-brand">
                            {t("Completed", "已完成")}
                          </span>
                        )}
                        {item.status === "current" && (
                          <span className="inline-flex items-center rounded-full bg-brand px-3 py-1 text-sm font-medium text-brand-foreground">
                            {t("In Progress", "进行中")}
                          </span>
                        )}
                        {item.status === "upcoming" && (
                          <span className="inline-flex items-center rounded-full bg-muted px-3 py-1 text-sm font-medium text-muted-foreground">
                            {t("Upcoming", "即将开始")}
                          </span>
                        )}
                      </div>
                      <p className="mt-4 text-base leading-relaxed text-muted-foreground sm:text-lg">
                        {tObj(item.desc)}
                      </p>
                      <div className="mt-5 flex flex-wrap gap-2">
                        {(lang === "zh" ? item.tags.zh : item.tags.en).map(
                          (tag: string) => (
                            <span
                              key={tag}
                              className="inline-flex items-center rounded-md border border-border bg-muted/50 px-2.5 py-1 text-sm text-muted-foreground"
                            >
                              {tag}
                            </span>
                          )
                        )}
                      </div>
                    </div>
                  </div>
                </FadeIn>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ─────────── Contribute ─────────── */}
      <section
        id="contribute"
        className="relative overflow-hidden border-t border-border bg-muted/30"
      >
        <div className="mx-auto max-w-6xl px-4 py-24 sm:px-6 sm:py-32">
          <FadeIn className="mx-auto max-w-2xl text-center">
            <p className="mb-4 text-sm font-semibold uppercase tracking-[0.2em] text-brand">
              {t("Get involved", "加入我们")}
            </p>
            <h2 className="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
              {t("Help us map the next noodle shop.", "和我们一起补上下一家小面馆。")}
            </h2>
            <p className="mt-5 text-xl text-muted-foreground sm:text-2xl">
              {t(
                "New places open every month, old favourites move or close. No single team can keep up alone — if you've spotted something we're missing, we'd love a hand.",
                "每个月都有新店开业，老馆子也在搬走或关门。仅靠几个人永远跟不上节奏——如果你发现一家我们还没收录的小店，欢迎一起把它补上。"
              )}
            </p>
          </FadeIn>

          <div className="mt-16 grid grid-cols-1 gap-5 md:grid-cols-3">
            {contributeWays.map((way, i) => (
              <FadeIn key={way.label.en} delay={i * 0.08}>
                <div className="h-full rounded-3xl border border-border bg-card p-8 transition-all duration-300 hover:-translate-y-1 hover:border-brand/40 hover:shadow-md">
                  <div className="text-4xl">{way.badge}</div>
                  <h3 className="mt-5 text-xl font-semibold sm:text-2xl">
                    {tObj(way.label)}
                  </h3>
                  <p className="mt-3 text-base leading-relaxed text-muted-foreground sm:text-lg">
                    {tObj(way.body)}
                  </p>
                </div>
              </FadeIn>
            ))}
          </div>

          <FadeIn delay={0.2} className="mt-16">
            <div className="rounded-3xl border border-border bg-card p-8 sm:p-10">
              <h3 className="text-2xl font-semibold sm:text-3xl">
                {t("What you can share", "你可以分享什么")}
              </h3>
              <ul className="mt-6 grid gap-3 sm:grid-cols-2">
                {contributeWhat.map((item) => (
                  <li
                    key={item.en}
                    className="flex gap-3 text-base leading-relaxed text-muted-foreground sm:text-lg"
                  >
                    <span className="mt-2.5 h-1.5 w-1.5 shrink-0 rounded-full bg-brand" />
                    <span>{tObj(item)}</span>
                  </li>
                ))}
              </ul>

              <div className="mt-8 rounded-2xl bg-muted/60 p-6 text-base leading-relaxed text-muted-foreground sm:text-lg">
                <strong className="text-foreground">
                  {t("What happens next: ", "提交之后：")}
                </strong>
                {t(
                  "Every submission is reviewed by a real person, cross-checked with Google Places, and goes live on the next data sync — usually within a few days.",
                  "每一条投稿都会经过真人审核，再用 Google Places 复核营业时间和坐标，下一次数据同步时正式上线，一般几天内可见。"
                )}
              </div>

              <div className="mt-8 flex flex-col gap-3 sm:flex-row">
                <a href={SUBMIT_URL} target="_blank" rel="noopener noreferrer">
                  <Button className="h-12 rounded-full px-7 text-base shadow-sm">
                    {t("Submit a restaurant", "提交餐厅")}
                  </Button>
                </a>
                <a href={REPO_URL} target="_blank" rel="noopener noreferrer">
                  <Button
                    variant="outline"
                    className="h-12 rounded-full px-7 text-base"
                  >
                    {t("Browse the repo on GitHub", "查看 GitHub 仓库")}
                  </Button>
                </a>
              </div>
            </div>
          </FadeIn>
        </div>
      </section>

      {/* ─────────── Team ─────────── */}
      <section id="team" className="border-t border-border">
        <div className="mx-auto max-w-4xl px-4 py-24 text-center sm:px-6 sm:py-32">
          <FadeIn>
            <p className="mb-4 text-sm font-semibold uppercase tracking-[0.2em] text-brand">
              {t("The crew", "幕后团队")}
            </p>
            <h2 className="text-4xl font-bold tracking-tight sm:text-5xl">
              {t("People behind the map", "地图背后的人")}
            </h2>
            <p className="mt-5 text-lg text-muted-foreground sm:text-xl">
              {t(
                "Pulled live from the project's git history — every commit that touches the restaurant data files. Open an issue if we missed you.",
                "名单直接来自仓库的 git 历史，每次有人提交餐厅数据的改动都会自动刷新。如果你贡献过却被漏掉了，欢迎提 Issue。"
              )}
            </p>
          </FadeIn>

          <FadeIn delay={0.15}>
            <div className="mt-12 flex flex-wrap justify-center gap-4">
              {contributors.map((c) => (
                <a
                  key={c.github ?? c.name}
                  href={
                    c.github ? `https://github.com/${c.github}` : undefined
                  }
                  target="_blank"
                  rel="noopener noreferrer"
                  className="group flex items-center gap-4 rounded-full border border-border bg-card py-3 pl-3 pr-6 transition-all hover:border-brand/40 hover:shadow-md"
                >
                  {c.github ? (
                    <Image
                      src={`https://github.com/${c.github}.png?size=80`}
                      alt={c.name}
                      width={48}
                      height={48}
                      className="h-12 w-12 rounded-full"
                      unoptimized
                    />
                  ) : (
                    <div className="flex h-12 w-12 items-center justify-center rounded-full bg-brand/10 text-base font-semibold text-brand">
                      {c.name.charAt(0)}
                    </div>
                  )}
                  <div className="text-left">
                    <div className="text-base font-semibold sm:text-lg">
                      {c.name}
                    </div>
                    {c.github && (
                      <div className="font-mono text-sm text-brand">
                        @{c.github}
                      </div>
                    )}
                  </div>
                </a>
              ))}
              <a
                href={SUBMIT_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="group flex items-center gap-3 rounded-full border border-dashed border-border bg-card/50 py-3 pl-4 pr-6 text-muted-foreground transition-colors hover:border-brand/40 hover:text-foreground"
              >
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-muted text-2xl">
                  +
                </div>
                <div className="text-left text-base sm:text-lg">
                  {t("Be the next contributor", "成为下一位贡献者")}
                </div>
              </a>
            </div>
          </FadeIn>
        </div>
      </section>

      {/* ─────────── Download ─────────── */}
      <section
        id="download"
        className="relative overflow-hidden border-t border-border bg-muted/30"
      >
        <div className="pointer-events-none absolute inset-0 -z-10">
          <div className="animate-blob-a absolute left-1/4 top-1/2 h-96 w-96 -translate-y-1/2 rounded-full bg-brand/15 blur-3xl" />
        </div>

        <div className="mx-auto max-w-4xl px-4 py-24 text-center sm:px-6 sm:py-32">
          <FadeIn>
            <h2 className="text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
              {t("Get the App", "获取应用")}
            </h2>
            <p className="mt-5 text-xl text-muted-foreground sm:text-2xl">
              {t(
                "Internal beta running now via TestFlight and Play Store internal track. Public release is next.",
                "内测版本已通过 TestFlight 与 Play Store 内测轨道运行中，正式发布即将到来。"
              )}
            </p>
          </FadeIn>

          <FadeIn delay={0.1}>
            <div className="mt-12 flex flex-col items-center justify-center gap-4 sm:flex-row">
              <a
                href="#"
                className="group inline-flex h-16 items-center gap-3 rounded-2xl border border-border bg-card px-7 transition-all hover:-translate-y-0.5 hover:border-brand/40 hover:shadow-md"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="32"
                  height="32"
                  viewBox="0 0 24 24"
                  fill="currentColor"
                >
                  <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z" />
                </svg>
                <div className="text-left">
                  <div className="text-sm text-muted-foreground">
                    {t("Download on the", "下载于")}
                  </div>
                  <div className="text-base font-semibold">App Store</div>
                </div>
              </a>

              <a
                href="#"
                className="group inline-flex h-16 items-center gap-3 rounded-2xl border border-border bg-card px-7 transition-all hover:-translate-y-0.5 hover:border-brand/40 hover:shadow-md"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="32"
                  height="32"
                  viewBox="0 0 24 24"
                  fill="currentColor"
                >
                  <path d="M3.609 1.814L13.792 12 3.61 22.186a.996.996 0 0 1-.61-.92V2.734a1 1 0 0 1 .609-.92zm10.89 10.893l2.302 2.302-10.937 6.333 8.635-8.635zm3.199-3.199l2.807 1.626a1 1 0 0 1 0 1.732l-2.808 1.626L15.206 12l2.492-2.492zM5.864 2.658L16.802 8.99l-2.303 2.303-8.635-8.635z" />
                </svg>
                <div className="text-left">
                  <div className="text-sm text-muted-foreground">
                    {t("Get it on", "下载于")}
                  </div>
                  <div className="text-base font-semibold">Google Play</div>
                </div>
              </a>
            </div>

            <p className="mt-10 text-base text-muted-foreground">
              {t(
                "Want a beta invite or to suggest a restaurant we missed? Open a GitHub issue — link in the navbar.",
                "想加入内测、或推荐我们漏掉的餐厅？欢迎通过导航栏的 GitHub 入口提交。"
              )}
            </p>
          </FadeIn>
        </div>
      </section>
    </div>
  );
}
