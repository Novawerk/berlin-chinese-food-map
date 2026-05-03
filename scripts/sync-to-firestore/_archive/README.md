# Archived migration scripts

One-shot scripts that ran during the May 2026 `cuisineType: SINGLE` →
`tags: List<Tag>` schema migration. Kept here for traceability — they're
not on any execution path. Active scripts live one level up.

| Script | Migrated | Notes |
|--------|----------|-------|
| `migrate-to-tags.mjs` | All 207 YAMLs | Joined the community CSV (`data/_archive/中餐地图 Mar-3版本.csv`) against each YAML, mapped `类别` + name keywords + editorial notes onto the new `tags` / `featured` / `editorialNote` / `chain` fields. Idempotent but irrelevant after the migration shipped. |
| `tag-untagged.mjs` | 49 / 76 untagged | Hand-curated mapping for restaurants where the auto-migrator's keyword pass found no signal. Mostly defaulted Berlin's legacy 酒楼/酒家 to CANTONESE. |
| `tag-from-places-manual.mjs` | 7 / 27 remaining | Curated tags after dry-running `tag-from-places.mjs` against the Places API editorial summaries. |
| `tag-from-research.mjs` | 16 / 20 remaining | Final pass that read full Places review text + did targeted web searches for the last hard cases (e.g. "Feast" 王家一号 was confirmed as multi-regional via web). |

Total: schema migration + 4 ad-hoc passes covered 203 / 207 restaurants.
The remaining 4 (china-garden, golden-garden, kaiser-drachen, zhou-s-fine)
are generic banquet places with no signal — best handled in the admin
panel as proper editor work.
