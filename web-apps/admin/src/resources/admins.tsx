import {
  List,
  DataTable,
  Edit,
  Create,
  SimpleForm,
  TextInput,
  SelectInput,
  BooleanInput,
  BooleanField,
  Show,
  SimpleShowLayout,
  TextField,
} from "@/components/admin";
import { useRecordContext } from "ra-core";
import { ShieldCheck, PenLine } from "lucide-react";

const ROLE_CHOICES = [
  { id: "admin", name: "Admin — full access" },
  { id: "editor", name: "Editor — restaurants + feedback" },
];

const RoleCell = () => {
  const record = useRecordContext();
  const role = record?.role as string | undefined;
  if (role === "admin") {
    return (
      <span className="inline-flex items-center gap-1 rounded bg-indigo-100 px-1.5 py-0.5 text-xs font-medium text-indigo-800">
        <ShieldCheck className="h-3 w-3" /> Admin
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 rounded bg-amber-100 px-1.5 py-0.5 text-xs font-medium text-amber-800">
      <PenLine className="h-3 w-3" /> Editor
    </span>
  );
};

const StatusCell = () => {
  const record = useRecordContext();
  return record?.disabled ? (
    <span className="inline-block rounded bg-rose-100 px-1.5 py-0.5 text-xs font-medium text-rose-700">
      disabled
    </span>
  ) : (
    <span className="inline-block rounded bg-emerald-100 px-1.5 py-0.5 text-xs font-medium text-emerald-700">
      active
    </span>
  );
};

export const AdminList = () => (
  <List perPage={50} sort={{ field: "email", order: "ASC" }}>
    <DataTable>
      <DataTable.Col source="email" label="Email" />
      <DataTable.Col source="displayName" label="Name" />
      <DataTable.Col source="role" label="Role" disableSort>
        <RoleCell />
      </DataTable.Col>
      <DataTable.Col source="disabled" label="Status" disableSort>
        <StatusCell />
      </DataTable.Col>
      <DataTable.Col source="note" label="Note" />
    </DataTable>
  </List>
);

// On create the email IS the document id, so it's required and editable. On
// edit it's read-only — changing it would orphan the entry (and break the
// security-rule lookup, which keys off the login email).
export const AdminCreate = () => (
  <Create>
    <SimpleForm>
      <TextInput
        source="email"
        label="Login email"
        isRequired
        helperText="Must match the teammate's Firebase sign-in email exactly. Stored lowercase."
        format={(v?: string) => v ?? ""}
        parse={(v?: string) => (v ? v.trim().toLowerCase() : v)}
      />
      <SelectInput source="role" label="Role" choices={ROLE_CHOICES} defaultValue="editor" isRequired />
      <TextInput source="displayName" label="Display name" />
      <TextInput source="note" label="Note" helperText="Optional — e.g. who invited them, area of ownership." />
      <BooleanInput
        source="disabled"
        label="Disabled"
        helperText="Disabled accounts keep their entry but are locked out of the panel."
        defaultValue={false}
      />
    </SimpleForm>
  </Create>
);

export const AdminEdit = () => (
  <Edit>
    <SimpleForm>
      <TextInput source="email" label="Login email" readOnly disabled />
      <SelectInput source="role" label="Role" choices={ROLE_CHOICES} isRequired />
      <TextInput source="displayName" label="Display name" />
      <TextInput source="note" label="Note" />
      <BooleanInput source="disabled" label="Disabled" defaultValue={false} />
    </SimpleForm>
  </Edit>
);

export const AdminShow = () => (
  <Show>
    <SimpleShowLayout>
      <TextField source="email" label="Login email" />
      <TextField source="displayName" label="Display name" />
      <TextField source="role" label="Role" />
      <BooleanField source="disabled" label="Disabled" />
      <TextField source="note" label="Note" />
    </SimpleShowLayout>
  </Show>
);
