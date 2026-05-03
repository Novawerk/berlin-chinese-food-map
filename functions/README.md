# Cloud Functions

Background Firestore triggers + admin helpers for Berlin Chinese Food
Map.

## What's deployed

| Function | Trigger | What it does |
|----------|---------|--------------|
| `aggregateViewCount` | Write to `restaurants/{id}/views/{uid}` | Atomically increments / decrements `restaurants/{id}.viewCount` by the diff in the doc's `count` field. |

The mobile client writes per-user view docs (anonymous-auth-allowed by
the Firestore rules). The trigger keeps the parent restaurant's
aggregate `viewCount` field in sync so the admin panel and the app's
`Restaurant.viewCount` model field show a meaningful total without the
client ever needing write access to the parent doc.

## Prerequisites

- **Firebase Blaze plan**. Cloud Functions deploy is gated on the
  pay-as-you-go plan; the free Spark plan only allows Auth + Firestore +
  Storage + Hosting. Our scale is well inside Blaze's free tier (2M
  invocations / 400k GB-seconds / month).
- Firebase CLI (`npm i -g firebase-tools`), authenticated against the
  `novawerk-7dd18` project.
- Node 20 (see `package.json` `engines`).

## Local setup

```bash
cd functions
npm install
```

## Deploy

```bash
cd functions
npm run deploy        # firebase deploy --only functions
```

Tail logs after a deploy to verify the trigger is firing:

```bash
npm run logs
# or, more focused:
firebase functions:log --only aggregateViewCount
```

## One-time backfill

After deploying for the first time, the existing `views/*` subcollection
docs aren't reflected in `restaurants/{id}.viewCount` (the trigger only
fires on *new* writes from this point onward). Run the backfill once:

```bash
cd functions
npm install     # if not already
GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json \
  npm run backfill:view-counts -- --dry-run     # preview
GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json \
  npm run backfill:view-counts                  # for real
```

The script walks every restaurant, sums the `count` across its `views/*`
docs, and writes the result to the restaurant's `viewCount` field
(skipping docs whose value already matches).

The same service-account JSON used by `scripts/sync-to-firestore`
works.

## Region

Deployed to `europe-west1`. Match the Firestore database region — if
the project's Firestore lives elsewhere, adjust `setGlobalOptions({ region })`
in `src/index.ts` so reads/writes don't cross-region.

## Why no CI deploy

The repo's GitHub Actions only sync restaurant data
(`sync-restaurants.yml`), not Firestore rules / functions. Both are
deployed manually for now — the surface area is small enough that it
doesn't justify a workflow + service-account-in-secrets setup yet.
When that changes, add a `deploy-functions.yml` modelled on the data
sync workflow.
