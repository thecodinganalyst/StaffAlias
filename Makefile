.PHONY: help layout

help:
	@echo "StaffAlias developer commands"
	@echo "  make layout  - verify expected monorepo directories exist"
	@echo ""
	@echo "Backend, frontend, database, test, and CI commands will be added by their respective setup issues."

layout:
	@test -d backend
	@test -d frontend
	@test -d docs
	@test -f .env.example
	@test -f docker-compose.yml
	@echo "Monorepo layout is present."
