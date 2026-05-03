/**
 * One-time backfill: recompute every restaurant's `viewCount` from the
 * sum of `count` across its `views/*` subcollection.
 *
 * Run from the `functions/` directory after `npm install`:
 *
 *     GOOGLE_APPLICATION_CREDENTIALS=path/to/service-account.json \
 *       npm run backfill:view-counts
 *
 * Pass `--dry-run` to print the planned writes without actually writing.
 *
 * Why a manual script and not a callable function: backfilling a few
 * hundred restaurants is a one-off after the trigger lands. Running
 * locally with the service account avoids the need to expose an admin
 * endpoint and lets us see the diff before committing to writes.
 */

import { initializeApp, applicationDefault } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

const dryRun = process.argv.includes("--dry-run");

initializeApp({ credential: applicationDefault() });
const db = getFirestore();

async function main(): Promise<void> {
    console.log(dryRun ? "DRY RUN — no writes" : "LIVE — writes will happen");

    const restaurants = await db.collection("restaurants").get();
    console.log(`Found ${restaurants.size} restaurants`);

    let updated = 0;
    let unchanged = 0;
    for (const doc of restaurants.docs) {
        const id = doc.id;
        const current = (doc.data().viewCount as number | undefined) ?? 0;

        const views = await doc.ref.collection("views").get();
        let total = 0;
        for (const view of views.docs) {
            const c = view.data().count;
            if (typeof c === "number") total += c;
        }

        if (total === current) {
            unchanged++;
            continue;
        }

        console.log(`  ${id}: ${current} → ${total} (${views.size} viewer${views.size === 1 ? "" : "s"})`);
        if (!dryRun) {
            await doc.ref.update({ viewCount: total });
        }
        updated++;
    }

    console.log(`Done — ${updated} updated, ${unchanged} unchanged.`);
}

main().catch((err) => {
    console.error(err);
    process.exit(1);
});
