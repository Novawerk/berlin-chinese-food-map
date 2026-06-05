import { initializeApp, cert } from "firebase-admin/app";
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
import { readFileSync, writeFileSync, appendFileSync } from "fs";
import { glob } from "glob";
import { basename, dirname } from "path";
import yaml from "js-yaml";

// --- Config ---
const DATA_DIR = "../../data/restaurants";
const COLLECTION = "restaurants";
const PLACES_REFRESH_MS = 14 * 24 * 60 * 60 * 1000; // 14 days
const MAX_PHOTOS = 10;
const PHOTO_MAX_WIDTH = 1600;
// Places API (New) — camelCase field mask. The legacy API is being deprecated.
// `primaryType` / `types` / `editorialSummary` / `reviews` feed the tag
// suggestor (scripts/sync-to-firestore/tag-from-places.mjs); they're cheap
// to include in the same Place Details call and let downstream auto-tagging
// run from cached data instead of issuing a second round of API calls.
const PLACES_FIELD_MASK = [
  "id",
  "rating",
  "userRatingCount",
  "currentOpeningHours.weekdayDescriptions",
  "currentOpeningHours.periods",
  "websiteUri",
  "googleMapsUri",
  "nationalPhoneNumber",
  "internationalPhoneNumber",
  "formattedAddress",
  "photos.name",
  "primaryType",
  "types",
  "editorialSummary",
  "reviews",
].join(",");

const DRY_RUN = process.argv.includes("--dry-run");
const RESOLVE_PLACE_IDS = process.argv.includes("--resolve-place-ids");
const RESOLVE_DRY_RUN = process.argv.includes("--resolve-dry-run");
// By default we re-fetch Place Details on the cache cycle but reuse already-
// uploaded photo URLs to avoid the bulk of the recurring Storage cost. Pass
// --refresh-photos to force a re-upload (e.g. if a restaurant's photos are
// stale or you want to refresh galleries).
const REFRESH_PHOTOS = process.argv.includes("--refresh-photos");
// Bypass the 14-day Place Details cache — useful when recovering from a
// failed batch (e.g. Storage was disabled) or when a schema change requires
// re-reading every place.
const FORCE_REFRESH = process.argv.includes("--force-refresh");
const RESTAURANT_ID_FILTER = (() => {
  const idx = process.argv.findIndex((a) => a === "--ids");
  if (idx >= 0 && process.argv[idx + 1]) {
    return new Set(process.argv[idx + 1].split(",").map((s) => s.trim()));
  }
  return null;
})();
// How far the Places match can be from the YAML lat/lng before we treat it
// as a probable mismatch (different branch / chain / wrong restaurant).
const RESOLVE_MAX_DISTANCE_METERS = 500;

// --- Init Firebase Admin ---
const serviceAccount = process.env.FIREBASE_SERVICE_ACCOUNT_JSON
  ? JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON)
  : null;

const NEEDS_FIRESTORE = !DRY_RUN && !RESOLVE_PLACE_IDS && !RESOLVE_DRY_RUN;
if (!serviceAccount && NEEDS_FIRESTORE) {
  console.error(
    "Error: FIREBASE_SERVICE_ACCOUNT_JSON env var is required " +
      "(or use --dry-run / --resolve-place-ids)",
  );
  process.exit(1);
}

// Newer Firebase projects ship `${projectId}.firebasestorage.app`; legacy
// ones used `${projectId}.appspot.com`. We require the explicit env var to
// avoid silently uploading to a bucket that doesn't exist.
const STORAGE_BUCKET = process.env.FIREBASE_STORAGE_BUCKET || null;

let db = null;
let storageBucket = null;
if (serviceAccount) {
  initializeApp({
    credential: cert(serviceAccount),
    storageBucket: STORAGE_BUCKET,
  });
  db = getFirestore();
  if (STORAGE_BUCKET) storageBucket = getStorage().bucket();
}

const PLACES_API_KEY = process.env.GOOGLE_PLACES_API_KEY || null;
if (!PLACES_API_KEY) {
  console.warn(
    "Warning: GOOGLE_PLACES_API_KEY not set — placeId fields will be ignored.",
  );
}

