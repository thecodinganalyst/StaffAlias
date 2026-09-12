.PHONY: help layout db-up db-down db-reset db-logs backend-test

help:
	@echo "StaffAlias developer commands"
	@echo "  make layout        - verify expected monorepo directories exist"
	@echo "  make db-up         - start local PostgreSQL"
	@echo "  make db-down       - stop local PostgreSQL"
	@echo "  make db-reset      - recreate local PostgreSQL and delete its data volume"
	@echo "  make db-logs       - follow PostgreSQL logs"
	@echo "  make backend-test  - run backend tests (requires Docker for Testcontainers)"

layout:
	@test -d backend
	@test -d frontend
	@test -d docs
	@test -f .env.example
	@test -f docker-compose.yml
	@echo "Monorepo layout is present."

db-up:
	docker compose up -d postgres

db-down:
	docker compose down

db-reset:
	docker compose down -v
	docker compose up -d postgres

db-logs:
	docker compose logs -f postgres

backend-test:
	cd backend && mvn test
