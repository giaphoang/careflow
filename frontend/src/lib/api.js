const API_BASE = import.meta.env.VITE_API_BASE_URL || "";

async function request(path, options = {}) {
  const { headers: optHeaders, ...rest } = options;
  const response = await fetch(`${API_BASE}${path}`, {
    ...rest,
    headers: {
      "Content-Type": "application/json",
      ...(optHeaders || {})
    }
  });

  let payload = null;
  try {
    payload = await response.json();
  } catch {
    payload = null;
  }

  if (!response.ok) {
    throw new Error(payload?.message || payload?.error || `Request failed: ${response.status}`);
  }

  return payload?.data ?? payload;
}

export const authApi = {
  login(username, password) {
    return request("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password })
    });
  },
  logout(accessToken, refreshToken) {
    return request("/api/auth/logout", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`
      },
      body: JSON.stringify({ refreshToken })
    });
  },
  getUsers(token) {
    return request("/api/auth/users", {
      headers: authHeader(token)
    });
  },
  updateUserRole(token, userId, role, details = {}) {
    return request(`/api/auth/users/${userId}/role`, {
      method: "PUT",
      headers: authHeader(token),
      body: JSON.stringify({ role, ...details })
    });
  }
};

function authHeader(token) {
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export const hmsApi = {
  getPatients(token) {
    return request("/api/patients", { headers: authHeader(token) });
  },
  createPatient(token, body) {
    return request("/api/patients", {
      method: "POST",
      headers: authHeader(token),
      body: JSON.stringify(body)
    });
  },
  getDoctors(token) {
    return request("/api/doctors", { headers: authHeader(token) });
  },
  createDoctor(token, body) {
    return request("/api/doctors", {
      method: "POST",
      headers: authHeader(token),
      body: JSON.stringify(body)
    });
  },
  bookAppointment(token, body) {
    return request("/api/appointments", {
      method: "POST",
      headers: authHeader(token),
      body: JSON.stringify(body)
    });
  },
  getPatientAppointments(token, patientId) {
    return request(`/api/appointments/patient/${patientId}`, {
      headers: authHeader(token)
    });
  },
  createInvoice(token, body) {
    return request("/api/billing/invoices", {
      method: "POST",
      headers: authHeader(token),
      body: JSON.stringify(body)
    });
  },
  getPatientInvoices(token, patientId) {
    return request(`/api/billing/invoices/patient/${patientId}`, {
      headers: authHeader(token)
    });
  },
  completeAppointment(token, appointmentId) {
    return request(`/api/appointments/${appointmentId}/complete`, {
      method: "POST",
      headers: authHeader(token),
    });
  },
  cancelAppointment(token, appointmentId) {
    return request(`/api/appointments/${appointmentId}/cancel`, {
      method: "POST",
      headers: authHeader(token),
    });
  },
  payInvoice(token, invoiceId) {
    return request(`/api/billing/invoices/${invoiceId}/pay`, {
      method: "POST",
      headers: authHeader(token),
    });
  },
  admitPatient(token, body) {
    return request("/api/admissions", {
      method: "POST",
      headers: authHeader(token),
      body: JSON.stringify(body)
    });
  },
  dischargePatient(token, admissionId) {
    return request(`/api/admissions/${admissionId}/discharge`, {
      method: "POST",
      headers: authHeader(token),
    });
  },
  getPatientAdmissions(token, patientId) {
    return request(`/api/admissions/patient/${patientId}`, {
      headers: authHeader(token)
    });
  }
};
