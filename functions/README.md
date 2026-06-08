# Cloud Functions

Background Firestore triggers + admin helpers for Berlin Chinese Food
Map.

## What's deployed

| Function | Trigger | What it does |
|----------|---------|--------------|
| `aggregateViewCount` | Write to `restaurants/{id}/views/{uid}` | Atomically increments / decrements `restaurants/{id}.viewCount` by the diff in the doc's `count` field. |
| `analyticsReport` | Callable (admin panel) | Proxies the GA4 Data API so the admin dashboard can show DAU/WAU/MAU, new users ("downloads" proxy), sessions and screen views. Gated on the `admins` allowlist. See [Analytics dashboard setup](#analytics-dashboard-setup). |

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
- Node 22 (see `package.json` `engines`). The Cloud Functions runtime is
  pinned to `nodejs22` in `firebase.json` — Node 20 was deprecated
  (decommissions 2026-10-30), so we run on 22.

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

## Analytics dashboard setup

`analyticsReport` powers the admin panel's **Analytics** dashboard. GA4
reporting data can't be read from the browser (it needs a Google service
account), so the panel calls this function, which queries the **GA4 Data
API** as the function's runtime service account and returns a small JSON
payload. One-time setup on the Google side:

1. **Enable the API.** In the Google Cloud console for `novawerk-7dd18`,
   enable **Google Analytics Data API** (`analyticsdata.googleapis.com`).

2. **Grant the function read access to the GA4 property.** Find the
   function's runtime service account (Gen 2 default is
   `<PROJECT_NUMBER>-compute@developer.gserviceaccount.com`; it's printed
   in the deploy output and in `npm run logs`). In **Google Analytics →
   Admin → Property → Property access management**, add that service
   account email with the **Viewer** role.

3. **Set the property id.** Copy the numeric **GA4 property id** (Analytics
   → Admin → Property settings — e.g. `123456789`; this is *not* the
   `G-XXXX` measurement id) into `functions/.env`:

   ```bash
   # functions/.env  (gitignored)
   GA4_PROPERTY_ID=123456789
   ```

   The function reads it via `defineString("GA4_PROPERTY_ID")`. Until it's
   set, the dashboard shows an "Analytics not configured yet" card instead
   of charts.

4. **Deploy** (`npm run deploy`). The dashboard appears at the top of the
   admin sidebar; both `admin` and `editor` roles can view it.

### Concrete values for `novawerk-7dd18`

These were resolved from the Firebase Management API and are stable:

| Thing | Value |
|-------|-------|
| GA4 property id | `531275650` (account `389944419`) |
| Function runtime service account | `364041827824-compute@developer.gserviceaccount.com` |
| Property access management (deep link) | https://analytics.google.com/analytics/web/#/a389944419p531275650/admin/suiteusermanagement/property |

Status as of the analytics-dashboard rollout: the **Data API is enabled**,
`GA4_PROPERTY_ID` is set in `functions/.env`, and `analyticsReport` is
**deployed and ACTIVE** in `europe-west3`. The one remaining step is the GA
property grant (step 2) — it can't be automated with the Firebase CLI
credential (GA access management needs an Analytics OAuth scope the CLI
token doesn't carry; the API returns `ACCESS_TOKEN_SCOPE_INSUFFICIENT`).
Add the service account above as **Viewer** via the deep link, and the
dashboard lights up with no redeploy.

**On "downloads":** GA4 has no true store-install metric — actual install
counts live in App Store Connect / Play Console. The dashboard uses
`newUsers` (≈ `first_open`) as the closest in-GA4 proxy and labels it
accordingly. Swapping in real store numbers would mean separate App Store
Connect / Play Reporting integrations.

## Region

Deployed to `europe-west3` (Frankfurt) — pinned in `src/index.ts` via
`setGlobalOptions({ region })`, with `analyticsReport` setting the same
region on its own `onCall` options (its module is imported before
`setGlobalOptions` runs). Match the Firestore database region so
reads/writes don't cross-region. The admin client must request the same
region (`getFunctions(app, "europe-west3")` in `web-apps/admin/src/firebase.ts`)
or callables resolve the wrong endpoint.

## Why no CI deploy

The repo's GitHub Actions only sync restaurant data
(`sync-restaurants.yml`), not Firestore rules / functions. Both are
deployed manually for now — the surface area is small enough that it
doesn't justify a workflow + service-account-in-secrets setup yet.
When that changes, add a `deploy-functions.yml` modelled on the data
sync workflow.
