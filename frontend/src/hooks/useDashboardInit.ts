import { useCallback, useEffect, useState } from "react";
import type { InitResponse } from "../types/initResponse";
import { apiClient, ApiError } from "../util/apiClient";

type DashboardState =
  | { status: "loading"; retry: () => void }
  | { status: "success"; data: InitResponse; retry: () => void }
  | { status: "error"; error: ApiError; retry: () => void };

type DashboardDataState =
  | { status: "loading" }
  | { status: "success"; data: InitResponse }
  | { status: "error"; error: ApiError };

export function useDashboardInit(): DashboardState {
  const [requestId, setRequestId] = useState(0);
  const [apiState, setApiState] = useState<DashboardDataState>({ status: "loading" });
  const retry = useCallback(() => {
    setApiState({ status: "loading" });
    setRequestId((current) => current + 1);
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    const fetchInit = async () => {
      try {
        const response = await apiClient.get<InitResponse>("/init", { signal: controller.signal });
        setApiState({ status: "success", data: response.data });
      } catch (error) {
        if (controller.signal.aborted) return;
        setApiState({ status: "error", error: error instanceof ApiError ? error : new ApiError({ status: 0, code: "UNKNOWN_ERROR", message: "予期しないエラーが発生しました" }) });
      }
    };
    void fetchInit();
    return () => controller.abort();
  }, [requestId]);

  return { ...apiState, retry };
}
