// One-shot migration: convert each restaurant YAML from `cuisineType: SINGLE`
// to `tags: [LIST]`, and inject `featured` / `editorialNote` / `chain`
// fields by joining against the original community-handed-over CSV
// (`data/中餐地图 Mar-3版本.csv`).
//
// Run from `scripts/sync-to-firestore`:   node migrate-to-tags.mjs
//
// Outputs:
//   - rewrites every YAML in `data/restaurants/**/*.yaml`
//   - prints a summary
//   - emits `data/migration-report.csv` for human review
//
// Idempotent: running twice produces no diff. Safe to re-run.

import { readFileSync, writeFileSync, appendFileSync } from "fs";
import { resolve, dirname, basename } from "path";
import { fileURLToPath } from "url";
import { glob } from "glob";
import yaml from "js-yaml";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const REPO_ROOT = resolve(__dirname, "..", "..");
const DATA_DIR = resolve(REPO_ROOT, "data", "restaurants");
const CSV_PATH = resolve(REPO_ROOT, "data", "中餐地图 Mar-3版本.csv");
const REPORT_PATH = resolve(REPO_ROOT, "data", "migration-report.csv");

// --- Tag taxonomy (must match Kotlin Tag enum + admin TS Tag union) ---
const REGIONAL = new Set([
  "SICHUAN", "CANTONESE", "NORTHERN", "NORTHEASTERN",
  "SHANGHAINESE", "HUNAN", "XINJIANG", "TAIWANESE", "MUSLIM",
]);
const FORMAT = new Set([
  "HOTPOT", "BBQ", "NOODLES", "DUMPLINGS", "DIM_SUM", "MALATANG",
  "VEGETARIAN", "BREAKFAST", "TEA_HOUSE", "BAKERY", "STREET_FOOD", "FUSION",
]);
const ALL_TAGS = new Set([...REGIONAL, ...FORMAT]);

// --- CSV loading (semicolon-separated, UTF-8, has headerless extra columns) ---
function parseCsv(raw) {
  const lines = raw.replace(/\r/g, "").split("\n");
  const rows = [];
  for (let i = 1; i < lines.length; i++) {
    const line = lines[i];
    if (!line.trim()) continue;
    const cells = line.split(";");
    const id = cells[0]?.trim();
    const nameRaw = cells[1]?.trim() ?? "";
    const enRaw = cells[2]?.trim() ?? "";
    const category = cells[3]?.trim() ?? "";
    const isChain = cells[4]?.trim() === "是";
    const address = cells[5]?.trim() ?? "";
    const ortsteil = cells[6]?.trim() ?? "";
    const note = cells[7]?.trim() ?? "";
    const editorial = cells[8]?.trim() ?? "";
    if (!nameRaw) continue;

    // CSV "店名" can be either "中文/EN" or just one of them. Split on "/".
    let zh = nameRaw, en = enRaw;
    if (nameRaw.includes("/")) {
      const [a, b] = nameRaw.split("/", 2).map((s) => s.trim());
      zh = a;
      if (!en) en = b;
    }

    rows.push({ id, zh, en, category, isChain, address, ortsteil, note, editorial });
  }
  return rows;
}

// Strip "(branch)" suffix and return { base, branch } — used to detect
// chain siblings even when the CSV `连锁` column disagrees between branches.
function splitBranch(name) {
  const m = name.match(/^(.*?)\s*\(([^()]+)\)\s*$/);
  if (!m) return { base: name.trim(), branch: null };
  return { base: m[1].trim(), branch: m[2].trim() };
}

