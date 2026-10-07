// Pre-sync validation of restaurant YAML *content*: the fields the app and the
// sync script rely on, plus cross-file uniqueness. Complements check-tags.mjs,
// which owns tag-name validity and taxonomy drift.
//
// Run from `scripts/sync-to-firestore`:   node check-data.mjs
//
// Why this exists: the app's `toRestaurant()` (FirestoreRestaurantRepository.kt)
// reads `name` / `address` as string maps and `latitude` / `longitude` as
// non-null doubles, with no per-document error handling — one malformed doc
// can break the whole restaurant list. And `name.zh` is rendered verbatim as
// the headline, so an empty one shows a blank title. The sync script only
// checks that `name` and `address` exist.
//
// Wired into tag-check.yml (every PR, including data-partner fork PRs) and
// sync-restaurants.yml (before the Firestore write). Errors fail CI; warnings
// are printed, and on GitHub Actions both show up as annotations on the PR.

import { readFileSync, readdirSync, statSync } from "fs";
import { resolve, dirname, basename, join, relative } from "path";
import { fileURLToPath } from "url";
import yaml from "js-yaml";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const REPO_ROOT = resolve(__dirname, "..", "..");
const RESTAURANTS_DIR = resolve(REPO_ROOT, "data/restaurants");

// Generous box around the Berlin city limits (52.34–52.68 N, 13.09–13.76 E).
// Mostly catches swapped lat/lng and coordinates pasted from the wrong place.
const BERLIN_BOUNDS = { minLat: 52.3, maxLat: 52.7, minLng: 13.0, maxLng: 13.8 };

// The id becomes the Firestore document id.
const ID_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
// Berlin postcodes are 10115–14199. Must be a quoted string in YAML — an
// unquoted 10625 is a number, which the app's string map can't read.
const POSTAL_CODE_PATTERN = /^1[0-4]\d{3}$/;

const KNOWN_KEYS = new Set([
  "name", "tags", "address", "latitude", "longitude", "placeId", "phone",
  "priceRange", "logoUrl", "galleries", "description", "editorialNote",
  "discountInfo", "chain", "featured", "hasDiscount", "hidden",
]);
const LOCALIZED_KEYS = new Set(["zh", "en", "de"]);

const errors = [];
const warnings = [];

function rel(file) {
  return relative(REPO_ROOT, file);
}
function fail(file, msg) { errors.push({ file, msg }); }
function warn(file, msg) { warnings.push({ file, msg }); }

const isObject = (v) => v !== null && typeof v === "object" && !Array.isArray(v);
const isNonEmptyString = (v) => typeof v === "string" && v.trim() !== "";

function walk(dir) {
  const out = [];
  for (const e of readdirSync(dir)) {
    const p = join(dir, e);
    if (statSync(p).isDirectory()) out.push(...walk(p));
    else if (e.endsWith(".yaml") && !e.startsWith("_")) out.push(p);
  }
  return out;
}

function checkOptionalString(file, data, key) {
  if (data[key] !== undefined && typeof data[key] !== "string") {
    fail(file, `\`${key}\` must be a string (quote it)`);
  }
}

function checkLocalized(file, data, key) {
  const v = data[key];
  if (v === undefined) return;
  if (!isObject(v)) {
    fail(file, `\`${key}\` must be a map of zh / en / de strings`);
    return;
  }
  for (const [lang, text] of Object.entries(v)) {
    if (!LOCALIZED_KEYS.has(lang)) fail(file, `\`${key}.${lang}\`: unknown language (use zh / en / de)`);
    else if (typeof text !== "string") fail(file, `\`${key}.${lang}\` must be a string`);
  }
}

