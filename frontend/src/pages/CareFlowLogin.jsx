import React, { useState } from "react";
import { useAuth } from "../context/AuthContext";
import Icon from "../components/CareFlowIcon";
import "../careflow.css";

export default function CareFlowLogin() {
  const { login, loading, error } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  async function submit(e) {
    e.preventDefault();
    try {
      await login(username, password);
    } catch {
      /* Auth context displays error. */
    }
  }
  return (
    <main className="cf-login cf">
      <section className="cf-login-story">
        <a href="/" className="cf-brand">
          <span className="cf-brand-mark">
            <Icon name="pulse" size={24} />
          </span>
          CareFlow<span className="cf-brand-dot">.</span>
        </a>
        <span className="cf-eyebrow">CONNECTED HEALTHCARE OPERATIONS</span>
        <h1>
          The right resources.
          <br />
          Ready when
          <br />
          <em>care needs them.</em>
        </h1>
        <p>
          A clearer path from supply request to fulfillment.
          <br />
          One workspace. Every handoff accounted for.
        </p>
        <div className="cf-login-flow">
          {[
            ["requests", "Request", "Resource needs, captured"],
            ["shield", "Approve", "Every decision, accountable"],
            ["box", "Deliver", "Every device, traceable"],
          ].map(([icon, title, text], i) => (
            <div key={title}>
              <span className="cf-flow-number">0{i + 1}</span>
              <Icon name={icon} size={24} />
              <strong>{title}</strong>
              <small>{text}</small>
            </div>
          ))}
        </div>
        <div className="cf-story-footer">
          <span className="cf-dot" /> SYNTHETIC OPERATIONS SIMULATOR{" "}
          <span>Built for a more connected supply chain</span>
        </div>
      </section>
      <section className="cf-login-panel">
        <div className="cf-login-form">
          <span className="cf-kicker">YOUR OPERATIONS WORKSPACE</span>
          <h2>Welcome to CareFlow</h2>
          <p>Sign in to keep things moving.</p>
          <form onSubmit={submit}>
            <label>
              Username
              <input
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                placeholder="Enter your username"
              />
            </label>
            <label>
              Password
              <input
                autoComplete="current-password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="Enter your password"
              />
            </label>
            {error && (
              <div className="cf-notice error" role="alert">
                {error}
              </div>
            )}
            <button className="cf-button primary" disabled={loading}>
              {loading ? (
                "Signing in…"
              ) : (
                <>
                  Sign in to workspace <Icon name="arrow" size={18} />
                </>
              )}
            </button>
          </form>
          <div className="cf-demo-signin">
            <span className="cf-kicker">EXPLORE THE LOCAL DEMO</span>
            <p>Choose a role to fill in its demo credentials.</p>
            <div>
              {[
                ["admin", "Administrator"],
                ["clinical", "Clinical staff"],
                ["inventory", "Inventory manager"],
                ["requester", "Requester"],
              ].map(([name, label]) => (
                <button
                  key={name}
                  type="button"
                  onClick={() => {
                    setUsername(name);
                    setPassword(name === "admin" ? "admin123" : "CareFlow@123");
                  }}
                >
                  {label}
                  <span aria-hidden="true">↗</span>
                </button>
              ))}
            </div>
          </div>
          <small className="cf-login-note">
            Demo accounts require demo mode. All records are synthetic.
            <br />
            An independent portfolio project; not affiliated with Medtronic.
          </small>
        </div>
      </section>
    </main>
  );
}
