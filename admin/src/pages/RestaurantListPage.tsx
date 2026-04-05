import { Link } from "react-router-dom";
import { useRestaurants } from "../hooks/useRestaurants";
import { useAuth } from "../hooks/useAuth";

export function RestaurantListPage() {
  const { restaurants, loading, deleteRestaurant } = useRestaurants();
  const { signOut } = useAuth();

  const handleDelete = async (id: string, name: string) => {
    if (!window.confirm(`Delete "${name}"?`)) return;
    try {
      await deleteRestaurant(id);
    } catch (err) {
      console.error("Delete failed:", err);
      alert("Failed to delete restaurant.");
    }
  };

  return (
    <div className="page">
      <header className="page-header">
        <h1>Restaurants</h1>
        <div className="header-actions">
          <Link to="/new" className="btn btn-primary">
            + Add Restaurant
          </Link>
          <button onClick={signOut} className="btn btn-secondary">
            Sign Out
          </button>
        </div>
      </header>

      {loading ? (
        <div className="loading">Loading restaurants...</div>
      ) : restaurants.length === 0 ? (
        <p className="empty">No restaurants yet. Add one to get started.</p>
      ) : (
        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Name (EN)</th>
                <th>Name (ZH)</th>
                <th>District</th>
                <th>Cuisine</th>
                <th>Visits</th>
                <th>Views</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {restaurants.map((r) => (
                <tr key={r.id}>
                  <td>{r.name.en}</td>
                  <td>{r.name.zh}</td>
                  <td>{r.address?.district ?? "—"}</td>
                  <td>
                    <span className="badge">{r.cuisineType}</span>
                  </td>
                  <td>{r.visitCount}</td>
                  <td>{r.viewCount}</td>
                  <td className="actions">
                    <Link to={`/edit/${r.id}`} className="btn btn-small">
                      Edit
                    </Link>
                    <button
                      className="btn btn-small btn-danger"
                      onClick={() => handleDelete(r.id!, r.name.en)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
