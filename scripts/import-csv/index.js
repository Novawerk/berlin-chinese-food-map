// Imports the master CSV ("中餐地图 Mar-3版本.csv") into per-district YAML files
// under data/restaurants/. Geocodes addresses via Nominatim with rate limiting
// and caches the results in geocode-cache.json so reruns are cheap.
//
// Usage:
//   node index.js                # generate YAMLs, geocode missing entries
//   node index.js --no-geocode   # skip geocoding (lat/lng = 0)
//   node index.js --dry-run      # print stats only, don't write files

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import yaml from "js-yaml";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(__dirname, "..", "..");
const CSV_PATH = path.resolve(REPO_ROOT, "data", "中餐地图 Mar-3版本.csv");
const OUTPUT_DIR = path.resolve(REPO_ROOT, "data", "restaurants");
const CACHE_PATH = path.resolve(__dirname, "geocode-cache.json");

const args = new Set(process.argv.slice(2));
const DO_GEOCODE = !args.has("--no-geocode");
const DRY_RUN = args.has("--dry-run");

// --- Category → CuisineType mapping ---
// CSV 类别 (col 4) → enum value used by the Kotlin app and Firestore.
const CATEGORY_MAP = {
  饭店: "GENERAL",
  小吃: "OTHER",
  面馆: "NOODLES",
  火锅: "HOTPOT",
  "麻辣烫/冒菜": "OTHER",
  素食: "OTHER",
  串烧: "BBQ",
  烧烤: "BBQ",
};

// --- District → folder slug ---
// German diacritics flattened (ö→oe, ü→ue, ß→ss). Kebab-cased.
function districtToFolder(district) {
  return district
    .trim()
    .toLowerCase()
    .replace(/ö/g, "oe")
    .replace(/ü/g, "ue")
    .replace(/ä/g, "ae")
    .replace(/ß/g, "ss")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");
}

