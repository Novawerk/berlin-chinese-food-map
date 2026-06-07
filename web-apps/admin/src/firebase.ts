import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getFunctions } from "firebase/functions";

// Trim env values: Vercel-configured vars can carry a trailing newline, which
// poisons the auth iframe URL ("Illegal url for new iframe ...%0A").
const env = (value?: string) => value?.trim() || undefined;

export const firebaseConfig = {
  apiKey: env(import.meta.env.VITE_FIREBASE_API_KEY),
  authDomain: env(import.meta.env.VITE_FIREBASE_AUTH_DOMAIN),
  projectId: env(import.meta.env.VITE_FIREBASE_PROJECT_ID) || "novawerk-7dd18",
  storageBucket: env(import.meta.env.VITE_FIREBASE_STORAGE_BUCKET),
  messagingSenderId: env(import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID),
  appId: env(import.meta.env.VITE_FIREBASE_APP_ID),
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
// Callable functions live in europe-west3 (see functions/src/index.ts) —
// the region must match or `httpsCallable` resolves the wrong endpoint.
export const functions = getFunctions(app, "europe-west3");
