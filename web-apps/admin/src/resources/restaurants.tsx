import {
  List,
  DataTable,
  Edit,
  Create,
  SimpleForm,
  TextInput,
  AutocompleteArrayInput,
  NumberInput,
  Show,
  SimpleShowLayout,
  TextField,
  NumberField,
  BooleanInput,
  BooleanField,
  ArrayInput,
  SimpleFormIterator,
} from "@/components/admin";
import { useUpdate, useRecordContext, useRefresh } from "ra-core";
import { Button } from "@/components/ui/button";
import { Download, EyeOff, Eye, Star, ImageOff, BadgePercent } from "lucide-react";
import { downloadYamlZip } from "@/lib/export-yaml";
import { useState } from "react";
import { REGIONAL_TAGS, FORMAT_TAGS } from "@/types/restaurant";

const TAG_LABEL: Record<string, string> = {
  SICHUAN: "Sichuan 川菜",
  CANTONESE: "Cantonese 粤菜",
  NORTHERN: "Northern 北方菜",
  NORTHEASTERN: "Northeastern 东北菜",
  SHANGHAINESE: "Shanghainese 江浙沪",
  HUNAN: "Hunan 湘菜",
  XINJIANG: "Xinjiang 新疆菜",
  TAIWANESE: "Taiwanese 台菜",
  MUSLIM: "Halal 清真",
  MONGOLIAN: "Mongolian Grill 蒙古烤肉",
  HOTPOT: "Hotpot 火锅",
  BBQ: "BBQ 烧烤",
  NOODLES: "Noodles 面食",
  DUMPLINGS: "Dumplings 饺子",
  DIM_SUM: "Dim Sum 点心",
  MALATANG: "Malatang 麻辣烫",
  VEGETARIAN: "Vegetarian 素食",
  BREAKFAST: "Breakfast 早餐",
  TEA_HOUSE: "Tea House 茶寮",
  BAKERY: "Bakery 烘焙",
  STREET_FOOD: "Street Food 小吃",
  FUSION: "Modern Chinese 创新中餐",
};

const REGIONAL_SET = new Set<string>(REGIONAL_TAGS);
const FORMAT_SET = new Set<string>(FORMAT_TAGS);

const TAG_CHOICES = [
  ...REGIONAL_TAGS.map((id) => ({ id, name: TAG_LABEL[id] })),
  ...FORMAT_TAGS.map((id) => ({ id, name: TAG_LABEL[id] })),
];

const ExportYamlButton = () => {
  const [loading, setLoading] = useState(false);
  const handleExport = async () => {
    setLoading(true);
    try {
      await downloadYamlZip();
    } finally {
      setLoading(false);
    }
  };
  return (
    <Button variant="outline" size="sm" onClick={handleExport} disabled={loading}>
      <Download className="mr-2 h-4 w-4" />
      {loading ? "Exporting..." : "Export YAML"}
    </Button>
  );
};

const ToggleHiddenButton = () => {
  const record = useRecordContext();
  const [update, { isPending }] = useUpdate();
  const refresh = useRefresh();
  if (!record) return null;
  const isHidden = record.hidden === true;
  return (
    <Button
      variant="ghost"
      size="sm"
      disabled={isPending}
      onClick={(e) => {
        e.stopPropagation();
        update(
          "restaurants",
          { id: record.id, data: { hidden: !isHidden }, previousData: record },
          { onSuccess: () => refresh() },
        );
      }}
    >
      {isHidden ? <Eye className="h-4 w-4" /> : <EyeOff className="h-4 w-4" />}
    </Button>
  );
};

const CoverThumb = () => {
  const record = useRecordContext();
  const url =
    (record?.galleries as string[] | undefined)?.[0] ??
    (record?.googleData?.coverPhotoUrl as string | undefined) ??
    (record?.googleData?.photoUrls as string[] | undefined)?.[0];
  if (!url) {
    return (
      <div className="flex h-10 w-10 items-center justify-center rounded bg-muted text-muted-foreground">
        <ImageOff className="h-4 w-4" />
      </div>
    );
  }
  return (
    <img
      src={url}
      alt=""
      className="h-10 w-10 rounded object-cover"
      loading="lazy"
    />
  );
};

const Check = ({ ok, label }: { ok: boolean; label: string }) => (
  <span
    className={
      ok
        ? "inline-block rounded bg-emerald-100 px-1.5 py-0.5 text-xs font-medium text-emerald-700"
        : "inline-block rounded bg-muted px-1.5 py-0.5 text-xs text-muted-foreground"
    }
  >
    {label}
  </span>
);

