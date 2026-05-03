// Pre-sync validation: catch tag drift between data/_tags.yaml (canonical
// taxonomy) and the four downstream copies (Kotlin enum, sync KNOWN_TAGS,
// admin REGIONAL_TAGS+FORMAT_TAGS, strings.xml). Also validates that every
// restaurant YAML's `tags` field references only known tags.
//
// Run from `scripts/sync-to-firestore`:   node check-tags.mjs
//
// Wired into the sync-restaurants.yml workflow so a typo'd tag fails CI
// instead of silently dropping out at sync time.

import { readFileSync, readdirSync, statSync } from "fs";
import { resolve, dirname, basename, join } from "path";
import { fileURLToPath } from "url";
import yaml from "js-yaml";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const REPO_ROOT = resolve(__dirname, "..", "..");

const PATHS = {
  manifest: resolve(REPO_ROOT, "data/_tags.yaml"),
  restaurants: resolve(REPO_ROOT, "data/restaurants"),
  kotlinEnum: resolve(REPO_ROOT, "composeApp/src/commonMain/kotlin/com/novawerk/berlinfoodmap/domain/restaurant/Tag.kt"),
  syncKnownTags: resolve(REPO_ROOT, "scripts/sync-to-firestore/index.js"),
  adminTypes: resolve(REPO_ROOT, "web-apps/admin/src/types/restaurant.ts"),
  stringsEn: resolve(REPO_ROOT, "composeApp/src/commonMain/composeResources/values/strings.xml"),
  stringsZh: resolve(REPO_ROOT, "composeApp/src/commonMain/composeResources/values-zh/strings.xml"),
};

const errors = [];
const warnings = [];

function fail(msg) { errors.push(msg); }
function warn(msg) { warnings.push(msg); }

// strings.xml has tag_family_regional / tag_family_format alongside the
// per-tag display strings. They're not tags, just filter-section headers.
const NON_TAG_STRING_KEYS = new Set(["tag_family_regional", "tag_family_format"]);

// --- Load canonical taxonomy ---
const manifest = yaml.load(readFileSync(PATHS.manifest, "utf-8"));
const REGIONAL = Object.keys(manifest.regional ?? {});
const FORMAT = Object.keys(manifest.format ?? {});
const ALL = new Set([...REGIONAL, ...FORMAT]);

console.log(`Canonical taxonomy: ${REGIONAL.length} regional + ${FORMAT.length} format = ${ALL.size} tags`);

// --- 1. Validate restaurant YAMLs use only known tags ---
function walk(dir) {
  const out = [];
  for (const e of readdirSync(dir)) {
    const p = join(dir, e);
    if (statSync(p).isDirectory()) out.push(...walk(p));
    else if (e.endsWith(".yaml") && !e.startsWith("_")) out.push(p);
  }
  return out;
}

let restaurantsChecked = 0;
let restaurantsUntagged = 0;
for (const file of walk(PATHS.restaurants)) {
  const id = basename(file, ".yaml");
  const data = yaml.load(readFileSync(file, "utf-8"));
  if (!Array.isArray(data?.tags) || data.tags.length === 0) {
    restaurantsUntagged++;
    continue;
  }
  restaurantsChecked++;
  const bad = data.tags.filter((t) => !ALL.has(t));
  if (bad.length) fail(`${id}: unknown tag(s) ${bad.join(", ")}`);
}
console.log(`Restaurants: ${restaurantsChecked} tagged, ${restaurantsUntagged} untagged`);

// --- 2. Cross-check downstream copies ---
function compareAgainst(label, downstream, expected) {
  const missing = [...expected].filter((t) => !downstream.has(t));
  const extra = [...downstream].filter((t) => !expected.has(t));
  if (missing.length) fail(`${label}: missing ${missing.join(", ")}`);
  if (extra.length) fail(`${label}: has unknown ${extra.join(", ")}`);
}
const REGIONAL_SET = new Set(REGIONAL);
const FORMAT_SET = new Set(FORMAT);

// Kotlin enum — extract identifiers between `enum class Tag {` and `}`.
{
  const src = readFileSync(PATHS.kotlinEnum, "utf-8");
  const m = src.match(/enum class Tag\s*\{([\s\S]*?)\}/);
  if (!m) fail("kotlin: cannot find `enum class Tag`");
  else {
    const body = m[1].replace(/\/\/.*$/gm, "").replace(/\/\*[\s\S]*?\*\//g, "");
    const ids = new Set(
      [...body.matchAll(/\b([A-Z][A-Z0-9_]*)\b/g)].map((x) => x[1]),
    );
    compareAgainst("kotlin enum Tag", ids, ALL);
  }
}

// Sync KNOWN_TAGS — extract between `KNOWN_TAGS = new Set([` and `]);`.
{
  const src = readFileSync(PATHS.syncKnownTags, "utf-8");
  const m = src.match(/KNOWN_TAGS\s*=\s*new\s+Set\(\[([\s\S]*?)\]\)/);
  if (!m) fail("sync index.js: cannot find KNOWN_TAGS");
  else {
    const ids = new Set(
      [...m[1].matchAll(/"([A-Z_]+)"/g)].map((x) => x[1]),
    );
    compareAgainst("sync KNOWN_TAGS", ids, ALL);
  }
}

// Admin TS — combine REGIONAL_TAGS + FORMAT_TAGS arrays.
{
  const src = readFileSync(PATHS.adminTypes, "utf-8");
  const grab = (varName) => {
    const m = src.match(new RegExp(`${varName}\\s*=\\s*\\[([\\s\\S]*?)\\]`));
    return m ? new Set([...m[1].matchAll(/"([A-Z_]+)"/g)].map((x) => x[1])) : null;
  };
  const reg = grab("REGIONAL_TAGS");
  const fmt = grab("FORMAT_TAGS");
  if (!reg || !fmt) fail("admin: cannot find REGIONAL_TAGS / FORMAT_TAGS");
  else {
    compareAgainst("admin REGIONAL_TAGS", reg, REGIONAL_SET);
    compareAgainst("admin FORMAT_TAGS", fmt, FORMAT_SET);
  }
}

// strings.xml — verify every tag has en + zh display strings.
function loadStringNames(path) {
  const src = readFileSync(path, "utf-8");
  return new Set(
    [...src.matchAll(/name="(tag_[a-z_]+)"/g)].map((m) => m[1]),
  );
}
{
  const enNames = loadStringNames(PATHS.stringsEn);
  const zhNames = loadStringNames(PATHS.stringsZh);
  for (const t of ALL) {
    const key = `tag_${t.toLowerCase()}`;
    if (!enNames.has(key)) fail(`strings.xml (en): missing ${key}`);
    if (!zhNames.has(key)) fail(`strings.xml (zh): missing ${key}`);
  }
  // Extra strings (tag_xxx that's not in manifest) are warnings — keep
  // them around so historical translations aren't lost on rename, but flag.
  for (const k of [...enNames, ...zhNames]) {
    if (NON_TAG_STRING_KEYS.has(k)) continue;
    const tag = k.slice(4).toUpperCase();
    if (!ALL.has(tag)) warn(`strings.xml: stale ${k} (not in manifest)`);
  }
}

// --- Report ---
if (warnings.length) {
  console.log("\nWarnings:");
  for (const w of warnings) console.log(`  ⚠ ${w}`);
}
if (errors.length) {
  console.error("\nErrors:");
  for (const e of errors) console.error(`  ✘ ${e}`);
  process.exit(1);
}
console.log("\nOK — all tag references are in sync.");
