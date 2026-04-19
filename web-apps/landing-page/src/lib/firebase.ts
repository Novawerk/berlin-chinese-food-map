import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";

const app = initializeApp({
  projectId: "novawerk-7dd18",
  apiKey: "AIzaSyDsc8xz_6kNjLpFHcax7Rlprn1i3GhaMfU",
  authDomain: "novawerk-7dd18.firebaseapp.com",
});

export const db = getFirestore(app);
