import React, { createContext, useContext, useEffect, useMemo, useRef, useState } from "react";
import { authApi } from "../lib/api";

const AuthContext = createContext(null);

function parseJwtPayload(token) {
  if (!token) return null;
  const parts = token.split(".");
  if (parts.length < 2) return null;

  try {
    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
    const json = atob(padded);
    return JSON.parse(json);
  } catch {
    return null;
  }
}

function getTokenExpiryTime(token) {
  const payload = parseJwtPayload(token);
  if (!payload?.exp) return null;
  return payload.exp * 1000;
}

function isTokenExpired(token) {
  const expiryTime = getTokenExpiryTime(token);
  if (!expiryTime) return true;
  return expiryTime <= Date.now();
}

function getStoredAuth() {
  const raw = localStorage.getItem("hms_auth");
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw);
    if (!parsed?.accessToken || isTokenExpired(parsed.accessToken)) {
      localStorage.removeItem("hms_auth");
      return null;
    }
    return parsed;
  } catch {
    localStorage.removeItem("hms_auth");
    return null;
  }
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => getStoredAuth());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const expiryTimeoutRef = useRef(null);

  const clearAuth = () => {
    localStorage.removeItem("hms_auth");
    setAuth(null);
  };

  const login = async (username, password) => {
    setLoading(true);
    setError("");
    try {
      const data = await authApi.login(username, password);
      const next = {
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        tokenType: data.tokenType,
        username: data.username,
        role: data.role
      };
      localStorage.setItem("hms_auth", JSON.stringify(next));
      setAuth(next);
    } catch (e) {
      setError(e.message || "Login failed");
      throw e;
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    if (auth?.accessToken || auth?.refreshToken) {
      try {
        await authApi.logout(auth.accessToken, auth.refreshToken);
      } catch {
      }
    }
    clearAuth();
  };

  const applyOAuthAuth = (payload) => {
    const next = {
      accessToken: payload.accessToken,
      refreshToken: payload.refreshToken,
      tokenType: payload.tokenType || "Bearer",
      username: payload.username,
      role: payload.role
    };
    localStorage.setItem("hms_auth", JSON.stringify(next));
    setAuth(next);
  };

  useEffect(() => {
    if (expiryTimeoutRef.current) {
      clearTimeout(expiryTimeoutRef.current);
      expiryTimeoutRef.current = null;
    }

    if (!auth?.accessToken) {
      return undefined;
    }

    const expiryTime = getTokenExpiryTime(auth.accessToken);
    if (!expiryTime || expiryTime <= Date.now()) {
      clearAuth();
      setError("Session expired. Please sign in again.");
      return undefined;
    }

    expiryTimeoutRef.current = setTimeout(() => {
      clearAuth();
      setError("Session expired. Please sign in again.");
    }, expiryTime - Date.now());

    return () => {
      if (expiryTimeoutRef.current) {
        clearTimeout(expiryTimeoutRef.current);
        expiryTimeoutRef.current = null;
      }
    };
  }, [auth]);

  const value = useMemo(
    () => ({
      auth,
      role: auth?.role || "",
      username: auth?.username || "",
      token: auth?.accessToken || "",
      isAuthenticated: Boolean(auth?.accessToken),
      loading,
      error,
      login,
      logout,
      applyOAuthAuth
    }),
    [auth, loading, error, login, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
