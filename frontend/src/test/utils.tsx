import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router";
import { vi } from "vitest";
import type { CreditApplication, CreditDecision } from "../api/types";
import type { Session } from "../features/auth/AuthContext";
import { buildRoutes } from "../routes";

export const API = "http://api.test";

export const ADMIN: Session = { token: "admin-token", username: "admin", role: "ADMIN", expiresAt: Date.now() + 3_600_000 };
export const ANALYST: Session = { token: "analyst-token", username: "ana", role: "ANALYST", expiresAt: Date.now() + 3_600_000 };

export interface RecordedCall {
  method: string;
  path: string;
  body: unknown;
  headers: Record<string, string>;
}

type Handler = (body: unknown) => { status?: number; body?: unknown };

/** Sustituye fetch: cada clave es "MÉTODO /ruta?query" y devuelve el status y cuerpo simulados del backend. */
export function mockApi(handlers: Record<string, Handler>): RecordedCall[] {
  const calls: RecordedCall[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string, init?: RequestInit) => {
      const method = init?.method ?? "GET";
      const path = url.replace(API, "");
      const body = typeof init?.body === "string" ? JSON.parse(init.body) : undefined;
      calls.push({ method, path, body, headers: (init?.headers ?? {}) as Record<string, string> });
      const handler = handlers[`${method} ${path}`];
      if (!handler) {
        return new Response(JSON.stringify({ status: 404, error: "Not Found", message: `Sin handler: ${method} ${path}` }), {
          status: 404,
        });
      }
      const result = handler(body);
      const status = result.status ?? 200;
      return new Response(result.body === undefined ? null : JSON.stringify(result.body), {
        status,
        headers: { "Content-Type": "application/json" },
      });
    }),
  );
  return calls;
}

export function renderApp(path: string, session: Session | null = ADMIN) {
  const router = createMemoryRouter(buildRoutes(API, session), { initialEntries: [path] });
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
  return router;
}

export const APPLICATION: CreditApplication = {
  id: "11111111-1111-1111-1111-111111111111",
  applicantFullName: "Ada Lovelace",
  documentId: "DOC-001",
  birthDate: "1990-05-10",
  monthlyIncome: 3500,
  requestedAmount: 12000,
  loanTermMonths: 24,
  employmentYears: 4.5,
  existingMonthlyDebt: 200,
  numberOfDependents: 1,
  creditHistory: {
    revolvingUtilization: 0.35,
    openCreditLines: 5,
    realEstateLoans: 1,
    latePayments30To59Days: 0,
    latePayments60To89Days: 0,
    latePayments90DaysOrMore: 0,
  },
  status: "PENDING",
  createdBy: "admin",
  createdAt: "2026-10-18T10:00:00Z",
  updatedAt: "2026-10-18T10:00:00Z",
};

export const POLICY = { approveMaxProbability: 0.3, rejectMinProbability: 0.6, maxDebtRatio: 0.5, minApplicantAge: 18 };

export const COMPLETED_DECISION: CreditDecision = {
  id: "d1",
  creditApplicationId: APPLICATION.id,
  status: "COMPLETED",
  outcome: "APPROVED",
  probabilityOfDefault: 0.12,
  riskScore: 784,
  modelVersion: "20261004T120000Z",
  reasons: ["Probabilidad de default de 12.0% dentro del umbral de aprobación de 30.0%"],
  explanation: {
    baseValue: -0.4,
    outputValue: -1.99,
    contributions: [
      { feature: "age", value: 36, shapValue: -0.62, direction: "DECREASES_RISK" },
      { feature: "number_of_time_30_59_days_past_due_not_worse", value: 1, shapValue: 0.35, direction: "INCREASES_RISK" },
    ],
  },
  modelInput: { age: 36 },
  policy: POLICY,
  failureReason: null,
  decidedBy: "admin",
  decidedAt: "2026-10-18T11:00:00Z",
};

export const FAILED_DECISION: CreditDecision = {
  ...COMPLETED_DECISION,
  id: "d0",
  status: "FAILED",
  outcome: null,
  probabilityOfDefault: null,
  riskScore: null,
  modelVersion: null,
  reasons: [],
  explanation: null,
  failureReason: "el servicio de ML no respondió correctamente en /score",
};
