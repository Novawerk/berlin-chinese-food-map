import { FirebaseDataProvider } from "react-admin-firebase";
import { CreateParams, DataProvider } from "ra-core";
import { addDoc, collection, doc, serverTimestamp, setDoc } from "firebase/firestore";
import { db, firebaseConfig } from "./firebase";

const baseProvider = FirebaseDataProvider(firebaseConfig, {
  logging: false,
  lazyLoading: { enabled: false },
  renameMetaFields: {
    created_at: "createdAt",
    updated_at: "updatedAt",
  },
});

async function logChange(action: string, resource: string, data: Record<string, unknown>) {
  if (resource !== "restaurants") return;

  const name = data.name as { en?: string; zh?: string } | undefined;
  const label = name ? `${name.zh ?? ""} / ${name.en ?? ""}` : String(data.id ?? "");
  const today = new Date().toISOString().slice(0, 10);

  const actionLabels: Record<string, { en: string; zh: string }> = {
    create: { en: `Added restaurant: ${name?.en ?? ""}`, zh: `新增餐厅：${name?.zh ?? ""}` },
    update: { en: `Updated restaurant: ${name?.en ?? ""}`, zh: `更新餐厅：${name?.zh ?? ""}` },
    delete: { en: `Removed restaurant: ${label}`, zh: `删除餐厅：${label}` },
  };

  const titles = actionLabels[action] ?? { en: action, zh: action };

  await addDoc(collection(db, "changelog"), {
    title: { en: titles.en, zh: titles.zh },
    content: { en: `Auto-generated: ${action} on ${label}`, zh: `自动生成：${label} ${action === "create" ? "新增" : action === "update" ? "更新" : "删除"}` },
    version: "",
    date: today,
    tags: ["auto", action],
    published: false,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });
}

// Admin allowlist docs are keyed by the teammate's login email so the
// Firestore security rules can look them up via request.auth.token.email.
// The generic provider would assign an auto id, so create it directly.
async function createAdmin(params: CreateParams) {
  const data = params.data as Record<string, unknown>;
  const email = String(data.email ?? "").trim().toLowerCase();
  if (!email) throw new Error("Login email is required");
  const payload = {
    ...data,
    email,
    role: data.role ?? "editor",
    disabled: data.disabled ?? false,
    createdAt: serverTimestamp(),
  };
  await setDoc(doc(db, "admins", email), payload);
  return { data: { ...payload, id: email, createdAt: undefined } };
}

export const dataProvider: DataProvider = {
  ...baseProvider,

  async create(resource, params) {
    if (resource === "admins") {
      return createAdmin(params);
    }
    const result = await baseProvider.create(resource, params);
    await logChange("create", resource, result.data as Record<string, unknown>);
    return result;
  },

  async update(resource, params) {
    const result = await baseProvider.update(resource, params);
    await logChange("update", resource, result.data as Record<string, unknown>);
    return result;
  },

  async delete(resource, params) {
    if (resource === "restaurants") {
      try {
        const existing = await baseProvider.getOne(resource, { id: params.id });
        await logChange("delete", resource, existing.data as Record<string, unknown>);
      } catch { /* ignore */ }
    }
    return baseProvider.delete(resource, params);
  },
};
