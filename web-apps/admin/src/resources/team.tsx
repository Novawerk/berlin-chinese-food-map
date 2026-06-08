import {
  List,
  DataTable,
  Edit,
  Create,
  SimpleForm,
  TextInput,
  NumberInput,
  Show,
  SimpleShowLayout,
  TextField,
  NumberField,
} from "@/components/admin";

export const TeamList = () => (
  <List>
    <DataTable>
      <DataTable.Col source="name.en" label="Name (EN)" />
      <DataTable.Col source="name.zh" label="Name (ZH)" />
      <DataTable.Col source="role.en" label="Role" />
      <DataTable.Col source="email" label="Email" />
      <DataTable.Col source="sortOrder" label="Order" />
    </DataTable>
  </List>
);

const TeamForm = () => (
  <SimpleForm>
    <TextInput source="name.en" label="Name (English)" isRequired />
    <TextInput source="name.zh" label="Name (Chinese)" isRequired />
    <TextInput source="name.de" label="Name (German)" />

    <TextInput source="role.en" label="Role (English)" isRequired />
    <TextInput source="role.zh" label="Role (Chinese)" isRequired />
    <TextInput source="role.de" label="Role (German)" />

    <TextInput source="avatarUrl" label="Avatar URL" />
    <TextInput source="email" label="Email" />
    <TextInput source="github" label="GitHub" />
    <NumberInput source="sortOrder" label="Sort Order" defaultValue={0} />
  </SimpleForm>
);

export const TeamEdit = () => (
  <Edit>
    <TeamForm />
  </Edit>
);

export const TeamCreate = () => (
  <Create>
    <TeamForm />
  </Create>
);

export const TeamShow = () => (
  <Show>
    <SimpleShowLayout>
      <TextField source="name.en" />
      <TextField source="name.zh" />
      <TextField source="role.en" />
      <TextField source="role.zh" />
      <TextField source="email" />
      <TextField source="github" />
      <NumberField source="sortOrder" />
    </SimpleShowLayout>
  </Show>
);
