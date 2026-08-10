# TicketWave Events

Modular monolith platform (Spring Boot 4, Java 21) for event management and ticket sales, with a unified **reserve + purchase** flow based on `TicketOrder`.

## Technologies

- **Java 21**
- **Spring Boot 4.0**
- **Spring Data JPA** + PostgreSQL (in-memory H2 only for tests)
- **Spring Security + JWT** (jjwt 0.12.6)
- **Spring Data Redis** (fraud detection)
- **Spring Boot Mail** (dependency present)
- **OpenAPI / Swagger UI** (springdoc 2.8.6)
- **Lombok**
- **Actuator**

## Requirements

- JDK 21
- Maven 3.9+
- PostgreSQL 15+
- Redis 7+ (required: `FraudService` uses Redis on every order/fraud check)

## Structure

```
ticketwave-monolith/
 ├── src/main/java/com/ticketwave/
 │   ├── TicketwaveApplication.java   # @EnableCaching @EnableScheduling @EnableMethodSecurity
 │   ├── config/        # Security, JwtAuthenticationFilter, OpenAPI, Cache, DataSeeder
 │   ├── controller/    # Event, Ticket, TicketOrder, User, Promotion, Payment, Notification, Fraud
 │   ├── service/       # Business logic + OrderExpiryJob (scheduled job)
 │   ├── domain/        # Entities and enums
 │   ├── repository/    # Data access
 │   ├── dto/           # Request/Response records
 │   ├── exception/     # Exceptions + GlobalExceptionHandler
 │   ├── util/          # QrCodeGenerator, PriceCalculator
 │   └── modules/       # Modular boundary (package-info, preparation for microservices)
 ├── src/main/resources/  # application.yml, application-local.yml, messages.properties
 ├── src/test/
 ├── Dockerfile           # Multi-stage build (maven → JRE)
 ├── .dockerignore
 └── diagrams/
     ├── c4model/         # C4 model: C1 context, C2 containers, C3 components
     ├── db/              # Database ER diagram
     └── architecture/    # Monolith architecture diagram
```

## Diagrams

Documentation diagrams (SVG, editable in draw.io):

### C4 model — `diagrams/c4model/`

| Level | File | Description |
|-------|------|-------------|
| C1 | `ticketwave-c1-context.drawio.svg` | System context: users and external systems around TicketWave |
| C2 | `ticketwave-c2-container.drawio.svg` | Containers: web/API, monolith, database, Redis |
| C3 | `ticketwave-c3-event-search.drawio.svg` | Component: event search |
| C3 | `ticketwave-c3-digital-ticket-service.drawio.svg` | Component: digital ticket service |
| C3 | `ticketwave-c3-ticket-purchase.drawio.svg` | Component: ticket purchase flow |
| C3 | `ticketwave-c3-payment-service.drawio.svg` | Component: payment service |
| C3 | `ticketwave-c3-promotions-service.drawio.svg` | Component: promotions service |
| C3 | `ticketwave-c3-notifications-service.drawio.svg` | Component: notifications service |
| C3 | `ticketwave-c3-refunds-cancellations.drawio.svg` | Component: refunds & cancellations |

### Database — `diagrams/db/`

| File | Description |
|------|-------------|
| `ticketwave-er-full.drawio.svg` | Full entity-relationship (ER) diagram of the database schema |

### Architecture — `diagrams/architecture/`

| File | Description |
|------|-------------|
| `ticketwave-monolith-architecture.drawio.svg` | Overall monolith architecture diagram |

## Running

```bash
# Local development ('local' profile → PostgreSQL at localhost:5432, ddl-auto: update)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Tests (in-memory H2, 'test' profile)
mvn test
```

Environment variable configuration (with defaults in `application-local.yml`):

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `local` | Active profile |
| `DB_URL` | `jdbc:postgresql://localhost:5432/ticketwave` | JDBC URL |
| `DB_USERNAME` | `postgres` | DB user |
| `DB_PASSWORD` | `postgres` | DB password |
| `JWT_SECRET` | local dev key | HMAC-SHA secret (≥ 32 bytes) |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis (used by `FraudService`) |