const TagChip = ({ tag, primary }: { tag: string; primary: boolean }) => {
  const isRegional = REGIONAL_SET.has(tag);
  const isFormat = FORMAT_SET.has(tag);
  const family = isRegional
    ? "bg-indigo-100 text-indigo-800"
    : isFormat
      ? "bg-amber-100 text-amber-800"
      : "bg-rose-100 text-rose-800";
  return (
    <span
      className={`inline-block rounded px-1.5 py-0.5 text-xs font-medium ${family} ${primary ? "ring-2 ring-offset-1 ring-emerald-400" : ""}`}
      title={primary ? "primary tag" : isRegional ? "regional" : isFormat ? "format" : "unknown tag"}
    >
      {TAG_LABEL[tag] ?? tag}
    </span>
  );
};

const TagsCell = () => {
  const record = useRecordContext();
  const tags = (record?.tags as string[] | undefined) ?? [];
  if (tags.length === 0) {
    return <span className="text-muted-foreground">—</span>;
  }
  return (
    <div className="flex flex-wrap gap-1">
      {tags.map((tag, i) => (
        <TagChip key={tag} tag={tag} primary={i === 0} />
      ))}
    </div>
  );
};

const NameDeCell = () => {
  const record = useRecordContext();
  const de = record?.name?.de as string | undefined;
  return de ? (
    <span>{de}</span>
  ) : (
    <span className="text-muted-foreground">—</span>
  );
};

const PlaceIdCell = () => {
  const record = useRecordContext();
  const placeId = record?.placeId as string | undefined;
  return <Check ok={!!placeId} label={placeId ? "✓" : "—"} />;
};

const DiscountCell = () => {
  const record = useRecordContext();
  if (!record?.hasDiscount) {
    return <span className="text-muted-foreground">—</span>;
  }
  const offer =
    (record?.discountInfo?.zh as string | undefined) ||
    (record?.discountInfo?.en as string | undefined);
  return (
    <span
      className="inline-flex items-center gap-1 text-rose-600"
      title={offer ? offer : "Pinwo discount partner"}
    >
      <BadgePercent className="h-4 w-4" />
    </span>
  );
};

const PriceCell = () => {
  const record = useRecordContext();
  const price = record?.priceRange as string | undefined;
  return price ? (
    <span className="tabular-nums">{price}</span>
  ) : (
    <span className="text-muted-foreground">—</span>
  );
};

const ChainCell = () => {
  const record = useRecordContext();
  const brand = record?.chain?.brand as string | undefined;
  const branch = record?.chain?.branch as string | undefined;
  if (!brand) return <span className="text-muted-foreground">—</span>;
  return (
    <span className="text-xs">
      <span className="font-medium">{brand}</span>
      {branch && <span className="text-muted-foreground"> · {branch}</span>}
    </span>
  );
};

const RatingCell = () => {
  const record = useRecordContext();
  const rating = record?.googleData?.rating as number | undefined;
  const total = record?.googleData?.userRatingsTotal as number | undefined;
  if (typeof rating !== "number") {
    return <span className="text-muted-foreground">—</span>;
  }
  return (
    <span className="inline-flex items-center gap-1 text-sm">
      <Star className="h-3 w-3 fill-amber-400 text-amber-400" />
      {rating.toFixed(1)}
      {typeof total === "number" && (
        <span className="text-muted-foreground">({total})</span>
      )}
    </span>
  );
};

const PhotoCountCell = () => {
  const record = useRecordContext();
  const count = (record?.googleData?.photoUrls as string[] | undefined)?.length ?? 0;
  return (
    <span className={count === 0 ? "text-muted-foreground" : "tabular-nums"}>
      {count}
    </span>
  );
};

const DescriptionCell = () => {
  const record = useRecordContext();
  const d = record?.description as { en?: string; zh?: string; de?: string } | undefined;
  const note = record?.editorialNote as { en?: string; zh?: string } | undefined;
  return (
    <div className="flex flex-wrap gap-1">
      <Check ok={!!d?.zh} label="zh" />
      <Check ok={!!d?.en} label="en" />
      <Check ok={!!d?.de} label="de" />
      <Check ok={!!(note?.zh || note?.en)} label="note" />
    </div>
  );
};

const HiddenCell = () => {
  const record = useRecordContext();
  return record?.hidden ? (
    <span className="inline-block rounded bg-rose-100 px-1.5 py-0.5 text-xs font-medium text-rose-700">
      hidden
    </span>
  ) : (
    <span className="text-muted-foreground">—</span>
  );
};

