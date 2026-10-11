import { useQueryClient } from "@tanstack/react-query";
import { createContext, type ReactNode, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { type ApiClient, createApiClient } from "../../api/client";
import type { LoginResponse, Role } from "../../api/types";

export interface Session {
  token: string;
  username: string;
  role: Role;
  /** Epoch en ms en que expira el JWT. */
  expiresAt: number;
}

interface AuthContextValue {
  session: Session | null;
  /** La última sesión terminó por expiración o por un 401, no por un cierre voluntario. */
  sessionExpired: boolean;
  api: ApiClient;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/**
 * El JWT vive solo en memoria (no en localStorage) para reducir la exposición ante XSS.
 * Contrapartida asumida: recargar la página obliga a iniciar sesión otra vez.
 */
export function AuthProvider({
  baseUrl,
  initialSession = null,
  children,
}: {
  baseUrl: string;
  initialSession?: Session | null;
  children: ReactNode;
}) {
  const [session, setSession] = useState<Session | null>(initialSession);
  const [sessionExpired, setSessionExpired] = useState(false);
  const tokenRef = useRef<string | null>(initialSession?.token ?? null);
  const queryClient = useQueryClient();

  // Sin navegación explícita: al quedar sin sesión, RequireAuth redirige al login conservando la
  // página de origen. El motivo vive aquí y no en el estado del router, para que ningún redirect lo pise.
  const endSession = useCallback(
    (expired: boolean) => {
      tokenRef.current = null;
      setSession(null);
      setSessionExpired(expired);
      queryClient.clear();
    },
    [queryClient],
  );

  const api = useMemo(
    () => createApiClient({ baseUrl, getToken: () => tokenRef.current, onUnauthorized: () => endSession(true) }),
    [baseUrl, endSession],
  );

  useEffect(() => {
    if (!session) return;
    const timer = setTimeout(() => endSession(true), Math.max(0, session.expiresAt - Date.now()));
    return () => clearTimeout(timer);
  }, [session, endSession]);

  const login = useCallback(
    async (username: string, password: string) => {
      const response = await api.post<LoginResponse>("/auth/login", { username, password });
      tokenRef.current = response.token;
      setSessionExpired(false);
      setSession({
        token: response.token,
        username: response.username,
        role: response.role,
        expiresAt: Date.now() + response.expiresInMs,
      });
    },
    [api],
  );

  const value = useMemo(
    () => ({ session, sessionExpired, api, login, logout: () => endSession(false) }),
    [session, sessionExpired, api, login, endSession],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth debe usarse dentro de <AuthProvider>");
  return context;
}

export function useApi(): ApiClient {
  return useAuth().api;
}
