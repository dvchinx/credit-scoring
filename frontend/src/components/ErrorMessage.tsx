import { ApiError } from "../api/client";

export function ErrorMessage({ error }: { error: unknown }) {
  if (!error) return null;
  const message = error instanceof Error ? error.message : "Error inesperado";
  const status = error instanceof ApiError && error.status > 0 ? ` (${error.status})` : "";
  return (
    <p role="alert" className="error">
      {message}
      {status}
    </p>
  );
}
