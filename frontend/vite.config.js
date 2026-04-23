import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const backendTarget = process.env.VITE_PROXY_TARGET || "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true,
    proxy: {
      "/api": {
        target: backendTarget,
        changeOrigin: true
      },
      "/oauth2": {
        target: backendTarget,
        changeOrigin: true
      },
      "/login": {
        target: backendTarget,
        changeOrigin: true
      }
    }
  }
});
