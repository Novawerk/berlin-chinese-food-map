// Tag the auto-untagged restaurants by querying Google Places API for
// richer fields (primaryType, types, editorialSummary, reviews) and
// running keyword rules against the result.
//
// Two modes:
//   node tag-from-places.mjs               # dry-run: writes suggestions CSV
//   node tag-from-places.mjs --apply       # writes tags into YAMLs
//
// Optional:
//   --ids id1,id2          Restrict to specific YAML ids
//   --all                  Process ALL restaurants (not just untagged ones)
//
// Requires GOOGLE_PLACES_API_KEY in env.

import { readFileSync, writeFileSync, readdirSync, statSync } from "fs";
import { resolve, dirname, basename, join } from "path";
import { fileURLToPath } from "url";
import yaml from "js-yaml";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const REPO_ROOT = resolve(__dirname, "..", "..");
const DATA_DIR = resolve(REPO_ROOT, "data", "restaurants");
const REPORT_PATH = resolve(REPO_ROOT, "data", "places-tag-suggestions.csv");

const APPLY = process.argv.includes("--apply");
const ALL = process.argv.includes("--all");
const ID_FILTER = (() => {
  const idx = process.argv.findIndex((a) => a === "--ids");
  if (idx >= 0 && process.argv[idx + 1]) {
    return new Set(process.argv[idx + 1].split(",").map((s) => s.trim()));
  }
  return null;
})();

const API_KEY = process.env.GOOGLE_PLACES_API_KEY;
if (!API_KEY) {
  console.error("Set GOOGLE_PLACES_API_KEY in env.");
  process.exit(1);
}

const FIELD_MASK = ["primaryType", "types", "editorialSummary", "reviews"].join(",");

// --- Tag inference rules ---
// Returns { tags: Set<string>, evidence: string[] }.
const PRIMARY_TYPE_TO_TAG = {
  cantonese_restaurant: ["CANTONESE"],
  dim_sum_restaurant: ["CANTONESE", "DIM_SUM"],
  noodle_shop: ["NOODLES"],
  noodle_restaurant: ["NOODLES"],
  hot_pot_restaurant: ["HOTPOT"],
  hotpot_restaurant: ["HOTPOT"],
  barbecue_restaurant: ["BBQ"],
  vegetarian_restaurant: ["VEGETARIAN"],
  vegan_restaurant: ["VEGETARIAN"],
  taiwanese_restaurant: ["TAIWANESE"],
  shanghainese_restaurant: ["SHANGHAINESE"],
  sichuan_restaurant: ["SICHUAN"],
  szechuan_restaurant: ["SICHUAN"],
  dumpling_restaurant: ["DUMPLINGS"],
  bakery: ["BAKERY"],
  tea_house: ["TEA_HOUSE"],
};

const TEXT_RULES = [
  // [regex, tag(s), evidence label]
  [/sichuan|szechuan|sichuanesisch|szechuanesisch|mapo|麻辣|川菜|川味|重庆|chongqing|chengdu|成都/i, ["SICHUAN"], "sichuan-keyword"],
  [/cantonese|kantonesisch|yum\s?cha|粤菜|广东菜|港式|hong\s?kong-style|guangzhou/i, ["CANTONESE"], "cantonese-keyword"],
  [/dim\s?sum|dimsum|点心|饮茶|早茶/i, ["DIM_SUM", "CANTONESE"], "dim-sum-keyword"],
  [/peking\s?duck|pekingente|北京烤鸭|peking\s?ente/i, ["NORTHERN"], "peking-duck"],
  [/hot\s?pot|hotpot|huo\s?guo|火锅/i, ["HOTPOT"], "hotpot-keyword"],
  [/lanzhou|halal|清真|muslim/i, ["MUSLIM"], "halal-keyword"],
  [/xinjiang|uyghur|uygur|新疆/i, ["XINJIANG"], "xinjiang-keyword"],
  [/taiwanese|taiwan|台湾|台菜|bento|卤肉饭|bubble\s?tea/i, ["TAIWANESE"], "taiwan-keyword"],
  [/hunan|湘菜/i, ["HUNAN"], "hunan-keyword"],
  [/shanghainese|江浙|上海菜|小笼包|xiaolongbao|苏州/i, ["SHANGHAINESE"], "shanghai-keyword"],
  [/dongbei|northeastern|东北/i, ["NORTHEASTERN"], "dongbei-keyword"],
  [/vegan|vegetarian|vegetarisch|素食|vegetarisches/i, ["VEGETARIAN"], "vegan-keyword"],
  [/dumpling|jiaozi|饺子|guo\s?tie|gyoza/i, ["DUMPLINGS"], "dumpling-keyword"],
  [/noodle|nudel|拉面|ramen|拉面|hand-pulled|手工面|biang\s?biang/i, ["NOODLES"], "noodle-keyword"],
  [/malatang|mala\s?tang|麻辣烫|maocai|冒菜/i, ["MALATANG", "SICHUAN"], "malatang-keyword"],
  [/breakfast|frühstück|早餐|早午餐|brunch/i, ["BREAKFAST"], "breakfast-keyword"],
  [/tea\s?(house|bar|room)|teehaus|茶寮|功夫茶/i, ["TEA_HOUSE"], "tea-keyword"],
  [/bakery|bäcker|烘焙|jian\s?bing|煎饼/i, ["BAKERY"], "bakery-keyword"],
  [/fusion|创意中餐|新派|modern\s?chinese|现代中餐/i, ["FUSION"], "fusion-keyword"],
  [/street\s?food|imbiss|snack|小吃/i, ["STREET_FOOD"], "street-food-keyword"],
];

