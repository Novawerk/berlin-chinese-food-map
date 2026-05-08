import {
  List,
  DataTable,
  Show,
  SimpleShowLayout,
  TextField,
  Edit,
  SimpleForm,
  SelectInput,
} from "@/components/admin";

const STATUS_CHOICES = [
  { id: "new", name: "New" },
  { id: "reviewing", name: "Reviewing" },
  { id: "actioned", name: "Actioned" },
  { id: "wontfix", name: "Won't fix" },
];

export const FeedbackList = () => (
  <List sort={{ field: "createdAt", order: "DESC" }}>
    <DataTable>
      <DataTable.Col source="category" label="Topic" />
      <DataTable.Col source="message" label="Message" />
      <DataTable.Col source="contact" label="Contact" />
      <DataTable.Col source="status" label="Status" />
      <DataTable.Col source="appVersion" label="Version" />
      <DataTable.Col source="platform" label="Platform" />
      <DataTable.Col source="locale" label="Locale" />
      <DataTable.Col source="createdAt" label="Submitted" />
    </DataTable>
  </List>
);

export const FeedbackShow = () => (
  <Show>
    <SimpleShowLayout>
      <TextField source="category" />
      <TextField source="message" />
      <TextField source="contact" />
      <TextField source="status" />
      <TextField source="appVersion" />
      <TextField source="platform" />
      <TextField source="locale" />
      <TextField source="uid" />
      <TextField source="createdAt" />
    </SimpleShowLayout>
  </Show>
);

export const FeedbackEdit = () => (
  <Edit>
    <SimpleForm>
      <SelectInput source="status" choices={STATUS_CHOICES} defaultValue="new" />
    </SimpleForm>
  </Edit>
);
