import React from "react";
import { useAuth } from "./context/AuthContext";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import OAuthCallbackPage from "./pages/OAuthCallbackPage";

export default function App() {
  const { isAuthenticated } = useAuth();
  if (window.location.pathname === "/oauth-callback") {
    return <OAuthCallbackPage />;
  }
  return isAuthenticated ? <DashboardPage /> : <LoginPage />;
}
