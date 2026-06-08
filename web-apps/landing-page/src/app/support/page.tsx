"use client";

import { useLanguage } from "@/lib/language";

const CONTACT_EMAIL = "info@novawerk.io";
const REPO_URL = "https://github.com/Novawerk/berlin-chinese-food-map";
const SUBMIT_URL =
  "https://github.com/Novawerk/berlin-chinese-food-map/issues/new?template=restaurant-submission.yml";
const BUG_URL =
  "https://github.com/Novawerk/berlin-chinese-food-map/issues/new";

export default function SupportPage() {
  const { t } = useLanguage();

  return (
    <div className="mx-auto max-w-3xl px-4 py-16 sm:px-6 sm:py-24">
      <div className="mb-12">
        <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">
          {t("Support", "帮助与支持")}
        </h1>
        <p className="mt-3 text-lg text-muted-foreground">
          {t(
            "We’re a small community project. Here’s how to get help, report problems, or contribute.",
            "我们是一个小型社区项目。以下是寻求帮助、反馈问题或参与贡献的方式。"
          )}
        </p>
      </div>

      <div className="space-y-8 text-base leading-relaxed text-foreground/90">
        <Section title={t("Contact", "联系我们")}>
          <p>
            {t(
              "The fastest way to reach us is email. We usually reply within a few days.",
              "电子邮件是与我们联系最快的方式，我们通常会在几天内回复。"
            )}
          </p>
          <p>
            <ExternalLink href={`mailto:${CONTACT_EMAIL}`}>
              {CONTACT_EMAIL}
            </ExternalLink>
          </p>
        </Section>

        <Section title={t("Report a bug or request a feature", "报告问题或建议新功能")}>
          <p>
            {t(
              "Public issue tracking lives on GitHub. When you open an issue, please include your device, OS version, and steps to reproduce.",
              "我们在 GitHub 上公开追踪问题。提交 issue 时，请附上您的设备型号、系统版本以及复现步骤。"
            )}
          </p>
          <p>
            <ExternalLink href={BUG_URL}>
              {t("Open a GitHub issue →", "前往 GitHub 提交 issue →")}
            </ExternalLink>
          </p>
        </Section>

        <Section title={t("Submit or correct a restaurant", "提交或修正餐厅信息")}>
          <p>
            {t(
              "Restaurants are community-curated. To add a new restaurant or fix incorrect details (address, hours, tags, photos), use the submission template:",
              "餐厅信息由社区共同维护。如需新增餐厅或修正现有信息（地址、营业时间、标签、照片等），请使用提交模板："
            )}
          </p>
          <p>
            <ExternalLink href={SUBMIT_URL}>
              {t("Submit a restaurant →", "提交餐厅 →")}
            </ExternalLink>
          </p>
          <p>
            {t(
              "If GitHub isn’t convenient, you can also email the details to us and we’ll add the entry on your behalf.",
              "若使用 GitHub 不方便，您也可以将信息发送邮件给我们，我们会代为提交。"
            )}
          </p>
        </Section>

        <Section title={t("Frequently asked", "常见问题")}>
          <FaqItem
            q={t("Do I need an account?", "需要注册账号吗？")}
            a={t(
              "No. The app does not require sign-up or sign-in. You can browse the entire map immediately after install.",
              "不需要。应用无需注册或登录，安装后即可浏览全部内容。"
            )}
          />
          <FaqItem
            q={t("Why does the app ask for location?", "为什么会请求位置权限？")}
            a={
              <>
                {t(
                  "Only when you tap the “my location” button. The coordinates stay on your device — see the ",
                  "仅当您点击“我的位置”按钮时才会请求。坐标只保存在本机 —— 详情请见 "
                )}
                <ExternalLink href="/privacy">
                  {t("Privacy Policy", "隐私政策")}
                </ExternalLink>
                {t(" for details.", "。")}
              </>
            }
          />
          <FaqItem
            q={t("Is the app free?", "应用收费吗？")}
            a={t(
              "Yes — completely free, no ads, no in-app purchases. The project is non-profit.",
              "完全免费，无广告、无内购。本项目为非营利性质。"
            )}
          />
          <FaqItem
            q={t("Which cities does it cover?", "覆盖哪些城市？")}
            a={t(
              "Currently Berlin only. We may expand later if the community asks for it.",
              "目前仅覆盖柏林。如社区有需求，未来可能扩展到其他城市。"
            )}
          />
        </Section>

        <Section title={t("Source code", "源代码")}>
          <p>
            {t(
              "The app and its data are open source. Browse, fork, or contribute on GitHub:",
              "应用代码与数据均开源。欢迎在 GitHub 浏览、fork 或贡献："
            )}
          </p>
          <p>
            <ExternalLink href={REPO_URL}>{REPO_URL}</ExternalLink>
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

function FaqItem({ q, a }: { q: string; a: React.ReactNode }) {
  return (
    <div className="rounded-lg border border-border bg-muted/30 p-4">
      <p className="font-semibold">{q}</p>
      <p className="mt-1 text-foreground/85">{a}</p>
    </div>
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