// --- Tag inference from any text blob (zh + en + editorial + category) ---
function inferTags({ zh, en, category, editorial }) {
  const text = [zh, en, editorial].filter(Boolean).join(" ");
  const lc = text.toLowerCase();
  const tags = new Set();

  // Region — keyword scan, highest confidence wins. We do NOT cap to 1
  // because some places are clearly multi-region (rare but allowed).
  if (/[川蜀椒]|麻辣|sichuan|szechuan|chongqing|重庆|成都/i.test(text)) tags.add("SICHUAN");
  if (/[粤广港]|cantonese|hong\s?kong|茶餐厅|烧腊|hong\s?kong|hk\b/i.test(lc)) tags.add("CANTONESE");
  if (/北京|peking|烤鸭|京味|京菜|\bduck\b/i.test(lc)) tags.add("NORTHERN");
  if (/东北|dongbei|northeast/i.test(lc)) tags.add("NORTHEASTERN");
  if (/上海|江南|苏他|杭|shanghai|jiangnan|小笼/i.test(text)) tags.add("SHANGHAINESE");
  if (/湘|hunan/i.test(text)) tags.add("HUNAN");
  if (/新疆|xinjiang|uyghur|uygur/i.test(lc)) tags.add("XINJIANG");
  if (/^台|台湾|台北|台式|taiwan|台菜|\btw\b|卤肉/i.test(text)) tags.add("TAIWANESE");
  if (/兰州|lanzhou|清真|halal|拉面/i.test(lc)) {
    tags.add("MUSLIM");
    tags.add("NOODLES");
  }

  // Format — derived primarily from CSV `类别`, then enhanced with name/note.
  switch (category) {
    case "面馆": tags.add("NOODLES"); break;
    case "火锅": tags.add("HOTPOT"); break;
    case "麻辣烫/冒菜":
      tags.add("MALATANG");
      tags.add("SICHUAN");
      break;
    case "串烧":
    case "烧烤":
      tags.add("BBQ");
      break;
    case "素食": tags.add("VEGETARIAN"); break;
    case "小吃": tags.add("STREET_FOOD"); break;
    case "饭店":
      // No format tag — banquet-style sit-down. Region tag (if detected) carries.
      break;
  }

  // Format keyword overrides (work on names like "饺" / "Dumpling" / "茶寮")
  if (/饺|dumpling|jiaozi/i.test(lc)) tags.add("DUMPLINGS");
  if (/[面麺]|nudel|noodle|mian\b|ramen/i.test(lc)) tags.add("NOODLES");
  if (/火锅|hotpot/i.test(lc)) tags.add("HOTPOT");
  if (/麻辣烫|冒菜|malatang|maocai|mala\s?house/i.test(lc)) tags.add("MALATANG");
  if (/[串][烧烤]|barbecue|\bbbq\b|grill/i.test(lc)) tags.add("BBQ");
  if (/点心|dim\s?sum|早茶|饮茶|茶餐厅/i.test(lc)) tags.add("DIM_SUM");
  if (/素食|蔬食|\bvegan\b|\bvegetarian\b|kamala/i.test(lc)) tags.add("VEGETARIAN");
  if (/早餐|breakfast/i.test(lc)) tags.add("BREAKFAST");
  if (/^茶|茶寮|茶缘|茶屋|tea\s?(house|bar)|teehaus|teegebäck/i.test(text)) tags.add("TEA_HOUSE");
  if (/煎饼|bing\b|jian\s?bing|bakery|bäcker|烘焙/i.test(lc)) tags.add("BAKERY");
  if (/便利|便当|便利店|market\/bistro/i.test(lc)) tags.add("STREET_FOOD");
  if (/fusion|创意中餐|现代中餐|新派|modern\s?chinese/i.test(lc)) tags.add("FUSION");

  // Stable sort: regional first, then format, alphabetical within bucket.
  const arr = [...tags].filter((t) => ALL_TAGS.has(t));
  arr.sort((a, b) => {
    const ra = REGIONAL.has(a) ? 0 : 1;
    const rb = REGIONAL.has(b) ? 0 : 1;
    if (ra !== rb) return ra - rb;
    return a.localeCompare(b);
  });
  return arr;
}

// --- Match a CSV row to a YAML by name (best-effort) ---
function buildCsvIndex(csvRows) {
  const byZh = new Map();
  const byEn = new Map();
  const byBaseZh = new Map(); // matches "wen-cheng-charlottenburg" → 5 candidates
  for (const row of csvRows) {
    if (row.zh) {
      if (!byZh.has(row.zh)) byZh.set(row.zh, []);
      byZh.get(row.zh).push(row);
    }
    if (row.en) {
      const key = row.en.toLowerCase();
      if (!byEn.has(key)) byEn.set(key, []);
      byEn.get(key).push(row);
    }
    const baseEn = splitBranch(row.en || "").base.toLowerCase();
    if (baseEn) {
      if (!byBaseZh.has(baseEn)) byBaseZh.set(baseEn, []);
      byBaseZh.get(baseEn).push(row);
    }
  }
  return { byZh, byEn, byBaseZh };
}

function matchCsvRow(yamlData, idx) {
  const zh = yamlData.name?.zh;
  const en = yamlData.name?.en;

  // Exact ZH match (most reliable, since we control the data)
  if (zh && idx.byZh.has(zh)) {
    const candidates = idx.byZh.get(zh);
    if (candidates.length === 1) return candidates[0];
    // Disambiguate by EN match (chains share zh names)
    const enMatch = candidates.find(
      (r) => r.en && en && r.en.toLowerCase() === en.toLowerCase(),
    );
    if (enMatch) return enMatch;
    return candidates[0];
  }
  // Exact EN match
  if (en) {
    const candidates = idx.byEn.get(en.toLowerCase());
    if (candidates?.length === 1) return candidates[0];
    if (candidates?.length) return candidates[0];
  }
  return null;
}

// --- Brand grouping for chain detection ---
function buildBrandGroups(csvRows) {
  // Strip parens, group by base ZH name; if 2+ entries share a base, they're branches.
  const groups = new Map();
  for (const row of csvRows) {
    const base = splitBranch(row.zh || row.en || "").base;
    if (!base) continue;
    if (!groups.has(base)) groups.set(base, []);
    groups.get(base).push(row);
  }
  return groups;
}

