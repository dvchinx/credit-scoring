import { type FormEvent, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router";
import { ApiError } from "../../api/client";
import { Brand } from "../../components/Brand";
import { useAuth } from "./AuthContext";

export function LoginPage() {
  const { session, sessionExpired, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as { from?: string } | null;
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (session) {
    return <Navigate to={state?.from ?? "/applications"} replace />;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(username, password);
      navigate(state?.from ?? "/applications", { replace: true });
    } catch (e) {
      setError(e instanceof ApiError && e.status === 401 ? "Usuario o contraseña inválidos" : messageOf(e));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="login">
      <aside className="login-hero">
        <Brand />
        <div>
          <h2>Cada decisión de crédito, explicada y trazable.</h2>
          <p>
            El motor evalúa el riesgo con un modelo versionado, aplica la política vigente y guarda por qué decidió lo
            que decidió.
          </p>
          <ul className="login-points">
            <li>
              <i aria-hidden="true">◐</i>
              <div>
                <strong>Explicable</strong>
                <span>Contribuciones SHAP por variable en cada decisión.</span>
              </div>
            </li>
            <li>
              <i aria-hidden="true">⧉</i>
              <div>
                <strong>Versionado</strong>
                <span>Cada decisión queda atada a la versión exacta del modelo.</span>
              </div>
            </li>
            <li>
              <i aria-hidden="true">↺</i>
              <div>
                <strong>Simulable</strong>
                <span>¿Qué pasaría si…? sin tocar la decisión real.</span>
              </div>
            </li>
          </ul>
        </div>
        <p className="login-foot">Proyecto de portafolio · Backend + IA</p>
      </aside>
      <div className="login-main">
        <form className="login-card" onSubmit={handleSubmit}>
          <div>
            <h1>Iniciar sesión</h1>
            <p className="muted">Motor de scoring crediticio explicable</p>
          </div>
          {sessionExpired && <p className="notice">Tu sesión expiró. Vuelve a iniciar sesión.</p>}
          <label>
            Usuario
            <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
          </label>
          <label>
            Contraseña
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </label>
          {error && (
            <p role="alert" className="error">
              {error}
            </p>
          )}
          <button type="submit" disabled={submitting}>
            {submitting ? "Entrando…" : "Iniciar sesión"}
          </button>
        </form>
      </div>
    </main>
  );
}

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : "Error inesperado";
}