// --- Defaults ---
const DEFAULTS = {
  "address.city": "Berlin",
  "address.country": "Germany",
  hidden: false,
  galleries: [],
  visitCount: 0,
  viewCount: 0,
};

// Generated from data/_tags.yaml — do not edit by hand. Run `npm run gen:tags`.
const KNOWN_TAGS = new Set([
  // @gen:tags:known-tags
  "SICHUAN",
  "CANTONESE",
  "NORTHERN",
  "NORTHEASTERN",
  "SHANGHAINESE",
  "HUNAN",
  "XINJIANG",
  "TAIWANESE",
  "MUSLIM",
  "MONGOLIAN",
  "HOTPOT",
  "BBQ",
  "NOODLES",
  "DUMPLINGS",
  "DIM_SUM",
  "MALATANG",
  "VEGETARIAN",
  "BREAKFAST",
  "TEA_HOUSE",
  "BAKERY",
  "STREET_FOOD",
  "FUSION",
  // @gen:tags:end
]);

// --- Load YAML files ---
async function loadRestaurants() {
  const files = await glob(`${DATA_DIR}/**/*.yaml`, {
    ignore: ["**/_schema.yaml"],
  });

  const restaurants = [];

  for (const file of files) {
    const id = basename(file, ".yaml");

    // Skip files starting with _ (like _schema.yaml, _example.yaml)
    if (id.startsWith("_")) continue;

    const raw = readFileSync(file, "utf-8");
    const data = yaml.load(raw);

    if (!data || !data.name || !data.address) {
      console.warn(`Skipping ${file}: missing required fields (name/address)`);
      continue;
    }
    // Tag taxonomy is the source of truth — surface unknown values rather
    // than silently dropping them, so the migration can be re-run.
    if (data.tags) {
      const bad = data.tags.filter((t) => !KNOWN_TAGS.has(t));
      if (bad.length) {
        console.warn(`  ⚠ ${id}: unknown tag(s) ${bad.join(", ")}`);
      }
    }

    // Apply defaults
    const restaurant = {
      ...data,
      address: {
        city: DEFAULTS["address.city"],
        country: DEFAULTS["address.country"],
        ...data.address,
      },
      tags: Array.isArray(data.tags) ? data.tags.filter((t) => KNOWN_TAGS.has(t)) : [],
      hidden: data.hidden ?? DEFAULTS.hidden,
      galleries: data.galleries ?? DEFAULTS.galleries,
      featured: data.featured ?? false,
      hasDiscount: data.hasDiscount ?? false,
    };

    restaurants.push({ id, file, data: restaurant });
  }

  return restaurants;
}

// --- Resolve placeId by name + address using Places API New `searchText` ---
function haversineMeters(lat1, lon1, lat2, lon2) {
  const R = 6371000;
  const toRad = (deg) => (deg * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(a));
}

function buildResolveQuery(data) {
  const namePart = data.name?.en || data.name?.zh || "";
  const addr = data.address || {};
  const addrLine = [addr.addressLine1, addr.postalCode, addr.city || "Berlin"]
    .filter(Boolean)
    .join(" ");
  return `${namePart} ${addrLine}`.trim();
}

