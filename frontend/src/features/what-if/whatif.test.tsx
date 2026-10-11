import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import type { WhatIfResponse } from "../../api/types";
import { APPLICATION, mockApi, POLICY, renderApp } from "../../test/utils";
import { buildWhatIfRequest } from "./WhatIfPanel";

describe("buildWhatIfRequest", () => {
  it("solo incluye las variables escritas que difieren del valor real, con el historial anidado", () => {
    expect(
      buildWhatIfRequest(APPLICATION, {
        monthlyIncome: "4200",
        requestedAmount: "12000",
        loanTermMonths: "",
        "creditHistory.revolvingUtilization": "0.1",
      }),
    ).toEqual({ monthlyIncome: 4200, creditHistory: { revolvingUtilization: 0.1 } });
  });

  it("sin cambios reales devuelve un pedido vacío", () => {
    expect(buildWhatIfRequest(APPLICATION, { monthlyIncome: "3500" })).toEqual({});
  });
});

const RESPONSE: WhatIfResponse = {
  id: "s1",
  creditApplicationId: APPLICATION.id,
  modelVersion: "20261004T120000Z",
  changes: [{ variable: "creditHistory.revolvingUtilization", baselineValue: 0.35, scenarioValue: 0.1 }],
  baseline: { outcome: "MANUAL_REVIEW", probabilityOfDefault: 0.41, riskScore: 625, outputValue: -0.3, reasons: ["entre umbrales"] },
  scenario: { outcome: "APPROVED", probabilityOfDefault: 0.27, riskScore: 701, outputValue: -1.0, reasons: ["dentro del umbral"] },
  decisionChanged: true,
  probabilityOfDefaultDelta: -0.14,
  riskScoreDelta: 76,
  baseValue: -0.4,
  featureImpacts: [
    { feature: "revolving_utilization_of_unsecured_lines", baselineValue: 0.35, scenarioValue: 0.1, baselineShap: 0.21, scenarioShap: -0.43, shapDelta: -0.64, changed: true },
    { feature: "age", baselineValue: 36, scenarioValue: 36, baselineShap: -0.6, scenarioShap: -0.6, shapDelta: 0, changed: false },
  ],
  policy: POLICY,
  requestedBy: "admin",
  simulatedAt: "2026-10-18T12:00:00Z",
};

describe("simulador what-if", () => {
  it("envía solo lo que cambió y compara la decisión real con la simulada", async () => {
    const calls = mockApi({
      [`GET /credit-applications/${APPLICATION.id}`]: () => ({ body: APPLICATION }),
      [`GET /credit-applications/${APPLICATION.id}/decisions`]: () => ({ body: [] }),
      [`POST /credit-applications/${APPLICATION.id}/what-if`]: () => ({ status: 201, body: RESPONSE }),
    });
    renderApp(`/applications/${APPLICATION.id}`);

    const simulate = await screen.findByRole("button", { name: "Simular" });
    expect(simulate).toBeDisabled();
    await userEvent.type(screen.getByLabelText("Utilización de crédito rotativo"), "0.1");
    await userEvent.click(simulate);

    expect(await screen.findByText(/La decisión cambiaría: de "Revisión manual" a "Aprobada"/)).toBeInTheDocument();
    const post = calls.find((call) => call.method === "POST");
    expect(post?.body).toEqual({ creditHistory: { revolvingUtilization: 0.1 } });
    expect(screen.getByRole("list", { name: "Efecto por variable" })).toHaveTextContent(
      "Utilización de crédito rotativo: 0,35 → 0,1 (-0.640 en log-odds)",
    );
    expect(screen.getByRole("list", { name: "Efecto por variable" })).not.toHaveTextContent("Edad");
  });

  it("si no hay modelo activo muestra el mensaje del backend", async () => {
    mockApi({
      [`GET /credit-applications/${APPLICATION.id}`]: () => ({ body: APPLICATION }),
      [`GET /credit-applications/${APPLICATION.id}/decisions`]: () => ({ body: [] }),
      [`POST /credit-applications/${APPLICATION.id}/what-if`]: () => ({
        status: 409,
        body: { status: 409, error: "Conflict", message: "No hay ninguna versión de modelo activa" },
      }),
    });
    renderApp(`/applications/${APPLICATION.id}`);

    await userEvent.type(await screen.findByLabelText("Ingreso mensual"), "5000");
    await userEvent.click(screen.getByRole("button", { name: "Simular" }));

    expect(await screen.findByText(/No hay ninguna versión de modelo activa/)).toBeInTheDocument();
  });
});
