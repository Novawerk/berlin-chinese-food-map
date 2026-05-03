"use client";

const TAGS = [
  "川菜", "粤菜", "京菜", "西北", "东北", "湖南", "上海", "云贵",
  "火锅", "烤肉", "小吃", "面食", "饺子", "烧烤", "煲仔", "粤式茶餐",
  "Sichuan", "Cantonese", "Hot Pot", "BBQ", "Dim Sum", "Noodles",
  "Dumplings", "Mongolian", "Yunnan", "Northeast", "Hunan",
];

export function MarqueeTags() {
  const items = [...TAGS, ...TAGS]; // duplicate for seamless loop

  return (
    <div className="relative overflow-hidden py-3">
      <div className="pointer-events-none absolute inset-y-0 left-0 z-10 w-24 bg-gradient-to-r from-background to-transparent" />
      <div className="pointer-events-none absolute inset-y-0 right-0 z-10 w-24 bg-gradient-to-l from-background to-transparent" />
      <div className="animate-marquee flex gap-3 whitespace-nowrap">
        {items.map((t, i) => (
          <span
            key={`${t}-${i}`}
            className="inline-flex items-center rounded-full border border-border bg-card px-4 py-2 text-base font-medium text-foreground/80 shadow-sm"
          >
            {t}
          </span>
        ))}
      </div>
    </div>
  );
}
