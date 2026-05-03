// One-shot: assign tags to the 75 restaurants the auto-migrator left untagged.
//
// Each entry below is hand-curated based on the restaurant name (Cantonese
// vs Mandarin romanisation, place-name hints like 天府 / Macao / Prince),
// the Berlin Chinese-restaurant scene (legacy 酒楼/酒家 are overwhelmingly
// Cantonese-diaspora-run), and known landmarks (Mayflower / Ming Dynastie
// West / Aroma Fhain are confirmed Cantonese). Anything I genuinely can't
// place stays untagged for the next human triage pass.
//
// Run from `scripts/sync-to-firestore`:   node tag-untagged.mjs

import { readFileSync, writeFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { glob } from "glob";
import yaml from "js-yaml";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const REPO_ROOT = resolve(__dirname, "..", "..");
const DATA_DIR = resolve(REPO_ROOT, "data", "restaurants");

// id → tags (in primary-first order). Empty array = leave untagged.
const ASSIGNMENTS = {
  // --- Cantonese (default for 酒楼/酒家 + Cantonese romanisation) ---
  "reuter": ["CANTONESE"],                     // 中国大酒楼
  "royal-gourmet": ["CANTONESE"],              // Royal Gourmet
  "ming-dynastie-west": ["CANTONESE", "DIM_SUM"], // sister of the dim-sum-tagged jannowitzbruecke branch
  "aroma-fhain": ["CANTONESE", "DIM_SUM"],     // sister of aroma-chburg (CANTONESE+DIM_SUM)
  "huating": ["CANTONESE"],                    // 华亭酒家
  "hui-feng": ["CANTONESE"],                   // 汇丰酒家
  "panda-garden": ["CANTONESE"],               // 熊猫园
  "fu-duo": ["CANTONESE"],                     // 福多酒家
  "bambus": ["CANTONESE"],                     // 竹园酒楼
  "fu-long": ["CANTONESE"],                    // 富隆酒楼
  "china-city": ["CANTONESE"],                 // 中国城大酒楼 (mitte)
  "lucky-star": ["CANTONESE"],                 // 全家福
  "prince": ["CANTONESE"],                     // 太子 (HK Prince Edward 区)
  "mayflower": ["CANTONESE"],                  // 五月花 — known Cantonese
  "phoenix-buffet": ["CANTONESE"],             // 凤凰酒楼
  "donghailou-restaurant": ["CANTONESE"],      // 东海楼
  "long-li-house": ["CANTONESE"],              // 龙利阁
  "happy-panda": ["CANTONESE"],
  "panda-ii": ["CANTONESE"],                   // 熊猫酒楼
  "wan-loi": ["CANTONESE"],                    // Wan-Loi (Cantonese rom.)
  "dschunke": ["CANTONESE"],                   // 帆船酒楼 — long-running Cantonese
  "rosengarten": ["CANTONESE"],                // 玫瑰苑
  "all-seasons": ["CANTONESE"],                // 五湖四海
  "evergreen": ["CANTONESE"],                  // 长青酒楼
  "macao": ["CANTONESE"],                      // 澳门酒楼 — Macao = 粤
  "man-far": ["CANTONESE"],                    // Man-Far (Cantonese rom.)
  "regent": ["CANTONESE"],                     // generic Berlin Chinese restaurant
  "fu-li-lai": ["CANTONESE"],                  // 福利来
  "hee-lam-mun": ["CANTONESE"],                // Hee Lam Mun (Cantonese rom.)
  "tai-lee": ["CANTONESE"],                    // Tai Lee (Cantonese rom.)
  "lon-min": ["CANTONESE"],                    // Lon-Min (Cantonese rom.)
  "hot-spot": ["CANTONESE"],                   // 聚友阁
  "sun-wah": ["CANTONESE"],                    // Sun Wah (Cantonese rom.)
  "lucky": ["CANTONESE"],                      // 福仁
  "pongs-china-restaurant": ["CANTONESE"],     // Pong's
  "neu-wuzhou": ["CANTONESE"],                 // 新五洲饭店
  "happy-buddha": ["CANTONESE"],               // 佛笑楼 — famous Cantonese tradition
  "zhong-hua": ["CANTONESE"],                  // 中华饭店
  "feng-yuan": ["CANTONESE"],                  // 丰源饭店
  "china-town-lichtenrade": ["CANTONESE"],     // 兄弟城/China-Town
  "good-friends": ["CANTONESE"],               // 老友记 — Cantonese diaspora classic
  "do-de-li": ["CANTONESE"],                   // 多得利 (Cantonese: do dak lei)
  "sonne": ["CANTONESE"],                      // 新阳光
  "mankee": ["CANTONESE"],                     // 文记 → Mankee (Cantonese rom.)
  "howhow": ["CANTONESE"],                     // 好好酒楼

  // --- Sichuan (天府 / 川 explicit hints) ---
  "tianfu": ["SICHUAN"],                       // 天府酒家 — 天府 = Sichuan ("天府之国")
  "chuan-house": ["SICHUAN"],                  // 寻鲜记 / Chuan House
  "guoguoxiang": ["SICHUAN", "HOTPOT"],        // 锅锅香 — name evokes hotpot

  // --- Vegetarian ---
  "green-flavor": ["VEGETARIAN"],              // 绿食膳

  // --- Generic Chinese (no strong regional signal — leave with no tag) ---
  // Skipped intentionally:
  //   chens-wok, en-lee-cai, holly, lulus, ming-jia, shifang-house, you-mi,
  //   miss-wu, jia-jia, china-garden, kaiser-drachen, tangs-kantine,
  //   golden-garden, royal-garden, hua-li-du, shanshan, wei-dao-jia,
  //   zhous-five, zhou-s-fine, chichikan, king-men, jing-yang, feast,
  //   lychee, mr-liu, wiling, wunderbau
};

const FIELD_ORDER = [
  "name", "tags", "address", "latitude", "longitude",
  "phone", "priceRange", "description", "logoUrl", "galleries",
  "hidden", "featured", "editorialNote", "chain", "placeId",
];

function reorderObject(obj) {
  const out = {};
  for (const key of FIELD_ORDER) if (key in obj) out[key] = obj[key];
  for (const key of Object.keys(obj)) if (!(key in out)) out[key] = obj[key];
  return out;
}

function serialise(obj) {
  return yaml.dump(reorderObject(obj), {
    lineWidth: 120,
    noRefs: true,
    quotingType: '"',
    forceQuotes: false,
  });
}

async function main() {
  const files = await glob(`${DATA_DIR}/**/*.yaml`, {
    ignore: ["**/_schema.yaml", "**/_example.yaml"],
  });
  const byId = new Map();
  for (const file of files) {
    const id = file.split("/").pop().replace(/\.yaml$/, "");
    if (id.startsWith("_")) continue;
    byId.set(id, file);
  }

  let updated = 0;
  let missing = [];
  for (const [id, tags] of Object.entries(ASSIGNMENTS)) {
    const file = byId.get(id);
    if (!file) {
      missing.push(id);
      continue;
    }
    const data = yaml.load(readFileSync(file, "utf-8"));
    data.tags = tags;
    writeFileSync(file, serialise(data), "utf-8");
    updated++;
    console.log(`  ${id} → [${tags.join(", ")}]`);
  }

  console.log(`\nUpdated ${updated} files.`);
  if (missing.length) {
    console.error(`Missing YAMLs (typo in ASSIGNMENTS?):`, missing);
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