async function resolvePlaceId(data) {
  const textQuery = buildResolveQuery(data);
  const body = {
    textQuery,
    pageSize: 3,
  };
  if (typeof data.latitude === "number" && typeof data.longitude === "number") {
    body.locationBias = {
      circle: {
        center: { latitude: data.latitude, longitude: data.longitude },
        radius: 500.0,
      },
    };
  }

  const res = await fetch("https://places.googleapis.com/v1/places:searchText", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Goog-Api-Key": PLACES_API_KEY,
      "X-Goog-FieldMask":
        "places.id,places.location,places.formattedAddress,places.displayName",
    },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    let detail = "";
    try {
      const err = await res.json();
      detail = err?.error?.message ?? JSON.stringify(err);
    } catch {
      detail = await res.text().catch(() => "");
    }
    throw new Error(`searchText HTTP ${res.status}: ${detail}`);
  }
  const json = await res.json();
  const candidates = json.places ?? [];
  if (candidates.length === 0) return { match: null, candidates: [] };

  // Pick the closest candidate to the YAML lat/lng if we have one; else first.
  const havePoint =
    typeof data.latitude === "number" && typeof data.longitude === "number";
  const ranked = candidates
    .map((p) => {
      const lat = p.location?.latitude;
      const lng = p.location?.longitude;
      const distance =
        havePoint && typeof lat === "number" && typeof lng === "number"
          ? haversineMeters(data.latitude, data.longitude, lat, lng)
          : null;
      return { place: p, distance };
    })
    .sort((a, b) => {
      if (a.distance == null && b.distance == null) return 0;
      if (a.distance == null) return 1;
      if (b.distance == null) return -1;
      return a.distance - b.distance;
    });

  return { match: ranked[0], candidates: ranked };
}

function appendPlaceIdToFile(filePath, placeId) {
  const raw = readFileSync(filePath, "utf-8");
  if (/^placeId\s*:/m.test(raw)) {
    // Already present — replace the line instead of appending.
    const next = raw.replace(/^placeId\s*:.*$/m, `placeId: ${placeId}`);
    writeFileSync(filePath, next, "utf-8");
    return;
  }
  const needsNewline = !raw.endsWith("\n");
  appendFileSync(filePath, `${needsNewline ? "\n" : ""}placeId: ${placeId}\n`, "utf-8");
}

async function resolveAllPlaceIds(restaurants) {
  if (!PLACES_API_KEY) {
    console.error("--resolve-place-ids needs GOOGLE_PLACES_API_KEY");
    process.exit(1);
  }

  let resolved = 0;
  let alreadyHad = 0;
  let noMatch = 0;
  let tooFar = 0;
  let failed = 0;

  for (const { id, file, data } of restaurants) {
    if (data.placeId) {
      alreadyHad++;
      continue;
    }
    process.stdout.write(`${id} ... `);
    try {
      const { match } = await resolvePlaceId(data);
      if (!match) {
        console.log("no match");
        noMatch++;
        continue;
      }
      const distLabel =
        match.distance == null ? "?" : `${Math.round(match.distance)} m`;
      const placeId = match.place.id;
      const display = match.place.displayName?.text ?? "(no name)";
      const addr = match.place.formattedAddress ?? "(no address)";

      if (match.distance != null && match.distance > RESOLVE_MAX_DISTANCE_METERS) {
        console.log(
          `SKIPPED (${distLabel} away): ${display} — ${addr}`,
        );
        tooFar++;
        continue;
      }
      console.log(
        `${placeId} (${distLabel}, ${display})${RESOLVE_DRY_RUN ? " [dry-run]" : ""}`,
      );
      if (!RESOLVE_DRY_RUN) appendPlaceIdToFile(file, placeId);
      resolved++;
    } catch (err) {
      console.log(`FAILED: ${err.message}`);
      failed++;
    }
  }

  console.log(
    `\nResolver summary: ${resolved} resolved, ${alreadyHad} already had placeId, ` +
      `${noMatch} no candidate, ${tooFar} too far (manual review), ${failed} errors.`,
  );
  if (tooFar > 0) {
    console.log(
      `(>${RESOLVE_MAX_DISTANCE_METERS} m mismatch usually means wrong branch ` +
        "or restaurant — open the YAML and resolve manually.)",
    );
  }
}

