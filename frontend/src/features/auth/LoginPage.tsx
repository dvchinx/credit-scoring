import { type FormEvent, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router";
import { ApiError } from "../../api/client";
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
      <form className="panel login-card" onSubmit={handleSubmit}>
        <h1>Credit Scoring</h1>
        <p className="muted">Motor de scoring crediticio explicable</p>
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
    </main>
  );
}

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : "Error inesperado";
}