function checkRestaurant(file, data) {
  const id = basename(file, ".yaml");
  if (!ID_PATTERN.test(id)) {
    fail(file, `file name "${id}" must be lowercase kebab-case (a-z, 0-9, -) — it becomes the Firestore id`);
  }

  // name — zh is the headline everywhere in the app.
  if (!isObject(data.name)) {
    fail(file, "`name` must be a map with zh / en");
  } else {
    if (!isNonEmptyString(data.name.zh)) {
      fail(file, "`name.zh` is empty — the app shows it as the headline. No Chinese name? Use the Latin name.");
    }
    if (!isNonEmptyString(data.name.en)) fail(file, "`name.en` is empty");
    if (data.name.de !== undefined && typeof data.name.de !== "string") fail(file, "`name.de` must be a string");
  }

  // address
  if (!isObject(data.address)) {
    fail(file, "`address` must be a map with addressLine1 / postalCode / district");
  } else {
    const a = data.address;
    if (!isNonEmptyString(a.addressLine1)) fail(file, "`address.addressLine1` is empty");
    if (typeof a.postalCode !== "string") {
      fail(file, `\`address.postalCode\` must be a quoted string, e.g. postalCode: "10625" (got ${JSON.stringify(a.postalCode)})`);
    } else if (!POSTAL_CODE_PATTERN.test(a.postalCode)) {
      fail(file, `\`address.postalCode\` "${a.postalCode}" is not a Berlin postcode`);
    }
    if (!isNonEmptyString(a.district)) fail(file, "`address.district` is empty");
    for (const key of ["addressLine2", "note"]) {
      if (a[key] !== undefined && typeof a[key] !== "string") fail(file, `\`address.${key}\` must be a string`);
    }
  }

  // coordinates
  const { latitude: lat, longitude: lng } = data;
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
    fail(file, "`latitude` and `longitude` must both be numbers");
  } else if (
    lat < BERLIN_BOUNDS.minLat || lat > BERLIN_BOUNDS.maxLat ||
    lng < BERLIN_BOUNDS.minLng || lng > BERLIN_BOUNDS.maxLng
  ) {
    fail(file, `coordinates ${lat}, ${lng} are outside Berlin — swapped latitude/longitude?`);
  }

  // tags — known-tag validity is check-tags.mjs's job; this covers the shape.
  if (data.tags === undefined || (Array.isArray(data.tags) && data.tags.length === 0)) {
    warn(file, "no tags — it won't match any tag filter");
  } else if (!Array.isArray(data.tags)) {
    fail(file, "`tags` must be a list");
  } else {
    if (new Set(data.tags).size !== data.tags.length) fail(file, "`tags` has duplicates");
    if (data.tags.length > 3) warn(file, `${data.tags.length} tags — the convention is 1–3`);
  }

  // placeId — uniqueness is checked across files below.
  if (data.placeId !== undefined && !isNonEmptyString(data.placeId)) fail(file, "`placeId` must be a non-empty string");

  for (const key of ["featured", "hasDiscount", "hidden"]) {
    if (data[key] !== undefined && typeof data[key] !== "boolean") {
      fail(file, `\`${key}\` must be true or false (unquoted)`);
    }
  }
  for (const key of ["phone", "priceRange", "logoUrl"]) checkOptionalString(file, data, key);
  for (const key of ["description", "editorialNote", "discountInfo"]) checkLocalized(file, data, key);

  if (data.discountInfo !== undefined && data.hasDiscount !== true) {
    warn(file, "`discountInfo` is set but `hasDiscount` isn't true — the text is never shown");
  }

  if (data.chain !== undefined) {
    if (!isObject(data.chain) || !isNonEmptyString(data.chain.brand)) {
      fail(file, "`chain` must be a map with a non-empty `brand` (and optional `branch`)");
    } else if (data.chain.branch !== undefined && typeof data.chain.branch !== "string") {
      fail(file, "`chain.branch` must be a string");
    }
  }

  if (data.galleries !== undefined && (!Array.isArray(data.galleries) || !data.galleries.every((g) => typeof g === "string"))) {
    fail(file, "`galleries` must be a list of URLs");
  }

  // The sync spreads every top-level key into Firestore, so a typo like
  // `hasDiscout` silently does nothing in the app.
  for (const key of Object.keys(data)) {
    if (!KNOWN_KEYS.has(key)) warn(file, `unknown field \`${key}\` — typo?`);
  }
}

// --- Per-file checks ---
const files = walk(RESTAURANTS_DIR);
const byId = new Map();
const byPlaceId = new Map();

for (const file of files) {
  let data;
  try {
    data = yaml.load(readFileSync(file, "utf-8"));
  } catch (err) {
    const where = err.mark ? ` at line ${err.mark.line + 1}` : "";
    fail(file, `YAML parse error${where}: ${err.reason ?? err.message}`);
    continue;
  }
  if (!isObject(data)) {
    fail(file, "file must contain a YAML map");
    continue;
  }
  checkRestaurant(file, data);

  const id = basename(file, ".yaml");
  byId.set(id, [...(byId.get(id) ?? []), file]);
  if (isNonEmptyString(data.placeId)) {
    byPlaceId.set(data.placeId, [...(byPlaceId.get(data.placeId) ?? []), file]);
  }
}

// --- Cross-file checks ---
// The sync uses the bare file name as the document id, so two files with the
// same name in different districts overwrite each other.
for (const [id, dupes] of byId) {
  if (dupes.length > 1) {
    for (const file of dupes) fail(file, `id "${id}" is also used by ${dupes.filter((f) => f !== file).map(rel).join(", ")}`);
  }
}
for (const [placeId, dupes] of byPlaceId) {
  if (dupes.length > 1) {
    for (const file of dupes) fail(file, `placeId ${placeId} is also used by ${dupes.filter((f) => f !== file).map(rel).join(", ")} — same place listed twice?`);
  }
}

// --- Report ---
const ANNOTATE = process.env.GITHUB_ACTIONS === "true";
function report(list, level, symbol, log) {
  for (const { file, msg } of list) {
    log(`  ${symbol} ${rel(file)}: ${msg}`);
    if (ANNOTATE) log(`::${level} file=${rel(file)}::${msg}`);
  }
}

console.log(`Restaurants: ${files.length} files checked`);
if (warnings.length) {
  console.log("\nWarnings:");
  report(warnings, "warning", "⚠", console.log);
}
if (errors.length) {
  console.error("\nErrors:");
  report(errors, "error", "✘", console.error);
  process.exit(1);
}
console.log("\nOK — restaurant data is valid.");
