// Pull every Firestore collection to local JSON so you can grep / diff
// what's actually in the cloud.
//
// Usage:
//   node dump-firestore.mjs                # dumps to data/firestore-dump/
//   node dump-firestore.mjs --out /tmp/dx  # custom output dir
//   node dump-firestore.mjs --collection restaurants   # just one
//   node dump-firestore.mjs --include-subcollections   # also pull visits/views
//
// Auth: same as sync — set FIREBASE_SERVICE_ACCOUNT_JSON in env, or point
// GOOGLE_APPLICATION_CREDENTIALS at a service-account key file.
//
// Output structure:
//   data/firestore-dump/
//     _summary.json          counts + last-fetched timestamp
//     restaurants.json       array of every restaurant doc
//     team_members.json
//     changelog.json
//     restaurants/{id}/      (only with --include-subcollections)
//       visits.json
//       views.json

import { initializeApp, cert, applicationDefault } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { mkdirSync, writeFileSync, existsSync } from "fs";
import { resolve, dirname, join } from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const REPO_ROOT = resolve(__dirname, "..", "..");

// --- Args ---
const argv = process.argv.slice(2);
function flag(name) { return argv.includes(name); }
function arg(name, fallback) {
  const i = argv.findIndex((a) => a === name);
  return i >= 0 && argv[i + 1] ? argv[i + 1] : fallback;
}

const OUT_DIR = resolve(REPO_ROOT, arg("--out", "data/firestore-dump"));
const COLLECTION_FILTER = arg("--collection", null);
const INCLUDE_SUBCOLLECTIONS = flag("--include-subcollections");

// --- Init Firebase Admin ---
const serviceAccountJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
const adcPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;

let credential;
if (serviceAccountJson) {
  credential = cert(JSON.parse(serviceAccountJson));
  console.log("Auth: FIREBASE_SERVICE_ACCOUNT_JSON env var");
} else if (adcPath && existsSync(adcPath)) {
  credential = applicationDefault();
  console.log(`Auth: GOOGLE_APPLICATION_CREDENTIALS (${adcPath})`);
} else {
  console.error(
    "Auth missing. Set one of:\n" +
      "  - FIREBASE_SERVICE_ACCOUNT_JSON (paste the service-account JSON)\n" +
      "  - GOOGLE_APPLICATION_CREDENTIALS (path to a service-account key file)",
  );
  process.exit(1);
}

initializeApp({ credential });
const db = getFirestore();

// --- Helpers ---
// firestore-admin returns native JS objects; Timestamps need to be made
// JSON-serialisable. We turn them into ISO strings so the dump diffs cleanly.
function normalise(value) {
  if (value === null || value === undefined) return value;
  if (value instanceof Timestamp) return value.toDate().toISOString();
  if (Array.isArray(value)) return value.map(normalise);
  if (typeof value === "object") {
    const out = {};
    for (const [k, v] of Object.entries(value)) out[k] = normalise(v);
    return out;
  }
  return value;
}

function ensureDir(path) {
  mkdirSync(path, { recursive: true });
}

function writeJson(path, data) {
  ensureDir(dirname(path));
  writeFileSync(path, JSON.stringify(data, null, 2) + "\n", "utf-8");
}

async function dumpCollection(name) {
  const snap = await db.collection(name).get();
  const docs = [];
  for (const doc of snap.docs) {
    docs.push({ id: doc.id, ...normalise(doc.data()) });
  }
  // Stable order so re-runs diff cleanly.
  docs.sort((a, b) => a.id.localeCompare(b.id));
  return docs;
}

async function dumpSubcollections(parentName, parentId) {
  const subSnap = await db
    .collection(parentName)
    .doc(parentId)
    .listCollections();
  const out = {};
  for (const sub of subSnap) {
    const subDocs = await sub.get();
    if (subDocs.empty) continue;
    out[sub.id] = subDocs.docs.map((d) => ({
      id: d.id,
      ...normalise(d.data()),
    }));
  }
  return out;
}

async function main() {
  console.log(`Output: ${OUT_DIR}\n`);
  ensureDir(OUT_DIR);

  // List top-level collections.
  const collections = await db.listCollections();
  const targets = collections
    .map((c) => c.id)
    .filter((id) => !COLLECTION_FILTER || id === COLLECTION_FILTER)
    .filter((id) => id !== "users"); // skip user-scoped data

  if (targets.length === 0) {
    console.error(`No matching collections (filter: ${COLLECTION_FILTER ?? "none"})`);
    process.exit(1);
  }

  const summary = {
    fetchedAt: new Date().toISOString(),
    project: process.env.GCLOUD_PROJECT || process.env.GOOGLE_CLOUD_PROJECT || "(default)",
    collections: {},
  };

  for (const name of targets) {
    process.stdout.write(`  ${name.padEnd(20)} `);
    const docs = await dumpCollection(name);
    writeJson(join(OUT_DIR, `${name}.json`), docs);
    summary.collections[name] = { count: docs.length };
    console.log(`${docs.length} docs`);

    if (INCLUDE_SUBCOLLECTIONS && docs.length > 0) {
      let subTotal = 0;
      for (const d of docs) {
        const subs = await dumpSubcollections(name, d.id);
        for (const [subName, items] of Object.entries(subs)) {
          writeJson(join(OUT_DIR, name, d.id, `${subName}.json`), items);
          subTotal += items.length;
        }
      }
      if (subTotal > 0) console.log(`    + ${subTotal} subcollection docs`);
    }
  }

  writeJson(join(OUT_DIR, "_summary.json"), summary);
  console.log(`\nDone. ${Object.keys(summary.collections).length} collection(s) dumped.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