Configurable properties:

- `ticketwave.jwt.expiration-ms` — JWT token expiration (default `3600000` = 1 hour)
- `ticketwave.order-ttl-minutes` — TTL of a `PENDING` reservation (default `15`)
- `ticketwave.order-expiry-cron` — cron for the order expiration job (default `*/30 * * * * *`, every 30 s)

Swagger UI: http://localhost:8081/swagger-ui/index.html
API docs (JSON): http://localhost:8081/v3/api-docs
Actuator health: http://localhost:8081/actuator/health

### Testing the endpoints with Swagger UI

1. Quick access to the Test Events endpoint: <a href="http://localhost:8081/swagger-ui/index.html#/Test%20Events/list" target="_blank">open Test Events list in Swagger UI</a>.
2. Public endpoints (`/api/events/**`, `/api/users/login`, `/api/users/register`) can be tested directly.
3. For authenticated or admin endpoints:
   - Call `POST /api/users/login` with `{"username": "admin", "password": "admin1234"}` (or `user`/`user1234`).
   - Copy the `token` from the response.
   - Click **Authorize** (padlock button) and paste the token in the value field (format `Bearer <token>`).
   - Protected endpoints (`/api/orders`, `/api/payments`, etc.) and ADMIN ones (`POST/PUT/DELETE /api/events/**`, `POST /api/tickets/validate`, `POST /api/promotions`) will now respond with authorization.

## Docker

The `Dockerfile` uses a multi-stage build: it compiles with `maven:3.9-eclipse-temurin-21` (caches dependency downloads) and produces a lightweight JRE image with `eclipse-temurin:21-jre`. The container exposes port `8081` and uses the default active profile (`local` → PostgreSQL).

```bash
# Build the image
docker build -t ticketwave-monolith .

# Run
docker run -p 8081:8081 ticketwave-monolith
```

The app requires PostgreSQL and Redis, so it is recommended to spin up both and point the environment variables to the container host:

```bash
docker run -p 8081:8081 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/ticketwave \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e REDIS_HOST=host.docker.internal \
  -e REDIS_PORT=6379 \
  -e JWT_SECRET=<32-byte-secret> \
  ticketwave-monolith
```

> On Docker Desktop for Windows/Mac, `host.docker.internal` resolves to the local host. On native Linux use `--add-host=host.docker.internal:host-gateway` or the host IP.

If your PostgreSQL and Redis already run on the host and you access the app from the browser, publish the port (`-p 8081:8081`) and adjust `DB_URL`/`REDIS_HOST` according to the container network.

## Demo credentials (auto seed)

`DataSeeder` only runs if the `app_users` table is empty and creates:

| User | Password | Role  |
|------|----------|-------|
| admin | admin1234 | ADMIN |
| user  | user1234  | USER  |

It also seeds a venue ("National Stadium"), a published event ("Summer Music Festival") and a `WELCOME10` promotion.

## Main endpoints

### Auth & users — `/api/users`
| Method | Route                         | Access         | Description |
|--------|-------------------------------|----------------|-------------|
| POST   | `/api/users/register`         | public         | Register → JWT |
| POST   | `/api/users/login`            | public         | Login → JWT |
| GET    | `/api/users/me`               | authenticated | Current user profile |
| GET    | `/api/users`                  | ADMIN          | List users |
| GET    | `/api/users/{id}`             | ADMIN          | Get user by id |

### Events — `/api/events`
| Method | Route                 | Access     | Description |
|--------|-----------------------|------------|-------------|
| GET    | `/api/events`         | public     | Paginated search (city, artist, venue, fromDate, toDate) |
| GET    | `/api/events/{id}`    | public     | Event details |
| POST   | `/api/events`         | ADMIN      | Create event |
| PUT    | `/api/events/{id}`    | ADMIN      | Update event |
| DELETE | `/api/events/{id}`    | ADMIN      | Cancel event |

