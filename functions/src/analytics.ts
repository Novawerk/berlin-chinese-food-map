/**
 * `analyticsReport` — a callable that proxies the Google Analytics 4 Data
 * API for the admin panel's dashboard.
 *
 * Why a Cloud Function and not a direct client call: GA4 reporting data is
 * NOT readable from the browser. The Data API authenticates with a Google
 * service account, and we can't ship service-account credentials into a
 * public SPA. So the admin page calls this function; the function runs as
 * the project's default service account (which must be granted Viewer on
 * the GA4 property — see functions/README.md), queries the Data API, and
 * returns a small, already-shaped JSON payload.
 *
 * Access is gated the same way the panel itself is: the caller must be
 * signed in and present (and not disabled) in the `admins` collection.
 * Both `admin` and `editor` roles may read analytics — it's view-only.
 *
 * Metrics returned:
 *   - rolling DAU / WAU / MAU (active 1/7/28-day users, as of yesterday)
 *   - per-day series: active users, new users, sessions, screen views
 *   - range totals for new users (our "downloads" proxy), sessions, views
 *
 * Note on "downloads": GA4 has no true store-install metric — that lives in
 * App Store Connect / Play Console. `newUsers` (≈ first_open) is the closest
 * in-GA4 proxy, so the dashboard labels it "Downloads (new users)".
 */

import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineString } from "firebase-functions/params";
import { getFirestore } from "firebase-admin/firestore";
import { BetaAnalyticsDataClient } from "@google-analytics/data";

// The numeric GA4 property id (e.g. "123456789"), NOT the "G-XXXX"
// measurement id. Set it with `firebase functions:config`-style params:
// add `GA4_PROPERTY_ID=...` to functions/.env (or pass at deploy time).
const GA4_PROPERTY_ID = defineString("GA4_PROPERTY_ID");

// Reuse one client across warm invocations. In Cloud Functions it picks up
// the runtime service account via Application Default Credentials.
let analyticsClient: BetaAnalyticsDataClient | null = null;
function getAnalyticsClient(): BetaAnalyticsDataClient {
    if (!analyticsClient) {
        analyticsClient = new BetaAnalyticsDataClient();
    }
    return analyticsClient;
}

interface SeriesPoint {
    date: string; // ISO yyyy-mm-dd
    activeUsers: number;
    newUsers: number;
    sessions: number;
    screenPageViews: number;
}

interface AnalyticsReport {
    range: { days: number; startDate: string; endDate: string };
    rolling: { dau: number; wau: number; mau: number };
    totals: { newUsers: number; sessions: number; screenPageViews: number };
    series: SeriesPoint[];
}

export const analyticsReport = onCall(
    { region: "europe-west3" },
    async (request): Promise<AnalyticsReport> => {
        await assertAuthorized(request.auth?.token?.email);

        const propertyId = GA4_PROPERTY_ID.value();
        if (!propertyId) {
            throw new HttpsError(
                "failed-precondition",
                "Analytics is not configured: GA4_PROPERTY_ID is unset. " +
                    "See functions/README.md for one-time setup.",
            );
        }

        const days = clampDays(request.data?.days);
        const property = `properties/${propertyId}`;
        const client = getAnalyticsClient();

        try {
            const [series, rolling] = await Promise.all([
                runSeries(client, property, days),
                runRolling(client, property),
            ]);

            const totals = series.reduce(
                (acc, p) => {
                    acc.newUsers += p.newUsers;
                    acc.sessions += p.sessions;
                    acc.screenPageViews += p.screenPageViews;
                    return acc;
                },
                { newUsers: 0, sessions: 0, screenPageViews: 0 },
            );

            return {
                range: {
                    days,
                    startDate: series[0]?.date ?? "",
                    endDate: series[series.length - 1]?.date ?? "",
                },
                rolling,
                totals,
                series,
            };
        } catch (err) {
            if (err instanceof HttpsError) throw err;
            // Surface the GA API error message so misconfiguration (API not
            // enabled, service account lacks property access, wrong property
            // id) is debuggable from the panel rather than a blank 500.
            const message = err instanceof Error ? err.message : String(err);
            console.error("[analyticsReport] GA4 Data API call failed", err);
            throw new HttpsError(
                "internal",
                `Google Analytics request failed: ${message}`,
            );
        }
    },
);

