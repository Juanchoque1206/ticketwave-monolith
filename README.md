# TicketWave Events

Plataforma monolítica modular (Spring Boot 4, Java 21) para gestión de eventos y venta de tickets, con un flujo unificado de **reserva + compra** basado en `TicketOrder`.

## Tecnologías

- **Java 21**
- **Spring Boot 4.0**
- **Spring Data JPA** + PostgreSQL (H2 en memoria solo para tests)
- **Spring Security + JWT** (jjwt 0.12.6)
- **Spring Data Redis** (detección de fraude)
- **Spring Boot Mail** (dependencia presente)
- **OpenAPI / Swagger UI** (springdoc 2.8.6)
- **Lombok**
- **Actuator**

## Requerimientos

- JDK 21
- Maven 3.9+
- PostgreSQL 15+
- Redis 7+ (requerido: `FraudService` usa Redis en cada orden/chequeo de fraude)

## Estructura

```
ticketwave-monolith/
 ├── src/main/java/com/ticketwave/
 │   ├── TicketwaveApplication.java   # @EnableCaching @EnableScheduling @EnableMethodSecurity
 │   ├── config/        # Security, JwtAuthenticationFilter, OpenAPI, Cache, DataSeeder
 │   ├── controller/    # Event, Ticket, TicketOrder, User, Promotion, Payment, Notification, Fraud
 │   ├── service/       # Lógica de negocio + OrderExpiryJob (job programado)
 │   ├── domain/        # Entidades y enums
 │   ├── repository/    # Acceso a datos
 │   ├── dto/           # Request/Response records
 │   ├── exception/     # Excepciones + GlobalExceptionHandler
 │   ├── util/          # QrCodeGenerator, PriceCalculator
 │   └── modules/       # Frontera modular (package-info, preparación para microservicios)
 ├── src/main/resources/  # application.yml, application-local.yml, messages.properties
 └── src/test/
```

## Ejecución

```bash
# Desarrollo local (perfil 'local' → PostgreSQL en localhost:5432, ddl-auto: update)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Tests (H2 en memoria, perfil 'test')
mvn test
```

Configuración por variables de entorno (con valores por defecto en `application-local.yml`):

| Variable | Default | Descripción |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `local` | Perfil activo |
| `DB_URL` | `jdbc:postgresql://localhost:5432/ticketwave` | JDBC URL |
| `DB_USERNAME` | `postgres` | Usuario de BD |
| `DB_PASSWORD` | `postgres` | Contraseña de BD |
| `JWT_SECRET` | clave local de desarrollo | Secreto HMAC-SHA (≥ 32 bytes) |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis (usado por `FraudService`) |

Propiedades configurables:

- `ticketwave.jwt.expiration-ms` — expiración del token JWT (default `3600000` = 1 hora)
- `ticketwave.order-ttl-minutes` — TTL de una reserva `PENDING` (default `15`)
- `ticketwave.order-expiry-cron` — cron del job de expiración de órdenes (default `*/30 * * * * *`, cada 30 s)

Swagger UI: http://localhost:8081/swagger-ui/index.html
API docs (JSON): http://localhost:8081/v3/api-docs
Actuator health: http://localhost:8081/actuator/health

## Credenciales de demostración (seed automático)

`DataSeeder` se ejecuta solo si la tabla `app_users` está vacía y crea:

| Usuario | Contraseña | Rol   |
|---------|------------|-------|
| admin   | admin1234  | ADMIN |
| user    | user1234   | USER  |

También siembra un venue ("National Stadium"), un evento publicado ("Summer Music Festival") y una promoción `WELCOME10`.

## Endpoints principales

### Auth y usuarios — `/api/users`
| Método | Ruta                          | Acceso         | Descripción |
|--------|-------------------------------|----------------|-------------|
| POST   | `/api/users/register`         | público        | Registro → JWT |
| POST   | `/api/users/login`            | público        | Login → JWT |
| GET    | `/api/users/me`               | autenticado    | Perfil del usuario actual |
| GET    | `/api/users`                  | ADMIN          | Listar usuarios |
| GET    | `/api/users/{id}`             | ADMIN          | Obtener usuario por id |

### Eventos — `/api/events`
| Método | Ruta                  | Acceso      | Descripción |
|--------|-----------------------|-------------|-------------|
| GET    | `/api/events`         | público     | Búsqueda paginada (city, artist, venue, fromDate, toDate) |
| GET    | `/api/events/{id}`    | público     | Detalle de evento |
| POST   | `/api/events`         | ADMIN       | Crear evento |
| PUT    | `/api/events/{id}`    | ADMIN       | Actualizar evento |
| DELETE | `/api/events/{id}`    | ADMIN       | Cancelar evento |

