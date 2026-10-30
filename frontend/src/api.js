const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

async function request(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  });
  const text = await res.text();
  let data;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }
  if (!res.ok) {
    const msg = data?.error || data?.message || res.statusText;
    throw new Error(msg);
  }
  return data;
}

export const api = {
  getAccounts: () => request('/api/accounts'),
  createAccount: (body) =>
    request('/api/accounts', { method: 'POST', body: JSON.stringify(body) }),
  getTransactions: () => request('/api/transactions'),
  createTransaction: (body) =>
    request('/api/transactions', { method: 'POST', body: JSON.stringify(body) }),
  transfer: (body) =>
    request('/api/transfers', { method: 'POST', body: JSON.stringify(body) }),
  chat: (message) =>
    request('/api/chat', { method: 'POST', body: JSON.stringify({ message }) }),
};

export { API_BASE };