// Encode Google Places periods array into compact "openMOW-closeMOW" strings.
// MOW = minute-of-week with Sunday = 0 (matching Google's `day` numbering).
// 24/7 venues are returned as the sentinel ["OPEN_24H"].
function encodePeriods(periods) {
  if (!Array.isArray(periods) || periods.length === 0) return null;
  // Google represents 24/7 as a single period with open at Sunday 00:00 and no close field.
  if (periods.length === 1 && !periods[0].close) {
    const o = periods[0].open;
    if (o && (o.day ?? 0) === 0 && (o.hour ?? 0) === 0 && (o.minute ?? 0) === 0) {
      return ["OPEN_24H"];
    }
  }
  const out = [];
  for (const p of periods) {
    if (!p.open || !p.close) continue;
    const oMow =
      (p.open.day ?? 0) * 1440 + (p.open.hour ?? 0) * 60 + (p.open.minute ?? 0);
    const cMow =
      (p.close.day ?? 0) * 1440 + (p.close.hour ?? 0) * 60 + (p.close.minute ?? 0);
    out.push(`${oMow}-${cMow}`);
  }
  return out.length > 0 ? out : null;
}

// --- Google Places enrichment (Places API New) ---
async function fetchPlaceDetails(placeId) {
  const url = `https://places.googleapis.com/v1/places/${encodeURIComponent(placeId)}`;
  const res = await fetch(url, {
    headers: {
      "X-Goog-Api-Key": PLACES_API_KEY,
      "X-Goog-FieldMask": PLACES_FIELD_MASK,
    },
  });
  if (!res.ok) {
    let detail = "";
    try {
      const body = await res.json();
      detail = body?.error?.message ?? JSON.stringify(body);
    } catch {
      detail = await res.text().catch(() => "");
    }
    throw new Error(`Places API HTTP ${res.status}: ${detail}`);
  }
  const r = await res.json();
  return {
    placeId,
    rating: r.rating ?? null,
    userRatingsTotal: r.userRatingCount ?? null,
    weekdayText: r.currentOpeningHours?.weekdayDescriptions ?? null,
    // Compact "openMOW-closeMOW" strings (Sunday=0 minute-of-week) so the
    // mobile client can compute "open now" without re-parsing locale-specific
    // weekdayText. ["OPEN_24H"] is the sentinel for 24/7 venues.
    periods: encodePeriods(r.currentOpeningHours?.periods),
    website: r.websiteUri ?? null,
    googleMapsUrl: r.googleMapsUri ?? `https://www.google.com/maps/place/?q=place_id:${placeId}`,
    formattedPhoneNumber:
      r.nationalPhoneNumber ?? r.internationalPhoneNumber ?? null,
    formattedAddress: r.formattedAddress ?? null,
    // Photo "name" looks like "places/{placeId}/photos/{photoRef}" in the new API.
    photoRefs: (r.photos ?? [])
      .slice(0, MAX_PHOTOS)
      .map((p) => p.name)
      .filter(Boolean),
    // Tag-suggestion inputs — cached so future re-tagging passes can run
    // straight from Firestore without re-hitting the Places API.
    primaryType: r.primaryType ?? null,
    types: r.types ?? null,
    editorialSummary: r.editorialSummary?.text ?? null,
    reviews: (r.reviews ?? []).slice(0, 5).map((rv) => ({
      text: rv.text?.text ?? null,
      rating: rv.rating ?? null,
      languageCode: rv.text?.languageCode ?? null,
    })),
    fetchedAt: Timestamp.now(),
  };
}

async function fetchPhotoBytes(photoName) {
  const url =
    `https://places.googleapis.com/v1/${photoName}/media` +
    `?maxWidthPx=${PHOTO_MAX_WIDTH}` +
    `&key=${PLACES_API_KEY}`;
  const res = await fetch(url, { redirect: "follow" });
  if (!res.ok) throw new Error(`Photo HTTP ${res.status}`);
  const arrayBuffer = await res.arrayBuffer();
  const contentType = res.headers.get("content-type") ?? "image/jpeg";
  return { buffer: Buffer.from(arrayBuffer), contentType };
}

function photoExtension(contentType) {
  if (contentType.includes("png")) return "png";
  if (contentType.includes("webp")) return "webp";
  return "jpg";
}

