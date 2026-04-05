import { initializeApp, cert } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { readFileSync } from "fs";
import { glob } from "glob";
import { basename, dirname } from "path";
import yaml from "js-yaml";

// --- Config ---
const DATA_DIR = "../../data/restaurants";
const COLLECTION = "restaurants";

// --- Init Firebase Admin ---
const serviceAccount = process.env.FIREBASE_SERVICE_ACCOUNT_JSON
  ? JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON)
  : null;

if (!serviceAccount) {
  console.error("Error: FIREBASE_SERVICE_ACCOUNT_JSON env var is required");
  process.exit(1);
}

initializeApp({ credential: cert(serviceAccount) });
const db = getFirestore();

// --- Defaults ---
const DEFAULTS = {
  "address.city": "Berlin",
  "address.country": "Germany",
  hidden: false,
  galleries: [],
  visitCount: 0,
  viewCount: 0,
};

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

    if (!data || !data.name || !data.cuisineType || !data.address) {
      console.warn(`Skipping ${file}: missing required fields`);
      continue;
    }

    // Apply defaults
    const restaurant = {
      ...data,
      address: {
        city: DEFAULTS["address.city"],
        country: DEFAULTS["address.country"],
        ...data.address,
      },
      hidden: data.hidden ?? DEFAULTS.hidden,
      galleries: data.galleries ?? DEFAULTS.galleries,
    };

    restaurants.push({ id, data: restaurant });
  }

  return restaurants;
}

// --- Upsert to Firestore ---
async function syncToFirestore(restaurants) {
  const batch = db.batch();
  let count = 0;

  for (const { id, data } of restaurants) {
    const ref = db.collection(COLLECTION).doc(id);
    const existing = await ref.get();

    const doc = { ...data };

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

  return count;
}

// --- Main ---
async function main() {
  console.log("Loading YAML files...");
  const restaurants = await loadRestaurants();
  console.log(`Found ${restaurants.length} restaurant(s)\n`);

  if (restaurants.length === 0) {
    console.log("No restaurants to sync.");
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
