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

// Generated from data/_tags.yaml — do not edit by hand.
// Run `npm run gen:tags` in scripts/sync-to-firestore.
// @gen:tags:admin
export const REGIONAL_TAGS = [
  "SICHUAN",
  "CANTONESE",
  "NORTHERN",
  "NORTHEASTERN",
  "SHANGHAINESE",
  "HUNAN",
  "XINJIANG",
  "TAIWANESE",
  "MUSLIM",
  "MONGOLIAN",
] as const;

export const FORMAT_TAGS = [
  "HOTPOT",
  "BBQ",
  "NOODLES",
  "DUMPLINGS",
  "DIM_SUM",
  "MALATANG",
  "VEGETARIAN",
  "BREAKFAST",
  "TEA_HOUSE",
  "BAKERY",
  "STREET_FOOD",
  "FUSION",
] as const;
// @gen:tags:end

export const TAGS = [...REGIONAL_TAGS, ...FORMAT_TAGS] as const;

export type Tag = (typeof TAGS)[number];

export interface Chain {
  brand: string;
  branch?: string;
}

export interface GoogleData {
  placeId: string;
  rating?: number;
  userRatingsTotal?: number;
  weekdayText?: string[];
  website?: string;
  googleMapsUrl?: string;
  formattedPhoneNumber?: string;
  formattedAddress?: string;
  photoUrls?: string[];
  coverPhotoUrl?: string;
  fetchedAt?: Timestamp;
}

export interface Restaurant {
  id?: string;
  name: Localizable;
  tags: Tag[];
  address: Address;
  latitude: number;
  longitude: number;
  phone?: string;
  priceRange?: string;
  description?: Localizable;
  logoUrl?: string;
  galleries: string[];
  placeId?: string;
  googleData?: GoogleData;
  visitCount: number;
  viewCount: number;
  hidden?: boolean;
  featured?: boolean;
  hasDiscount?: boolean;
  discountInfo?: Localizable;
  editorialNote?: Localizable;
  chain?: Chain;
  createdAt?: Timestamp;
  updatedAt?: Timestamp;
}
