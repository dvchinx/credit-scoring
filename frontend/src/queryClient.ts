import { QueryClient } from "@tanstack/react-query";
import { ApiError } from "./api/client";

/** Los errores 4xx son definitivos (validación, permisos, no encontrado): no se reintentan. */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: (failureCount, error) =>
          !(error instanceof ApiError && error.status >= 400 && error.status < 500) && failureCount < 2,
        refetchOnWindowFocus: false,
      },
      mutations: { retry: false },
    },
  });
}
