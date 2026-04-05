import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthGuard } from "./components/AuthGuard";
import { LoginPage } from "./pages/LoginPage";
import { RestaurantListPage } from "./pages/RestaurantListPage";
import { RestaurantFormPage } from "./pages/RestaurantFormPage";
import "./App.css";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <AuthGuard>
              <RestaurantListPage />
            </AuthGuard>
          }
        />
        <Route
          path="/new"
          element={
            <AuthGuard>
              <RestaurantFormPage />
            </AuthGuard>
          }
        />
        <Route
          path="/edit/:id"
          element={
            <AuthGuard>
              <RestaurantFormPage />
            </AuthGuard>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
