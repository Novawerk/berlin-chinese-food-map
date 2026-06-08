import { AuthProvider } from "ra-core";
import {
  GoogleAuthProvider,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
  onAuthStateChanged,
  User,
} from "firebase/auth";
import { auth } from "./firebase";
import { canRoleAccess, getRole, resetRoleCache } from "./lib/access";

let cachedUser: User | null = null;

// Listen for auth state
onAuthStateChanged(auth, (user) => {
  cachedUser = user;
  if (!user) resetRoleCache();
});

export const authProvider: AuthProvider = {
  async login(params: {
    email?: string;
    password?: string;
    method?: string;
  }) {
    resetRoleCache();
    if (params?.method === "google") {
      await signInWithPopup(auth, new GoogleAuthProvider());
    } else {
      await signInWithEmailAndPassword(
        auth,
        params.email ?? "",
        params.password ?? "",
      );
    }
    // Reject right at the login step if the (valid) account isn't on the team,
    // so the user gets a clear message instead of a silent bounce later.
    const role = await getRole();
    if (!role) {
      await signOut(auth);
      throw new Error("This account is not authorized for the admin panel.");
    }
  },

  async logout() {
    resetRoleCache();
    await signOut(auth);
  },

  async checkError({ status }: { status: number }) {
    if (status === 401 || status === 403) {
      await signOut(auth);
      throw new Error("Session expired");
    }
  },

  async checkAuth() {
    if (!cachedUser) {
      // Wait briefly for auth state to initialize
      await new Promise<void>((resolve, reject) => {
        const unsubscribe = onAuthStateChanged(auth, (user) => {
          unsubscribe();
          if (user) {
            cachedUser = user;
            resolve();
          } else {
            reject(new Error("Not authenticated"));
          }
        });
      });
    }
    // Gate on the admins allowlist: a valid Firebase account that isn't on the
    // team (or has been disabled) is signed out. Writes are independently
    // enforced by Firestore rules; this keeps the panel itself locked too.
    const role = await getRole();
    if (!role) {
      await signOut(auth);
      throw new Error("Your account is not authorized for the admin panel.");
    }
  },

  async getPermissions() {
    return (await getRole()) ?? "";
  },

  async canAccess({ resource, action }: { resource: string; action: string }) {
    return canRoleAccess(await getRole(), resource, action);
  },

  async getIdentity() {
    if (!cachedUser) throw new Error("Not authenticated");
    return {
      id: cachedUser.uid,
      fullName: cachedUser.email ?? "Admin",
    };
  },
};
