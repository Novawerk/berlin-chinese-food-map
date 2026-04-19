import { collection, getDocs } from "firebase/firestore";
import { db } from "@/firebase";
import type { Restaurant } from "@/types/restaurant";

/**
 * Convert a restaurant document to YAML string.
 * We manually serialize to avoid adding a YAML library dependency.
 */
function toYaml(r: Restaurant & { id: string }): string {
  const lines: string[] = [];

  // name
  lines.push("name:");
  lines.push(`  en: "${esc(r.name.en)}"`);
  lines.push(`  zh: "${esc(r.name.zh)}"`);
  if (r.name.de) lines.push(`  de: "${esc(r.name.de)}"`);

  lines.push("");
  lines.push(`cuisineType: ${r.cuisineType}`);

  // address
  lines.push("");
  lines.push("address:");
  lines.push(`  addressLine1: "${esc(r.address.addressLine1)}"`);
  if (r.address.addressLine2) lines.push(`  addressLine2: "${esc(r.address.addressLine2)}"`);
  if (r.address.note) lines.push(`  note: "${esc(r.address.note)}"`);
  lines.push(`  postalCode: "${r.address.postalCode}"`);
  lines.push(`  district: "${esc(r.address.district)}"`);
  if (r.address.city && r.address.city !== "Berlin") lines.push(`  city: "${esc(r.address.city)}"`);
  if (r.address.country && r.address.country !== "Germany") lines.push(`  country: "${esc(r.address.country)}"`);

  lines.push("");
  lines.push(`latitude: ${r.latitude}`);
  lines.push(`longitude: ${r.longitude}`);

  // optional fields
  if (r.phone) {
    lines.push("");
    lines.push(`phone: "${esc(r.phone)}"`);
  }
  if (r.priceRange) {
    lines.push(`priceRange: "${esc(r.priceRange)}"`);
  }

  if (r.description && (r.description.en || r.description.zh)) {
    lines.push("");
    lines.push("description:");
    if (r.description.en) lines.push(`  en: "${esc(r.description.en)}"`);
    if (r.description.zh) lines.push(`  zh: "${esc(r.description.zh)}"`);
    if (r.description.de) lines.push(`  de: "${esc(r.description.de)}"`);
  }

  if (r.logoUrl) {
    lines.push("");
    lines.push(`logoUrl: "${esc(r.logoUrl)}"`);
  }

  if (r.galleries && r.galleries.length > 0) {
    lines.push("");
    lines.push("galleries:");
    for (const url of r.galleries) {
      lines.push(`  - "${esc(url)}"`);
    }
  }

  if (r.hidden) {
    lines.push("");
    lines.push("hidden: true");
  }

  return lines.join("\n") + "\n";
}

function esc(s: string): string {
  return s.replace(/\\/g, "\\\\").replace(/"/g, '\\"');
}

function toSlug(district: string): string {
  return district
    .toLowerCase()
    .replace(/ä/g, "ae")
    .replace(/ö/g, "oe")
    .replace(/ü/g, "ue")
    .replace(/ß/g, "ss")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/(^-|-$)/g, "");
}

interface YamlFile {
  path: string;
  content: string;
}

export async function exportRestaurantsAsYaml(): Promise<YamlFile[]> {
  const snapshot = await getDocs(collection(db, "restaurants"));
  const files: YamlFile[] = [];

  for (const doc of snapshot.docs) {
    const data = doc.data() as Restaurant;
    const id = doc.id;
    const district = toSlug(data.address?.district || "other");
    const yaml = toYaml({ ...data, id });
    files.push({
      path: `${district}/${id}.yaml`,
      content: yaml,
    });
  }

  return files;
}

export async function downloadYamlZip() {
  const files = await exportRestaurantsAsYaml();

  const JSZip = (await import("jszip")).default;
  const zip = new JSZip();

  for (const file of files) {
    zip.file(file.path, file.content);
  }

  const blob = await zip.generateAsync({ type: "blob" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `restaurants-${new Date().toISOString().slice(0, 10)}.zip`;
  a.click();
  URL.revokeObjectURL(url);
}
