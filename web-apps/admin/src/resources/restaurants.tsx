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
} from "@/components/admin";
import { useUpdate, useRecordContext, useRefresh } from "ra-core";
import { Button } from "@/components/ui/button";
import { Download, EyeOff, Eye } from "lucide-react";
import { downloadYamlZip } from "@/lib/export-yaml";
import { useState } from "react";

const TAG_CHOICES = [
  // Regional
  { id: "SICHUAN", name: "Sichuan 川菜" },
  { id: "CANTONESE", name: "Cantonese 粤菜" },
  { id: "NORTHERN", name: "Northern 北方菜" },
  { id: "NORTHEASTERN", name: "Northeastern 东北菜" },
  { id: "SHANGHAINESE", name: "Shanghainese 江浙沪" },
  { id: "HUNAN", name: "Hunan 湘菜" },
  { id: "XINJIANG", name: "Xinjiang 新疆菜" },
  { id: "TAIWANESE", name: "Taiwanese 台菜" },
  { id: "MUSLIM", name: "Halal 清真" },
  // Format
  { id: "HOTPOT", name: "Hotpot 火锅" },
  { id: "BBQ", name: "BBQ 烧烤" },
  { id: "NOODLES", name: "Noodles 面食" },
  { id: "DUMPLINGS", name: "Dumplings 饺子" },
  { id: "DIM_SUM", name: "Dim Sum 点心" },
  { id: "MALATANG", name: "Malatang 麻辣烫" },
  { id: "VEGETARIAN", name: "Vegetarian 素食" },
  { id: "BREAKFAST", name: "Breakfast 早餐" },
  { id: "TEA_HOUSE", name: "Tea House 茶寮" },
  { id: "BAKERY", name: "Bakery 烘焙" },
  { id: "STREET_FOOD", name: "Street Food 小吃" },
  { id: "FUSION", name: "Modern Chinese 创新中餐" },
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

export const RestaurantList = () => (
  <List actions={<ExportYamlButton />}>
    <DataTable>
      <DataTable.Col source="name.zh" label="Name (ZH)" />
      <DataTable.Col source="name.en" label="Name (EN)" />
      <DataTable.Col source="tags" label="Tags" />
      <DataTable.Col source="address.district" label="District" />
      <DataTable.Col source="featured" label="Featured" />
      <DataTable.Col source="hidden" label="Hidden" />
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
      helperText="Pick 1–3 tags. First one is treated as the primary tag."
    />
    <TextInput source="phone" label="Phone" />
    <TextInput source="priceRange" label="Price Range" />
    <TextInput source="logoUrl" label="Logo URL" />

    <TextInput source="address.addressLine1" label="Address Line 1" isRequired />
    <TextInput source="address.addressLine2" label="Address Line 2" />
    <TextInput source="address.note" label="Note" />
    <TextInput source="address.postalCode" label="Postal Code" isRequired />
    <TextInput source="address.district" label="District" isRequired />
    <TextInput source="address.city" label="City" defaultValue="Berlin" />
    <TextInput source="address.country" label="Country" defaultValue="Germany" />

    <NumberInput source="latitude" label="Latitude" isRequired />
    <NumberInput source="longitude" label="Longitude" isRequired />

    <TextInput source="description.en" label="Description (English)" multiline />
    <TextInput source="description.zh" label="Description (Chinese)" multiline />
    <TextInput source="description.de" label="Description (German)" multiline />

    <BooleanInput source="featured" label="Featured (柏林慢慢游甄选)" defaultValue={false} />
    <TextInput source="editorialNote.zh" label="Editorial note (中文)" />
    <TextInput source="editorialNote.en" label="Editorial note (English)" />

    <TextInput source="chain.brand" label="Chain brand" helperText="Set on every branch sharing the same operator." />
    <TextInput source="chain.branch" label="Branch label" />

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

export const RestaurantShow = () => (
  <Show>
    <SimpleShowLayout>
      <TextField source="name.zh" />
      <TextField source="name.en" />
      <TextField source="tags" />
      <TextField source="address.addressLine1" />
      <TextField source="address.district" />
      <TextField source="phone" />
      <TextField source="priceRange" />
      <BooleanField source="featured" />
      <TextField source="editorialNote.zh" />
      <TextField source="chain.brand" />
      <TextField source="chain.branch" />
      <NumberField source="viewCount" />
      <BooleanField source="hidden" />
    </SimpleShowLayout>
  </Show>
);
