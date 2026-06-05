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

// Mirror of the Kotlin `Tag` enum (composeApp/.../domain/restaurant/Tag.kt)
// and the migration script's `KNOWN_TAGS`. Keep all three in sync.
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

export const TAGS = [...REGIONAL_TAGS, ...FORMAT_TAGS] as const;

export type Tag = (typeof TAGS)[number];

export interface Chain {
  brand: string;
  branch?: string;
}

export interface GoogleReview {
  text?: string | null;
  rating?: number | null;
  languageCode?: string | null;
}

export interface GoogleData {
  placeId: string;
  rating?: number;
  userRatingsTotal?: number;
  weekdayText?: string[];
  // Compact "openMOW-closeMOW" strings (Sunday=0 minute-of-week) the sync
  // pipeline emits so the mobile client can compute "open now" without
  // re-parsing locale-specific weekdayText. ["OPEN_24H"] = always open.
  periods?: string[];
  website?: string;
  googleMapsUrl?: string;
  formattedPhoneNumber?: string;
  formattedAddress?: string;
  photoUrls?: string[];
  coverPhotoUrl?: string;
  // Tag-suggestion inputs cached by the sync pipeline so re-tagging passes can
  // run straight from Firestore without re-hitting the Places API.
  primaryType?: string | null;
  types?: string[] | null;
  editorialSummary?: string | null;
  reviews?: GoogleReview[];
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