/** Per-day active/new users, sessions and screen views over the range. */
async function runSeries(
    client: BetaAnalyticsDataClient,
    property: string,
    days: number,
): Promise<SeriesPoint[]> {
    const [response] = await client.runReport({
        property,
        dateRanges: [{ startDate: `${days - 1}daysAgo`, endDate: "today" }],
        dimensions: [{ name: "date" }],
        metrics: [
            { name: "activeUsers" },
            { name: "newUsers" },
            { name: "sessions" },
            { name: "screenPageViews" },
        ],
        orderBys: [{ dimension: { dimensionName: "date" }, desc: false }],
    });

    return (response.rows ?? []).map((row) => {
        const raw = row.dimensionValues?.[0]?.value ?? ""; // "yyyymmdd"
        const metrics = row.metricValues ?? [];
        return {
            date: toIsoDate(raw),
            activeUsers: num(metrics[0]?.value),
            newUsers: num(metrics[1]?.value),
            sessions: num(metrics[2]?.value),
            screenPageViews: num(metrics[3]?.value),
        };
    });
}

/**
 * Rolling DAU / WAU / MAU as of yesterday (a full, settled day — "today" is
 * still accumulating). GA computes the 1/7/28-day active windows relative to
 * the date range's end, so a single-day range is all we need.
 */
async function runRolling(
    client: BetaAnalyticsDataClient,
    property: string,
): Promise<{ dau: number; wau: number; mau: number }> {
    const [response] = await client.runReport({
        property,
        dateRanges: [{ startDate: "yesterday", endDate: "yesterday" }],
        metrics: [
            { name: "active1DayUsers" },
            { name: "active7DayUsers" },
            { name: "active28DayUsers" },
        ],
    });

    const metrics = response.rows?.[0]?.metricValues ?? [];
    return {
        dau: num(metrics[0]?.value),
        wau: num(metrics[1]?.value),
        mau: num(metrics[2]?.value),
    };
}

/**
 * Authorize the caller against the `admins` allowlist, mirroring the panel's
 * own access check (lib/access.ts). Both roles may view analytics. Bootstrap:
 * if the allowlist is empty, any signed-in caller is allowed so the first
 * teammate isn't locked out before seeding the team.
 */
async function assertAuthorized(email: string | undefined): Promise<void> {
    if (!email) {
        throw new HttpsError(
            "unauthenticated",
            "Sign in to view analytics.",
        );
    }
    const db = getFirestore();
    const snap = await db.doc(`admins/${email.trim().toLowerCase()}`).get();
    if (snap.exists) {
        if (snap.get("disabled") === true) {
            throw new HttpsError(
                "permission-denied",
                "This account is disabled.",
            );
        }
        return;
    }
    const anyAdmin = await db.collection("admins").limit(1).get();
    if (!anyAdmin.empty) {
        throw new HttpsError(
            "permission-denied",
            "This account is not authorized for the admin panel.",
        );
    }
}

function clampDays(value: unknown): number {
    const n = typeof value === "number" ? value : Number(value);
    if (!Number.isFinite(n)) return 30;
    return Math.min(365, Math.max(1, Math.floor(n)));
}

function num(value: string | null | undefined): number {
    if (value == null) return 0;
    const n = Number(value);
    return Number.isFinite(n) ? n : 0;
}

function toIsoDate(yyyymmdd: string): string {
    if (yyyymmdd.length !== 8) return yyyymmdd;
    return `${yyyymmdd.slice(0, 4)}-${yyyymmdd.slice(4, 6)}-${yyyymmdd.slice(6, 8)}`;
}
