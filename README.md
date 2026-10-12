# orderflow

E-commerce order fulfillment and delivery tracking platform (Jun 2024 – Dec 2024).

Modular order, inventory and shipment services expose REST APIs with persisted state. Orders are validated against
inventory and move through `PENDING → CONFIRMED → SHIPPED → DELIVERED` (or `EXCEPTION` / `CANCELLED`). Fulfillment updates
are decoupled with Kafka events, and a React console handles order lookup, delivery tracking and exception handling.

**Stack:** Java 17, Spring Boot, React.js, PostgreSQL, Apache Kafka, Docker, Maven, Postman

## How it works
```
POST /api/orders ─► OrderService ─► reserve stock (InventoryService) ─► CONFIRMED
                         │ order-events (ORDER_CONFIRMED / ORDER_CANCELLED)
                         ▼
                 ShipmentService creates shipment ◄── ShipmentSimulator / POST .../advance (carrier scans)
                         │ shipment-events (status, note)
                         ▼
                 OrderService maps shipment status → order status (DELIVERED commits stock)
```
- Events are published **after the DB transaction commits**, keyed by order id, and consumers are idempotent.
- Stock is reserved with a pessimistic lock; cancelling releases it, delivery removes it from on-hand.
- `shipments.simulator` emits a carrier scan every 15 s per in-flight shipment (set `SIMULATOR_ENABLED=false` to drive scans manually).

## Layout
| Path | Purpose |
|---|---|
| `backend/` | Spring Boot app (`order`, `inventory`, `shipment`, `events` packages) |
| `frontend/` | React (Vite) operations console |
| `postman/` | Postman collection |
| `docker-compose.yml` | Postgres, Kafka, backend, frontend |

## Run everything
```bash
docker compose up --build
# console: http://localhost:3000   API: http://localhost:8080
```

## Run for development
```bash
docker compose up postgres kafka          # (add `ports: ["9092:9092"]` style overrides if connecting from the host)
cd backend && mvn spring-boot:run
cd frontend && npm install && npm run dev  # http://localhost:5173, proxies /api to :8080
```

## API
| Method | Path | |
|---|---|---|
| POST | `/api/orders` | create (validates, reserves stock) |
| GET | `/api/orders?status=&email=` | search |
| GET | `/api/orders/{id}` | detail |
| POST | `/api/orders/{id}/cancel` | cancel and release stock |
| GET | `/api/shipments/by-order/{orderId}` | tracking timeline |
| POST | `/api/shipments/{id}/advance` | simulate next carrier scan |
| POST | `/api/shipments/{id}/exception` · `/resolve` | exception handling |
| GET | `/api/inventory` · POST `/api/inventory/{sku}/restock` | stock |

Seeded SKUs: `SKU-1001` … `SKU-1004`. Import `postman/orderflow.postman_collection.json` to try the flow.

## Test
```bash
cd backend && mvn test   # H2 + in-process event dispatch, no broker needed
```
