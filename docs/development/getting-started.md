# Getting started

## Prerequisites

Install:

- Java 21
- Maven
- Node.js and npm
- Docker with Docker Compose
- Git

## Clone and configure

```bash
git clone https://github.com/thecodinganalyst/StaffAlias.git
cd StaffAlias
cp .env.example .env
```

For the frontend:

```bash
cp frontend/.env.example frontend/.env.local
```

Set `VITE_API_URL` in `frontend/.env.local` to the backend URL, normally `http://localhost:8080` for local development.

## Start PostgreSQL

From the repository root:

```bash
make db-up
```

See [database.md](database.md) for lifecycle/reset commands and Flyway guidance.

## Start the backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The Actuator health endpoint is available at `/actuator/health`.

## Start the frontend

In another terminal:

```bash
cd frontend
npm install
npm run dev
```

Vite serves the application on the configured frontend port, normally 5173 for local development.

## Quality checks

Backend:

```bash
cd backend
mvn test
```

Frontend:

```bash
cd frontend
npm run lint
npm run typecheck
npm run test
npm run build
```

Some backend integration tests use Testcontainers and therefore require Docker.

## Repository responsibilities

- `backend/` — backend application and database migrations.
- `frontend/` — browser application.
- `docs/` — architecture, development instructions, and ADRs.
- `docker-compose.yml` — local supporting infrastructure.
- `Makefile` — common developer commands.

Keep documentation changes in the same PR as architecture or workflow changes whenever practical.
