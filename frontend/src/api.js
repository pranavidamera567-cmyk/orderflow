async function request(path, options = {}) {
  const res = await fetch(`/api${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(body.error || `Request failed (${res.status})`);
  return body;
}

const post = (path, body) =>
  request(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined });

export const api = {
  orders: (status, email) => {
    const q = new URLSearchParams();
    if (status) q.set('status', status);
    if (email) q.set('email', email);
    return request(`/orders?${q}`);
  },
  order: (id) => request(`/orders/${id}`),
  createOrder: (order) => post('/orders', order),
  cancelOrder: (id) => post(`/orders/${id}/cancel`),
  shipmentByOrder: (orderId) => request(`/shipments/by-order/${orderId}`),
  advance: (id) => post(`/shipments/${id}/advance`),
  raiseException: (id, reason) => post(`/shipments/${id}/exception`, { reason }),
  resolve: (id, note) => post(`/shipments/${id}/resolve`, { note }),
  inventory: () => request('/inventory'),
  restock: (sku, quantity) => post(`/inventory/${sku}/restock`, { quantity }),
};

export const ORDER_STATUSES = ['CONFIRMED', 'SHIPPED', 'DELIVERED', 'EXCEPTION', 'CANCELLED'];
