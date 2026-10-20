# Project KEYSTONE — Field Service Management Platform

Complete practice implementation of the supplied Zidio Development KEYSTONE brief.

## Stack
- Java 21 + Spring Boot 3
- Spring Web, Validation, Spring Data JPA/Hibernate
- Spring Security + JWT + BCrypt
- PostgreSQL + Flyway
- React + TypeScript + Vite
- Swagger/OpenAPI
- Docker + Docker Compose

The supplied brief defines four roles, a governed work-order lifecycle, server-side authorization, parts/time tracking, SLA monitoring, dashboard/reporting and a customer portal.

## Project structure
```text
keystone/
├── backend/
│   ├── src/main/java/com/zidio/keystone/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── domain/
│   │   ├── dto/
│   │   ├── security/
│   │   ├── config/
│   │   └── exception/
│   ├── src/main/resources/db/migration/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/
│   ├── src/
│   ├── package.json
│   ├── Dockerfile
│   └── nginx.conf
├── docker-compose.yml
├── .env.example
└── README.md
```

## 1. Prerequisites
Install:
- JDK 21
- Maven 3.9+
- PostgreSQL 16+
- Node.js 20+
- Git
- Docker Desktop
- IntelliJ IDEA / VS Code

## 2. Run with Docker (recommended)
From the project root:
```bash
docker compose up --build
```
Then open:
- Frontend: http://localhost
- Backend health: http://localhost:8080/api/health
- Swagger: http://localhost:8080/swagger-ui.html

Stop:
```bash
docker compose down
```
Reset database completely:
```bash
docker compose down -v
```

## 3. Run locally without Docker
### PostgreSQL
Create a database and user:
```sql
CREATE USER keystone WITH PASSWORD 'keystone';
CREATE DATABASE keystone OWNER keystone;
```

### Backend
```bash
cd backend
mvn clean test
mvn spring-boot:run
```
Flyway automatically runs the versioned migrations against the database.

### Frontend
In a second terminal:
```bash
cd frontend
npm install
npm run dev
```
Open http://localhost:5173.

## 4. Demo accounts
All demo accounts use:
`Password@123`

| Role | Email |
|---|---|
| Manager | manager@keystone.local |
| Dispatcher | dispatcher@keystone.local |
| Technician | technician@keystone.local |
| Customer | customer@keystone.local |

The seed creates a Meridian demo customer, a site, five parts and one user for each role on an empty database.

## 5. Work-order lifecycle
Server-side transition rules:
```text
NEW → ASSIGNED → IN_PROGRESS → ON_HOLD → IN_PROGRESS → COMPLETED → CLOSED
```
Cancellation is allowed from the applicable non-terminal open states. CLOSED and CANCELLED are terminal. Every status transition writes an append-only history row.

## 6. Main API
Authentication:
- `POST /api/auth/login`

Customers/sites:
- `GET /api/customers`
- `POST /api/customers`
- `PUT /api/customers/{id}`
- `GET /api/customers/{id}/sites`
- `POST /api/customers/{id}/sites`
- `PUT /api/sites/{id}`

Work orders:
- `GET /api/work-orders?page=0&size=10&search=&status=`
- `POST /api/work-orders`
- `GET /api/work-orders/{id}`
- `GET /api/work-orders/{id}/history`
- `POST /api/work-orders/{id}/assign`
- `POST /api/work-orders/{id}/status`
- `POST /api/work-orders/{id}/parts`
- `POST /api/work-orders/{id}/time`
- `POST /api/work-orders/{id}/attachments`
- `GET /api/work-orders/{id}/attachments`

Reporting/operations:
- `GET /api/reports/summary`
- `GET /api/technicians`
- `GET /api/parts`
- `POST /api/parts` (manager)
- `GET /api/notifications`

## 7. Security rules
- Login is public; protected business endpoints require a valid JWT.
- Passwords are BCrypt hashes.
- Role authorization is enforced server-side with Spring Security method security.
- Customers are scoped to their own organization.
- Technicians can act only on their assigned work orders.
- Only managers can close completed work orders.
- Parts stock is decremented inside a transaction and cannot become negative.
- Validation failures use a structured JSON error response.
- JWT secret, database credentials and CORS origins are configurable environment values.

## 8. SLA
SLA due time is calculated from priority:
- CRITICAL: 4 hours
- HIGH: 8 hours
- MEDIUM: 24 hours
- LOW: 72 hours

A scheduled job runs every five minutes and creates an in-app notification for assigned technicians when an active work order is overdue.

## 9. Submission checklist
Before submitting to Zidio:
- [ ] Push backend and frontend to GitHub.
- [ ] Deploy backend.
- [ ] Deploy frontend.
- [ ] Deploy PostgreSQL/managed database.
- [ ] Verify Swagger is publicly reachable.
- [ ] Provide four seed logins.
- [ ] Record a 3–5 minute demo covering all roles and the lifecycle.
- [ ] Add live URLs to the submission form.
- [ ] Never commit `.env` or production secrets.

Deployment hosting and credentials are intentionally not hard-coded because they depend on your own hosting accounts.
