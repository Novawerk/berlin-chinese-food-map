import { Resource } from "ra-core";
import { Admin } from "@/components/admin";
import { UtensilsCrossed, Users, FileText, MessageSquare, ShieldCheck } from "lucide-react";
import { dataProvider } from "./dataProvider";
import { authProvider } from "./authProvider";
import {
  RestaurantList,
  RestaurantEdit,
  RestaurantCreate,
  RestaurantShow,
} from "./resources/restaurants";
import {
  TeamList,
  TeamEdit,
  TeamCreate,
  TeamShow,
} from "./resources/team";
import {
  ChangelogList,
  ChangelogEdit,
  ChangelogCreate,
  ChangelogShow,
} from "./resources/changelog";
import {
  FeedbackList,
  FeedbackShow,
  FeedbackEdit,
} from "./resources/feedback";
import {
  AdminList,
  AdminEdit,
  AdminCreate,
  AdminShow,
} from "./resources/admins";
import { Dashboard } from "./pages/Dashboard";

const App = () => (
  <Admin
    dataProvider={dataProvider}
    authProvider={authProvider}
    dashboard={Dashboard}
  >
    <Resource
      name="restaurants"
      icon={UtensilsCrossed}
      list={RestaurantList}
      edit={RestaurantEdit}
      create={RestaurantCreate}
      show={RestaurantShow}
      recordRepresentation={(record) =>
        record?.name?.zh || record?.name?.en || String(record?.id ?? "")
      }
    />
    <Resource
      name="team_members"
      icon={Users}
      list={TeamList}
      edit={TeamEdit}
      create={TeamCreate}
      show={TeamShow}
    />
    <Resource
      name="changelog"
      icon={FileText}
      list={ChangelogList}
      edit={ChangelogEdit}
      create={ChangelogCreate}
      show={ChangelogShow}
    />
    <Resource
      name="feedback"
      icon={MessageSquare}
      list={FeedbackList}
      show={FeedbackShow}
      edit={FeedbackEdit}
    />
    <Resource
      name="admins"
      icon={ShieldCheck}
      options={{ label: "Team Access" }}
      list={AdminList}
      edit={AdminEdit}
      create={AdminCreate}
      show={AdminShow}
      recordRepresentation={(record) =>
        record?.displayName || record?.email || String(record?.id ?? "")
      }
    />
  </Admin>
);

export default App;
