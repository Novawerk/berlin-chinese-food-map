import { Resource } from "ra-core";
import { Admin } from "@/components/admin";
import { UtensilsCrossed, Users, FileText, MessageSquare } from "lucide-react";
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

const App = () => (
  <Admin dataProvider={dataProvider} authProvider={authProvider}>
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
  </Admin>
);

export default App;
