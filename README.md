# SpazaCred

**Small Business Credit & Payment Management Platform for Spaza Shops**

[![CI](https://github.com/lundi-m/SpazaCred/actions/workflows/ci.yml/badge.svg)](https://github.com/lundi-m/SpazaCred/actions/workflows/ci.yml)

SpazaCred is a REST API that lets a small shop sell goods on credit and keep an honest record of it. Staff manage customers and their credit accounts, record credit purchases and payments, and the system tracks what everyone owes, who is overdue, and who did what.

It is built as a production-style backend rather than a demo: the financial history is the source of truth, nothing is silently overwritten, and every important action is audited.

**Live API:** [Swagger UI](https://spazacred-api.onrender.com/swagger-ui/index.html) · [OpenAPI Specification](https://spazacred-api.onrender.com/v3/api-docs) · [Health Check](https://spazacred-api.onrender.com/actuator/health)

## Contents

- [Key Ideas](#key-ideas)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Trying It Out](#trying-it-out)
- [Roles and Permissions](#roles-and-permissions)
- [API Reference](#api-reference)
- [Business Rules](#business-rules)
- [Error Responses](#error-responses)
- [Design Decisions](#design-decisions)
- [Testing](#testing)
- [CI and Deployment](#ci-and-deployment)
- [Project Structure](#project-structure)
- [Future Improvements](#future-improvements)

## Key Ideas

- **Two kinds of people.** A `User` is a staff member who logs in (`ADMIN`, `SHOP_OWNER`, `EMPLOYEE`). A `Customer` buys on credit and never logs in. They are separate entities.
- **Balances are never stored.** Outstanding balance is calculated from active credit purchases minus active payment allocations. There is no balance column that can drift out of sync.
- **Financial records are not edited or deleted.** Purchases and payments are corrected by voiding them. A void retains the original record, who voided it and why, while reversing its financial effect.
- **Oldest debt first.** A payment is split across a customer's debts, oldest due date first. A customer who owes R500, R700 and R300 and pays R800 clears the first debt and puts R300 towards the second, leaving R400 owing on it.
- **Everything is audited.** Every important state change records who did it, what happened, which record was affected, and when. Actions performed by the system itself (such as the overdue job) are recorded without a user.

## Tech Stack

Java 21 · Spring Boot 4.1.2 (Web MVC, Data JPA, Security, Validation, Actuator) · PostgreSQL 16 · Flyway · JWT (JJWT 0.13.0) · Springdoc OpenAPI 3.2.0 · Maven · JUnit 5 · Mockito · MockMvc · Testcontainers · Docker · Docker Compose · GitHub Actions

**Architecture:** A modular monolith. Each domain has its own package with controller, service, repository, entity and DTO layers. Controllers stay thin and the rules live in services.

## Getting Started

### Option A: Docker Compose (Recommended)

Requires Docker and Docker Compose.

```bash
cp .env.example .env
```

Edit `.env` and set `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` and `JWT_SECRET`.

Generate a strong JWT secret, for example:

```bash
openssl rand -base64 48
```

Start the application:

```bash
docker compose up --build
```

The API is then available at `http://localhost:8080`.

Check it with:

```bash
curl http://localhost:8080/actuator/health
```

A healthy response should include:

```json
{
  "status": "UP"
}
```

`docker compose down` stops the containers and keeps your database data in the named volume.

`docker compose down -v` also deletes the database volume and its data.

### Option B: Run It Locally

Requires Java 21 and a local PostgreSQL 16 instance.

Create a database and user:

```sql
CREATE USER spazacred_user WITH PASSWORD 'choose_a_password';
CREATE DATABASE spazacred_db OWNER spazacred_user;
```

Set the environment variables and start the application:

```bash
export DB_NAME=spazacred_db
export DB_USERNAME=spazacred_user
export DB_PASSWORD=choose_a_password
export JWT_SECRET=$(openssl rand -base64 48)

./mvnw spring-boot:run
```

Flyway creates the database schema on first start. Hibernate only validates that the entities match it (`ddl-auto: validate`), so the schema is changed through migrations rather than Hibernate automatically modifying tables.

## Configuration

| Setting               | Environment Variable / Property       | Default                                         | Notes                                                |
|-----------------------|---------------------------------------|-------------------------------------------------|------------------------------------------------------|
| Database name         | `DB_NAME`                             | `spazacred_db`                                  | Used by Docker Compose                               |
| Database URL          | `DB_URL`                              | `jdbc:postgresql://localhost:5432/spazacred_db` | In Docker Compose this points to the `db` service    |
| Database user         | `DB_USERNAME`                         | `spazacred_user`                                |                                                      |
| Database password     | `DB_PASSWORD`                         | `spazacred_pass`                                | Override with a strong password                      |
| JWT signing secret    | `JWT_SECRET`                          | None                                            | Required; use a strong secret of at least 32 bytes   |
| Token lifetime        | `jwt.expiration-hours`                | `12`                                            |                                                      |
| Overdue grace period  | `SPAZACRED_CREDIT_OVERDUE_GRACE_DAYS` | `7`                                             | See Business Rules                                   |
| Overdue job schedule  | `credit.overdue-job-cron`             | `0 0 1 * * *`                                   | Daily at 01:00                                       |
| Overdue job time zone | `credit.overdue-job-zone`             | `Africa/Johannesburg`                           |                                                      |
| Swagger/OpenAPI       | `SWAGGER_ENABLED`                     | `true`                                          | Enables or disables Swagger UI and OpenAPI endpoints |

Environment variables can override application properties. For example:

```bash
SPAZACRED_CREDIT_OVERDUE_GRACE_DAYS=14
```

**Time zone:** Business dates use the `Africa/Johannesburg` time zone. The application is configured to use this zone for date-dependent operations.

## Trying It Out

### Live Deployment

SpazaCred is deployed using Render for the Spring Boot API and Neon for PostgreSQL.

- **Swagger UI:** [https://spazacred-api.onrender.com/swagger-ui/index.html](https://spazacred-api.onrender.com/swagger-ui/index.html)
- **OpenAPI Specification:** [https://spazacred-api.onrender.com/v3/api-docs](https://spazacred-api.onrender.com/v3/api-docs)
- **Health Check:** [https://spazacred-api.onrender.com/actuator/health](https://spazacred-api.onrender.com/actuator/health)

The deployed API can be explored and tested directly through Swagger UI without installing the application or setting up a local database.

The application runs on Render's free hosting tier. The service may become idle after inactivity, so the first request after a period of inactivity can take longer while the application starts.

### Initial Administrator Account

An initial administrator account is created through a Flyway database migration. The administrator must change the temporary password before accessing normal API functionality.

The initial login credentials are provided separately for the deployed demonstration.

### First-Time Login

1. Open the [Swagger UI](https://spazacred-api.onrender.com/swagger-ui/index.html).
2. Find `POST /api/v1/auth/login`.
3. Log in using the supplied administrator credentials.
4. Copy the JWT returned in the response.
5. Open `POST /api/v1/auth/change-password`.
6. Authorize the request using the JWT.
7. Provide the current temporary password and a new password.
8. Log in again using the new password.

### API Examples

The following examples use Bash. They assume the API is running locally. To test the deployed application, use the deployed base URL instead.

Set the API base URL:

```bash
BASE=http://localhost:8080/api/v1
```

For the deployed API:

```bash
BASE=https://spazacred-api.onrender.com/api/v1
```

**1. Log in**

```bash
curl -s -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"your-password"}'
```

**2. Create a customer with a R2,000 credit limit**

```bash
curl -s -X POST "$BASE/customers" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Thabo","lastName":"Mokoena","phone":"0821234567","idNumber":"9001015009087","address":"12 Main Road","creditLimit":2000.00}'
```

**3. Record a purchase on credit**

The due date is optional and defaults to 30 days.

```bash
curl -s -X POST "$BASE/customers/1/credit-transactions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":500.00,"description":"Groceries"}'
```

**4. Record a payment**

Payments are allocated to the oldest outstanding debts automatically.

```bash
curl -s -X POST "$BASE/customers/1/payments" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount":200.00,"paymentMethod":"CASH"}'
```

**5. View the customer's account**

```bash
curl -s "$BASE/customers/1/account" \
  -H "Authorization: Bearer $TOKEN"
```

Replace customer ID `1` with the ID returned when you create a customer.

## Roles and Permissions

| Action                                                         | EMPLOYEE | SHOP_OWNER | ADMIN |
|----------------------------------------------------------------|:--------:|:----------:|:-----:|
| Create, read and update customers                              |   Yes    |    Yes     |  Yes  |
| Record purchases and payments, view history                    |   Yes    |    Yes     |  Yes  |
| View balances, eligibility and reports                         |   Yes    |    Yes     |  Yes  |
| Deactivate a customer                                          |          |    Yes     |  Yes  |
| Change a credit limit, suspend, reactivate or close an account |          |    Yes     |  Yes  |
| Void a purchase or a payment                                   |          |    Yes     |  Yes  |
| Manage users, read the audit log                               |          |            |  Yes  |

Roles and the enabled status are read from the database on every request, so changing a user's role or disabling their account takes effect immediately, even for tokens already issued.

## API Reference

Base path: `/api/v1`.

All endpoints except login and the health check require:

```http
Authorization: Bearer <token>
```

Request and response bodies use JSON.

### Auth

| Method and Path              | Description                                                                      |
|------------------------------|----------------------------------------------------------------------------------|
| `POST /auth/login`           | Log in with email and password. Returns the token, role and `mustChangePassword` |
| `POST /auth/change-password` | Change your own password                                                         |

### Users (Admin)

| Method and Path | Description                                                                 |
|-----------------|-----------------------------------------------------------------------------|
| `POST /users`   | Create a staff user. They must change the temporary password at first login |
| `GET /users`    | List users (paginated)                                                      |

### Customers

| Method and Path                    | Description                                                      |
|------------------------------------|------------------------------------------------------------------|
| `POST /customers`                  | Create a customer and their credit account with an initial limit |
| `GET /customers`                   | List customers. Query: `search`, `page`, `size`, `sort`          |
| `GET /customers/{id}`              | Get one customer                                                 |
| `PUT /customers/{id}`              | Update customer details (the ID number cannot be changed)        |
| `PATCH /customers/{id}/deactivate` | Deactivate a customer. Requires a reason and a zero balance      |

### Credit Account (`/customers/{customerId}/account`)

| Method and Path     | Description                                                              |
|---------------------|--------------------------------------------------------------------------|
| `GET /`             | Get the credit limit, status, outstanding balance and available credit   |
| `GET /eligibility`  | Check whether the customer may take new credit, including reasons if not |
| `PATCH /limit`      | Change the credit limit. Requires `newLimit` and a reason                |
| `PATCH /suspend`    | Suspend an account. Requires a reason                                    |
| `PATCH /reactivate` | Reactivate a suspended or closed account. Requires a reason              |
| `PATCH /close`      | Close an account. Requires a reason and zero balance                     |

### Purchases and Payments

| Method and Path                            | Description                                                     |
|--------------------------------------------|-----------------------------------------------------------------|
| `POST /customers/{id}/credit-transactions` | Record a purchase on credit                                     |
| `GET /customers/{id}/credit-transactions`  | Get a customer's purchase history (paginated)                   |
| `POST /credit-transactions/{id}/void`      | Void a purchase that has no active payments. Requires a reason  |
| `POST /customers/{id}/payments`            | Record a payment. Returns how it was allocated                  |
| `GET /customers/{id}/payments`             | Get a customer's payment history (paginated)                    |
| `POST /payments/{id}/void`                 | Void a payment. Its allocations are reversed. Requires a reason |

### Reports

| Method and Path                        | Description                                                              |
|----------------------------------------|--------------------------------------------------------------------------|
| `GET /reports/overdue`                 | Get overdue debts, oldest first. Query: `minDaysOverdue` (default 1)     |
| `GET /reports/outstanding-by-customer` | Get customers who currently owe money, ordered by name                   |
| `GET /reports/payments`                | Get payments in a date range with totals per method. Query: `from`, `to` |

### Audit (Admin)

| Method and Path   | Description                                                                     |
|-------------------|---------------------------------------------------------------------------------|
| `GET /audit-logs` | Query: `entityType`, `entityId`, `userId`, `action`, `from`, `to`. Newest first |

### Operations

| Method and Path        | Description         |
|------------------------|---------------------|
| `GET /actuator/health` | Public health check |

**Paging and sorting:** List endpoints accept `page` (starting at 0), `size` (maximum 100) and `sort=field,asc`. Reports have a fixed order and ignore `sort`.

**Dates and money:** Dates use `YYYY-MM-DD`. Amounts have at most two decimal places and are stored as `NUMERIC(19,2)`.

## Business Rules

### Credit

- A purchase is refused if the outstanding balance plus the purchase amount would exceed the credit limit.
- Only an ACTIVE account belonging to an active customer can take new credit. Suspended accounts can still receive payments.
- A purchase's due date defaults to 30 days; staff can choose another date, but not one in the past.
- Closing requires a zero balance. A closed account can be reactivated through the authorised reactivation operation.

### Payments

- A payment cannot exceed what the customer owes. There are no credit balances.
- A payment is split across the customer's debts, oldest due date first (ties: the debt created first). No debt receives more than its outstanding amount.

### Corrections

- Purchases and payments are not edited or deleted. An incorrect entry is voided with a reason.
- A purchase with active payments cannot be voided. Void the payment first.
- Voiding a payment reverses its allocations, so the debts it covered become outstanding again immediately.

### Overdue

- A debt is overdue when its due date has passed and money is still owed. This is calculated, never stored.
- A scheduled job runs daily and suspends accounts with a debt more than 7 days overdue (a debt exactly 7 days overdue is not yet suspended). It records an `ACCOUNT_AUTO_SUSPENDED` audit entry without a user.
- The job never automatically reactivates an account. If staff reactivate an account that is still overdue, the next run suspends it again.

### Customers and Staff

- Customers are deactivated, never deleted, and only when they owe nothing.
- New staff accounts and the seeded admin must change their password before doing anything else.

## Error Responses

Every error has the same structure:

```json
{
  "timestamp": "2026-10-15T08:00:00Z",
  "status": 409,
  "error": "Conflict",
  "code": "CREDIT_LIMIT_EXCEEDED",
  "message": "Purchase of 600.00 would bring outstanding balance to 2100.00, exceeding the credit limit of 2000.00",
  "fieldErrors": null
}
```

Clients should branch on `code`, not on the message text. Validation errors include details for each invalid field in `fieldErrors`.

| Status    | Codes                                                                                                                                                                               |
|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 400       | `VALIDATION_FAILED`, `MALFORMED_REQUEST`, `INVALID_PARAMETER`, `MISSING_PARAMETER`, `INVALID_SORT_PROPERTY`, `INVALID_DATE_RANGE`, `INVALID_CURRENT_PASSWORD`, `PASSWORD_UNCHANGED` |
| 401       | `AUTHENTICATION_REQUIRED`, `TOKEN_INVALID`, `TOKEN_EXPIRED`, `INVALID_CREDENTIALS`                                                                                                  |
| 403       | `FORBIDDEN`, `PASSWORD_CHANGE_REQUIRED`                                                                                                                                             |
| 404       | Resource not found, or `ENDPOINT_NOT_FOUND`                                                                                                                                         |
| 405 / 415 | `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE`                                                                                                                                      |
| 409       | Business-rule conflicts (credit limit exceeded, payment above balance, invalid status change, duplicate phone/ID/email, and so on), `CONCURRENT_MODIFICATION`, `DATA_CONFLICT`      |
| 500       | `INTERNAL_ERROR` (the cause is logged, never returned)                                                                                                                              |

A wrong password and an unknown email return the same `INVALID_CREDENTIALS` response, so the API cannot be used to determine which email addresses exist.

## Design Decisions

**Derived values over stored values.** Balance, available credit and overdue status come from the financial history. The cost is a few extra queries. The benefit is that they cannot contradict the records.

**Money rules are enforced twice.** In the services, and again in the database with CHECK, NOT NULL and UNIQUE constraints, so a forgotten check in code cannot write invalid data. Amounts use `BigDecimal` and `NUMERIC(19,2)`, never floating-point types.

**Concurrency.** Two simultaneous purchases could both pass the limit check. Purchases, payments and relevant void operations therefore use database locking to prevent conflicting financial operations. `@Version` on the account also guards against concurrent account updates.

**A person behind every financial record.** Financial records require a user (NOT NULL in the database and checked in the constructors). Only the audit log allows no user, through a separate method used by the overdue job.

**Per-account transactions in the job.** The job suspends each account in its own short transaction and re-checks it under a lock, so one failure does not stop the rest and a payment arriving mid-run is respected.

**An injectable Clock** instead of direct `now()` calls, so date-dependent rules are testable.

**Stateless authentication.** JWT is supplied in the Authorization header. Roles and the enabled flag are re-read from the database on every request.

## Testing

Run the test suite:

```bash
./mvnw test
```

Docker must be running for tests that use Testcontainers.

| Layer                             | What It Covers                                                                                                                      |
|-----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| Unit tests (JUnit 5, Mockito)     | Money rules: allocation order, credit limit, status transitions, voids, overdue decisions and the "no user, no record" guard        |
| Repository tests (Testcontainers) | Hand-written queries against a real PostgreSQL database, including boundaries and paging counts                                     |
| Database constraint tests         | Database constraints such as required users, valid amounts and uniqueness                                                           |
| API tests (MockMvc)               | Security and tokens, role permissions, temporary-password flow, validation and error shapes, money flows, reports and audit records |
| End-to-end tests                  | Real parallel requests (race conditions), overdue job behaviour and lazy-loading scenarios                                          |

Flyway runs migrations against the test database, allowing the suite to verify that the schema can be created from scratch.

## CI and Deployment

**GitHub Actions:** `.github/workflows/ci.yml` runs the Maven verification lifecycle (`./mvnw -B verify`) on pushes to `master` and on pull requests. Tests must pass before the Docker image job runs.

**Container registry:** Successful builds on `master` publish the application image to GitHub Container Registry (GHCR).

**Dependabot:** Checks Maven dependencies and GitHub Actions dependencies weekly and opens update pull requests.

**Docker:** A multi-stage Dockerfile builds the application image. Docker Compose runs the API and PostgreSQL with health checks and stores database data in a persistent named volume.

**Hosted deployment:**

- Render hosts the Spring Boot API.
- Neon hosts the PostgreSQL database.
- The application image is published to GitHub Container Registry.
- Environment-specific configuration and secrets are supplied through Render environment variables.

The hosted deployment is intended for demonstration and portfolio evaluation, not for storing real customer financial information.

For self-hosted production deployments:

- Put the application behind HTTPS.
- Set strong secrets in a server-only environment configuration. Never commit secrets.
- Back up the database regularly. A Docker volume is not a backup.
- Monitor application health and database availability.

For example, a PostgreSQL backup can be created with:

```bash
docker compose exec -T db pg_dump -U "$DB_USERNAME" "$DB_NAME" | gzip > backup-$(date +%F).sql.gz
```

## Project Structure

```text
src/main/java/com/lundi_m/spazacred_api/
├── auth        users, login, password change
├── customer    customers
├── credit      credit accounts, purchases, eligibility, overdue job
├── payment     payments and allocation
├── report      overdue, outstanding and payment reports
├── audit       audit log
├── security    JWT filter, security rules, current user
├── exception   error types and the global error handler
└── config      auditing, clock, scheduling

src/main/resources/db/migration     Flyway migrations
src/test/java/...                   unit, repository, API and end-to-end tests
```

## Future Improvements

- Login rate limiting
- Logout and token revocation
- Automatic credit-limit growth based on repayment history
