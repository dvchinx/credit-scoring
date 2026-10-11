import { QueryClientProvider } from "@tanstack/react-query";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router";
import { apiBaseUrl } from "./api/config";
import { createQueryClient } from "./queryClient";
import { buildRoutes } from "./routes";
import "./styles.css";

const router = createBrowserRouter(buildRoutes(apiBaseUrl()));
const queryClient = createQueryClient();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
);