export const RestaurantList = () => (
  <List actions={<ExportYamlButton />} perPage={50} sort={{ field: "name.zh", order: "ASC" }}>
    <DataTable>
      <DataTable.Col label="" disableSort>
        <CoverThumb />
      </DataTable.Col>
      <DataTable.Col source="name.zh" label="Name (ZH)" />
      <DataTable.Col source="name.en" label="Name (EN)" />
      <DataTable.Col source="name.de" label="Name (DE)" disableSort>
        <NameDeCell />
      </DataTable.Col>
      <DataTable.Col label="Tags" disableSort>
        <TagsCell />
      </DataTable.Col>
      <DataTable.Col source="address.district" label="District" />
      <DataTable.Col source="priceRange" label="€" disableSort>
        <PriceCell />
      </DataTable.Col>
      <DataTable.Col source="hasDiscount" label="优惠" disableSort>
        <DiscountCell />
      </DataTable.Col>
      <DataTable.Col label="Chain" disableSort>
        <ChainCell />
      </DataTable.Col>
      <DataTable.Col label="Place" disableSort>
        <PlaceIdCell />
      </DataTable.Col>
      <DataTable.Col label="Rating" disableSort>
        <RatingCell />
      </DataTable.Col>
      <DataTable.Col label="Photos" disableSort>
        <PhotoCountCell />
      </DataTable.Col>
      <DataTable.Col label="Desc / Note" disableSort>
        <DescriptionCell />
      </DataTable.Col>
      <DataTable.Col source="hidden" label="State" disableSort>
        <HiddenCell />
      </DataTable.Col>
      <DataTable.Col source="viewCount" label="Views" />
      <DataTable.Col label="" disableSort>
        <ToggleHiddenButton />
      </DataTable.Col>
    </DataTable>
  </List>
);

const RestaurantForm = () => (
  <SimpleForm>
    <TextInput source="name.en" label="Name (English)" isRequired />
    <TextInput source="name.zh" label="Name (Chinese)" isRequired />
    <TextInput source="name.de" label="Name (German)" />

    <AutocompleteArrayInput
      source="tags"
      label="Tags"
      choices={TAG_CHOICES}
      helperText="Pick 1–3 tags. First one is the primary (drives default filter / icon)."
    />

    <TextInput source="address.addressLine1" label="Address Line 1" isRequired />
    <TextInput source="address.addressLine2" label="Address Line 2" />
    <TextInput source="address.note" label="Note" />
    <TextInput source="address.postalCode" label="Postal Code" isRequired />
    <TextInput source="address.district" label="District" isRequired />
    <TextInput source="address.city" label="City" defaultValue="Berlin" />
    <TextInput source="address.country" label="Country" defaultValue="Germany" />

    <NumberInput source="latitude" label="Latitude" isRequired />
    <NumberInput source="longitude" label="Longitude" isRequired />

    <TextInput
      source="phone"
      label="Phone"
      helperText="Manual phone number. Google's synced number shows separately on the detail view."
    />
    <TextInput
      source="priceRange"
      label="Price range"
      helperText="€, €€, or €€€."
    />

    <TextInput
      source="placeId"
      label="Google placeId"
      helperText="Powers ratings, hours, photos sync. Leave empty to skip enrichment."
    />

    <TextInput source="description.en" label="Description (English)" multiline />
    <TextInput source="description.zh" label="Description (Chinese)" multiline />
    <TextInput source="description.de" label="Description (German)" multiline />

    <BooleanInput
      source="hasDiscount"
      label="Discount partner (Pinwo 合作优惠)"
      helperText="Mark this restaurant as offering Pinwo partner discounts/coupons."
      defaultValue={false}
    />
    <TextInput
      source="discountInfo.zh"
      label="Offer text (中文)"
      helperText="Short partner offer shown in the detail-sheet perks card, e.g. 会员到店赠送一道小菜. Only used when Discount partner is on."
      multiline
    />
    <TextInput source="discountInfo.en" label="Offer text (English)" multiline />
    <TextInput source="editorialNote.zh" label="Editorial note (中文)" multiline />
    <TextInput source="editorialNote.en" label="Editorial note (English)" multiline />

    <TextInput
      source="chain.brand"
      label="Chain brand"
      helperText="Set on every branch sharing the same operator."
    />
    <TextInput source="chain.branch" label="Branch label" />

    <TextInput
      source="logoUrl"
      label="Logo URL"
      helperText="Optional brand logo. Falls back to gallery / Google cover when empty."
    />

    <ArrayInput source="galleries" label="Manual gallery URLs">
      <SimpleFormIterator inline>
        <TextInput source="" label="URL" />
      </SimpleFormIterator>
    </ArrayInput>

    <BooleanInput source="hidden" label="Hidden (soft-delete)" defaultValue={false} />
  </SimpleForm>
);

