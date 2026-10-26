const API_BASE = import.meta.env.VITE_API_BASE_URL || "";

export async function careflow(token, path, method = "GET", body, key) {
  const response = await fetch(`${API_BASE}/api/careflow${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
      ...(key ? { "Idempotency-Key": key } : {}),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok)
    throw new Error(payload.message || `Request failed (${response.status})`);
  return payload.data;
}

export async function allPages(token, path) {
  const rows = [];
  let page = 0;
  let result;
  do {
    result = await careflow(
      token,
      `${path}${path.includes("?") ? "&" : "?"}page=${page++}&size=200`,
    );
    rows.push(...result.content);
  } while (!result.last);
  return rows;
}

export function fulfillmentKey(id) {
  const storageKey = `careflow:fulfillment:${id}`;
  let key = sessionStorage.getItem(storageKey);
  if (!key) {
    key = crypto.randomUUID();
    sessionStorage.setItem(storageKey, key);
  }
  return key;
}
