import React, { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";

export default function OAuthCallbackPage() {
  const { applyOAuthAuth } = useAuth();
  const [error, setError] = useState("");

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const accessToken = params.get("accessToken");
    const refreshToken = params.get("refreshToken");
    const tokenType = params.get("tokenType");
    const username = params.get("username");
    const role = params.get("role");

    if (!accessToken || !refreshToken || !username || !role) {
      setError("OAuth callback data is incomplete.");
      return;
    }

    applyOAuthAuth({ accessToken, refreshToken, tokenType, username, role });
    window.history.replaceState({}, document.title, "/");
  }, [applyOAuthAuth]);

  return (
    <main className="auth-shell">
      <section className="auth-card">
        <h1>{error ? "OAuth Failed" : "Signing You In"}</h1>
        <p className="subtext">{error || "Completing Google authentication..."}</p>
      </section>
    </main>
  );
}
