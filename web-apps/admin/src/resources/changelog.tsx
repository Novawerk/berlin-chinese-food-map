import {
  List,
  DataTable,
  Edit,
  Create,
  SimpleForm,
  TextInput,
  BooleanInput,
  Show,
  SimpleShowLayout,
  TextField,
  BooleanField,
} from "@/components/admin";

export const ChangelogList = () => (
  <List>
    <DataTable>
      <DataTable.Col source="title.en" label="Title" />
      <DataTable.Col source="version" label="Version" />
      <DataTable.Col source="date" label="Date" />
      <DataTable.Col source="published" label="Published" />
    </DataTable>
  </List>
);

const ChangelogForm = () => (
  <SimpleForm>
    <TextInput source="title.en" label="Title (English)" isRequired />
    <TextInput source="title.zh" label="Title (Chinese)" isRequired />
    <TextInput source="title.de" label="Title (German)" />

    <TextInput source="content.en" label="Content (English, Markdown)" multiline />
    <TextInput source="content.zh" label="Content (Chinese, Markdown)" multiline />
    <TextInput source="content.de" label="Content (German, Markdown)" multiline />

    <TextInput source="version" label="Version" />
    <TextInput source="date" label="Date (YYYY-MM-DD)" isRequired />
    <TextInput source="tags" label="Tags (comma separated)" />
    <BooleanInput source="published" label="Published" defaultValue={true} />
  </SimpleForm>
);

export const ChangelogEdit = () => (
  <Edit>
    <ChangelogForm />
  </Edit>
);

export const ChangelogCreate = () => (
  <Create>
    <ChangelogForm />
  </Create>
);

export const ChangelogShow = () => (
  <Show>
    <SimpleShowLayout>
      <TextField source="title.en" />
      <TextField source="title.zh" />
      <TextField source="version" />
      <TextField source="date" />
      <BooleanField source="published" />
      <TextField source="content.en" />
    </SimpleShowLayout>
  </Show>
);
