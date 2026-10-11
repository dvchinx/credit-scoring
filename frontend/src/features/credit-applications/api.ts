import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { CreditApplication, CreditApplicationRequest, Page } from "../../api/types";
import { useApi } from "../auth/AuthContext";

export const applicationKeys = {
  all: ["applications"] as const,
  page: (page: number, size: number) => ["applications", "page", page, size] as const,
  detail: (id: string) => ["applications", "detail", id] as const,
};

export function useApplicationsPage(page: number, size: number) {
  const api = useApi();
  return useQuery({
    queryKey: applicationKeys.page(page, size),
    queryFn: () => api.get<Page<CreditApplication>>(`/credit-applications?page=${page}&size=${size}&sort=createdAt,desc`),
  });
}

export function useApplication(id: string) {
  const api = useApi();
  return useQuery({
    queryKey: applicationKeys.detail(id),
    queryFn: () => api.get<CreditApplication>(`/credit-applications/${id}`),
  });
}

export function useSaveApplication(id?: string) {
  const api = useApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreditApplicationRequest) =>
      id
        ? api.put<CreditApplication>(`/credit-applications/${id}`, request)
        : api.post<CreditApplication>("/credit-applications", request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: applicationKeys.all }),
  });
}
