import { useCallback, useEffect, useState } from 'react';
import { api } from './api.js';

export default function Inventory() {
  const [items, setItems] = useState([]);
  const load = useCallback(() => api.inventory().then(setItems), []);
  useEffect(() => { load(); }, [load]);

  return (
    <section className="panel">
      <h2>Inventory</h2>
      <table>
        <thead><tr><th>SKU</th><th>Name</th><th>On hand</th><th>Reserved</th><th>Available</th><th /></tr></thead>
        <tbody>
          {items.map((i) => (
            <tr key={i.sku}>
              <td>{i.sku}</td><td>{i.name}</td><td>{i.onHand}</td><td>{i.reserved}</td>
              <td className={i.available < 10 ? 'low' : ''}>{i.available}</td>
              <td><button onClick={() => api.restock(i.sku, 25).then(load)}>+25</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
