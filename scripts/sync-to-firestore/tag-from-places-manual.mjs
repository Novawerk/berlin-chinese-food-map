// Tags derived from a manual read of Places API editorialSummary + reviews
// for the restaurants where the auto-suggester underscored or mis-classified.
// Companion to tag-from-places.mjs — runs after a dry-run review.

import { readFileSync, writeFileSync, readdirSync, statSync } from "fs";
import { resolve, dirname, basename, join } from "path";
import { fileURLToPath } from "url";
import yaml from "js-yaml";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const REPO_ROOT = resolve(__dirname, "..", "..");
const DATA_DIR = resolve(REPO_ROOT, "data", "restaurants");

// Each entry justified by what Google's editorialSummary or review text says.
// See data/places-tag-suggestions.csv for the raw evidence each was based on.
const ASSIGNMENTS = {
  // Google `types[]` includes noodle_shop + dim_sum_restaurant + cantonese_restaurant;
  // editorial summary mentions "all-you-can-eat hot pot".
  "chens-wok": ["NOODLES", "CANTONESE", "DIM_SUM", "HOTPOT"],

  // Editorial: "A mix of traditional & fusion Chinese dishes ... lunch buffet".
  "chichikan": ["FUSION"],

  // Editorial: "Noodle soups, dumplings & modern Chinese cuisine".
  "lychee": ["NOODLES", "DUMPLINGS", "FUSION"],

  // Editorial: "Dim sum & classic Szechuan & Shanghai cuisine". Override
  // the suggester which auto-added CANTONESE from the dim-sum keyword —
  // the summary is explicit that the regional cuisines are Sichuan + Shanghai.
  "tangs-kantine": ["SICHUAN", "SHANGHAINESE", "DIM_SUM"],

  // Reviews repeatedly mention "Xiao long bao" + "scallion oil noodles" +
  // "homemade dumplings, buns and noodles".
  "jing-yang": ["NOODLES", "DUMPLINGS", "SHANGHAINESE"],

  // Editorial: "housemade Chinese cuisine, plus tasting menus & cooking lessons".
  // Reviews: "dumplings in chili oil", "Xiao long bao", "very, very spicy".
  "wunderbau": ["SICHUAN", "DUMPLINGS", "FUSION"],

  // Reviews: "La Zi Ji ... easily one of the best in Berlin". 辣子鸡 is a
  // Sichuan staple. (Dim sum mention exists but isn't the headline.)
  "mr-liu": ["SICHUAN"],
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

function main() {
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
}

main();
