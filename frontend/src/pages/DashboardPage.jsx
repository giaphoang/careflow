import React, { useEffect, useMemo, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { authApi, hmsApi } from "../lib/api";

const Icons = {
  overview: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" />
      <rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" />
    </svg>
  ),
  patients: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" />
      <path d="M22 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>
  ),
  doctors: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
    </svg>
  ),
  appointments: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" />
      <path d="M8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01" />
    </svg>
  ),
  billing: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="5" width="20" height="14" rx="2" /><line x1="2" y1="10" x2="22" y2="10" />
    </svg>
  ),
  admissions: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 21s-6-4.35-8.33-8.15A4.96 4.96 0 0 1 3 10a5 5 0 0 1 9-3 5 5 0 0 1 9 3 4.96 4.96 0 0 1-.67 2.85C18 16.65 12 21 12 21Z" />
    </svg>
  ),
  logout: (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" />
    </svg>
  ),
};

const ROLE_RULES = {
  ADMIN: {
    label: "Administrator",
    accent: "admin",
    tabs: ["overview", "patients", "admissions", "doctors", "appointments", "billing"],
    summary: "Full operational control across staff, patients, appointments, admissions, and billing.",
  },
  RECEPTIONIST: {
    label: "Receptionist",
    accent: "reception",
    tabs: ["overview", "patients", "admissions", "appointments", "billing"],
    summary: "Front desk access for patient intake, admissions, appointment scheduling, and invoices.",
  },
  DOCTOR: {
    label: "Doctor",
    accent: "doctor",
    tabs: ["overview", "patients", "appointments"],
    summary: "Clinical access for reviewing patients and updating appointment outcomes.",
  },
};

const TAB_DEFS = [
  { key: "overview", label: "Overview", icon: Icons.overview },
  { key: "patients", label: "Patients", icon: Icons.patients },
  { key: "admissions", label: "Admissions", icon: Icons.admissions },
  { key: "doctors", label: "Doctors", icon: Icons.doctors },
  { key: "appointments", label: "Appointments", icon: Icons.appointments },
  { key: "billing", label: "Billing", icon: Icons.billing },
];

function Badge({ value }) {
  if (!value) return "-";
  const cls = {
    SCHEDULED: "badge-scheduled",
    COMPLETED: "badge-completed",
    CANCELLED: "badge-cancelled",
    PAID: "badge-paid",
    UNPAID: "badge-unpaid",
    ADMITTED: "badge-scheduled",
    DISCHARGED: "badge-completed",
  }[value] || "";
  return <span className={`badge ${cls}`}>{value}</span>;
}

function Metric({ label, value, iconClass, icon }) {
  return (
    <article className="metric-card">
      <div className={`metric-icon ${iconClass}`}>{icon}</div>
      <span className="metric-label">{label}</span>
      <strong className="metric-value">{value}</strong>
    </article>
  );
}

