# OpsPilot - Local Development Guide

## Overview

This guide covers everything needed to run OpsPilot on your local machine for development and testing.

> [!IMPORTANT]
> **Prerequisites**: Docker Engine 24+, Git 2.40+, Java 21 LTS, Python 3.11+, Node.js 20+ (Node 22 recommended)

---

## 1. First-Time Setup

### Clone the repository
```bash
git clone https://github.com/subrahmanyam-padala/distributed-enterprise-incident-finops-copilot.git
cd distributed-enterprise-incident-finops-copilot
```

### Copy environment configuration
```bash
cp .env.example .env
# Edit .env with your local values if needed (defaults work for development)
```

---

## 2. Start Infrastructure (Docker)

This starts PostgreSQL 16 (with pgvector), Kafka (KRaft), and Redis:

```bash
# Start all infrastructure containers
docker compose up -d

# Check all containers are healthy
docker compose ps

# Wait for Kafka init to create all topics (runs once, exits)
docker compose logs kafka-init

# Check Kafka topics were created
docker compose exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Expected output from `kafka --list`:
```
incident.alerts.v1
incident.triage.requested.v1
incident.triage.completed.v1
incident.triage.dlq.v1
```

Verify pgvector is active:
```bash
docker compose exec postgres psql -U opspilot_user -d opspilot -c "\dx"
```
Expected extensions: `uuid-ossp`, `vector`, `pg_trgm`

---

## 3. Run the Java Control Plane (Spring Boot)

> [!NOTE]
> Maven must be installed on your machine. Download from https://maven.apache.org/download.cgi or install via `choco install maven` on Windows.

```bash
cd services/control-plane

# First build (downloads dependencies)
mvn clean install -DskipTests

# Run the service
mvn spring-boot:run

# In a new terminal, verify health:
curl http://localhost:8080/api/v1/health
# Expected: {"status":"UP","service":"opspilot-control-plane",...}

# Swagger UI: http://localhost:8080/swagger-ui/index.html
# Actuator:   http://localhost:8080/actuator/health
```

---

## 4. Run the Python AI Triage Engine (FastAPI)

```bash
cd services/ai-triage

# Create virtual environment
python -m venv .venv

# Activate (Windows)
.venv\Scripts\activate
# Activate (Mac/Linux)
source .venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Start the service
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

# Verify health (new terminal):
curl http://localhost:8000/api/v1/health
# Swagger: http://localhost:8000/docs
```

---

## 5. Run the MCP Server (FastAPI)

```bash
cd services/mcp-server

python -m venv .venv
.venv\Scripts\activate      # Windows
# source .venv/bin/activate  # Mac/Linux

pip install -r requirements.txt

uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload

# Verify:
curl http://localhost:8001/api/v1/health
```

---

## 6. Run the React Dashboard (Vite)

```bash
cd apps/web

# Install npm packages
npm install

# Start development server
npm run dev

# Dashboard: http://localhost:5173
# Health page: http://localhost:5173/health
```

---

## 7. Run All Tests

### Java
```bash
cd services/control-plane
mvn test
```

### Python AI Triage
```bash
cd services/ai-triage
source .venv/bin/activate   # or .venv\Scripts\activate on Windows
pytest -v
```

### Python MCP Server
```bash
cd services/mcp-server
source .venv/bin/activate
pytest -v
```

### React Frontend
```bash
cd apps/web
npm test
```

---

## 8. Stop Everything

```bash
# Stop infrastructure containers
docker compose down

# To completely reset data volumes:
docker compose down -v
```

---

## 9. Ports Reference

| Service | Port | URL |
| :--- | :---: | :--- |
| Spring Boot Control Plane | 8080 | http://localhost:8080 |
| Swagger UI | 8080 | http://localhost:8080/swagger-ui/index.html |
| Spring Actuator | 8080 | http://localhost:8080/actuator/health |
| Python AI Triage Engine | 8000 | http://localhost:8000 |
| AI Triage Docs | 8000 | http://localhost:8000/docs |
| MCP Server | 8001 | http://localhost:8001 |
| React Dashboard (Vite) | 5173 | http://localhost:5173 |
| PostgreSQL | 5432 | `postgresql://localhost:5432/opspilot` |
| Apache Kafka | 9092 | `localhost:9092` |
| Redis | 6379 | `localhost:6379` |
