import { describe, expect, it, vi } from "vitest";
import { ApiError, createApiClient, parseFieldErrors } from "./client";

function stubFetch(status: number, body?: unknown) {
  const fetchMock = vi.fn(async () =>
    new Response(body === undefined ? null : JSON.stringify(body), { status }),
  );
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("parseFieldErrors", () => {
  it("convierte 'campo: mensaje' del backend en errores por campo, incluidos los anidados", () => {
    expect(
      parseFieldErrors([
        "monthlyIncome: el ingreso mensual debe ser positivo",
        "creditHistory.revolvingUtilization: no puede ser negativa",
        "mensaje sin campo",
      ]),
    ).toEqual({
      monthlyIncome: "el ingreso mensual debe ser positivo",
      "creditHistory.revolvingUtilization": "no puede ser negativa",
    });
  });
});

describe("createApiClient", () => {
  it("envía el JWT y el cuerpo JSON a la URL configurada", async () => {
    const fetchMock = stubFetch(200, { ok: true });
    const client = createApiClient({ baseUrl: "http://api.test/", getToken: () => "abc", onUnauthorized: vi.fn() });

    await client.post("/credit-applications", { a: 1 });

    expect(fetchMock).toHaveBeenCalledWith("http://api.test/credit-applications", {
      method: "POST",
      headers: { Authorization: "Bearer abc", "Content-Type": "application/json" },
      body: JSON.stringify({ a: 1 }),
    });
  });

  it("lanza ApiError con mensaje y errores de validación del backend", async () => {
    stubFetch(400, { status: 400, error: "Bad Request", message: "La solicitud contiene datos inválidos", details: ["documentId: obligatorio"] });
    const client = createApiClient({ baseUrl: "http://api.test", getToken: () => "abc", onUnauthorized: vi.fn() });

    const error = await client.get("/x").catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(400);
    expect((error as ApiError).message).toBe("La solicitud contiene datos inválidos");
    expect((error as ApiError).fieldErrors).toEqual({ documentId: "obligatorio" });
  });

  it("avisa de sesión expirada cuando una petición autenticada recibe 401", async () => {
    stubFetch(401, { status: 401, error: "Unauthorized", message: "Unauthorized" });
    const onUnauthorized = vi.fn();
    const client = createApiClient({ baseUrl: "http://api.test", getToken: () => "expired", onUnauthorized });

    await expect(client.get("/credit-applications")).rejects.toBeInstanceOf(ApiError);
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });

  it("un 401 sin token (credenciales inválidas en el login) no se trata como sesión expirada", async () => {
    stubFetch(401, { status: 401, error: "Unauthorized", message: "Usuario o contraseña inválidos" });
    const onUnauthorized = vi.fn();
    const client = createApiClient({ baseUrl: "http://api.test", getToken: () => null, onUnauthorized });

    await expect(client.post("/auth/login", {})).rejects.toBeInstanceOf(ApiError);
    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it("un fallo de red se reporta como ApiError con status 0", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => { throw new TypeError("Failed to fetch"); }));
    const client = createApiClient({ baseUrl: "http://api.test", getToken: () => null, onUnauthorized: vi.fn() });

    await expect(client.get("/x")).rejects.toMatchObject({ status: 0, message: "No se pudo conectar con el servidor" });
  });
});
