import { NavLink, Outlet } from "react-router";
import { useAuth } from "../features/auth/AuthContext";

export function Layout() {
  const { session, logout } = useAuth();
  return (
    <div className="app">
      <header className="topbar">
        <span className="brand">Credit Scoring</span>
        <nav aria-label="Principal">
          <NavLink to="/applications">Solicitudes</NavLink>
          <NavLink to="/audit">Auditoría</NavLink>
          {session?.role === "ADMIN" && <NavLink to="/models">Modelos</NavLink>}
        </nav>
        <div className="user">
          <span>
            {session?.username} <span className="muted">· {session?.role}</span>
          </span>
          <button type="button" className="secondary" onClick={logout}>
            Cerrar sesión
          </button>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