function DataTable({ columns, rows }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>{columns.map((c) => <th key={c}>{c}</th>)}</tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr><td colSpan={columns.length} style={{ textAlign: "center", color: "var(--ink-faint)", padding: 24 }}>No data yet</td></tr>
          ) : (
            rows.map((r, i) => (
              <tr key={i}>
                {r.map((v, idx) => (
                  <td key={idx}>
                    {v !== null && typeof v === "object" && v.$$badge ? (
                      <Badge value={v.value} />
                    ) : v !== null && typeof v === "object" && v.$$jsx ? (
                      v.value
                    ) : (
                      String(v ?? "-")
                    )}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

function badge(val) { return { $$badge: true, value: val }; }
function jsxCell(val) { return { $$jsx: true, value: val }; }

function SectionHeader({ icon, title, subtitle }) {
  return (
    <div className="section-header">
      <div className="section-icon">{icon}</div>
      <div>
        <h3>{title}</h3>
        {subtitle ? <p>{subtitle}</p> : null}
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const { token, role, username, logout } = useAuth();
  const roleConfig = ROLE_RULES[role] || ROLE_RULES.RECEPTIONIST;
  const visibleTabs = TAB_DEFS.filter((tab) => roleConfig.tabs.includes(tab.key));

  const [activeTab, setActiveTab] = useState(visibleTabs[0]?.key || "overview");
  const [patients, setPatients] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [invoices, setInvoices] = useState([]);
  const [admissions, setAdmissions] = useState([]);
  const [users, setUsers] = useState([]);
  const [roleUpdateUserId, setRoleUpdateUserId] = useState(null);
  const [selectedPatientId, setSelectedPatientId] = useState("");
  const [message, setMessage] = useState("");

  const [patientForm, setPatientForm] = useState({ fullName: "", email: "", phone: "", dateOfBirth: "" });
  const [doctorForm, setDoctorForm] = useState({ fullName: "", specialization: "", email: "" });
  const [appointmentForm, setAppointmentForm] = useState({ patientId: "", doctorId: "", appointmentTime: "", reason: "" });
  const [invoiceForm, setInvoiceForm] = useState({ patientId: "", amount: "", description: "" });
  const [admissionForm, setAdmissionForm] = useState({ patientId: "", ward: "", reason: "", admittedOn: "" });

  const canManagePatients = ["ADMIN", "RECEPTIONIST"].includes(role);
  const canViewPatients = ["ADMIN", "RECEPTIONIST", "DOCTOR"].includes(role);
  const canManageDoctors = ["ADMIN"].includes(role);
  const canViewDoctors = ["ADMIN", "RECEPTIONIST", "DOCTOR"].includes(role);
  const canManageAppointments = ["ADMIN", "RECEPTIONIST"].includes(role);
  const canCompleteAppointments = ["ADMIN", "DOCTOR"].includes(role);
  const canCancelAppointments = ["ADMIN", "RECEPTIONIST"].includes(role);
  const canManageBilling = ["ADMIN", "RECEPTIONIST"].includes(role);
  const canManageAdmissions = ["ADMIN", "RECEPTIONIST"].includes(role);
  const canManageUsers = role === "ADMIN";

  useEffect(() => {
    if (!visibleTabs.some((tab) => tab.key === activeTab)) {
      setActiveTab(visibleTabs[0]?.key || "overview");
    }
  }, [activeTab, visibleTabs]);

  useEffect(() => {
    if (!message) return;
    const t = setTimeout(() => setMessage(""), 4000);
    return () => clearTimeout(t);
  }, [message]);

  const loadBase = async () => {
    try {
      const requests = [];
      if (canViewPatients) requests.push(hmsApi.getPatients(token));
      if (canViewDoctors) requests.push(hmsApi.getDoctors(token));
      if (canManageUsers) requests.push(authApi.getUsers(token));

      const [patientData = [], doctorData = [], userData = []] = await Promise.all(requests);
      setPatients(patientData);
      setDoctors(doctorData);
      setUsers(userData);
      if (patientData.length && !selectedPatientId) {
        setSelectedPatientId(String(patientData[0].id));
      }
    } catch (e) {
      setMessage(e.message);
    }
  };

  useEffect(() => { loadBase(); }, []);

  const dashboardStats = useMemo(() => {
    const totalRevenue = invoices.reduce((sum, inv) => sum + Number(inv.amount || 0), 0);
    return [
      { label: "Patients", value: patients.length, iconClass: "patients", icon: Icons.patients },
      { label: "Doctors", value: doctors.length, iconClass: "doctors", icon: Icons.doctors },
      { label: "Appointments", value: appointments.length, iconClass: "appointments", icon: Icons.appointments },
      { label: "Revenue", value: `Rs ${totalRevenue.toFixed(0)}`, iconClass: "billing", icon: Icons.billing },
    ];
  }, [patients, doctors, appointments, invoices]);

  const submitPatient = async (e) => {
    e.preventDefault();
    try {
      await hmsApi.createPatient(token, patientForm);
      setPatientForm({ fullName: "", email: "", phone: "", dateOfBirth: "" });
      setMessage("Patient created successfully");
      await loadBase();
    } catch (err) { setMessage(err.message); }
  };

  const submitDoctor = async (e) => {
    e.preventDefault();
    try {
      await hmsApi.createDoctor(token, doctorForm);
      setDoctorForm({ fullName: "", specialization: "", email: "" });
      setMessage("Doctor created successfully");
      await loadBase();
    } catch (err) { setMessage(err.message); }
  };

  const submitAppointment = async (e) => {
    e.preventDefault();
    try {
      await hmsApi.bookAppointment(token, {
        ...appointmentForm,
        patientId: Number(appointmentForm.patientId),
        doctorId: Number(appointmentForm.doctorId),
      });
      setAppointmentForm({ patientId: "", doctorId: "", appointmentTime: "", reason: "" });
      setMessage("Appointment booked and WhatsApp notification queued");
      await fetchPatientAppointments();
    } catch (err) { setMessage(err.message); }
  };

  const fetchPatientAppointments = async () => {
    if (!selectedPatientId) return;
    try {
      setAppointments(await hmsApi.getPatientAppointments(token, selectedPatientId));
    } catch (err) { setMessage(err.message); }
  };

  const submitInvoice = async (e) => {
    e.preventDefault();
    try {
      await hmsApi.createInvoice(token, {
        patientId: Number(invoiceForm.patientId),
        amount: Number(invoiceForm.amount),
        description: invoiceForm.description,
      });
      setInvoiceForm({ patientId: "", amount: "", description: "" });
      setMessage("Invoice created successfully");
      await fetchPatientInvoices();
    } catch (err) { setMessage(err.message); }
  };

  const fetchPatientInvoices = async () => {
    if (!selectedPatientId) return;
    try {
      setInvoices(await hmsApi.getPatientInvoices(token, selectedPatientId));
    } catch (err) { setMessage(err.message); }
  };

  const submitAdmission = async (e) => {
    e.preventDefault();
    try {
      if (!admissionForm.patientId) throw new Error("Select a patient");
      await hmsApi.admitPatient(token, {
        patientId: Number(admissionForm.patientId),
        ward: admissionForm.ward,
        reason: admissionForm.reason,
        admittedOn: admissionForm.admittedOn,
      });
      setAdmissionForm({ patientId: "", ward: "", reason: "", admittedOn: "" });
      setMessage("Patient admitted successfully");
      await fetchPatientAdmissions();
    } catch (err) { setMessage(err.message); }
  };

  const fetchPatientAdmissions = async () => {
    if (!selectedPatientId) return;
    try {
      setAdmissions(await hmsApi.getPatientAdmissions(token, selectedPatientId));
    } catch (err) { setMessage(err.message); }
  };

  const handleAction = async (actionFn, ...args) => {
    try {
      await actionFn(token, ...args);
      setMessage("Action successful");
      if (activeTab === "appointments") await fetchPatientAppointments();
      if (activeTab === "billing") await fetchPatientInvoices();
      if (activeTab === "admissions") await fetchPatientAdmissions();
    } catch (err) { setMessage(err.message); }
  };

  const updateUserRole = async (userId, nextRole, currentRole, usernameValue) => {
    try {
      setRoleUpdateUserId(userId);
      let details = {};
      if (nextRole === "DOCTOR" && currentRole !== "DOCTOR") {
        const defaultEmail = usernameValue.includes("@") ? usernameValue : "";
        const fullName = window.prompt("Enter doctor full name");
        if (fullName === null) {
          return;
        }
        const specialization = window.prompt("Enter doctor specialization", "General Medicine");
        if (specialization === null) {
          return;
        }
        const email = window.prompt("Enter doctor email", defaultEmail);
        if (email === null) {
          return;
        }
        details = {
          fullName: fullName.trim(),
          specialization: specialization.trim(),
          email: email.trim(),
        };
      }

      await authApi.updateUserRole(token, userId, nextRole, details);
      setMessage(`User role updated to ${nextRole}`);
      if (canManageUsers) {
        setUsers(await authApi.getUsers(token));
      }
    } catch (err) {
      setMessage(err.message);
    } finally {
      setRoleUpdateUserId(null);
    }
  };

  const friendlyName = useMemo(() => {
    if (!username) return "Operator";
    if (username.includes("@")) {
      return username.split("@")[0];
    }
    return username;
  }, [username]);

  const initials = friendlyName ? friendlyName.slice(0, 2).toUpperCase() : "??";

  return (
    <main className="dash-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <h2>HMS</h2>
          <span className="brand-sub">Hospital Management</span>
        </div>

        <div className="sidebar-user">
          <div className="avatar">{initials}</div>
          <div className="user-info">
            <div className="user-name">{friendlyName}</div>
            <div className={`user-role role-${roleConfig.accent}`}>{roleConfig.label}</div>
          </div>
        </div>

        <nav>
          {visibleTabs.map((tab) => (
            <button
              key={tab.key}
              className={`nav-item${activeTab === tab.key ? " active" : ""}`}
              onClick={() => setActiveTab(tab.key)}
            >
              {tab.icon}
              {tab.label}
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <button className="danger" onClick={logout}>
            {Icons.logout}
            <span style={{ marginLeft: 8 }}>Logout</span>
          </button>
        </div>
      </aside>

      <section className="content">
        <header className="page-header role-hero">
          <div>
            <p className="welcome-text">Welcome back, {friendlyName}</p>
            <h1>{roleConfig.label} Console</h1>
            <p>{roleConfig.summary}</p>
          </div>
          <div className={`role-chip role-${roleConfig.accent}`}>{roleConfig.label}</div>
        </header>

        {message && <div className="toast">{message}</div>}

        {activeTab === "overview" && (
          <>
            <div className="metrics-grid">
              {dashboardStats.map((m) => (
                <Metric key={m.label} label={m.label} value={m.value} iconClass={m.iconClass} icon={m.icon} />
              ))}
            </div>
            <section className="panel role-summary-panel">
              <SectionHeader
                icon={Icons.overview}
                title="Role permissions"
                subtitle="This dashboard now shows only the parts of the system your role can actually use."
              />
              <div className="permission-list">
                <div className="permission-item"><strong>Patients</strong><span>{canManagePatients ? "Create and manage patient records" : "Read patient records only"}</span></div>
                <div className="permission-item"><strong>Doctors</strong><span>{canManageDoctors ? "Create and manage doctors" : canViewDoctors ? "View doctor directory only" : "No doctor access"}</span></div>
                <div className="permission-item"><strong>Appointments</strong><span>{canManageAppointments ? "Book and cancel appointments" : canCompleteAppointments ? "Review and complete appointments" : "No appointment actions"}</span></div>
                <div className="permission-item"><strong>Admissions</strong><span>{canManageAdmissions ? "Admit and discharge patients" : "No admissions access"}</span></div>
                <div className="permission-item"><strong>Billing</strong><span>{canManageBilling ? "Create and update invoices" : "No billing access"}</span></div>
              </div>
            </section>
            {canManageUsers ? (
              <section className="panel role-summary-panel">
                <SectionHeader
                  icon={Icons.doctors}
                  title="User Access Control"
                  subtitle="New OAuth users begin as receptionist. Administrators can promote or change roles here."
                />
                <DataTable
                  columns={["Username", "Current Role", "Change Role"]}
                  rows={users.map((user) => [
                    user.username,
                    badge(user.role),
                    jsxCell(
                      <select
                        value={user.role}
                        disabled={roleUpdateUserId === user.id}
                        onChange={(e) => updateUserRole(user.id, e.target.value, user.role, user.username)}
                      >
                        <option value="RECEPTIONIST">Receptionist</option>
                        <option value="DOCTOR">Doctor</option>
                        <option value="ADMIN">Admin</option>
                      </select>
                    )
                  ])}
                />
              </section>
            ) : null}
          </>
        )}

        {activeTab === "patients" && (
          <div className="split-grid">
            {canManagePatients ? (
              <article className="panel">
                <SectionHeader icon={Icons.patients} title="Register Patient" subtitle="Create a patient record with contact information and date of birth." />
                <form onSubmit={submitPatient} className="stack gap-10">
                  <label>Full Name<input placeholder="John Doe" value={patientForm.fullName} onChange={(e) => setPatientForm({ ...patientForm, fullName: e.target.value })} required /></label>
                  <label>Email<input placeholder="john@example.com" value={patientForm.email} onChange={(e) => setPatientForm({ ...patientForm, email: e.target.value })} required /></label>
                  <label>Phone<input placeholder="+91 98765 43210" value={patientForm.phone} onChange={(e) => setPatientForm({ ...patientForm, phone: e.target.value })} required /></label>
                  <label>Date of Birth<input type="date" value={patientForm.dateOfBirth} onChange={(e) => setPatientForm({ ...patientForm, dateOfBirth: e.target.value })} required /></label>
                  <button>Create Patient</button>
                </form>
              </article>
            ) : null}
            <article className="panel">
              <SectionHeader icon={Icons.patients} title="Patient Records" subtitle="Browse current patient records." />
              <DataTable
                columns={["ID", "Name", "Email", "Phone", "DOB"]}
                rows={patients.map((p) => [p.id, p.fullName, p.email, p.phone, p.dateOfBirth])}
              />
            </article>
          </div>
        )}

        {activeTab === "admissions" && canManageAdmissions && (
          <div className="split-grid">
            <article className="panel">
              <SectionHeader icon={Icons.admissions} title="Admit Patient" subtitle="Create a new admission and assign the patient to a ward." />
              <form onSubmit={submitAdmission} className="stack gap-10">
                <label>Patient
                  <select value={admissionForm.patientId} onChange={(e) => setAdmissionForm({ ...admissionForm, patientId: e.target.value })} required>
                    <option value="">Select patient</option>
                    {patients.map((p) => <option key={p.id} value={p.id}>{p.fullName}</option>)}
                  </select>
                </label>
                <label>Ward<input placeholder="General / ICU" value={admissionForm.ward} onChange={(e) => setAdmissionForm({ ...admissionForm, ward: e.target.value })} required /></label>
                <label>Reason<input placeholder="Observation" value={admissionForm.reason} onChange={(e) => setAdmissionForm({ ...admissionForm, reason: e.target.value })} required /></label>
                <label>Admit Date/Time<input type="datetime-local" value={admissionForm.admittedOn} onChange={(e) => setAdmissionForm({ ...admissionForm, admittedOn: e.target.value })} required /></label>
                <button>Admit Patient</button>
              </form>
            </article>
            <article className="panel">
              <SectionHeader icon={Icons.admissions} title="Admissions" subtitle="Review active admissions and discharge patients when needed." />
              <div className="stack gap-10">
                <label>Patient
                  <select value={selectedPatientId} onChange={(e) => setSelectedPatientId(e.target.value)}>
                    <option value="">Select patient</option>
                    {patients.map((p) => <option key={p.id} value={p.id}>{p.fullName}</option>)}
                  </select>
                </label>
                <button type="button" onClick={fetchPatientAdmissions}>Load Admissions</button>
              </div>
              <DataTable
                columns={["ID", "Ward", "Admitted", "Status", "Actions"]}
                rows={admissions.map((a) => [
                  a.id,
                  a.ward,
                  new Date(a.admittedOn).toLocaleDateString(),
                  badge(a.status),
                  a.status === "ADMITTED" ? jsxCell(
                    <button style={{ padding: "4px 8px" }} onClick={() => handleAction(hmsApi.dischargePatient, a.id)}>Discharge</button>
                  ) : null
                ])}
              />
            </article>
          </div>
        )}

        {activeTab === "doctors" && canViewDoctors && (
          <div className="split-grid">
            {canManageDoctors ? (
              <article className="panel">
                <SectionHeader icon={Icons.doctors} title="Add Doctor" subtitle="Only administrators can create new doctor profiles." />
                <form onSubmit={submitDoctor} className="stack gap-10">
                  <label>Full Name<input placeholder="Dr. Smith" value={doctorForm.fullName} onChange={(e) => setDoctorForm({ ...doctorForm, fullName: e.target.value })} required /></label>
                  <label>Specialization<input placeholder="Cardiology" value={doctorForm.specialization} onChange={(e) => setDoctorForm({ ...doctorForm, specialization: e.target.value })} required /></label>
                  <label>Email<input placeholder="smith@hospital.com" value={doctorForm.email} onChange={(e) => setDoctorForm({ ...doctorForm, email: e.target.value })} required /></label>
                  <button>Create Doctor</button>
                </form>
              </article>
            ) : (
              <article className="panel compact-panel">
                <SectionHeader icon={Icons.doctors} title="Doctor Directory" subtitle="Your role can view doctors but cannot add or edit them." />
              </article>
            )}
            <article className="panel">
              <SectionHeader icon={Icons.doctors} title="Doctor Directory" subtitle="Current specialists available in the hospital system." />
              <DataTable
                columns={["ID", "Name", "Specialization", "Email"]}
                rows={doctors.map((d) => [d.id, d.fullName, d.specialization, d.email])}
              />
            </article>
          </div>
        )}

        {activeTab === "appointments" && (
          <div className="split-grid">
            {canManageAppointments ? (
              <article className="panel">
                <SectionHeader icon={Icons.appointments} title="Book Appointment" subtitle="Create a scheduled visit and queue a WhatsApp notification." />
                <form onSubmit={submitAppointment} className="stack gap-10">
                  <label>Patient
                    <select value={appointmentForm.patientId} onChange={(e) => setAppointmentForm({ ...appointmentForm, patientId: e.target.value })} required>
                      <option value="">Select patient</option>
                      {patients.map((p) => <option key={p.id} value={p.id}>{p.fullName}</option>)}
                    </select>
                  </label>
                  <label>Doctor
                    <select value={appointmentForm.doctorId} onChange={(e) => setAppointmentForm({ ...appointmentForm, doctorId: e.target.value })} required>
                      <option value="">Select doctor</option>
                      {doctors.map((d) => <option key={d.id} value={d.id}>{d.fullName}</option>)}
                    </select>
                  </label>
                  <label>Date & Time<input type="datetime-local" value={appointmentForm.appointmentTime} onChange={(e) => setAppointmentForm({ ...appointmentForm, appointmentTime: e.target.value })} required /></label>
                  <label>Reason<input placeholder="General checkup" value={appointmentForm.reason} onChange={(e) => setAppointmentForm({ ...appointmentForm, reason: e.target.value })} required /></label>
                  <button>Book + Notify</button>
                </form>
              </article>
            ) : (
              <article className="panel compact-panel">
                <SectionHeader icon={Icons.appointments} title="Appointment Actions" subtitle="Doctors can review schedules and mark appointments complete, but cannot create or cancel them." />
              </article>
            )}
            <article className="panel">
              <SectionHeader icon={Icons.appointments} title="Appointments by Patient" subtitle="Load a patient’s current appointments and act according to your role." />
              <div className="stack gap-10">
                <label>Patient
                  <select value={selectedPatientId} onChange={(e) => setSelectedPatientId(e.target.value)}>
                    <option value="">Select patient</option>
                    {patients.map((p) => <option key={p.id} value={p.id}>{p.fullName}</option>)}
                  </select>
                </label>
                <button type="button" onClick={fetchPatientAppointments}>Load Appointments</button>
              </div>
              <DataTable
                columns={["ID", "Doctor", "Time", "Status", "Reason", "Actions"]}
                rows={appointments.map((a) => {
                  const actionButtons = [];
                  if (a.status === "SCHEDULED" && canCompleteAppointments) {
                    actionButtons.push(
                      <button key="complete" style={{ padding: "4px 8px" }} onClick={() => handleAction(hmsApi.completeAppointment, a.id)}>Complete</button>
                    );
                  }
                  if (a.status === "SCHEDULED" && canCancelAppointments) {
                    actionButtons.push(
                      <button key="cancel" style={{ padding: "4px 8px" }} className="danger" onClick={() => handleAction(hmsApi.cancelAppointment, a.id)}>Cancel</button>
                    );
                  }

                  return [
                    a.id,
                    a.doctorName,
                    new Date(a.appointmentTime).toLocaleString(),
                    badge(a.status),
                    a.reason,
                    actionButtons.length ? jsxCell(<div style={{ display: "flex", gap: "4px" }}>{actionButtons}</div>) : null
                  ];
                })}
              />
            </article>
          </div>
        )}

        {activeTab === "billing" && canManageBilling && (
          <div className="split-grid">
            <article className="panel">
              <SectionHeader icon={Icons.billing} title="Create Invoice" subtitle="Generate invoices for patients and keep billing status up to date." />
              <form onSubmit={submitInvoice} className="stack gap-10">
                <label>Patient
                  <select value={invoiceForm.patientId} onChange={(e) => setInvoiceForm({ ...invoiceForm, patientId: e.target.value })} required>
                    <option value="">Select patient</option>
                    {patients.map((p) => <option key={p.id} value={p.id}>{p.fullName}</option>)}
                  </select>
                </label>
                <label>Amount (Rs)<input type="number" step="0.01" placeholder="500.00" value={invoiceForm.amount} onChange={(e) => setInvoiceForm({ ...invoiceForm, amount: e.target.value })} required /></label>
                <label>Description<input placeholder="Consultation fee" value={invoiceForm.description} onChange={(e) => setInvoiceForm({ ...invoiceForm, description: e.target.value })} required /></label>
                <button>Create Invoice</button>
              </form>
            </article>
            <article className="panel">
              <SectionHeader icon={Icons.billing} title="Invoices by Patient" subtitle="Review invoice status and mark unpaid invoices as paid." />
              <div className="stack gap-10">
                <label>Patient
                  <select value={selectedPatientId} onChange={(e) => setSelectedPatientId(e.target.value)}>
                    <option value="">Select patient</option>
                    {patients.map((p) => <option key={p.id} value={p.id}>{p.fullName}</option>)}
                  </select>
                </label>
                <button type="button" onClick={fetchPatientInvoices}>Load Invoices</button>
              </div>
              <DataTable
                columns={["ID", "Amount", "Issued", "Status", "Description", "Actions"]}
                rows={invoices.map((inv) => [
                  inv.id,
                  `Rs ${inv.amount}`,
                  inv.issuedOn,
                  badge(inv.status),
                  inv.description,
                  inv.status === "UNPAID" ? jsxCell(
                    <button style={{ padding: "4px 8px" }} onClick={() => handleAction(hmsApi.payInvoice, inv.id)}>Pay</button>
                  ) : null
                ])}
              />
            </article>
          </div>
        )}
      </section>
    </main>
  );
}
