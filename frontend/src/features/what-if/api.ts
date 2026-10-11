import { useMutation } from "@tanstack/react-query";
import type { WhatIfRequest, WhatIfResponse } from "../../api/types";
import { useApi } from "../auth/AuthContext";

export function useSimulateWhatIf(applicationId: string) {
  const api = useApi();
  return useMutation({
    mutationFn: (request: WhatIfRequest) =>
      api.post<WhatIfResponse>(`/credit-applications/${applicationId}/what-if`, request),
  });
}
