# Data pipeline

```
                  edit YAML, push                         CI runs sync-to-firestore
data/restaurants/  ───────────►  GitHub  ───────────►  Firestore  ◄──────  Mobile app
   *.yaml                                                  ▲                & landing page
                                                           │
                              admin panel writes ──────────┘
                              (web-apps/admin, hosted on Vercel)
```

**Source of truth: the YAMLs in this folder.** CI re-syncs Firestore from
them on every `data/restaurants/**` push. Anything an admin user changes
through the web panel goes straight to Firestore but will be overwritten
on the next CI sync — see *Editing via the admin panel* below for the
round-trip.

## Layout

```
data/
├── README.md                 ← this file
├── _tags.yaml                ← canonical tag taxonomy (single source of truth)
├── _schema.yaml              ← restaurant YAML schema reference
├── _archive/                 ← historical CSVs (community handover, migration report)
├── restaurants/              ← one YAML per restaurant, grouped by district
│   ├── britz/
│   ├── charlottenburg/
│   └── …
└── firestore-dump/           ← gitignored; output of dump-firestore.mjs
```

District folder names are fixed by the existing structure. Pick the closest
one when adding a new restaurant — no need to be perfectly canonical, the
display district comes from the YAML's `address.district` field.

## Tag taxonomy

Defined in `_tags.yaml`. 10 regional + 12 format = 22 tags. A restaurant
carries 1–3, with the first being the "primary" tag (used for the default
chip ordering). The same list is mirrored in:

- `composeApp/src/commonMain/kotlin/com/novawerk/berlinfoodmap/domain/restaurant/Tag.kt` (Kotlin enum)
- `scripts/sync-to-firestore/index.js` (`KNOWN_TAGS` set)
- `web-apps/admin/src/types/restaurant.ts` (`REGIONAL_TAGS`/`FORMAT_TAGS`)
- `composeApp/src/commonMain/composeResources/values{,-zh}/strings.xml` (display strings)

Drift is caught at CI time by `scripts/sync-to-firestore/check-tags.mjs`,
which runs as a pre-sync step. **When you add or rename a tag you must
update all five places.** The validator will fail the workflow if any
diverge.

## Common operations

### Add a restaurant

1. Pick the right district folder (see above).
2. Create `{slug-id}.yaml` matching `_schema.yaml`. The id becomes the
   Firestore document id and must be unique across all districts.
3. `tags`: 1–3 from `_tags.yaml`. Pick the regional tag first, then format(s).
4. `placeId` is optional but strongly recommended — Google Places fills
   in cover photos / hours / rating / phone on the next CI run. If you
   don't have one, run the resolver: `node scripts/sync-to-firestore/index.js
   --resolve-place-ids` (writes the matched id back to the YAML).
5. Push to `main`. CI validates tags and syncs to Firestore.

### Mark a restaurant as a Pinwo discount partner

Set `hasDiscount: true` in the restaurant's YAML (or toggle "Discount partner"
in the admin panel). The map renders these with a special outlined-circle
marker variant, and the search/filter surface can highlight them. The field
defaults to `false` and is independent from `featured`.

To describe the actual offer, add an optional `discountInfo` block (bilingual,
same shape as `editorialNote`):

```yaml
hasDiscount: true
discountInfo:
  zh: "会员到店赠送一道招牌小菜"
  en: ""        # optional
```

When `hasDiscount` is on, the detail sheet shows a perks card with this text
(falling back to generic copy if `discountInfo` is empty) plus a button that
opens **pinwo.de** so users can see the full offer.

### Add a new tag

1. Add it to `data/_tags.yaml` under the right family.
2. Add to the Kotlin enum in `Tag.kt` and the `TAG_FAMILY` map.
3. Add to `KNOWN_TAGS` in `scripts/sync-to-firestore/index.js`.
4. Add to `REGIONAL_TAGS` or `FORMAT_TAGS` in `web-apps/admin/src/types/restaurant.ts`,
   and add a label to `TAG_LABEL` / `TAG_CHOICES` in `restaurants.tsx`.
5. Add `tag_xxx` strings to both `strings.xml` files (en + zh).
6. Add a case to `tagDisplayName` in `TagChips.kt`.
7. Run `node scripts/sync-to-firestore/check-tags.mjs` locally to confirm.

### Editing via the admin panel

The admin (https://admin-…vercel.app) writes Firestore directly, so
edits made there will be overwritten on the next CI sync from YAML.
Workflow for keeping admin edits durable:

1. Make edits in the admin.
2. Click **Export YAML** in the list view header → downloads a zip of the
   current Firestore state in YAML form.
3. Unzip into `data/restaurants/`, `git add`, commit, push.
4. CI re-syncs (idempotent — the YAML now matches Firestore so nothing
   visible changes, but future re-syncs preserve the admin edits).

This is manual on purpose — having the admin auto-commit to git would
require giving Vercel a write-scoped GitHub token, which we'd rather not.

### Audit what's in the cloud

```
GOOGLE_APPLICATION_CREDENTIALS=path/to/service-account.json \
  node scripts/sync-to-firestore/dump-firestore.mjs
```

Output goes to `data/firestore-dump/` (gitignored). One JSON file per
collection. Useful for diffing against local YAMLs or grepping fields
that aren't surfaced in the app yet.

### Re-tag a batch via Places API

`scripts/sync-to-firestore/tag-from-places.mjs` queries Places (uses the
`primaryType` / `types` / `editorialSummary` / `reviews` cached on each
restaurant's `googleData` after the next sync) and prints suggested tags
for any restaurant that's currently untagged. Default is dry-run; pass
`--apply` to write back into YAMLs. Pass `--all` to consider already-tagged
restaurants too.

## Scripts cheat-sheet

| Path | When to run |
|------|-------------|
| `scripts/sync-to-firestore/index.js` | Auto-runs in CI on push to `data/restaurants/**`. Manual: `workflow_dispatch` from GitHub Actions. |
| `scripts/sync-to-firestore/check-tags.mjs` | Pre-sync in CI. Run locally before pushing tag-touching changes. |
| `scripts/sync-to-firestore/dump-firestore.mjs` | Ad-hoc; dumps Firestore to local JSON for inspection. |
| `scripts/sync-to-firestore/tag-from-places.mjs` | Ad-hoc; suggests tags for untagged restaurants from Places API enrichment. |
| `scripts/sync-to-firestore/_archive/` | One-shot migration scripts from the May 2026 schema change — not on any execution path. |
