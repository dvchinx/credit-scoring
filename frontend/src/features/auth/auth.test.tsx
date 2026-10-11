import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { ADMIN, ANALYST, APPLICATION, mockApi, renderApp } from "../../test/utils";

const LIST = "GET /credit-applications?page=0&size=10&sort=createdAt,desc";
const page = { content: [APPLICATION], page: { size: 10, number: 0, totalElements: 1, totalPages: 1 } };

describe("autenticación", () => {
  it("sin sesión, una ruta protegida redirige al login", async () => {
    renderApp("/applications", null);

    expect(await screen.findByRole("button", { name: "Iniciar sesión" })).toBeInTheDocument();
  });

  it("al iniciar sesión guarda el token en memoria y lo usa en las siguientes peticiones", async () => {
    const calls = mockApi({
      "POST /auth/login": () => ({
        body: { token: "jwt-1", tokenType: "Bearer", expiresInMs: 3_600_000, username: "admin", role: "ADMIN" },
      }),
      [LIST]: () => ({ body: page }),
    });
    renderApp("/login", null);

    await userEvent.type(screen.getByLabelText("Usuario"), "admin");
    await userEvent.type(screen.getByLabelText("Contraseña"), "secret");
    await userEvent.click(screen.getByRole("button", { name: "Iniciar sesión" }));

    expect(await screen.findByText("Ada Lovelace")).toBeInTheDocument();
    const listCall = calls.find((call) => call.path.startsWith("/credit-applications"));
    expect(listCall?.headers.Authorization).toBe("Bearer jwt-1");
    expect(localStorage.length).toBe(0);
  });

  it("con credenciales inválidas muestra el error sin salir del login", async () => {
    mockApi({ "POST /auth/login": () => ({ status: 401, body: { status: 401, error: "Unauthorized", message: "x" } }) });
    renderApp("/login", null);

    await userEvent.type(screen.getByLabelText("Usuario"), "admin");
    await userEvent.type(screen.getByLabelText("Contraseña"), "mala");
    await userEvent.click(screen.getByRole("button", { name: "Iniciar sesión" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Usuario o contraseña inválidos");
  });

  it("si el backend responde 401 con sesión abierta, vuelve al login avisando que expiró y recuerda la página", async () => {
    mockApi({ [LIST]: () => ({ status: 401, body: { status: 401, error: "Unauthorized", message: "Unauthorized" } }) });
    const router = renderApp("/applications", ADMIN);

    expect(await screen.findByText("Tu sesión expiró. Vuelve a iniciar sesión.")).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/login");
    expect(router.state.location.state).toEqual({ from: "/applications" });
  });

  it("un ANALYST no ve la sección de modelos ni puede abrirla", async () => {
    mockApi({ [LIST]: () => ({ body: page }) });
    const router = renderApp("/applications", ANALYST);

    expect(await screen.findByText("Ada Lovelace")).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Modelos" })).not.toBeInTheDocument();

    await router.navigate("/models");
    await waitFor(() => expect(screen.getByText("Acceso restringido")).toBeInTheDocument());
  });
});
