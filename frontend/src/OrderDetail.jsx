import { useCallback, useEffect, useState } from 'react';
import { api } from './api.js';

export default function OrderDetail({ orderId, onChange }) {
  const [order, setOrder] = useState(null);
  const [shipment, setShipment] = useState(null);
  const [reason, setReason] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      const o = await api.order(orderId);
      setOrder(o);
      setShipment(await api.shipmentByOrder(orderId).catch(() => null));
      setError('');
    } catch (e) {
      setError(e.message);
    }
  }, [orderId]);

  useEffect(() => {
    load();
    const t = setInterval(load, 4000);
    return () => clearInterval(t);
  }, [load]);

  // run an action, then refresh; shipment-driven order changes arrive via Kafka shortly after
  const act = async (fn) => {
    try {
      await fn();
      setReason('');
      setTimeout(() => { load(); onChange(); }, 500);
    } catch (e) {
      setError(e.message);
    }
  };

  if (!order) return <p>{error || 'Loading…'}</p>;
  const inException = shipment?.status === 'EXCEPTION';
  const canAdvance = shipment && !['DELIVERED', 'CANCELLED', 'EXCEPTION'].includes(shipment.status);
  const canCancel = ['CONFIRMED', 'EXCEPTION'].includes(order.status);

  return (
    <div>
      <h2>Order {order.id.slice(0, 8)} <span className={`badge ${order.status}`}>{order.status}</span></h2>
      <p>{order.customerEmail}</p>
      <ul>{order.items.map((i) => <li key={i.sku}>{i.quantity} × {i.sku}</li>)}</ul>
      {error && <p className="error">{error}</p>}

      {shipment ? (
        <>
          <h3>Shipment · {shipment.carrier} · {shipment.trackingNumber}</h3>
          <ol className="timeline">
            {shipment.history.map((h, i) => (
              <li key={i} className={h.status === 'EXCEPTION' ? 'exc' : ''}>
                <b>{h.status}</b> <small>{new Date(h.at).toLocaleString()}</small>
                {h.note && <div>{h.note}</div>}
              </li>
            ))}
          </ol>
          <div className="actions">
            {canAdvance && <button onClick={() => act(() => api.advance(shipment.id))}>Simulate next carrier scan</button>}
            {inException && <button onClick={() => act(() => api.resolve(shipment.id))}>Resolve exception</button>}
            {canCancel && <button className="danger" onClick={() => act(() => api.cancelOrder(order.id))}>Cancel order</button>}
          </div>
          {canAdvance && (
            <div className="actions">
              <input placeholder="Exception reason (e.g. address invalid)" value={reason} onChange={(e) => setReason(e.target.value)} />
              <button disabled={!reason.trim()} onClick={() => act(() => api.raiseException(shipment.id, reason))}>Flag exception</button>
            </div>
          )}
        </>
      ) : (
        <p>No shipment yet (waiting for fulfillment event)…</p>
      )}
    </div>
  );
}
