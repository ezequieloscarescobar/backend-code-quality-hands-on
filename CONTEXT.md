# orders-service — Contexto de Negocio

## ¿Qué hace este servicio?

El `orders-service` es el microservicio responsable de gestionar el ciclo de vida de las órdenes de compra dentro de la plataforma. Una orden representa la intención de un cliente de adquirir uno o más productos.

## Ciclo de vida de una orden

Una orden transita por los siguientes estados:

```
PENDING → CONFIRMED → PAID → (fin)
                   ↘
               CANCELLED (desde cualquier estado)
```

- **PENDING**: la orden fue creada pero aún no fue confirmada.
- **CONFIRMED**: el equipo o el sistema validó la orden y está lista para ser pagada.
- **PAID**: el pago fue procesado exitosamente.
- **CANCELLED**: la orden fue cancelada. Si ya estaba paga, debe procesarse un reembolso.

## Reglas de negocio relevantes

- Toda orden debe tener al menos un ítem y un cliente identificado.
- El precio final de una orden incluye descuentos por cupón (si aplica) e impuestos.
- Existen cupones de descuento vigentes que el cliente puede aplicar al momento de crear la orden.
- Los impuestos se calculan sobre el subtotal ya descontado.
- Una orden cancelada con pago previo debe gatillar un reembolso antes de cambiar de estado.

## Interacciones con otros servicios

El `orders-service` interactúa con tres servicios externos:

### payments-service
Responsable de procesar y reembolsar pagos. El `orders-service` lo consulta al momento de pagar o cancelar una orden paga.

### inventory-service
Responsable de gestionar el stock de productos. El `orders-service` debe coordinar con él para reservar, confirmar o liberar unidades según el estado de la orden.

### notifications-service
Responsable de enviar comunicaciones al cliente (email, push, etc.). El `orders-service` debe notificar al cliente en los momentos relevantes del ciclo de vida de su orden.

## Reportes

El servicio expone un conjunto de endpoints de reporting que permiten consultar métricas operativas: órdenes por estado, ingresos totales, impuestos recaudados y órdenes en un rango de fechas. Estos reportes son consumidos por un dashboard interno del equipo de operaciones.

## Lo que NO hace este servicio

- No gestiona el catálogo de productos (eso es responsabilidad de otro servicio).
- No autentica usuarios.
- No procesa pagos directamente.