async function uploadPhotos(placeId, photoRefs) {
  if (!storageBucket) {
    throw new Error("Storage bucket not initialised — cannot upload photos");
  }
  const urls = [];
  for (let i = 0; i < photoRefs.length; i++) {
    try {
      const { buffer, contentType } = await fetchPhotoBytes(photoRefs[i]);
      const ext = photoExtension(contentType);
      const path = `places/${placeId}/${i}.${ext}`;
      const file = storageBucket.file(path);
      await file.save(buffer, {
        contentType,
        metadata: { cacheControl: "public, max-age=2592000" }, // 30 days
        public: true,
        resumable: false,
      });
      urls.push(
        `https://storage.googleapis.com/${storageBucket.name}/${encodeURIComponent(path)}`,
      );
    } catch (err) {
      console.warn(`    ! photo ${i} for ${placeId} failed: ${err.message}`);
    }
  }
  return urls;
}

function shouldRefreshPlace(existingGoogleData, placeId) {
  if (FORCE_REFRESH) return true;
  if (!existingGoogleData) return true;
  if (existingGoogleData.placeId !== placeId) return true;
  // Recover from a failed photo batch: if we have Place Details cached but
  // no photo URLs, refresh so we retry the upload.
  if ((existingGoogleData.photoUrls?.length ?? 0) === 0) return true;
  const fetchedAt = existingGoogleData.fetchedAt;
  if (!fetchedAt || typeof fetchedAt.toMillis !== "function") return true;
  const age = Date.now() - fetchedAt.toMillis();
  return age > PLACES_REFRESH_MS;
}

async function buildGoogleData(id, data, existingGoogleData) {
  const placeId = data.placeId;
  if (!placeId || !PLACES_API_KEY) return null;

  if (!shouldRefreshPlace(existingGoogleData, placeId)) {
    return { googleData: existingGoogleData, status: "cached" };
  }

  let details;
  try {
    details = await fetchPlaceDetails(placeId);
  } catch (err) {
    console.warn(`  ! Places fetch failed for ${id}: ${err.message}`);
    return { googleData: existingGoogleData ?? null, status: "failed" };
  }

  const photoRefs = details.photoRefs ?? [];
  const reusedUrls = existingGoogleData?.photoUrls ?? [];

  // Skip the (expensive) photo upload step when we already have URLs cached
  // and --refresh-photos wasn't passed. Place Details still refreshes per
  // PLACES_REFRESH_MS to keep ratings/hours fresh without paying for photos.
  let photoUrls;
  if (DRY_RUN) {
    photoUrls = photoRefs.map((_, i) => `(dry-run photo #${i})`);
  } else if (photoRefs.length === 0) {
    photoUrls = reusedUrls;
  } else if (reusedUrls.length > 0 && !REFRESH_PHOTOS) {
    photoUrls = reusedUrls;
  } else if (storageBucket) {
    photoUrls = await uploadPhotos(placeId, photoRefs);
  } else {
    photoUrls = reusedUrls;
  }

  // Drop the photoRefs from the persisted document — they're transient.
  delete details.photoRefs;

  const googleData = {
    ...details,
    photoUrls,
    coverPhotoUrl: photoUrls[0] ?? null,
  };
  return { googleData, status: "fetched" };
}

