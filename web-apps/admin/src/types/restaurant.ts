import { Timestamp } from "firebase/firestore";

export interface Localizable {
  en: string;
  zh: string;
  de?: string;
}

export interface Address {
  addressLine1: string;
  addressLine2?: string;
  note?: string;
  postalCode: string;
  district: string;
  city: string;
  country: string;
}

export const CUISINE_TYPES = [
  "SICHUAN",
  "CANTONESE",
  "HOTPOT",
  "BBQ",
  "DIM_SUM",
  "NOODLES",
  "GENERAL",
  "OTHER",
] as const;

export type CuisineType = (typeof CUISINE_TYPES)[number];

export interface Restaurant {
  id?: string;
  name: Localizable;
  cuisineType: CuisineType;
  address: Address;
  latitude: number;
  longitude: number;
  phone?: string;
  priceRange?: string;
  description?: Localizable;
  logoUrl?: string;
  galleries: string[];
  visitCount: number;
  viewCount: number;
  hidden?: boolean;
  createdAt?: Timestamp;
  updatedAt?: Timestamp;
}
