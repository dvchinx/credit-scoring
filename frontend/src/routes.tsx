import { lazy, type ReactNode, Suspense } from "react";
import { Navigate, Outlet, type RouteObject } from "react-router";
import { Layout } from "./components/Layout";
import { AuthProvider, type Session } from "./features/auth/AuthContext";
import { LoginPage } from "./features/auth/LoginPage";
import { RequireAuth, RequireRole } from "./features/auth/RequireAuth";
import { EditApplicationPage, NewApplicationPage } from "./features/credit-applications/ApplicationFormPage";
import { ApplicationsListPage } from "./features/credit-applications/ApplicationsListPage";

// Las páginas con gráficos (Recharts) se cargan bajo demanda para no inflar el bundle inicial.
const ApplicationDetailPage = lazy(() =>
  import("./features/credit-applications/ApplicationDetailPage").then((m) => ({ default: m.ApplicationDetailPage })),
);
const AuditDashboardPage = lazy(() =>
  import("./features/audit/AuditDashboardPage").then((m) => ({ default: m.AuditDashboardPage })),
);
const ModelVersionsPage = lazy(() =>
  import("./features/models/ModelVersionsPage").then((m) => ({ default: m.ModelVersionsPage })),
);

function Lazy({ children }: { children: ReactNode }) {
  return <Suspense fallback={<p className="loading">Cargando…</p>}>{children}</Suspense>;
}

/** Rutas de la app; los tests las montan en un router en memoria con una sesión inicial. */
export function buildRoutes(baseUrl: string, initialSession: Session | null = null): RouteObject[] {
  return [
    {
      element: (
        <AuthProvider baseUrl={baseUrl} initialSession={initialSession}>
          <Outlet />
        </AuthProvider>
      ),
      children: [
        { path: "/login", element: <LoginPage /> },
        {
          element: (
            <RequireAuth>
              <Layout />
            </RequireAuth>
          ),
          children: [
            { index: true, element: <Navigate to="/applications" replace /> },
            { path: "/applications", element: <ApplicationsListPage /> },
            { path: "/applications/new", element: <NewApplicationPage /> },
            { path: "/applications/:id", element: <Lazy><ApplicationDetailPage /></Lazy> },
            { path: "/applications/:id/edit", element: <EditApplicationPage /> },
            { path: "/audit", element: <Lazy><AuditDashboardPage /></Lazy> },
            {
              path: "/models",
              element: (
                <RequireRole role="ADMIN">
                  <Lazy>
                    <ModelVersionsPage />
                  </Lazy>
                </RequireRole>
              ),
            },
            { path: "*", element: <div className="panel empty"><strong>Página no encontrada</strong>Revisa la dirección o vuelve a las solicitudes.</div> },
          ],
        },
      ],
    },
  ];
}
