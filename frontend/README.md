# Frontend — Credit Scoring

SPA en React + TypeScript (Vite) que consume el API de Spring Boot del backend. Es una capa de presentación: no contiene reglas de negocio ni habla con el servicio de ML. Ver el [README raíz](../README.md#frontend-frontend) para las pantallas y las decisiones de diseño.

## Desarrollo

```bash
cp .env.example .env   # VITE_API_BASE_URL=http://localhost:8080
npm install
npm run dev            # http://localhost:5173
```

El backend debe aceptar el origen del frontend en `CORS_ALLOWED_ORIGINS` (por defecto incluye `http://localhost:5173` y `http://localhost:3000`).

## Scripts

| Comando | Qué hace |
|---|---|
| `npm run dev` | Servidor de desarrollo de Vite |
| `npm test` | Tests con Vitest + Testing Library (fetch simulado, no necesita backend) |
| `npm run typecheck` | Comprobación de tipos con TypeScript |
| `npm run build` | Typecheck + build de producción en `dist/` |

## Estructura

```
src/
├── api/          Cliente HTTP tipado (JWT, errores por campo, 401 → sesión expirada) y tipos de los DTOs
├── components/   Layout, badges de estado, formato de números y fechas
├── features/
│   ├── auth/                 Sesión en memoria, login, guardas de ruta y de rol
│   ├── credit-applications/  Listado, alta/edición y detalle de solicitudes
│   ├── decisions/            Historial de decisiones y gráfico SHAP
│   ├── what-if/              Simulador "¿qué pasaría si…?"
│   ├── models/               Versiones de modelo y activaciones (ADMIN)
│   └── audit/                Dashboard de auditoría
├── routes.tsx    Rutas (también las monta la suite de tests en un router en memoria)
└── test/         Setup de Vitest y utilidades (app completa sobre un fetch simulado)
```

## Docker

```bash
docker build --build-arg VITE_API_BASE_URL=http://localhost:8080 -t credit-scoring-frontend .
```

La URL del API se incrusta en el build (Vite la reemplaza en el bundle). nginx sirve los estáticos con fallback a `index.html` para las rutas del cliente.
