/** URL del API, inyectada en el build desde VITE_API_BASE_URL. Nunca se hardcodea. */
export function apiBaseUrl(): string {
  const url = import.meta.env.VITE_API_BASE_URL;
  if (!url) {
    throw new Error("Falta VITE_API_BASE_URL: copia frontend/.env.example a frontend/.env");
  }
  return url;
}
