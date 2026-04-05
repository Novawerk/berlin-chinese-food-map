import { AuthProvider } from "ra-core";
import {
  signInWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
  User,
} from "firebase/auth";
import { auth } from "./firebase";

let cachedUser: User | null = null;

// Listen for auth state
onAuthStateChanged(auth, (user) => {
  cachedUser = user;
});

export const authProvider: AuthProvider = {
  async login({ username, password }: { username: string; password: string }) {
    await signInWithEmailAndPassword(auth, username, password);
  },

  async logout() {
    await signOut(auth);
  },

  async checkError({ status }: { status: number }) {
    if (status === 401 || status === 403) {
      await signOut(auth);
      throw new Error("Session expired");
    }
  },

  async checkAuth() {
    if (cachedUser) return;
    // Wait briefly for auth state to initialize
    return new Promise<void>((resolve, reject) => {
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
  },

  async getIdentity() {
    if (!cachedUser) throw new Error("Not authenticated");
    return {
      id: cachedUser.uid,
      fullName: cachedUser.email ?? "Admin",
    };
  },
};
