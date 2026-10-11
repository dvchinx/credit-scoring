import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { CreditDecision } from "../../api/types";
import { useApi } from "../auth/AuthContext";
import { applicationKeys } from "../credit-applications/api";

export const decisionKeys = {
  forApplication: (applicationId: string) => ["decisions", applicationId] as const,
};

export function useDecisions(applicationId: string) {
  const api = useApi();
  return useQuery({
    queryKey: decisionKeys.forApplication(applicationId),
    queryFn: () => api.get<CreditDecision[]>(`/credit-applications/${applicationId}/decisions`),
  });
}

/** Un intento fallido también queda en el historial, así que se refresca en éxito y en error. */
export function useEvaluateApplication(applicationId: string) {
  const api = useApi();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api.post<CreditDecision>(`/credit-applications/${applicationId}/decisions`),
    onSettled: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: decisionKeys.forApplication(applicationId) }),
        queryClient.invalidateQueries({ queryKey: applicationKeys.all }),
      ]),
  });
}
