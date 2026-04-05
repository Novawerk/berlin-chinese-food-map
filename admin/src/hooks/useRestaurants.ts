import { useState, useEffect } from "react";
import {
  collection,
  getDocs,
  getDoc,
  addDoc,
  updateDoc,
  deleteDoc,
  doc,
  Timestamp,
  orderBy,
  query,
} from "firebase/firestore";
import { db } from "../firebase";
import type { Restaurant } from "../types/restaurant";

const COLLECTION = "restaurants";

export function useRestaurants() {
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchRestaurants = async () => {
    setLoading(true);
    try {
      const q = query(collection(db, COLLECTION), orderBy("createdAt", "desc"));
      const snapshot = await getDocs(q);
      const data = snapshot.docs.map((d) => ({
        id: d.id,
        ...d.data(),
      })) as Restaurant[];
      setRestaurants(data);
    } catch (err) {
      console.error("Failed to fetch restaurants:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRestaurants();
  }, []);

  const getRestaurant = async (id: string): Promise<Restaurant | null> => {
    const snap = await getDoc(doc(db, COLLECTION, id));
    if (!snap.exists()) return null;
    return { id: snap.id, ...snap.data() } as Restaurant;
  };

  const addRestaurant = async (data: Omit<Restaurant, "id" | "createdAt" | "updatedAt">) => {
    const now = Timestamp.now();
    await addDoc(collection(db, COLLECTION), {
      ...data,
      createdAt: now,
      updatedAt: now,
    });
    await fetchRestaurants();
  };

  const updateRestaurant = async (
    id: string,
    data: Partial<Omit<Restaurant, "id" | "createdAt" | "updatedAt">>
  ) => {
    await updateDoc(doc(db, COLLECTION, id), {
      ...data,
      updatedAt: Timestamp.now(),
    });
    await fetchRestaurants();
  };

  const deleteRestaurant = async (id: string) => {
    await deleteDoc(doc(db, COLLECTION, id));
    await fetchRestaurants();
  };

  return {
    restaurants,
    loading,
    addRestaurant,
    updateRestaurant,
    deleteRestaurant,
    getRestaurant,
  };
}
