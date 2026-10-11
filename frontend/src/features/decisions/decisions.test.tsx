import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { APPLICATION, COMPLETED_DECISION, FAILED_DECISION, mockApi, renderApp } from "../../test/utils";
import { DecisionCard } from "./DecisionCard";

describe("DecisionCard", () => {
  it("muestra resultado, riesgo, versión de modelo, motivos y las variables que más influyeron", () => {
    render(<DecisionCard decision={COMPLETED_DECISION} />);

    expect(screen.getByText("Aprobada")).toBeInTheDocument();
    expect(screen.getByText("12.0%")).toBeInTheDocument();
    expect(screen.getByText("784")).toBeInTheDocument();
    expect(screen.getByText(/modelo 20261004T120000Z/)).toBeInTheDocument();
    expect(screen.getByText(COMPLETED_DECISION.reasons[0])).toBeInTheDocument();
    const factors = within(screen.getByRole("list", { name: "Variables que más influyeron" })).getAllByRole("listitem");
    expect(factors[0]).toHaveTextContent("Edad = 36: baja el riesgo (-0.620)");
    expect(factors[1]).toHaveTextContent("Moras de 30-59 días = 1: sube el riesgo (+0.350)");
  });

  it("un intento fallido muestra el motivo y ningún score ni explicación", () => {
    render(<DecisionCard decision={FAILED_DECISION} />);

    expect(screen.getByText("Fallida")).toBeInTheDocument();
    expect(screen.getByText(/el servicio de ML no respondió/)).toBeInTheDocument();
    expect(screen.queryByText("Probabilidad de default")).not.toBeInTheDocument();
  });
});

describe("evaluar una solicitud", () => {
  it("si el ML no está disponible muestra el 503 y el intento fallido aparece en el historial", async () => {
    let decisions = [] as (typeof FAILED_DECISION)[];
    mockApi({
      [`GET /credit-applications/${APPLICATION.id}`]: () => ({ body: APPLICATION }),
      [`GET /credit-applications/${APPLICATION.id}/decisions`]: () => ({ body: decisions }),
      [`POST /credit-applications/${APPLICATION.id}/decisions`]: () => {
        decisions = [FAILED_DECISION];
        return {
          status: 503,
          body: { status: 503, error: "Service Unavailable", message: "No se pudo evaluar la solicitud: el servicio de ML no respondió" },
        };
      },
    });
    renderApp(`/applications/${APPLICATION.id}`);

    await userEvent.click(await screen.findByRole("button", { name: "Evaluar con el modelo activo" }));

    expect(await screen.findByText(/No se pudo evaluar la solicitud.*\(503\)/)).toBeInTheDocument();
    expect(await screen.findByText("Fallida")).toBeInTheDocument();
  });
});
