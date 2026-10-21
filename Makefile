SHELL := bash

# Variables
BACKEND_PORT=8080
FRONTEND_PORT=5173
DB_PORT=3306

.PHONY: help dev deploy stop clean logs logs-backend schema db-shell

# Show available targets
help:
	@echo "Available targets:"
	@echo "  make dev      - Start MySQL + phpMyAdmin in Docker only, for local hot-reload dev."
	@echo "  make deploy   - Build and run full stack (frontend, backend, db, phpMyAdmin) with Docker Compose."
	@echo "  make stop     - Stop all running Docker Compose services."
	@echo "  make clean    - Stop and remove all Docker containers and volumes."
	@echo "  make logs     - Tail logs for all Docker services."
	@echo "  make logs-backend - Tail logs for the Spring Boot backend service."
	@echo "  make schema   - Apply backend/db/schema.sql to the running MySQL database."
	@echo "  make db-shell - Open an interactive MySQL shell against flight_tracker_db."

# Local dev: starts DB in Docker, then prints commands to run in separate terminals
dev:
	@echo "Starting DB and phpMyAdmin in Docker..."
	docker compose up -d db phpmyadmin
	@echo "Waiting for DB to be healthy..."
	@until docker compose exec -T db mysqladmin ping -h localhost -u root -ppassword --silent 2>/dev/null; do \
		printf '.'; \
		sleep 1; \
	done
	
	@echo ""
	@echo " --- DB IS READY --- "
	@echo ""
	@echo "Now open two terminal tabs and run:"
	@echo ""
	@echo "  [terminal 1 - backend]"
	@echo "    cd backend && ./gradlew bootRun"
	@echo ""
	@echo "  [terminal 2 - frontend]"
	@echo "    cd frontend && npm run dev"
	@echo ""
	@echo "Frontend:   http://localhost:$(FRONTEND_PORT)"
	@echo "Backend:    http://localhost:$(BACKEND_PORT)"
	@echo "phpMyAdmin: http://localhost:8081"

# Full Docker deploy (production-like, all containers)
deploy:
	@echo "Checking ports..."
	@lsof -i :$(BACKEND_PORT) > /dev/null && (echo "Port $(BACKEND_PORT) busy"; exit 1) || true
	@lsof -i :$(FRONTEND_PORT) > /dev/null && (echo "Port $(FRONTEND_PORT) busy"; exit 1) || true
	@lsof -i :$(DB_PORT) > /dev/null && (echo "Port $(DB_PORT) busy"; exit 1) || true
	@echo "Building and starting all containers..."
	docker compose up -d --build
	@echo "Waiting for backend to be healthy (this takes 10-20 seconds)..."
	@until $$(curl --output /dev/null --silent --head --fail http://localhost:$(BACKEND_PORT)/api/hello); do \
		printf '.'; \
		sleep 1; \
	done
	@echo "\n --- STACK IS LIVE --- "
	@echo "Frontend:   http://localhost:$(FRONTEND_PORT)"
	@echo "Backend:    http://localhost:$(BACKEND_PORT)"
	@echo "phpMyAdmin: http://localhost:8081"

stop:
	docker compose stop

logs:
	docker compose logs -f

# Backend API logs only (follow = stream)
logs-backend:
	docker compose logs -f backend

# Execute schema.sql on existing database (useful for incremental changes)
schema:
	@echo "Executing schema.sql on existing database..."
	@docker compose exec -T db mysql -u root -ppassword flight_tracker_db < backend/db/schema.sql
	@echo "Schema executed!"

# Open MySQL shell for manual database management
db-shell:
	@echo "Opening MySQL shell..."
	@docker compose exec db mysql -u root -ppassword flight_tracker_db

clean:
	@echo "Stopping and removing all containers..."
	docker compose down -v
	@echo "Cleaned up!"