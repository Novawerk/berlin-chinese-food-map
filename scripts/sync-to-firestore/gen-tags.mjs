// Code-gen the downstream tag mirrors from data/_tags.yaml (the canonical
// taxonomy). Rewrites the content between `@gen:tags:*` / `@gen:tags:end`
// fences in:
//   - scripts/sync-to-firestore/index.js          (KNOWN_TAGS set)
//   - web-apps/admin/src/types/restaurant.ts       (REGIONAL_TAGS + FORMAT_TAGS)
//   - composeApp/.../values/strings.xml            (tag display names, en)
//   - composeApp/.../values-zh/strings.xml         (tag display names, zh)
//
// The Kotlin `Tag` enum and the strings.xml `*_desc` editorial strings stay
// hand-written; `check-tags.mjs` guards the enum against drift, and the
// descriptions have no source in the YAML.
//
// Usage (run from scripts/sync-to-firestore):
//   node gen-tags.mjs            # rewrite the fenced regions in place
//   node gen-tags.mjs --check    # exit 1 if any file would change (CI guard)

import { readFileSync, writeFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import yaml from "js-yaml";

const __dirname = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(__dirname, "..", "..");

const PATHS = {
  manifest: resolve(REPO_ROOT, "data/_tags.yaml"),
  syncKnownTags: resolve(REPO_ROOT, "scripts/sync-to-firestore/index.js"),
  adminTypes: resolve(REPO_ROOT, "web-apps/admin/src/types/restaurant.ts"),
  stringsEn: resolve(REPO_ROOT, "composeApp/src/commonMain/composeResources/values/strings.xml"),
  stringsZh: resolve(REPO_ROOT, "composeApp/src/commonMain/composeResources/values-zh/strings.xml"),
};

// --- Load canonical taxonomy (order-preserving) ---
const manifest = yaml.load(readFileSync(PATHS.manifest, "utf-8"));
const regional = Object.entries(manifest.regional ?? {});
const format = Object.entries(manifest.format ?? {});
const all = [...regional, ...format];

const xmlEscape = (s) =>
  s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

// --- Generators (return arrays of body lines, indentation included) ---
function knownTagsBody() {
  return all.map(([key]) => `  "${key}",`);
}

function adminBody() {
  const arr = (name, entries) => [
    `export const ${name} = [`,
    ...entries.map(([key]) => `  "${key}",`),
    `] as const;`,
  ];
  return [...arr("REGIONAL_TAGS", regional), "", ...arr("FORMAT_TAGS", format)];
}

function xmlNames(entries, locale) {
  return entries.map(([key, val]) => {
    const name = `tag_${key.toLowerCase()}`;
    return `    <string name="${name}">${xmlEscape(val[locale])}</string>`;
  });
}

// --- Region rewriting: replace lines strictly between the two markers,
//     keeping the marker lines themselves. Idempotent. ---
function replaceRegion(text, startNeedle, endNeedle, bodyLines) {
  const lines = text.split("\n");
  const start = lines.findIndex((l) => l.includes(startNeedle));
  if (start === -1) throw new Error(`marker not found: ${startNeedle}`);
  const end = lines.findIndex((l, i) => i > start && l.includes(endNeedle));
  if (end === -1) throw new Error(`end marker not found after: ${startNeedle}`);
  return [...lines.slice(0, start + 1), ...bodyLines, ...lines.slice(end)].join("\n");
}

const END = "@gen:tags:end";

const FILES = [
  {
    path: PATHS.syncKnownTags,
    regions: [["@gen:tags:known-tags", END, knownTagsBody()]],
  },
  {
    path: PATHS.adminTypes,
    regions: [["@gen:tags:admin", END, adminBody()]],
  },
  {
    path: PATHS.stringsEn,
    regions: [
      ["@gen:tags:names-regional", END, xmlNames(regional, "en")],
      ["@gen:tags:names-format", END, xmlNames(format, "en")],
    ],
  },
  {
    path: PATHS.stringsZh,
    regions: [
      ["@gen:tags:names-regional", END, xmlNames(regional, "zh")],
      ["@gen:tags:names-format", END, xmlNames(format, "zh")],
    ],
  },
];

// --- Run ---
const checkOnly = process.argv.includes("--check");
let drift = false;

for (const { path, regions } of FILES) {
  const before = readFileSync(path, "utf-8");
  let after = before;
  for (const [startNeedle, endNeedle, body] of regions) {
    after = replaceRegion(after, startNeedle, endNeedle, body);
  }
  if (after === before) continue;
  if (checkOnly) {
    drift = true;
    console.error(`✘ out of date: ${path.replace(REPO_ROOT + "/", "")}`);
  } else {
    writeFileSync(path, after);
    console.log(`✓ wrote ${path.replace(REPO_ROOT + "/", "")}`);
  }
}

if (checkOnly && drift) {
  console.error("\nTag mirrors are stale. Run `npm run gen:tags` and commit the result.");
  process.exit(1);
}
console.log(
  checkOnly ? "OK — tag mirrors are up to date." : `Generated ${all.length} tags into ${FILES.length} files.`,
);
