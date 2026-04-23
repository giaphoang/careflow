import React, { useState } from "react";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const { login, loading, error } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const oauthError = new URLSearchParams(window.location.search).get("oauthError");

  const onSubmit = async (e) => {
    e.preventDefault();
    await login(username, password);
  };

  return (
    <main className="auth-shell">
      <section className="auth-card">
        {/* Logo icon */}
        <div className="auth-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
          </svg>
        </div>

        <p className="eyebrow">Hospital Management</p>
        <h1>Welcome Back</h1>
        <p className="subtext">Sign in to access your operations console — manage patients, appointments, billing, and more.</p>

        <form onSubmit={onSubmit} className="stack gap-14">
          <label>
            Username
            <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Enter your username" required />
          </label>

          <label>
            Password
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Enter your password" required />
          </label>

          {error ? <div className="error-box">{error}</div> : null}
          {oauthError ? <div className="error-box">Google authentication failed. Please try again.</div> : null}

          <button disabled={loading} type="submit">
            {loading ? (
              <span style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" style={{ animation: "spin 1s linear infinite" }}>
                  <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                </svg>
                Signing in…
              </span>
            ) : "Sign In"}
          </button>

          <div className="divider"><span>or</span></div>

          <a className="oauth-btn" href="/oauth2/authorization/google">
            <svg width="18" height="18" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" />
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
            </svg>
            Continue with Google
          </a>
        </form>

      </section>
    </main>
  );
}
