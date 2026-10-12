import { useEffect, useState } from 'react';
import { api } from './api.js';

export default function NewOrder({ onCreated }) {
  const [inventory, setInventory] = useState([]);
  const [email, setEmail] = useState('');
  const [qty, setQty] = useState({});
  const [error, setError] = useState('');

  useEffect(() => { api.inventory().then(setInventory); }, []);

  const submit = async (e) => {
    e.preventDefault();
    const items = Object.entries(qty).filter(([, q]) => q > 0).map(([sku, quantity]) => ({ sku, quantity }));
    try {
      await api.createOrder({ customerEmail: email, items });
      onCreated();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <form className="panel" onSubmit={submit}>
      <h2>Place order</h2>
      <input type="email" required placeholder="Customer email" value={email} onChange={(e) => setEmail(e.target.value)} />
      <table>
        <thead><tr><th>Item</th><th>Available</th><th>Quantity</th></tr></thead>
        <tbody>
          {inventory.map((i) => (
            <tr key={i.sku}>
              <td>{i.name} ({i.sku})</td>
              <td>{i.available}</td>
              <td><input type="number" min="0" max={i.available} value={qty[i.sku] || 0}
                         onChange={(e) => setQty({ ...qty, [i.sku]: Number(e.target.value) })} /></td>
            </tr>
          ))}
        </tbody>
      </table>
      {error && <p className="error">{error}</p>}
      <button type="submit">Create order</button>
    </form>
  );
}
