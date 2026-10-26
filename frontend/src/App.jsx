import React from "react";
import { useAuth } from "./context/AuthContext";
import LoginPage from "./pages/CareFlowLogin";
import CareFlowPage from "./pages/CareFlowPage";
import DashboardPage from "./pages/DashboardPage";
import OAuthCallbackPage from "./pages/OAuthCallbackPage";

export default function App() {
  const { isAuthenticated } = useAuth();
  if (window.location.pathname === "/oauth-callback") {
    return <OAuthCallbackPage />;
  }
  if (isAuthenticated && window.location.pathname === "/hospital") return <><a className="cf-return" href="/">← CareFlow workspace</a><DashboardPage /></>;
  return isAuthenticated ? <CareFlowPage /> : <LoginPage />;
}
