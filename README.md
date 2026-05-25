# HRMS Backend - Worker Attendance & Overtime Settlement Engine

This project is a robust, scalable HRMS backend built for a construction company to manage their blue-collar workforce. It features a real-time attendance tracking system, an automated overtime calculation engine, and a monthly settlement processor.

## Tech Stack
- **Java 17** with **Spring Boot 3.2.5**
- **Spring Data JPA** (Hibernate)
- **PostgreSQL** (via Supabase or Local Docker)
- **Redis** (Caching layer)
- **Lombok** (Boilerplate reduction)
- **Maven** (Build tool)
- **Docker & Docker Compose** (Local infrastructure)
- **React 18** (Frontend Management Dashboard)

## Key Features & Design Decisions

### 1. Attendance & Overtime Engine
- **Atomic Clock-in/Clock-out:** Prevents double entries and ensures data integrity.
- **Dynamic Overtime Calculation:** Implements a tiered rate system (1.5x for first 2 hours, 2x beyond) and enforces a 60-hour monthly cap.
- **Real-time Monitoring:** Active workers are cached in Redis with a 16-hour TTL for high-performance site supervisor dashboards.

### 2. SOLID Principles & Design Patterns
- **Interface-based Services:** Adheres to the Dependency Inversion Principle for all service layer abstractions.
- **DTO Pattern:** Decouples internal entities from API contracts using specialized request/response objects.
- **Observer Pattern:** Uses Spring Events and `@TransactionalEventListener` for decoupled post-commit notifications (SMS).
- **Strategy/Override Pattern:** Supports site-specific business rule overrides with global fallbacks.

### 3. Performance & Auditability
- **N+1 Query Prevention:** Uses `@EntityGraph` to optimize database retrieval.
- **Self-Healing Cache:** Automatically re-populates Redis from the DB after a server restart or cache miss.
- **Automated Auditing:** Every record tracks its own `createdAt` and `updatedAt` timestamps automatically.

---

## Setup Instructions (Newbie Friendly)

### Prerequisites
- **JDK 17+** (Ensure `java -version` shows 17 or higher)
- **Maven** (`mvn -v`)
- **Docker Desktop** (Required for local Redis and Database)
- **Node.js 18+** (Required for the Frontend)

### 1. Start Local Infrastructure
Run this command in the project root to start Redis and PostgreSQL in the background:
```bash
docker-compose up -d
```
*Note: Hibernate will automatically create all required tables once the backend starts.*

### 2. Configure Environment
1.  Copy the example configuration: `cp .env.example .env`
2.  Open `.env` and ensure the values match your setup (The defaults work out-of-the-box with Docker).

### 3. Start the Backend
Open a terminal in the project root and run:
```bash
# On Linux/macOS:
export $(cat .env | xargs) && ./mvnw spring-boot:run -DskipTests -P!build-frontend

# On Windows (PowerShell):
foreach($line in Get-Content .env) { $split = $line.Split('='); if($split[0]) { [System.Environment]::SetEnvironmentVariable($split[0], $split[1]) } }; ./mvnw.cmd spring-boot:run -DskipTests -P!build-frontend
```

### 4. Start the Frontend
Open a **new** terminal window:
```bash
cd src/frontend
npm install
npm start
```
*The dashboard will open at `http://localhost:3000`.*

---

## API Endpoints

### Attendance
- `POST /api/attendance/clock-in`: Clock in a worker.
- `POST /api/attendance/clock-out`: Clock out and trigger overtime calculation.
- `GET /api/attendance/active`: List currently clocked-in workers (Redis-backed).
- `GET /api/attendance/log`: View a worker's full history (Paginated).

### Overtime & Management
- `GET /api/overtime/summary/{workerId}?month=2026-05`: Monthly breakdown.
- `POST /api/overtime/settle/{workerId}?month=2026-05`: Process settlement.
- `POST /api/workers`: Register a new worker.
- `POST /api/sites`: Register a site (optionally provide custom shift rules).

## Ticket Blitz Resolutions (Production Fixes)
- **LF-201 (CORS):** Global `WebMvcConfigurer` implementation.
- **LF-202 (Resilience):** `CacheErrorHandler` + Self-Healing DB fallback.
- **LF-203 (N+1):** Fetch Join optimization via `@EntityGraph`.
- **LF-204 (Transactions):** Post-commit SMS notifications using the Observer pattern.
- **LF-205 (Pool Optimization):** HikariCP tuning and pre-transactional external API fetching.

---
*Developed for the DeepThought Hiring Assignment, May 2026.*