function inferTags(place) {
  const evidence = [];
  const tagScores = new Map(); // tag -> score (for ranking)

  function add(tags, score, label) {
    for (const t of tags) tagScores.set(t, (tagScores.get(t) ?? 0) + score);
    evidence.push(label);
  }

  // primaryType: very high confidence
  if (place.primaryType && PRIMARY_TYPE_TO_TAG[place.primaryType]) {
    add(PRIMARY_TYPE_TO_TAG[place.primaryType], 100, `primaryType=${place.primaryType}`);
  }
  // types[] array: medium confidence
  for (const t of place.types ?? []) {
    if (PRIMARY_TYPE_TO_TAG[t]) {
      add(PRIMARY_TYPE_TO_TAG[t], 50, `types[]=${t}`);
    }
  }

  // editorialSummary: high confidence (Google's own description)
  if (place.editorialSummary) {
    for (const [re, tags, label] of TEXT_RULES) {
      if (re.test(place.editorialSummary)) {
        add(tags, 40, `summary:${label}`);
      }
    }
  }

  // reviews: low confidence per match, but cumulative (if 3 reviewers
  // mention "Sichuan" that's a strong signal)
  let reviewMatchCount = 0;
  for (const review of place.reviews ?? []) {
    if (!review.text) continue;
    for (const [re, tags, label] of TEXT_RULES) {
      if (re.test(review.text)) {
        add(tags, 8, `review:${label}`);
        reviewMatchCount++;
      }
    }
  }

  // Pick everything that scored ≥ 30 (one strong source or three weak).
  const picked = [...tagScores.entries()]
    .filter(([, score]) => score >= 30)
    .sort((a, b) => b[1] - a[1])
    .map(([t]) => t);

  return { tags: picked, evidence, reviewMatchCount };
}

// --- Walk YAML files ---
function walk(dir) {
  const out = [];
  for (const e of readdirSync(dir)) {
    const p = join(dir, e);
    if (statSync(p).isDirectory()) out.push(...walk(p));
    else if (e.endsWith(".yaml") && !e.startsWith("_")) out.push(p);
  }
  return out;
}

async function fetchPlace(placeId) {
  const url = `https://places.googleapis.com/v1/places/${encodeURIComponent(placeId)}`;
  const res = await fetch(url, {
    headers: {
      "X-Goog-Api-Key": API_KEY,
      "X-Goog-FieldMask": FIELD_MASK,
    },
  });
  if (!res.ok) {
    let detail = "";
    try { detail = (await res.json())?.error?.message ?? ""; } catch {}
    throw new Error(`HTTP ${res.status}: ${detail}`);
  }
  const r = await res.json();
  return {
    primaryType: r.primaryType ?? null,
    types: r.types ?? [],
    editorialSummary: r.editorialSummary?.text ?? null,
    reviews: (r.reviews ?? []).map((rv) => ({
      text: rv.text?.text ?? null,
      rating: rv.rating ?? null,
    })),
  };
}

// --- YAML serialiser (matches the migrator's field order) ---
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

function csvEscape(s) {
  const v = String(s ?? "");
  return v.includes(",") || v.includes('"') || v.includes("\n")
    ? `"${v.replace(/"/g, '""')}"`
    : v;
}

async function main() {
  const files = walk(DATA_DIR);
  const candidates = [];

  for (const file of files) {
    const id = basename(file, ".yaml");
    if (id.startsWith("_")) continue;
    const data = yaml.load(readFileSync(file, "utf-8"));
    if (!data?.placeId) continue;

    if (ID_FILTER && !ID_FILTER.has(id)) continue;
    const isUntagged = !data.tags || data.tags.length === 0;
    if (!ALL && !ID_FILTER && !isUntagged) continue;

    candidates.push({ id, file, data });
  }

  console.log(`Querying Places API for ${candidates.length} restaurants…\n`);

  const reportRows = [
    ["id", "zh", "en", "current_tags", "primaryType", "types", "editorialSummary", "review_match_count", "suggested_tags", "evidence"],
  ];
  let willChange = 0;

  for (const { id, file, data } of candidates) {
    let place;
    try {
      place = await fetchPlace(data.placeId);
    } catch (err) {
      console.warn(`  ${id}: API ${err.message}`);
      continue;
    }
    const inferred = inferTags(place);

    const current = (data.tags ?? []).join("|");
    const suggested = inferred.tags.join("|");
    const changes = current !== suggested && inferred.tags.length > 0;
    if (changes) willChange++;

    console.log(
      `  ${id.padEnd(34)} primary=${(place.primaryType ?? "—").padEnd(22)} ` +
      `→ [${suggested || "—"}]${changes ? "  ✱" : ""}`,
    );

    reportRows.push([
      id,
      data.name?.zh ?? "",
      data.name?.en ?? "",
      current,
      place.primaryType ?? "",
      (place.types ?? []).join("|"),
      place.editorialSummary ?? "",
      inferred.reviewMatchCount,
      suggested,
      inferred.evidence.join(" / "),
    ]);

    if (APPLY && inferred.tags.length > 0) {
      const next = { ...data, tags: inferred.tags };
      writeFileSync(file, serialise(next), "utf-8");
    }
  }

  // Write CSV report
  const csv = reportRows.map((r) => r.map(csvEscape).join(",")).join("\n");
  writeFileSync(REPORT_PATH, csv, "utf-8");

  console.log(
    `\n${APPLY ? "Applied" : "Suggested"} tags for ${willChange} restaurants.`,
  );
  console.log(`Report: ${REPORT_PATH}`);
  if (!APPLY) {
    console.log(`Re-run with --apply to write the suggested tags into YAMLs.`);
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
