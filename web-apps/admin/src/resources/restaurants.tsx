import {
  List,
  DataTable,
  Edit,
  Create,
  SimpleForm,
  TextInput,
  SelectInput,
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

const CUISINE_CHOICES = [
  { id: "SICHUAN", name: "Sichuan" },
  { id: "CANTONESE", name: "Cantonese" },
  { id: "HOTPOT", name: "Hotpot" },
  { id: "BBQ", name: "BBQ" },
  { id: "DIM_SUM", name: "Dim Sum" },
  { id: "NOODLES", name: "Noodles" },
  { id: "GENERAL", name: "General" },
  { id: "OTHER", name: "Other" },
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
      <DataTable.Col source="cuisineType" label="Cuisine" />
      <DataTable.Col source="address.district" label="District" />
      <DataTable.Col source="hidden" label="Hidden" />
      <DataTable.Col source="visitCount" label="Visits" />
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

    <SelectInput
      source="cuisineType"
      label="Cuisine Type"
      choices={CUISINE_CHOICES}
      isRequired
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
      <TextField source="cuisineType" />
      <TextField source="address.addressLine1" />
      <TextField source="address.district" />
      <TextField source="phone" />
      <TextField source="priceRange" />
      <NumberField source="visitCount" />
      <NumberField source="viewCount" />
      <BooleanField source="hidden" />
    </SimpleShowLayout>
  </Show>
);