function deriveChainInfo(csvRow, brandGroups) {
  if (!csvRow) return null;

  const baseFromZh = splitBranch(csvRow.zh || "");
  const baseFromEn = splitBranch(csvRow.en || "");
  const sameBaseGroup = brandGroups.get(baseFromZh.base) ?? [];

  const isExplicit = csvRow.isChain;
  const hasSiblings = sameBaseGroup.length > 1;

  if (!isExplicit && !hasSiblings) return null;

  // Brand: prefer a clean EN base (no parens), fall back to ZH base.
  const brand = (baseFromEn.base && baseFromEn.base !== csvRow.en)
    ? baseFromEn.base
    : baseFromZh.base || baseFromEn.base || csvRow.zh;

  const branch = baseFromEn.branch || baseFromZh.branch || null;

  if (!brand) return null;
  return branch ? { brand, branch } : { brand };
}

// --- Sequential YAML serialiser ---
// js-yaml's defaults preserve key insertion order; we control the order here.
const FIELD_ORDER = [
  "name", "tags", "address", "latitude", "longitude",
  "phone", "priceRange", "description", "logoUrl", "galleries",
  "hidden", "featured", "editorialNote", "chain", "placeId",
];

function reorderObject(obj) {
  const out = {};
  for (const key of FIELD_ORDER) {
    if (key in obj) out[key] = obj[key];
  }
  // Preserve any unknown keys (shouldn't happen, but defensive)
  for (const key of Object.keys(obj)) {
    if (!(key in out)) out[key] = obj[key];
  }
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

// --- Main ---
async function main() {
  const csvRaw = readFileSync(CSV_PATH, "utf-8");
  const csvRows = parseCsv(csvRaw);
  const idx = buildCsvIndex(csvRows);
  const brandGroups = buildBrandGroups(csvRows);

  console.log(`Loaded ${csvRows.length} CSV rows`);

  const files = await glob(`${DATA_DIR}/**/*.yaml`, {
    ignore: ["**/_schema.yaml", "**/_example.yaml"],
  });

  const reportRows = [
    ["id", "zh", "en", "district", "old_cuisine", "new_tags", "featured", "editorialNote_zh", "chain_brand", "chain_branch", "csv_matched"],
  ];

  let written = 0;
  let untagged = 0;
  let featuredCount = 0;
  let chainCount = 0;
  let csvMatched = 0;

  for (const file of files) {
    const id = basename(file, ".yaml");
    if (id.startsWith("_")) continue; // skip _schema, _example, etc.
    const raw = readFileSync(file, "utf-8");
    const data = yaml.load(raw);
    if (!data || !data.name) continue;

    const csvRow = matchCsvRow(data, idx);
    if (csvRow) csvMatched++;

    // Pull editorial blob from CSV when available — feeds tag inference too.
    const editorial = csvRow?.editorial || "";
    const category = csvRow?.category || "";

    const tags = inferTags({
      zh: data.name.zh,
      en: data.name.en,
      category,
      editorial,
    });
    if (tags.length === 0) untagged++;

    const featured = !!editorial && editorial !== "TBD" && editorial.trim() !== "";
    if (featured) featuredCount++;

    const chain = deriveChainInfo(csvRow, brandGroups);
    if (chain) chainCount++;

    const oldCuisine = data.cuisineType || "";

    // Build the new doc — start from the existing doc, drop cuisineType,
    // add new fields. Preserve everything else.
    const next = { ...data };
    delete next.cuisineType;
    next.tags = tags;
    if (featured) {
      next.featured = true;
      next.editorialNote = { zh: editorial, en: "" };
    }
    if (chain) next.chain = chain;

    writeFileSync(file, serialise(next), "utf-8");
    written++;

    reportRows.push([
      id,
      data.name.zh || "",
      data.name.en || "",
      data.address?.district || "",
      oldCuisine,
      tags.join("|"),
      featured ? "yes" : "",
      featured ? editorial : "",
      chain?.brand || "",
      chain?.branch || "",
      csvRow ? "yes" : "no",
    ]);
  }

  // Emit report
  const csvOut = reportRows.map((r) =>
    r.map((c) => {
      const s = String(c ?? "");
      return s.includes(",") || s.includes('"') ? `"${s.replace(/"/g, '""')}"` : s;
    }).join(","),
  ).join("\n");
  writeFileSync(REPORT_PATH, csvOut, "utf-8");

  console.log(`\nMigration complete:`);
  console.log(`  Written:        ${written}`);
  console.log(`  CSV matched:    ${csvMatched} / ${written}`);
  console.log(`  Featured:       ${featuredCount}`);
  console.log(`  Chains:         ${chainCount}`);
  console.log(`  Untagged:       ${untagged} (review report)`);
  console.log(`  Report:         ${REPORT_PATH}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