export const RestaurantEdit = () => (
  <Edit>
    <RestaurantForm />
  </Edit>
);

export const RestaurantCreate = () => (
  <Create>
    <RestaurantForm />
  </Create>
);

const TagsPanel = () => {
  const record = useRecordContext();
  const tags = (record?.tags as string[] | undefined) ?? [];
  if (tags.length === 0) {
    return <span className="text-muted-foreground">(none)</span>;
  }
  return (
    <div className="flex flex-wrap gap-1">
      {tags.map((tag, i) => (
        <TagChip key={tag} tag={tag} primary={i === 0} />
      ))}
    </div>
  );
};

// Decode the sync pipeline's compact "openMOW-closeMOW" period strings
// (MOW = day*1440 + hour*60 + minute, Sunday = 0) into readable ranges so an
// admin can sanity-check the "open now" signal against the weekdayText below.
// Mirrors encodePeriods() in scripts/sync-to-firestore/index.js.
const DOW = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

const fmtMow = (mow: number): string => {
  const day = Math.floor(mow / 1440) % 7;
  const minutesIntoDay = mow % 1440;
  const hh = String(Math.floor(minutesIntoDay / 60)).padStart(2, "0");
  const mm = String(minutesIntoDay % 60).padStart(2, "0");
  return `${DOW[day]} ${hh}:${mm}`;
};

const decodePeriod = (p: string): string => {
  const [open, close] = p.split("-");
  const o = Number(open);
  const c = Number(close);
  if (!Number.isFinite(o) || !Number.isFinite(c)) return p;
  return `${fmtMow(o)} → ${fmtMow(c)}`;
};

const OpeningPeriods = ({ periods }: { periods?: string[] }) => {
  if (!periods || periods.length === 0) {
    return (
      <div>
        <span className="text-muted-foreground">Structured hours:</span> —
      </div>
    );
  }
  if (periods.length === 1 && periods[0] === "OPEN_24H") {
    return (
      <div>
        <span className="text-muted-foreground">Structured hours:</span>{" "}
        <span className="rounded bg-emerald-100 px-1.5 py-0.5 text-xs font-medium text-emerald-700">
          Open 24h
        </span>
      </div>
    );
  }
  return (
    <div>
      <div className="text-muted-foreground mb-1">
        Structured hours ({periods.length} periods — drives "open now"):
      </div>
      <ul className="space-y-0.5">
        {periods.map((p) => (
          <li key={p} className="font-mono text-xs">
            {decodePeriod(p)}
          </li>
        ))}
      </ul>
    </div>
  );
};

