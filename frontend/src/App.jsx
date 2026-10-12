import { useCallback, useEffect, useState } from 'react';
import { api, ORDER_STATUSES } from './api.js';
import OrderDetail from './OrderDetail.jsx';
import NewOrder from './NewOrder.jsx';
import Inventory from './Inventory.jsx';

const short = (id) => id.slice(0, 8);

function Orders() {
  const [status, setStatus] = useState('');
  const [email, setEmail] = useState('');
  const [orders, setOrders] = useState([]);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    api.orders(status, email).then(setOrders).catch((e) => setError(e.message));
  }, [status, email]);

  // refresh on filter change and poll so asynchronous shipment updates show up
  useEffect(() => {
    load();
    const t = setInterval(load, 5000);
    return () => clearInterval(t);
  }, [load]);

  return (
    <div className="split">
      <section className="panel">
        <div className="filters">
          <input placeholder="Search by customer email" value={email} onChange={(e) => setEmail(e.target.value)} />
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All statuses</option>
            {ORDER_STATUSES.map((s) => <option key={s}>{s}</option>)}
          </select>
        </div>
        {error && <p className="error">{error}</p>}
        <table>
          <thead><tr><th>Order</th><th>Customer</th><th>Status</th><th>Created</th></tr></thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.id} className={selected === o.id ? 'selected' : ''} onClick={() => setSelected(o.id)}>
                <td>{short(o.id)}</td>
                <td>{o.customerEmail}</td>
                <td><span className={`badge ${o.status}`}>{o.status}</span></td>
                <td>{new Date(o.createdAt).toLocaleString()}</td>
              </tr>
            ))}
            {orders.length === 0 && <tr><td colSpan="4">No orders</td></tr>}
          </tbody>
        </table>
      </section>
      <section className="panel">
        {selected ? <OrderDetail orderId={selected} onChange={load} /> : <p>Select an order to see delivery tracking.</p>}
      </section>
    </div>
  );
}

export default function App() {
  const [tab, setTab] = useState('orders');
  return (
    <>
      <header>
        <h1>Orderflow Operations Console</h1>
        <nav>
          {['orders', 'new order', 'inventory'].map((t) => (
            <button key={t} className={tab === t ? 'active' : ''} onClick={() => setTab(t)}>{t}</button>
          ))}
        </nav>
      </header>
      <main>
        {tab === 'orders' && <Orders />}
        {tab === 'new order' && <NewOrder onCreated={() => setTab('orders')} />}
        {tab === 'inventory' && <Inventory />}
      </main>
    </>
  );
}