### Órdenes — `/api/orders`
| Método | Ruta                           | Acceso      | Descripción |
|--------|--------------------------------|-------------|-------------|
| POST   | `/api/orders`                  | autenticado | Crear reserva (TicketOrder) |
| GET    | `/api/orders`                  | autenticado | Listar órdenes del usuario |
| GET    | `/api/orders/{orderId}`        | autenticado | Detalle de orden |
| POST   | `/api/orders/{orderId}/cancel` | autenticado | Cancelar antes del pago |

### Pagos — `/api/payments`
| Método | Ruta                          | Acceso      | Descripción |
|--------|-------------------------------|-------------|-------------|
| POST   | `/api/payments`               | autenticado | Confirmar reserva con pago |
| GET    | `/api/payments/order/{orderId}` | autenticado | Pagos de una orden |

### Tickets — `/api/tickets`
| Método | Ruta                          | Acceso      | Descripción |
|--------|-------------------------------|-------------|-------------|
| GET    | `/api/tickets/{id}`           | autenticado | Detalle de ticket |
| GET    | `/api/tickets/order/{orderId}`| autenticado | Tickets de una orden |
| POST   | `/api/tickets/validate`       | ADMIN       | Validar ticket QR en el venue |
| POST   | `/api/tickets/{id}/refund`    | autenticado | Reembolsar ticket |

### Promociones — `/api/promotions`
| Método | Ruta                | Acceso  | Descripción |
|--------|---------------------|---------|-------------|
| POST   | `/api/promotions`   | ADMIN   | Crear promoción |
| GET    | `/api/promotions`   | público | Listar promociones activas |

### Notificaciones — `/api/notifications`
| Método | Ruta                           | Acceso      | Descripción |
|--------|--------------------------------|-------------|-------------|
| GET    | `/api/notifications`           | autenticado | Notificaciones del usuario |
| PATCH  | `/api/notifications/{id}/read` | autenticado | Marcar como leída |

### Fraude — `/api/fraud`
| Método | Ruta                | Acceso      | Descripción |
|--------|---------------------|-------------|-------------|
| GET    | `/api/fraud/check`  | autenticado | Evaluación de riesgo de fraude |

## Flujo de adquisición (TicketOrder)

1. `POST /api/orders` → reserva temporal de tickets (bloquea capacidad del evento, aplica promoción, asigna `expiresAt` según `ticketwave.order-ttl-minutes`).
2. `POST /api/payments` → pago con Stripe/PayPal (simulado); al confirmarse se emiten tickets digitales con código QR (`TW-<hash SHA-256>`).
3. `POST /api/orders/{id}/cancel` → cancela la reserva y libera capacidad (solo antes del pago).
4. Las órdenes `PENDING` expiran automáticamente vía `OrderExpiryJob` (cada 30 s) y liberan capacidad.

## Seguridad

- JWT bearer token emitido en `/api/users/login` y `/api/users/register` (expiración configurable).
- Sesiones stateless, CSRF deshabilitado.
- Rutas públicas: `/api/users/register`, `/api/users/login`, `/api/events/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/h2-console/**`, `/actuator/health`, `/`. Todo lo demás requiere autenticación.
- Endpoints administrativos protegidos con `@PreAuthorize("hasRole('ADMIN')")`.
- Detección de fraude: límite de intentos por usuario/IP en Redis (bloqueo tras 10 intentos en 10 min) y prevención de órdenes duplicadas (clave `fraud:dup:*` con TTL 5 min).

## Entidades y enums

**Entidades:** `AppUser`, `Event`, `Venue`, `TicketOrder`, `OrderItem`, `Ticket`, `Payment`, `Promotion`, `Notification` (todas con id `UUID`).

**Enums:** `Role` (USER, ADMIN) · `EventStatus` (DRAFT, PUBLISHED, SOLD_OUT, CANCELLED) · `OrderStatus` (PENDING, CONFIRMED, COMPLETED, CANCELLED, EXPIRED, REFUNDED) · `TicketStatus` (EMITTED, VALIDATED, REFUNDED, REVOKED) · `PaymentStatus` (PENDING, SUCCEEDED, FAILED, REFUNDED) · `PaymentProvider` (STRIPE, PAYPAL) · `PromotionType` (PERCENTAGE, FIXED_AMOUNT) · `PromotionScope` (NATIONAL, VENUE_SPECIFIC) · `NotificationType` (ORDER_CONFIRMATION, EVENT_CHANGE, ORDER_CANCELLED, PAYMENT_RECEIVED) · `NotificationChannel` (EMAIL, PUSH).
