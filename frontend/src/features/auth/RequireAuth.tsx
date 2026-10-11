import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router";
import type { Role } from "../../api/types";
import { useAuth } from "./AuthContext";

export function RequireAuth({ children }: { children: ReactNode }) {
  const { session } = useAuth();
  const location = useLocation();
  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <>{children}</>;
}

/**
 * Oculta secciones según el rol solo por usabilidad: la autorización real la aplica el backend
 * (un ANALYST que llame directamente a un endpoint de ADMIN recibe 403).
 */
export function RequireRole({ role, children }: { role: Role; children: ReactNode }) {
  const { session } = useAuth();
  if (session?.role !== role) {
    return (
      <section className="panel">
        <h1>Acceso restringido</h1>
        <p className="muted">Esta sección requiere el rol {role}.</p>
      </section>
    );
  }
  return <>{children}</>;
}
