// Hand-curated tags for the last 20 untagged restaurants based on a deeper
// pass: full Places review text, editorial summaries, restaurant websites,
// and targeted web searches for the ones that needed it.
//
// Each entry below cites the strongest evidence in a comment. Run as:
//   node tag-from-research.mjs

import { readFileSync, writeFileSync, readdirSync, statSync } from "fs";
import { resolve, dirname, basename, join } from "path";
import { fileURLToPath } from "url";
import yaml from "js-yaml";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const REPO_ROOT = resolve(__dirname, "..", "..");
const DATA_DIR = resolve(REPO_ROOT, "data", "restaurants");

const ASSIGNMENTS = {
  // Reviewer from Hong Kong: "feel like home"; "best wantons in my life";
  // "dumplings homemade by the owner". Wonton/雲吞 is a Cantonese signature.
  "en-lee-cai": ["CANTONESE", "DUMPLINGS", "NOODLES"],

  // Reviews repeatedly mention skewers ("traditionally made"), Liangfen,
  // 酸菜鱼, spicy eggplant, lamb dumplings. Spicy + grilled skewers fits
  // the Sichuan-style 烧烤 tradition.
  "holly": ["SICHUAN", "BBQ"],

  // Reviews: "Xiao Long Bao", "pan fried dumplings", "noodles". XLB is the
  // Shanghai signature.
  "hua-li-du": ["SHANGHAINESE", "DUMPLINGS", "NOODLES"],

  // Top review explicitly: "Northeast China style skewers". Lamb-heavy menu.
  "jia-jia": ["NORTHEASTERN", "BBQ"],

  // Reviews: wan tan, spring rolls, noodles — Cantonese classics. Kingmen is
  // also a Cantonese romanization (similar to 京門/景门 transliteration).
  "king-men": ["CANTONESE", "NOODLES"],

  // Reviews highlight Mapo Tofu, Kungpao chicken (Sichuan staples) plus the
  // Shandong Huangmenji set. Sichuan is the headline.
  "lulus": ["SICHUAN"],

  // Reviews: wonton soup, dumplings, double-fried pork — Cantonese-leaning
  // generalist. Bami goreng on the menu hints at SE Asian crossover.
  "ming-jia": ["CANTONESE"],

  // Reviewer: "spicy or sichuan style", "La zi ji crispy" (辣子鸡 = Sichuan),
  // "steamed dumpling".
  "miss-wu": ["SICHUAN", "DUMPLINGS"],

  // Google's types[] explicitly includes mongolian_barbecue_restaurant +
  // buffet_restaurant. The German "Mongolisches Grill" buffet is the format.
  "royal-garden": ["MONGOLIAN", "BBQ"],

  // Editorial: "Chinese & European fare". Review specifically: "hunan beef
  // was excellent". Single mention but it's the standout dish reviewed.
  "shanshan": ["HUNAN"],

  // Reviews: "eggplant in salted egg yolk" (Cantonese-Singapore signature)
  // + "braised pork with chestnut" (Cantonese style).
  "shifang-house": ["CANTONESE"],

  // Multiple reviews: "Sichuan options if you like heat", "Sichuan style
  // beef", "ribbon noodles", "spicy braised pork belly". Heat is consistent.
  "wei-dao-jia": ["SICHUAN", "NOODLES"],

  // Reviews: "Liangpi" (a Northwest noodle dish), "spicy Chinese Sichuan
  // food", "Spicy Fish, Chicken and Prawns".
  "wiling": ["SICHUAN", "NOODLES"],

  // Review: "spicy / szechuan side", "spicy rice and noodle dishes through
  // to Cantonese style foods" — explicitly multi-regional.
  "you-mi": ["SICHUAN", "NOODLES", "CANTONESE"],

  // Web search confirms "North + South Chinese, authentic Peking duck +
  // Sichuan spicy chicken + Wenzhou/Qingtian snacks; chef says NOT fusion".
  // Two strongest regional pillars: Sichuan + Northern (Peking duck).
  "feast": ["SICHUAN", "NORTHERN"],

  // Web search: "the largest Chinese buffet in Berlin", explicitly features
  // a "Mongolian show kitchen where guests have food freshly prepared",
  // serves 5 cuisines (Chinese, Japanese, Mongolian, Thai, Vietnamese).
  "zhous-five": ["MONGOLIAN", "BBQ"],

  // Skipped intentionally — these stay untagged for manual admin pickup:
  //   china-garden   (vegan-friendly buffet, no clear region)
  //   golden-garden  (generic neighbourhood Chinese, "spicy not really spicy")
  //   kaiser-drachen (German-Chinese generalist, plum wine pitch)
  //   zhou-s-fine    (pan-Asian buffet — Reinickendorf branch dropped its
  //                   Mongolian grill, now Chinese + Thai + sushi)
};

const FIELD_ORDER = [
  "name", "tags", "address", "latitude", "longitude",
  "phone", "priceRange", "description", "logoUrl", "galleries",
  "hidden", "featured", "editorialNote", "chain", "placeId",
];

function reorderObject(obj) {
  const out = {};
  for (const k of FIELD_ORDER) if (k in obj) out[k] = obj[k];
  for (const k of Object.keys(obj)) if (!(k in out)) out[k] = obj[k];
  return out;
}

function serialise(obj) {
  return yaml.dump(reorderObject(obj), { lineWidth: 120, noRefs: true, quotingType: '"' });
}

function walk(dir) {
  const out = [];
  for (const e of readdirSync(dir)) {
    const p = join(dir, e);
    if (statSync(p).isDirectory()) out.push(...walk(p));
    else if (e.endsWith(".yaml") && !e.startsWith("_")) out.push(p);
  }
  return out;
}

const byId = new Map();
for (const f of walk(DATA_DIR)) byId.set(basename(f, ".yaml"), f);

let updated = 0;
for (const [id, tags] of Object.entries(ASSIGNMENTS)) {
  const file = byId.get(id);
  if (!file) {
    console.warn(`  ${id}: NOT FOUND`);
    continue;
  }
  const data = yaml.load(readFileSync(file, "utf-8"));
  data.tags = tags;
  writeFileSync(file, serialise(data), "utf-8");
  updated++;
  console.log(`  ${id} → [${tags.join(", ")}]`);
}
console.log(`\nUpdated ${updated} files.`);
