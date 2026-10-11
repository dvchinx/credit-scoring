import type { ApiErrorBody } from "./types";

export class ApiError extends Error {
  readonly status: number;
  readonly details: string[];
  /** Errores de validación por campo, p. ej. {"creditHistory.revolvingUtilization": "no puede ser negativa"}. */
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, message: string, details: string[] = []) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
    this.fieldErrors = parseFieldErrors(details);
  }
}

/** El backend devuelve los errores de validación como "campo: mensaje". */
export function parseFieldErrors(details: string[]): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const detail of details) {
    const separator = detail.indexOf(": ");
    if (separator > 0) {
      errors[detail.slice(0, separator)] = detail.slice(separator + 2);
    }
  }
  return errors;
}

export interface ApiClientOptions {
  baseUrl: string;
  getToken: () => string | null;
  /** Se llama cuando una petición autenticada recibe 401: el token expiró o dejó de ser válido. */
  onUnauthorized: () => void;
}

export interface ApiClient {
  get<T>(path: string): Promise<T>;
  post<T>(path: string, body?: unknown): Promise<T>;
  put<T>(path: string, body: unknown): Promise<T>;
  delete(path: string): Promise<void>;
}

export function createApiClient({ baseUrl, getToken, onUnauthorized }: ApiClientOptions): ApiClient {
  const root = baseUrl.replace(/\/+$/, "");

  async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
    const token = getToken();
    const headers: Record<string, string> = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    if (body !== undefined) headers["Content-Type"] = "application/json";

    let response: Response;
    try {
      response = await fetch(`${root}${path}`, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch {
      throw new ApiError(0, "No se pudo conectar con el servidor");
    }

    if (response.status === 401 && token) {
      onUnauthorized();
    }
    if (!response.ok) {
      const errorBody = await readJson<Partial<ApiErrorBody>>(response);
      throw new ApiError(
        response.status,
        errorBody?.message ?? `Error ${response.status}`,
        errorBody?.details ?? [],
      );
    }
    if (response.status === 204) {
      return undefined as T;
    }
    return (await readJson<T>(response)) as T;
  }

  return {
    get: (path) => request("GET", path),
    post: (path, body) => request("POST", path, body),
    put: (path, body) => request("PUT", path, body),
    delete: (path) => request<void>("DELETE", path),
  };
}

async function readJson<T>(response: Response): Promise<T | null> {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text) as T;
  } catch {
    return null;
  }
}