### Orders — `/api/orders`
| Method | Route                          | Access         | Description |
|--------|--------------------------------|----------------|-------------|
| POST   | `/api/orders`                  | authenticated | Create reservation (TicketOrder) |
| GET    | `/api/orders`                  | authenticated | List current user's orders |
| GET    | `/api/orders/{orderId}`        | authenticated | Order details |
| POST   | `/api/orders/{orderId}/cancel` | authenticated | Cancel before payment |

### Payments — `/api/payments`
| Method | Route                         | Access         | Description |
|--------|-------------------------------|----------------|-------------|
| POST   | `/api/payments`               | authenticated | Confirm reservation with payment |
| GET    | `/api/payments/order/{orderId}` | authenticated | Payments of an order |

### Tickets — `/api/tickets`
| Method | Route                         | Access         | Description |
|--------|-------------------------------|----------------|-------------|
| GET    | `/api/tickets/{id}`           | authenticated | Ticket details |
| GET    | `/api/tickets/order/{orderId}`| authenticated | Tickets of an order |
| POST   | `/api/tickets/validate`       | ADMIN          | Validate ticket QR at the venue |
| POST   | `/api/tickets/{id}/refund`    | authenticated | Refund ticket |

### Promotions — `/api/promotions`
| Method | Route              | Access | Description |
|--------|--------------------|--------|-------------|
| POST   | `/api/promotions`  | ADMIN  | Create promotion |
| GET    | `/api/promotions`  | public | List active promotions |

### Notifications — `/api/notifications`
| Method | Route                          | Access         | Description |
|--------|--------------------------------|----------------|-------------|
| GET    | `/api/notifications`           | authenticated | Current user's notifications |
| PATCH  | `/api/notifications/{id}/read` | authenticated | Mark as read |

### Fraud — `/api/fraud`
| Method | Route               | Access         | Description |
|--------|---------------------|----------------|-------------|
| GET    | `/api/fraud/check`  | authenticated | Fraud risk assessment |

## Acquisition flow (TicketOrder)

1. `POST /api/orders` → temporary ticket reservation (locks event capacity, applies promotion, sets `expiresAt` according to `ticketwave.order-ttl-minutes`).
2. `POST /api/payments` → Stripe/PayPal payment (simulated); once confirmed, digital tickets are issued with a QR code (`TW-<SHA-256 hash>`).
3. `POST /api/orders/{id}/cancel` → cancels the reservation and releases capacity (only before payment).
4. `PENDING` orders expire automatically via `OrderExpiryJob` (every 30 s) and release capacity.

## Security

- JWT bearer token issued at `/api/users/login` and `/api/users/register` (configurable expiration).
- Stateless sessions, CSRF disabled.
- Public routes: `/api/users/register`, `/api/users/login`, `/api/events/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/h2-console/**`, `/actuator/health`, `/`. Everything else requires authentication.
- Admin endpoints protected with `@PreAuthorize("hasRole('ADMIN')")`.
- Fraud detection: per-user/IP attempt limit in Redis (blocked after 10 attempts within 10 min) and duplicate order prevention (`fraud:dup:*` key with a 5-min TTL).

## Entities and enums

**Entities:** `AppUser`, `Event`, `Venue`, `TicketOrder`, `OrderItem`, `Ticket`, `Payment`, `Promotion`, `Notification` (all with `UUID` ids).

**Enums:** `Role` (USER, ADMIN) · `EventStatus` (DRAFT, PUBLISHED, SOLD_OUT, CANCELLED) · `OrderStatus` (PENDING, CONFIRMED, COMPLETED, CANCELLED, EXPIRED, REFUNDED) · `TicketStatus` (EMITTED, VALIDATED, REFUNDED, REVOKED) · `PaymentStatus` (PENDING, SUCCEEDED, FAILED, REFUNDED) · `PaymentProvider` (STRIPE, PAYPAL) · `PromotionType` (PERCENTAGE, FIXED_AMOUNT) · `PromotionScope` (NATIONAL, VENUE_SPECIFIC) · `NotificationType` (ORDER_CONFIRMATION, EVENT_CHANGE, ORDER_CANCELLED, PAYMENT_RECEIVED) · `NotificationChannel` (EMAIL, PUSH).
