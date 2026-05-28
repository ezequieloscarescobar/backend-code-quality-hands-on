# orders-service

Microservicio responsable de gestionar el ciclo de vida de las órdenes de compra dentro de la plataforma.

## Contexto de negocio

El `orders-service` administra:

- Creación de órdenes con uno o más ítems
- Confirmación y pago de órdenes
- Cancelación con reembolso automático si ya fueron pagadas
- Cálculo de precios con descuentos por cupón e impuestos
- Reportes operativos para el equipo de operaciones

Su objetivo principal es garantizar la consistencia del ciclo de vida de una orden y coordinar con los servicios de pagos, inventario y notificaciones.

## Ciclo de vida de una orden

```
                        createOrder()
                             │
                             ▼
                          PENDING ──────────────────────────────────────┐
                             │                                          │
             confirmOrder()  │                                          │ cancelOrder()
                             ▼                                          │
                         CONFIRMED ──────────────────────────────────► CANCELLED
                             │                                          ▲
                  payOrder() │                                          │
                             ▼                                          │
                           PAID ────────────────────────────────────────┘
                                          cancelOrder() + refund
```

| Estado        | Descripción                                                            |
|---------------|------------------------------------------------------------------------|
| `PENDING`     | La orden fue creada pero aún no fue confirmada                         |
| `CONFIRMED`   | El sistema validó la orden y está lista para ser pagada                |
| `PAID`        | El pago fue procesado exitosamente                                     |
| `CANCELLED`   | La orden fue cancelada; si estaba paga, se procesa un reembolso        |

## Endpoints

### Órdenes

| Método   | Path                        | Descripción                              | Respuesta exitosa |
|----------|-----------------------------|------------------------------------------|-------------------|
| POST     | `/api/orders`               | Crea una nueva orden                     | 201 Created       |
| PUT      | `/api/orders/{id}/confirm`  | Confirma una orden pendiente             | 200 OK            |
| POST     | `/api/orders/{id}/pay`      | Procesa el pago de una orden confirmada  | 200 OK            |
| DELETE   | `/api/orders/{id}`          | Cancela una orden                        | 200 OK            |
| GET      | `/api/orders/{id}`          | Obtiene el detalle de una orden          | 200 OK            |
| GET      | `/api/orders`               | Lista órdenes (filtro por status o cliente) | 200 OK         |
| GET      | `/api/orders/revenue`       | Ingresos totales                         | 200 OK            |

### Reportes

| Método | Path                    | Descripción                                        | Respuesta exitosa |
|--------|-------------------------|----------------------------------------------------|-------------------|
| GET    | `/api/reports`          | Reporte completo de órdenes                        | 200 OK            |
| GET    | `/api/reports/status`   | Cantidad de órdenes agrupadas por estado           | 200 OK            |
| GET    | `/api/reports/revenue`  | Ingresos totales e impuestos recaudados            | 200 OK            |
| GET    | `/api/reports/paid`     | Listado de órdenes pagadas                         | 200 OK            |
| GET    | `/api/reports/range`    | Órdenes en un rango de fechas (`from` y `to`)      | 200 OK            |

### POST /api/orders
```json
// Request
{
  "customer": "cliente-1",
  "mail": "cliente@example.com",
  "productos": [
    { "id": "prod-1", "name": "Notebook", "cantidad": 1, "valor": 1500.00 }
  ],
  "cupon": "DESCUENTO10"
}

// 201 Created
{
  "orderId": 1,
  "orderStatus": "PENDING",
  "clientId": "cliente-1",
  "clientEmail": "cliente@example.com",
  "subtotalAmount": 1500.00,
  "discountAmount": 150.00,
  "taxAmount": 148.50,
  "totalAmount": 1498.50,
  "orderItems": [...],
  "creationDate": "2024-01-15T10:30:00",
  "paymentReference": null
}
```
Errores: `400` (customer o email faltante) · `422` (sin productos)

### PUT /api/orders/{id}/confirm
```json
// 200 OK → orderStatus: "CONFIRMED"
```
Errores: `404` (orden inexistente) · `409` (estado inválido)

### POST /api/orders/{id}/pay
```json
// Request
{ "method": "CREDIT_CARD" }

// 200 OK → orderStatus: "PAID"
```
Errores: `400` (método de pago faltante) · `402` (pago rechazado) · `404` (orden inexistente) · `409` (estado inválido)

### DELETE /api/orders/{id}
```json
// 200 OK → orderStatus: "CANCELLED"
```
Errores: `404` (orden inexistente) · `409` (estado inválido)

### GET /api/orders/{id}
```json
{
  "orderId": 1,
  "orderStatus": "PAID",
  "clientId": "cliente-1",
  "totalAmount": 1498.50,
  ...
}
```

### GET /api/orders
```
GET /api/orders                        → todas las órdenes
GET /api/orders?status=PAID            → filtradas por estado
GET /api/orders?customerId=cliente-1   → filtradas por cliente
```

### GET /api/reports/revenue
```json
{ "totalRevenue": 4500.00, "totalTaxes": 445.50 }
```

### GET /api/reports/range
```
GET /api/reports/range?from=2024-01-01T00:00:00&to=2024-01-31T23:59:59
```

## Interacciones con otros servicios

| Servicio                 | URL configurada                         | Uso                                              |
|--------------------------|-----------------------------------------|--------------------------------------------------|
| `payments-service`       | `http://payments-service:8081`          | Procesar pagos y reembolsos                      |
| `inventory-service`      | `http://inventory-service:8082`         | Reservar, confirmar o liberar unidades de stock  |
| `notifications-service`  | `http://notifications-service:8083`     | Notificar al cliente en eventos del ciclo de vida|

## Cómo correr

```bash
# Compilar y ejecutar
mvn spring-boot:run

# Ejecutar tests
mvn test

# Consola H2 (base de datos en memoria)
# http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:ordersdb
# User: sa  /  Password: (vacío)
```

## Stack

- Java 21
- Spring Boot 3.2.5
- Spring Data JPA + H2 (in-memory)
- Maven
- JUnit 5 + Mockito
