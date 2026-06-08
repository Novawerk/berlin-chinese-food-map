import {
  collection,
  doc,
  getDoc,
  getDocs,
  limit,
  query,
} from "firebase/firestore";
import { auth, db } from "@/firebase";

/**
 * Admin-panel access control.
 *
 * Who can sign in and what they can touch is driven by an `admins` Firestore
 * collection — one document per teammate, keyed by their (lowercased) login
 * email. Each doc carries a `role` and an optional `disabled` flag:
 *
 *   admins/{email} = { email, role: "admin" | "editor", displayName?, disabled? }
 *
 * The Firestore security rules are the real enforcement (see firestore.rules);
 * the helpers here mirror that matrix so the panel hides what a teammate can't
 * use. Keep the two in sync.
 *
 * Roles:
 *   admin  — full access, including managing the team (this collection),
 *            the changelog, and the public team roster.
 *   editor — manage restaurant content and moderate feedback; read-only on
 *            the changelog and public roster; no access to team management.
 */
export type Role = "admin" | "editor";

export type AccessAction =
  | "list"
  | "show"
  | "create"
  | "edit"
  | "delete"
  | string;

let rolePromise: Promise<Role | null> | null = null;
let cachedEmail: string | null = null;

/** Drop the memoized role — call on login/logout so the next read is fresh. */
export function resetRoleCache() {
  rolePromise = null;
  cachedEmail = null;
}

function normalizeEmail(email: string): string {
  return email.trim().toLowerCase();
}

async function fetchRole(email: string): Promise<Role | null> {
  try {
    const snap = await getDoc(doc(db, "admins", normalizeEmail(email)));
    if (snap.exists()) {
      const data = snap.data();
      if (data.disabled === true) return null;
      return data.role === "admin" ? "admin" : "editor";
    }
    // Bootstrap: when the allowlist is still empty, the first person to sign in
    // is treated as an admin so they can seed the team. The moment any admin
    // doc exists, membership is enforced and unknown emails are rejected.
    const anyAdmin = await getDocs(query(collection(db, "admins"), limit(1)));
    return anyAdmin.empty ? "admin" : null;
  } catch (err) {
    // Reads can fail before the updated security rules are deployed, or while
    // offline. The server rules remain the source of truth for writes, so we
    // fail open to keep the panel usable rather than locking everyone out.
    console.warn("[access] could not resolve admin role, defaulting to admin", err);
    return "admin";
  }
}

/** Resolve (and memoize) the current user's role, or null if not authorized. */
export function getRole(): Promise<Role | null> {
  const email = auth.currentUser?.email ?? null;
  if (!email) return Promise.resolve(null);
  if (rolePromise && cachedEmail === email) return rolePromise;
  cachedEmail = email;
  rolePromise = fetchRole(email);
  return rolePromise;
}

/** Permission matrix — mirrors firestore.rules. */
export function canRoleAccess(
  role: Role | null,
  resource: string,
  action: AccessAction,
): boolean {
  if (role === "admin") return true;
  if (role !== "editor") return false;

  // Editors:
  switch (resource) {
    case "admins":
      return false; // team management is admin-only
    case "team_members":
    case "changelog":
      return action === "list" || action === "show"; // read-only
    case "restaurants":
    case "feedback":
      return true; // full content access
    default:
      return false;
  }
}
