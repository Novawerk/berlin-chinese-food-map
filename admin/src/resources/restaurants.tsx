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
} from "@/components/admin";

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

export const RestaurantList = () => (
  <List>
    <DataTable>
      <DataTable.Col source="name.zh" label="Name (ZH)" />
      <DataTable.Col source="name.en" label="Name (EN)" />
      <DataTable.Col source="cuisineType" label="Cuisine" />
      <DataTable.Col source="address.district" label="District" />
      <DataTable.Col source="visitCount" label="Visits" />
      <DataTable.Col source="viewCount" label="Views" />
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
    </SimpleShowLayout>
  </Show>
);
