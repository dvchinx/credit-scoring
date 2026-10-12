import { NavLink, Outlet } from "react-router";
import { useAuth } from "../features/auth/AuthContext";
import { Brand, initials } from "./Brand";

export function Layout() {
  const { session, logout } = useAuth();
  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar-inner">
          <Brand />
          <nav aria-label="Principal">
            <NavLink to="/applications">Solicitudes</NavLink>
            <NavLink to="/audit">Auditoría</NavLink>
            {session?.role === "ADMIN" && <NavLink to="/models">Modelos</NavLink>}
          </nav>
          <div className="user">
            <span className="user-chip">
              <span className="avatar avatar-sm" aria-hidden="true">
                {initials(session?.username ?? "")}
              </span>
              <span className="who">
                {session?.username}
                <span className="role"> · {session?.role}</span>
              </span>
            </span>
            <button type="button" className="ghost" onClick={logout}>
              Cerrar sesión
            </button>
          </div>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
