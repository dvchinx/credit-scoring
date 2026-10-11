import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { APPLICATION, mockApi, renderApp } from "../../test/utils";

describe("formulario de solicitud", () => {
  it("muestra cada error de validación del backend junto a su campo, incluido el historial crediticio", async () => {
    const calls = mockApi({
      "POST /credit-applications": () => ({
        status: 400,
        body: {
          status: 400,
          error: "Bad Request",
          message: "La solicitud contiene datos inválidos",
          details: [
            "monthlyIncome: el ingreso mensual es obligatorio",
            "creditHistory.revolvingUtilization: la utilización de líneas rotativas es obligatoria",
          ],
        },
      }),
    });
    renderApp("/applications/new");

    await userEvent.type(await screen.findByLabelText("Nombre completo"), "Ada Lovelace");
    await userEvent.click(screen.getByRole("button", { name: "Crear solicitud" }));

    expect(await screen.findByText("el ingreso mensual es obligatorio")).toBeInTheDocument();
    expect(screen.getByLabelText(/Ingreso mensual/)).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByLabelText(/Utilización de crédito rotativo/)).toHaveAccessibleDescription(
      "la utilización de líneas rotativas es obligatoria",
    );
    expect(screen.getByRole("alert")).toHaveTextContent("La solicitud contiene datos inválidos");
    expect(calls[0].body).toMatchObject({ applicantFullName: "Ada Lovelace", monthlyIncome: null, creditHistory: { revolvingUtilization: null } });
  });

  it("al crear la solicitud navega a su detalle", async () => {
    mockApi({
      "POST /credit-applications": () => ({ status: 201, body: APPLICATION }),
      [`GET /credit-applications/${APPLICATION.id}`]: () => ({ body: APPLICATION }),
      [`GET /credit-applications/${APPLICATION.id}/decisions`]: () => ({ body: [] }),
    });
    const router = renderApp("/applications/new");

    await userEvent.type(await screen.findByLabelText("Nombre completo"), "Ada Lovelace");
    await userEvent.click(screen.getByRole("button", { name: "Crear solicitud" }));

    expect(await screen.findByText("Esta solicitud todavía no se ha evaluado.")).toBeInTheDocument();
    expect(router.state.location.pathname).toBe(`/applications/${APPLICATION.id}`);
  });
});
