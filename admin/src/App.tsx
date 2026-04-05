import { Resource } from "ra-core";
import { Admin } from "@/components/admin";
import { UtensilsCrossed, Users, FileText } from "lucide-react";
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

const App = () => (
  <Admin dataProvider={dataProvider} authProvider={authProvider}>
    <Resource
      name="restaurants"
      icon={UtensilsCrossed}
      list={RestaurantList}
      edit={RestaurantEdit}
      create={RestaurantCreate}
      show={RestaurantShow}
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
  </Admin>
);

export default App;
