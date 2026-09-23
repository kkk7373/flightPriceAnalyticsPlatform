import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), ["BACKEND_"]);

  return {
    plugins: [react()],
    server: {
      proxy: {
        "/api": {
          target: env.BACKEND_SERVER ?? "http://localhost:8080",
          changeOrigin: true,
        },
      },
    },
  };
});