// --- Slugify name → file id ---
function slugify(name) {
  return name
    .trim()
    .toLowerCase()
    .replace(/ö/g, "oe")
    .replace(/ü/g, "ue")
    .replace(/ä/g, "ae")
    .replace(/ß/g, "ss")
    .replace(/['’`]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");
}

// --- Parse the 店名 column ("中文/English" or pure English) ---
function parseDianMing(dianming) {
  const trimmed = dianming.trim();
  const slashIdx = trimmed.indexOf("/");
  if (slashIdx === -1) {
    return { zh: "", enFromDianMing: trimmed };
  }
  return {
    zh: trimmed.slice(0, slashIdx).trim(),
    enFromDianMing: trimmed.slice(slashIdx + 1).trim(),
  };
}

// --- Address parser ---
// Examples:
//   "Kantstraße 62, 10627 Berlin" → line1="Kantstraße 62", postal="10627"
//   "Markthalle Neun, Eisenbahnstraße 42-43, 10997 Berlin"
//     → note="Markthalle Neun", line1="Eisenbahnstraße 42-43", postal="10997"
//   "Kantini 1.OG, Bikini, Budapester Str. 38-50, 10787 Berlin"
//     → note="Kantini 1.OG, Bikini", line1="Budapester Str. 38-50", postal="10787"
function parseAddress(raw) {
  const trimmed = raw.trim();
  const m = trimmed.match(/^(.*?),\s*(\d{5})\s*Berlin\s*$/);
  if (!m) {
    return { addressLine1: trimmed, postalCode: "", note: null };
  }
  const rest = m[1].trim();
  const postal = m[2];
  const segments = rest.split(",").map((s) => s.trim()).filter(Boolean);
  if (segments.length === 1) {
    return { addressLine1: segments[0], postalCode: postal, note: null };
  }
  // Last segment is the street with house number; earlier segments are venue/building.
  const line1 = segments[segments.length - 1];
  const note = segments.slice(0, -1).join(", ");
  return { addressLine1: line1, postalCode: postal, note };
}

// --- Geocoding via Nominatim ---
function loadCache() {
  if (!fs.existsSync(CACHE_PATH)) return {};
  try {
    return JSON.parse(fs.readFileSync(CACHE_PATH, "utf-8"));
  } catch {
    return {};
  }
}

function saveCache(cache) {
  fs.writeFileSync(CACHE_PATH, JSON.stringify(cache, null, 2));
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

async function geocode(query, cache) {
  if (cache[query]) return cache[query];
  const url =
    "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" +
    encodeURIComponent(query);
  const res = await fetch(url, {
    headers: {
      // Nominatim usage policy requires a descriptive UA.
      "User-Agent":
        "berlin-chinese-food-map/1.0 (https://berlinfoodmap.novawerk.io; juhaodong@gmail.com)",
    },
  });
  if (!res.ok) {
    cache[query] = null;
    return null;
  }
  const data = await res.json();
  if (!Array.isArray(data) || data.length === 0) {
    cache[query] = null;
    return null;
  }
  const hit = {
    lat: parseFloat(data[0].lat),
    lon: parseFloat(data[0].lon),
  };
  cache[query] = hit;
  return hit;
}

// --- CSV parser (semicolon-delimited, no quoted fields in this file) ---
function parseCsv(content) {
  const lines = content.split(/\r?\n/).filter((l) => l.trim().length > 0);
  // Drop header
  lines.shift();
  return lines.map((line) => {
    const fields = line.split(";");
    return {
      index: fields[0]?.trim(),
      dianming: fields[1] ?? "",
      en: (fields[2] ?? "").trim(),
      category: (fields[3] ?? "").trim(),
      isChain: (fields[4] ?? "").trim() === "是",
      address: (fields[5] ?? "").trim(),
      district: (fields[6] ?? "").trim(),
      // 推荐备注 (general recommendation notes — currently empty in source)
      note: (fields[7] ?? "").trim(),
      // 柏林慢慢游甄选20 (Berlin SlowTravel curated Top-20 highlight tags,
      // joined with ｜ — populated for ~20 featured spots).
      featuredNote: (fields[8] ?? "").trim(),
      featured: (fields[8] ?? "").trim().length > 0,
    };
  });
}

// --- Build a restaurant record from a CSV row ---
function buildRestaurant(row) {
  const { zh, enFromDianMing } = parseDianMing(row.dianming);
  // Prefer the EN column when it's substantial, else fall back to slash-form English.
  let en = row.en;
  if (!en || en.length < 3 || (enFromDianMing && en !== enFromDianMing && en.length < enFromDianMing.length / 2)) {
    en = enFromDianMing || row.en;
  }
  if (!en) en = zh;
  const zhFinal = zh || en;

  const cuisineType = CATEGORY_MAP[row.category] ?? "OTHER";
  const folder = districtToFolder(row.district);
  const id = slugify(en);

  const addr = parseAddress(row.address);

  const yamlData = {
    name: { en, zh: zhFinal },
    cuisineType,
    address: {
      addressLine1: addr.addressLine1,
      ...(addr.note ? { note: addr.note } : {}),
      postalCode: addr.postalCode,
      district: row.district,
    },
    latitude: 0,
    longitude: 0,
  };

  // Combine generic recommendation notes (field 7) with the editorial Top-20
  // tag string (field 8). At most one is populated in practice. Drop "TBD"
  // placeholders since they aren't real data.
  const isPlaceholder = (s) => /^tbd$/i.test(s.trim());
  const descParts = [row.note, row.featuredNote]
    .filter(Boolean)
    .filter((s) => !isPlaceholder(s));
  const descZh = descParts.join(" ｜ ");
  if (descZh) {
    // No translation pipeline yet — mirror the Chinese note into en so the
    // Localizable contract holds. Translators can refine later.
    yamlData.description = { en: descZh, zh: descZh };
  }

  return { id, folder, yamlData, geocodeQuery: addressToGeocodeQuery(addr, row.address) };
}

function addressToGeocodeQuery(addr, raw) {
  if (addr.addressLine1 && addr.postalCode) {
    return `${addr.addressLine1}, ${addr.postalCode} Berlin, Germany`;
  }
  return raw;
}

// --- Main ---
async function main() {
  if (!fs.existsSync(CSV_PATH)) {
    console.error(`CSV not found: ${CSV_PATH}`);
    process.exit(1);
  }

  const csv = fs.readFileSync(CSV_PATH, "utf-8");
  const rows = parseCsv(csv);
  console.log(`Parsed ${rows.length} CSV rows`);

  const restaurants = rows.map(buildRestaurant);

  // Resolve ID collisions: when two different restaurants slug to the same id,
  // append the district folder, then a numeric suffix if still colliding.
  const usedIds = new Set();
  for (const r of restaurants) {
    if (!usedIds.has(r.id)) {
      usedIds.add(r.id);
      continue;
    }
    const withDistrict = `${r.id}-${r.folder}`;
    if (!usedIds.has(withDistrict)) {
      console.warn(`ID collision: ${r.id} → ${withDistrict}`);
      r.id = withDistrict;
      usedIds.add(withDistrict);
      continue;
    }
    let n = 2;
    while (usedIds.has(`${r.id}-${n}`)) n++;
    const numbered = `${r.id}-${n}`;
    console.warn(`ID collision: ${r.id} → ${numbered}`);
    r.id = numbered;
    usedIds.add(numbered);
  }
  const unique = restaurants;

  // Stats
  const folderCounts = {};
  const cuisineCounts = {};
  for (const r of unique) {
    folderCounts[r.folder] = (folderCounts[r.folder] ?? 0) + 1;
    cuisineCounts[r.yamlData.cuisineType] =
      (cuisineCounts[r.yamlData.cuisineType] ?? 0) + 1;
  }
  console.log("\nBy folder:");
  for (const [k, v] of Object.entries(folderCounts).sort()) {
    console.log(`  ${k}: ${v}`);
  }
  console.log("\nBy cuisineType:");
  for (const [k, v] of Object.entries(cuisineCounts).sort()) {
    console.log(`  ${k}: ${v}`);
  }

  if (DRY_RUN) {
    console.log("\n--dry-run: not writing files");
    return;
  }

  // Geocode
  const cache = loadCache();
  let geocoded = 0;
  let geocodeMisses = 0;
  if (DO_GEOCODE) {
    console.log("\nGeocoding (1 req/sec)…");
    for (let i = 0; i < unique.length; i++) {
      const r = unique[i];
      const q = r.geocodeQuery;
      const cached = cache[q] !== undefined;
      if (!cached) {
        await sleep(1100); // Nominatim usage policy: ≤ 1 req/sec
      }
      const hit = await geocode(q, cache);
      if (hit) {
        r.yamlData.latitude = hit.lat;
        r.yamlData.longitude = hit.lon;
        geocoded++;
      } else {
        geocodeMisses++;
        console.warn(`  miss [${i + 1}/${unique.length}]: ${r.id} — ${q}`);
      }
      if (!cached && i % 25 === 24) saveCache(cache);
    }
    saveCache(cache);
    console.log(`Geocoded ${geocoded}, missed ${geocodeMisses}`);
  } else {
    // Apply cached values if present (lat/lng remains 0 otherwise)
    for (const r of unique) {
      const hit = cache[r.geocodeQuery];
      if (hit) {
        r.yamlData.latitude = hit.lat;
        r.yamlData.longitude = hit.lon;
      }
    }
  }

  // Write YAMLs
  let written = 0;
  for (const r of unique) {
    const dir = path.join(OUTPUT_DIR, r.folder);
    fs.mkdirSync(dir, { recursive: true });
    const file = path.join(dir, `${r.id}.yaml`);
    const out = yaml.dump(r.yamlData, {
      lineWidth: 120,
      noRefs: true,
      sortKeys: false,
    });
    fs.writeFileSync(file, out);
    written++;
  }
  console.log(`\nWrote ${written} YAML files to ${OUTPUT_DIR}`);
}

main().catch((err) => {
  console.error("Import failed:", err);
  process.exit(1);
});
