import axios, { AxiosError } from "axios";

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly retryable: boolean;

  constructor({
    status,
    code,
    message,
  }: {
    status: number;
    code: string;
    message: string;
  }) {
    super(message);

    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.retryable =
      status === 0 || status === 502 || status === 503 || status === 504;
  }
}
export const apiClient = axios.create({
  baseURL: "/api",
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 40_000,
});

apiClient.interceptors.response.use(
  (response) => response,

  (error: AxiosError<ProblemDetail>) => {
    if (error.code === AxiosError.ERR_CANCELED) {
      return Promise.reject(error);
    }

    if (!error.response) {
      return Promise.reject(
        new ApiError({
          status: 0,
          code: "NETWORK_ERROR",
          message: "サーバーに接続できませんでした",
        }),
      );
    }

    const problem = error.response.data;

    return Promise.reject(
      new ApiError({
        status: problem?.status ?? error.response.status,
        code: problem?.code ?? "API_ERROR",
        message: problem?.detail ?? "データを取得できませんでした",
      }),
    );
  },
);
