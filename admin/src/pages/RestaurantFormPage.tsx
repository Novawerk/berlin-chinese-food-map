import { useState, useEffect } from "react";
import type { FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useRestaurants } from "../hooks/useRestaurants";
import type { Restaurant, CuisineType } from "../types/restaurant";
import { CUISINE_TYPES } from "../types/restaurant";

function emptyRestaurant(): Omit<Restaurant, "id" | "createdAt" | "updatedAt"> {
  return {
    name: { en: "", zh: "" },
    cuisineType: "GENERAL",
    address: {
      addressLine1: "",
      postalCode: "",
      district: "",
      city: "Berlin",
      country: "Germany",
    },
    latitude: 52.52,
    longitude: 13.405,
    phone: "",
    priceRange: "",
    description: { en: "", zh: "" },
    logoUrl: "",
    galleries: [],
    visitCount: 0,
    viewCount: 0,
  };
}

export function RestaurantFormPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const { addRestaurant, updateRestaurant, getRestaurant } = useRestaurants();

  const [form, setForm] = useState(emptyRestaurant());
  const [galleriesInput, setGalleriesInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [fetchingData, setFetchingData] = useState(isEdit);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    (async () => {
      try {
        const r = await getRestaurant(id);
        if (cancelled) return;
        if (!r) {
          setError("Restaurant not found.");
          setFetchingData(false);
          return;
        }
        setForm({
          name: r.name,
          cuisineType: r.cuisineType,
          address: r.address,
          latitude: r.latitude,
          longitude: r.longitude,
          phone: r.phone ?? "",
          priceRange: r.priceRange ?? "",
          description: r.description ?? { en: "", zh: "" },
          logoUrl: r.logoUrl ?? "",
          galleries: r.galleries ?? [],
          visitCount: r.visitCount ?? 0,
          viewCount: r.viewCount ?? 0,
        });
        setGalleriesInput((r.galleries ?? []).join("\n"));
      } catch {
        if (!cancelled) setError("Failed to load restaurant.");
      } finally {
        if (!cancelled) setFetchingData(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    const galleries = galleriesInput
      .split("\n")
      .map((s) => s.trim())
      .filter(Boolean);

    const data = { ...form, galleries };

    try {
      if (isEdit && id) {
        await updateRestaurant(id, data);
      } else {
        await addRestaurant(data);
      }
      navigate("/");
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Save failed";
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  if (fetchingData) return <div className="loading">Loading restaurant...</div>;

  return (
    <div className="page">
      <header className="page-header">
        <h1>{isEdit ? "Edit Restaurant" : "Add Restaurant"}</h1>
        <button onClick={() => navigate("/")} className="btn btn-secondary">
          Back
        </button>
      </header>

      {error && <div className="error-message">{error}</div>}

      <form onSubmit={handleSubmit} className="restaurant-form">
        <fieldset>
          <legend>Name</legend>
          <label>
            English *
            <input
              required
              value={form.name.en}
              onChange={(e) => setForm({ ...form, name: { ...form.name, en: e.target.value } })}
            />
          </label>
          <label>
            Chinese *
            <input
              required
              value={form.name.zh}
              onChange={(e) => setForm({ ...form, name: { ...form.name, zh: e.target.value } })}
            />
          </label>
          <label>
            German
            <input
              value={form.name.de ?? ""}
              onChange={(e) => setForm({ ...form, name: { ...form.name, de: e.target.value || undefined } })}
            />
          </label>
        </fieldset>

        <fieldset>
          <legend>Basic Info</legend>
          <label>
            Cuisine Type *
            <select
              required
              value={form.cuisineType}
              onChange={(e) => setForm({ ...form, cuisineType: e.target.value as CuisineType })}
            >
              {CUISINE_TYPES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </label>
          <label>
            Phone
            <input
              value={form.phone ?? ""}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
            />
          </label>
          <label>
            Price Range
            <input
              placeholder="e.g. $$ or 10-20 EUR"
              value={form.priceRange ?? ""}
              onChange={(e) => setForm({ ...form, priceRange: e.target.value })}
            />
          </label>
          <label>
            Logo URL
            <input
              value={form.logoUrl ?? ""}
              onChange={(e) => setForm({ ...form, logoUrl: e.target.value })}
            />
          </label>
        </fieldset>

        <fieldset>
          <legend>Address</legend>
          <label>
            Address Line 1 *
            <input
              required
              value={form.address.addressLine1}
              onChange={(e) =>
                setForm({ ...form, address: { ...form.address, addressLine1: e.target.value } })
              }
            />
          </label>
          <label>
            Address Line 2
            <input
              value={form.address.addressLine2 ?? ""}
              onChange={(e) =>
                setForm({ ...form, address: { ...form.address, addressLine2: e.target.value || undefined } })
              }
            />
          </label>
          <label>
            Note
            <input
              value={form.address.note ?? ""}
              onChange={(e) =>
                setForm({ ...form, address: { ...form.address, note: e.target.value || undefined } })
              }
            />
          </label>
          <label>
            Postal Code *
            <input
              required
              value={form.address.postalCode}
              onChange={(e) =>
                setForm({ ...form, address: { ...form.address, postalCode: e.target.value } })
              }
            />
          </label>
          <label>
            District *
            <input
              required
              value={form.address.district}
              onChange={(e) =>
                setForm({ ...form, address: { ...form.address, district: e.target.value } })
              }
            />
          </label>
          <label>
            City
            <input
              value={form.address.city}
              onChange={(e) =>
                setForm({ ...form, address: { ...form.address, city: e.target.value } })
              }
            />
          </label>
          <label>
            Country
            <input
              value={form.address.country}
              onChange={(e) =>
                setForm({ ...form, address: { ...form.address, country: e.target.value } })
              }
            />
          </label>
        </fieldset>

        <fieldset>
          <legend>Coordinates</legend>
          <label>
            Latitude *
            <input
              type="number"
              step="any"
              required
              value={form.latitude}
              onChange={(e) => setForm({ ...form, latitude: parseFloat(e.target.value) || 0 })}
            />
          </label>
          <label>
            Longitude *
            <input
              type="number"
              step="any"
              required
              value={form.longitude}
              onChange={(e) => setForm({ ...form, longitude: parseFloat(e.target.value) || 0 })}
            />
          </label>
        </fieldset>

        <fieldset>
          <legend>Description</legend>
          <label>
            English
            <textarea
              rows={3}
              value={form.description?.en ?? ""}
              onChange={(e) =>
                setForm({ ...form, description: { ...(form.description ?? { en: "", zh: "" }), en: e.target.value } })
              }
            />
          </label>
          <label>
            Chinese
            <textarea
              rows={3}
              value={form.description?.zh ?? ""}
              onChange={(e) =>
                setForm({ ...form, description: { ...(form.description ?? { en: "", zh: "" }), zh: e.target.value } })
              }
            />
          </label>
          <label>
            German
            <textarea
              rows={3}
              value={form.description?.de ?? ""}
              onChange={(e) =>
                setForm({
                  ...form,
                  description: { ...(form.description ?? { en: "", zh: "" }), de: e.target.value || undefined },
                })
              }
            />
          </label>
        </fieldset>

        <fieldset>
          <legend>Gallery URLs (one per line)</legend>
          <textarea
            rows={4}
            value={galleriesInput}
            onChange={(e) => setGalleriesInput(e.target.value)}
            placeholder="https://example.com/image1.jpg&#10;https://example.com/image2.jpg"
          />
        </fieldset>

        <fieldset>
          <legend>Stats</legend>
          <label>
            Visit Count
            <input
              type="number"
              min={0}
              value={form.visitCount}
              onChange={(e) => setForm({ ...form, visitCount: parseInt(e.target.value) || 0 })}
            />
          </label>
          <label>
            View Count
            <input
              type="number"
              min={0}
              value={form.viewCount}
              onChange={(e) => setForm({ ...form, viewCount: parseInt(e.target.value) || 0 })}
            />
          </label>
        </fieldset>

        <div className="form-actions">
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? "Saving..." : isEdit ? "Update Restaurant" : "Create Restaurant"}
          </button>
          <button type="button" className="btn btn-secondary" onClick={() => navigate("/")}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
