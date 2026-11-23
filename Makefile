# ==============================================================================
# OpsPilot - Developer Makefile
# Standardized orchestration commands for development and testing
# ==============================================================================

.PHONY: help up down restart status logs clean test

help:
	@echo "OpsPilot Development Commands:"
	@echo "  make up          - Start local container infrastructure (Postgres, Kafka, Redis)"
	@echo "  make down        - Stop local container infrastructure"
	@echo "  make restart     - Restart local container infrastructure"
	@echo "  make status      - Check running infrastructure services status"
	@echo "  make logs        - Tail logs from container infrastructure"
	@echo "  make clean       - Stop containers and remove volume data"
	@echo "  make test        - Run all unit and integration test suites"

up:
	docker compose up -d

down:
	docker compose down

restart:
	docker compose restart

status:
	docker compose ps

logs:
	docker compose logs -f

clean:
	docker compose down -v

test:
	@echo "Running tests across all services..."
	@if exist services\control-plane (cd services/control-plane && mvn test)
	@if exist services\ai-triage (cd services/ai-triage && pytest)
	@if exist apps\web (cd apps/web && npm test)
