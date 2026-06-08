# Historical artifacts

Source materials and one-shot reports kept for traceability after the
May 2026 schema migration. Nothing here is on any execution path.

## `中餐地图-mar-3.csv`

The community-handed-over spreadsheet (March 3 version). 207 rows of
店名 / EN name / 类别 (`饭店 / 面馆 / 小吃 / 麻辣烫·冒菜 / 火锅 / 素食 / 串烧 / 烧烤`) /
连锁 / address / Ortsteil / a curated `柏林慢慢游甄选20` short-note column.

This CSV is the source of truth for what restaurants were *originally*
contributed — useful when reconciling historical questions ("did this
chain entry come from the original list or was it added later?"). The
current source of truth lives in `data/restaurants/**/*.yaml`.

## `migration-report.csv`

Output of `scripts/sync-to-firestore/_archive/migrate-to-tags.mjs`. One
row per restaurant with `old_cuisine` / `new_tags` / `featured` /
`editorialNote_zh` / `chain_brand` / `chain_branch` columns. Generated
during the cuisineType→tags migration to give a triage-ready audit log
of every auto-derived tag and which CSV row it joined to.