const GoogleDataPanel = () => {
  const record = useRecordContext();
  const g = record?.googleData;
  if (!g) {
    return (
      <p className="text-sm text-muted-foreground">
        No Google Places data — set a placeId to enable enrichment.
      </p>
    );
  }
  return (
    <div className="space-y-3 text-sm">
      <div className="flex flex-wrap gap-x-6 gap-y-1">
        <div>
          <span className="text-muted-foreground">Rating:</span>{" "}
          {typeof g.rating === "number"
            ? `${g.rating.toFixed(1)} (${g.userRatingsTotal ?? "?"})`
            : "—"}
        </div>
        <div>
          <span className="text-muted-foreground">Phone:</span>{" "}
          {g.formattedPhoneNumber ?? "—"}
        </div>
        <div>
          <span className="text-muted-foreground">Photos:</span>{" "}
          {g.photoUrls?.length ?? 0}
        </div>
      </div>
      {g.website && (
        <div>
          <span className="text-muted-foreground">Website:</span>{" "}
          <a className="text-blue-600 underline" href={g.website} target="_blank" rel="noreferrer">
            {g.website}
          </a>
        </div>
      )}
      {g.googleMapsUrl && (
        <div>
          <span className="text-muted-foreground">Maps:</span>{" "}
          <a className="text-blue-600 underline" href={g.googleMapsUrl} target="_blank" rel="noreferrer">
            Open in Google Maps
          </a>
        </div>
      )}
      {g.formattedAddress && (
        <div>
          <span className="text-muted-foreground">Formatted address:</span>{" "}
          {g.formattedAddress}
        </div>
      )}
      {g.weekdayText && g.weekdayText.length > 0 && (
        <div>
          <div className="text-muted-foreground mb-1">Hours:</div>
          <ul className="space-y-0.5">
            {g.weekdayText.map((line: string) => (
              <li key={line} className="font-mono text-xs">{line}</li>
            ))}
          </ul>
        </div>
      )}
      <OpeningPeriods periods={g.periods} />
      {(g.primaryType || (g.types && g.types.length > 0) || g.editorialSummary || (g.reviews && g.reviews.length > 0)) && (
        <div className="border-t pt-3">
          <div className="text-muted-foreground mb-1 text-xs uppercase">
            Tag-suggestion inputs (cached)
          </div>
          {g.primaryType && (
            <div>
              <span className="text-muted-foreground">Primary type:</span>{" "}
              <code className="text-xs">{g.primaryType}</code>
            </div>
          )}
          {g.types && g.types.length > 0 && (
            <div>
              <span className="text-muted-foreground">Types:</span>{" "}
              <code className="text-xs">{g.types.join(", ")}</code>
            </div>
          )}
          {g.editorialSummary && (
            <div className="mt-1">
              <span className="text-muted-foreground">Editorial summary:</span>{" "}
              {g.editorialSummary}
            </div>
          )}
          {g.reviews && g.reviews.length > 0 && (
            <details className="mt-1">
              <summary className="cursor-pointer text-muted-foreground">
                Reviews ({g.reviews.length})
              </summary>
              <ul className="mt-1 space-y-1">
                {g.reviews.map((rv, i: number) => (
                  <li key={i} className="text-xs">
                    {typeof rv.rating === "number" && (
                      <span className="font-medium">{rv.rating.toFixed(1)}★ </span>
                    )}
                    {rv.text ?? "(no text)"}
                  </li>
                ))}
              </ul>
            </details>
          )}
        </div>
      )}
      {g.coverPhotoUrl && (
        <div>
          <div className="text-muted-foreground mb-1">Cover photo:</div>
          <img src={g.coverPhotoUrl} alt="" className="h-32 w-auto rounded" />
        </div>
      )}
      {g.photoUrls && g.photoUrls.length > 0 && (
        <div>
          <div className="text-muted-foreground mb-1">All photos ({g.photoUrls.length}):</div>
          <div className="flex flex-wrap gap-2">
            {g.photoUrls.map((url: string) => (
              <img
                key={url}
                src={url}
                alt=""
                className="h-20 w-20 rounded object-cover"
                loading="lazy"
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

const ManualGalleries = () => {
  const record = useRecordContext();
  const urls = (record?.galleries as string[] | undefined) ?? [];
  if (urls.length === 0) {
    return <p className="text-sm text-muted-foreground">(none)</p>;
  }
  return (
    <div className="flex flex-wrap gap-2">
      {urls.map((url) => (
        <img
          key={url}
          src={url}
          alt=""
          className="h-20 w-20 rounded object-cover"
          loading="lazy"
        />
      ))}
    </div>
  );
};

export const RestaurantShow = () => (
  <Show>
    <SimpleShowLayout>
      <TextField source="name.zh" />
      <TextField source="name.en" />
      <TextField source="name.de" />
      <div className="my-2">
        <div className="mb-1 text-xs uppercase text-muted-foreground">Tags</div>
        <TagsPanel />
      </div>
      <TextField source="address.addressLine1" />
      <TextField source="address.district" />
      <TextField source="address.postalCode" />
      <NumberField source="latitude" />
      <NumberField source="longitude" />
      <TextField source="phone" label="Phone" />
      <TextField source="priceRange" label="Price range" />
      <TextField source="logoUrl" label="Logo URL" />
      <TextField source="description.zh" />
      <TextField source="description.en" />
      <TextField source="description.de" />
      <BooleanField source="hasDiscount" label="Discount partner (优惠)" />
      <TextField source="discountInfo.zh" label="Offer text (中文)" />
      <TextField source="discountInfo.en" label="Offer text (English)" />
      <TextField source="editorialNote.zh" label="Editorial note (中文)" />
      <TextField source="editorialNote.en" label="Editorial note (English)" />
      <TextField source="chain.brand" label="Chain brand" />
      <TextField source="chain.branch" label="Branch label" />
      <TextField source="placeId" label="Google placeId" />
      <NumberField source="viewCount" />
      <BooleanField source="hidden" />
      <div className="my-4 border-t pt-4">
        <h3 className="mb-3 text-sm font-semibold uppercase text-muted-foreground">
          Manual gallery
        </h3>
        <ManualGalleries />
      </div>
      <div className="my-4 border-t pt-4">
        <h3 className="mb-3 text-sm font-semibold uppercase text-muted-foreground">
          Google Places (auto-synced)
        </h3>
        <GoogleDataPanel />
      </div>
    </SimpleShowLayout>
  </Show>
);
