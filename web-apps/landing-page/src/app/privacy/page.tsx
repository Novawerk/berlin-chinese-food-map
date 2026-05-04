"use client";

import { useLanguage } from "@/lib/language";

const LAST_UPDATED_EN = "May 4, 2026";
const LAST_UPDATED_ZH = "2026 年 5 月 4 日";
const CONTACT_EMAIL = "info@novawerk.io";

export default function PrivacyPage() {
  const { t } = useLanguage();

  return (
    <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6 sm:py-24">
      <div className="mb-12">
        <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">
          {t("Privacy Policy", "隐私政策")}
        </h1>
        <p className="mt-3 text-base text-muted-foreground">
          {t(
            `Last updated: ${LAST_UPDATED_EN}`,
            `最后更新：${LAST_UPDATED_ZH}`
          )}
        </p>
      </div>

      <div className="space-y-8 text-base leading-relaxed text-foreground/90">
        <p>
          {t(
            "Berlin Chinese Food Map (“the app”) is a non-profit, community-driven guide to Chinese restaurants in Berlin, operated by Novawerk. Privacy is a core design goal: we collect as little data as possible, store it locally whenever we can, and never sell or share it.",
            "「柏林中餐地图」（以下简称「本应用」）是由 Novawerk 运营的一款非营利、社区驱动的柏林中餐厅指南。我们将隐私视为核心设计目标：尽可能少地收集数据、能在本地存储就在本地存储，且永不出售或共享。"
          )}
        </p>

        <Section title={t("1. Information we collect", "1. 我们收集的信息")}>
          <p>
            {t(
              "We do not ask for an account, email address, name, or any other personal identifier. The only data automatically created on our backend is:",
              "我们不要求您注册账号，也不收集邮箱、姓名或其他个人身份信息。系统在后端自动创建的数据仅包括："
            )}
          </p>
          <ul className="mt-3 list-disc space-y-2 pl-6">
            <li>
              <strong>{t("Anonymous user ID", "匿名用户 ID")}</strong>
              {t(
                ": A random identifier issued by Firebase Authentication when the app starts. It is not linked to your name, email, phone, or device. It exists so we can count distinct visits without tracking you.",
                "：应用启动时由 Firebase 匿名认证签发的随机 ID，不与您的姓名、邮箱、手机号或设备绑定，仅用于在不追踪个人的前提下统计独立访问数。"
              )}
            </li>
            <li>
              <strong>{t("Per-restaurant view records", "每家餐厅的浏览记录")}</strong>
              {t(
                ": When you open a restaurant detail page, the app writes a small record under that restaurant keyed by your anonymous user ID, containing only the timestamp and a count. We use it to show how many distinct visitors a restaurant has had, never to build a profile of you. Because the record is indexed only by a randomly-issued anonymous ID, we cannot tie it back to your name, email, or device.",
                "：当您打开某家餐厅的详情页时，应用会在该餐厅下写入一条记录，索引键为您的匿名用户 ID，内容仅包含时间戳与计数。我们仅用它来统计每家餐厅的独立访客数，绝不用于给您建立画像。由于记录只通过随机签发的匿名 ID 索引，我们无法将其追溯到您的姓名、邮箱或设备。"
              )}
            </li>
          </ul>
        </Section>

        <Section title={t("2. Information we do NOT collect", "2. 我们不收集的信息")}>
          <ul className="list-disc space-y-2 pl-6">
            <li>{t("No advertising or analytics SDKs (no Google Analytics, no Facebook SDK, no Firebase Analytics).", "不集成广告或分析 SDK（没有 Google Analytics、Facebook SDK、Firebase Analytics 等）。")}</li>
            <li>{t("No third-party trackers, fingerprinting, or cross-app identifiers.", "没有第三方追踪、指纹识别或跨应用标识符。")}</li>
            <li>{t("No contacts, photos, microphone, or camera access.", "不读取通讯录、相册，也不访问麦克风或摄像头。")}</li>
            <li>{t("No purchase, payment, or financial information — the app is free and contains no in-app purchases.", "不收集购买、支付或财务信息 —— 应用完全免费且无任何内购。")}</li>
          </ul>
        </Section>

        <Section title={t("3. Location", "3. 位置信息")}>
          <p>
            {t(
              "If you tap the “my location” button on the map, the app asks the operating system for your current location and renders a dot on the map. Your coordinates stay on your device — they are never uploaded to our servers, written to Firestore, or shared with any third party. You can deny or revoke the permission at any time in your device settings; the rest of the app works without it.",
              "当您在地图上点击“我的位置”按钮时，应用会向操作系统请求当前位置，并在地图上显示一个圆点。坐标始终保留在您的设备上 —— 不会上传至我们的服务器、不会写入 Firestore，也不会分享给任何第三方。您可以随时在系统设置中拒绝或撤销该权限，应用的其他功能不受影响。"
            )}
          </p>
        </Section>

        <Section title={t("4. Settings stored on your device", "4. 仅存储在本机的设置")}>
          <p>
            {t(
              "Theme (light/dark) and language preference are stored locally using the operating system’s standard preferences storage. They never leave your device.",
              "主题（浅色/深色）与语言偏好通过操作系统的标准偏好存储保存在本机，从不离开您的设备。"
            )}
          </p>
        </Section>

        <Section title={t("5. Restaurant data", "5. 餐厅数据")}>
          <p>
            {t(
              "Restaurant entries (name, address, photos, opening hours, tags) are submitted by community contributors via our public GitHub repository, reviewed, and published. They are not personal data and they are public by design — anyone can read them on the map.",
              "餐厅条目（名称、地址、照片、营业时间、标签）由社区贡献者通过我们公开的 GitHub 仓库提交，经审核后发布。它们并非个人数据，且本就设计为公开内容 —— 任何人都能在地图上看到。"
            )}
          </p>
        </Section>

        <Section title={t("6. Third-party services", "6. 第三方服务")}>
          <ul className="list-disc space-y-2 pl-6">
            <li>
              <strong>{t("Firebase (Google LLC)", "Firebase（Google LLC）")}</strong>
              {t(
                ": hosts the restaurant database (Cloud Firestore) and issues anonymous IDs (Firebase Authentication). See ",
                "：托管餐厅数据库（Cloud Firestore）并签发匿名 ID（Firebase Authentication）。请参阅 "
              )}
              <ExternalLink href="https://firebase.google.com/support/privacy">
                {t("Firebase’s privacy policy", "Firebase 隐私政策")}
              </ExternalLink>
              {t(".", "。")}
            </li>
            <li>
              <strong>{t("Map providers", "地图供应商")}</strong>
              {t(
                ": Google Maps on Android and Apple MapKit on iOS render the base map. They may load tiles and process the map view region locally on your device per their own privacy policies (",
                "：Android 端使用 Google 地图、iOS 端使用 Apple MapKit 渲染底图。它们会根据各自的隐私政策在您的设备上加载瓦片并处理地图视域（"
              )}
              <ExternalLink href="https://policies.google.com/privacy">Google</ExternalLink>
              {t(", ", "、")}
              <ExternalLink href="https://www.apple.com/legal/privacy/">Apple</ExternalLink>
              {t(").", "）。")}
            </li>
          </ul>
        </Section>

        <Section title={t("7. Children", "7. 未成年人")}>
          <p>
            {t(
              "The app is suitable for all ages and is not specifically directed at children under 13. We do not knowingly collect data from children.",
              "应用适合所有年龄使用，并非专门面向 13 岁以下儿童。我们也不会主动收集儿童数据。"
            )}
          </p>
        </Section>

        <Section title={t("8. Data retention", "8. 数据留存")}>
          <p>
            {t(
              "The anonymous ID and view counters live in Cloud Firestore. Because they are not linked to you, we cannot “delete your data” on request — there is nothing personal to delete. Uninstalling the app discards the local anonymous ID; a fresh install issues a new one.",
              "匿名 ID 与浏览计数存储在 Cloud Firestore 中。由于它们与您本人无关联，我们也无法按个人请求“删除您的数据” —— 因为没有可识别您的内容可删除。卸载应用会丢弃本地的匿名 ID，重新安装时会签发一个新的。"
            )}
          </p>
        </Section>

        <Section title={t("9. Changes to this policy", "9. 本政策的变更")}>
          <p>
            {t(
              "If we materially change how the app handles data, we will update this page and bump the “Last updated” date above. The history is also tracked in the app’s public GitHub repository.",
              "如果应用处理数据的方式发生实质性变化，我们会更新本页面并修改顶部的“最后更新”日期。变更记录也会同步到我们公开的 GitHub 仓库。"
            )}
          </p>
        </Section>

        <Section title={t("10. Contact", "10. 联系我们")}>
          <p>
            {t("Questions or concerns about privacy? Email ", "对隐私有任何疑问或建议？请发送邮件至 ")}
            <ExternalLink href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</ExternalLink>
            {t(" or open an issue on ", "，或在 ")}
            <ExternalLink href="https://github.com/Novawerk/berlin-chinese-food-map">GitHub</ExternalLink>
            {t(".", " 上提交 issue。")}
          </p>
        </Section>
      </div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section>
      <h2 className="mb-3 text-xl font-semibold tracking-tight">{title}</h2>
      <div className="space-y-3 text-foreground/85">{children}</div>
    </section>
  );
}

function ExternalLink({ href, children }: { href: string; children: React.ReactNode }) {
  const isExternal = href.startsWith("http");
  return (
    <a
      href={href}
      {...(isExternal ? { target: "_blank", rel: "noopener noreferrer" } : {})}
      className="text-primary underline-offset-4 hover:underline"
    >
      {children}
    </a>
  );
}