// --- Upsert to Firestore ---
async function syncToFirestore(restaurants) {
  const batch = db.batch();
  let count = 0;
  let placesFetched = 0;
  let placesSkipped = 0;
  let placesFailed = 0;

  for (const { id, data } of restaurants) {
    const ref = db.collection(COLLECTION).doc(id);
    const existing = await ref.get();
    const existingData = existing.exists ? existing.data() : null;

    const doc = { ...data };

    // Google Places enrichment
    if (data.placeId && PLACES_API_KEY) {
      const result = await buildGoogleData(id, data, existingData?.googleData);
      if (result?.status === "fetched") placesFetched++;
      else if (result?.status === "cached") placesSkipped++;
      else if (result?.status === "failed") placesFailed++;
      doc.googleData = result?.googleData ?? null;
    } else if (existingData?.googleData && !data.placeId) {
      // YAML cleared placeId — drop cached googleData
      doc.googleData = FieldValue.delete();
    }

    // Drop the legacy single-cuisine field — superseded by `tags`. Only
    // queue the delete when the existing doc actually still carries it,
    // so post-migration runs stay idempotent in the audit log.
    if (existingData && "cuisineType" in existingData) {
      doc.cuisineType = FieldValue.delete();
    }
    // Same idea for editorialNote / discountInfo / chain — when YAML drops
    // them we need to remove them from Firestore (merge:true alone won't).
    if (existingData?.editorialNote && !data.editorialNote) {
      doc.editorialNote = FieldValue.delete();
    }
    if (existingData?.discountInfo && !data.discountInfo) {
      doc.discountInfo = FieldValue.delete();
    }
    if (existingData?.chain && !data.chain) {
      doc.chain = FieldValue.delete();
    }

    if (existing.exists) {
      // Upsert: preserve visitCount, viewCount, createdAt
      doc.updatedAt = FieldValue.serverTimestamp();
      // Don't overwrite these counters from YAML
      delete doc.visitCount;
      delete doc.viewCount;
      batch.set(ref, doc, { merge: true });
      console.log(`  Updated: ${id} (${data.name.zh || data.name.en})`);
    } else {
      // New document
      doc.visitCount = 0;
      doc.viewCount = 0;
      doc.createdAt = FieldValue.serverTimestamp();
      doc.updatedAt = FieldValue.serverTimestamp();
      batch.set(ref, doc);
      console.log(`  Created: ${id} (${data.name.zh || data.name.en})`);
    }

    count++;
  }

  if (count > 0) {
    await batch.commit();
  }

  console.log(
    `  Places: ${placesFetched} fetched, ${placesSkipped} cached, ${placesFailed} failed`,
  );

  return count;
}

// --- Dry-run: print what we would write, do not touch Firestore/Storage ---
async function dryRun(restaurants) {
  if (!PLACES_API_KEY) {
    console.error("Dry-run still needs GOOGLE_PLACES_API_KEY to call the Places API.");
    process.exit(1);
  }
  for (const { id, data } of restaurants) {
    if (!data.placeId) {
      console.log(`\n${id}: (no placeId — skipped)`);
      continue;
    }
    console.log(`\n${id} → ${data.placeId}`);
    try {
      const details = await fetchPlaceDetails(data.placeId);
      const summary = {
        rating: details.rating,
        userRatingsTotal: details.userRatingsTotal,
        weekdayText: details.weekdayText,
        periods: details.periods,
        website: details.website,
        googleMapsUrl: details.googleMapsUrl,
        formattedPhoneNumber: details.formattedPhoneNumber,
        formattedAddress: details.formattedAddress,
        photoCount: details.photoRefs?.length ?? 0,
      };
      console.log(JSON.stringify(summary, null, 2));
    } catch (err) {
      console.warn(`  ! ${err.message}`);
    }
  }
}

// --- Main ---
async function main() {
  console.log("Loading YAML files...");
  let restaurants = await loadRestaurants();
  if (RESTAURANT_ID_FILTER) {
    const before = restaurants.length;
    restaurants = restaurants.filter(({ id }) => RESTAURANT_ID_FILTER.has(id));
    console.log(`Filtered to ${restaurants.length}/${before} by --ids`);
  }
  console.log(`Found ${restaurants.length} restaurant(s)\n`);

  if (restaurants.length === 0) {
    console.log("No restaurants to sync.");
    return;
  }

  if (RESOLVE_PLACE_IDS) {
    console.log(
      `Resolving placeIds via Places searchText` +
        (RESOLVE_DRY_RUN ? " (dry-run, no YAML writes)" : "") +
        "...\n",
    );
    await resolveAllPlaceIds(restaurants);
    return;
  }

  if (DRY_RUN) {
    console.log("Dry-run — Places API will be called, no writes performed.");
    await dryRun(restaurants);
    return;
  }

  console.log("Syncing to Firestore (upsert)...");
  const count = await syncToFirestore(restaurants);
  console.log(`\nDone! Synced ${count} restaurant(s).`);
}

main().catch((err) => {
  console.error("Sync failed:", err);
  process.exit(1);
});
