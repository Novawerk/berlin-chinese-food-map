/**
 * Cloud Functions for Berlin Chinese Food Map.
 *
 * Currently exports one trigger:
 *   `aggregateViewCount` — keeps `restaurants/{id}.viewCount` in sync with
 *   the sum of `count` across `restaurants/{id}/views/*`. This is the
 *   "total view events" metric (revisits add 1 each), not "unique viewers"
 *   — for that, count the number of view docs instead.
 *
 * Why a trigger and not a client-side write: the mobile client runs as
 * an anonymous user and is allowed to write to its own
 * `restaurants/{id}/views/{uid}` doc only. Updating the parent
 * `restaurants/{id}` doc requires admin auth, which Cloud Functions have
 * by default. This keeps the security model simple — clients can only
 * touch their own subcollection entry; a trusted backend reconciles the
 * aggregate.
 */

import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { setGlobalOptions } from "firebase-functions/v2";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

initializeApp();

// Pin the region close to the Firestore database (Firebase project default
// for restaurants in europe-west). Adjust if the Firestore region differs.
setGlobalOptions({ region: "europe-west1", maxInstances: 5 });

/**
 * Mirrors the diff in `count` from a single user's view doc up to the
 * parent restaurant's `viewCount` field, atomically.
 *
 * Cases handled:
 *  - Create: `before` doesn't exist, before count = 0, after count = 1
 *    (or whatever client wrote). Delta = after.
 *  - Update: typically client incremented `count` by 1 on revisit.
 *    Delta = after - before.
 *  - Delete: aggregate decrements by `before` so a deleted view doc
 *    doesn't permanently inflate the total.
 *
 * `FieldValue.increment` is atomic at the Firestore level, so concurrent
 * triggers from the same restaurant don't race.
 */
export const aggregateViewCount = onDocumentWritten(
    "restaurants/{restaurantId}/views/{uid}",
    async (event) => {
        const { restaurantId } = event.params;

        const beforeCount = readCount(event.data?.before?.data());
        const afterCount = readCount(event.data?.after?.data());
        const delta = afterCount - beforeCount;

        if (delta === 0) {
            // Could fire on no-op writes (e.g. updating only `viewedAt`).
            // Skip the parent write to avoid burning a Firestore op.
            return;
        }

        const restaurantRef = getFirestore().doc(`restaurants/${restaurantId}`);
        await restaurantRef.update({
            viewCount: FieldValue.increment(delta),
        });
    },
);

function readCount(data: FirebaseFirestore.DocumentData | undefined): number {
    if (!data) return 0;
    const raw = data.count;
    if (typeof raw === "number") return raw;
    if (typeof raw === "string") {
        const parsed = Number(raw);
        return Number.isFinite(parsed) ? parsed : 0;
    }
    return 0;
}
