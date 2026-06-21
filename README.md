# AegisFlow

Production-grade workflow orchestration platform with a visual DAG editor, immutable versioning, and pluggable task execution.

## Tech Stack

### Backend
- **Java 21** + **Spring Boot 3.4**
- **PostgreSQL 16** — workflow persistence with JSONB columns
- **Redis 7** — caching layer
- **Flyway** — database migrations
- **MapStruct** — DTO mapping
- **Lombok** — boilerplate reduction
- **SpringDoc OpenAPI** — API documentation

### Frontend
- **React 19** + **TypeScript**
- **Vite** — build tooling
- **Tailwind CSS 4** — styling
- **React Flow** (`@xyflow/react`) — visual DAG editor
- **React Query** — server state management
- **Axios** — HTTP client
- **React Router** — navigation
- **Lucide React** — icons

### Infrastructure
- **Docker Compose** — local dev environment
- **Maven** — build & dependency management

## Prerequisites

- Java 21 (e.g. [Eclipse Adoptium](https://adoptium.net/))
- Maven 3.9+
- Node.js 20+
- Docker Desktop

## Getting Started

### 1. Start dev infrastructure

```bash
docker compose -f docker-compose-dev.yml up -d
```

This starts PostgreSQL (port 5432) and Redis (port 6379).

### 2. Run the backend

```bash
cd backend
mvn spring-boot:run
```

The API starts at `http://localhost:8080`. Flyway runs migrations automatically on startup.

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI spec:** http://localhost:8080/v3/api-docs

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The app starts at `http://localhost:5173`. API requests are proxied to the backend.

### 4. Run tests

```bash
# All tests (requires dev containers running)
cd backend
mvn test

# Unit tests only
mvn test -Dtest=DagValidationServiceTest
```

## Project Structure

```
AegisFlow/
├── backend/
│   └── src/main/java/com/aegisflow/
│       ├── common/              # BaseEntity, exceptions, global error handler
│       └── workflow/
│           ├── controller/      # REST endpoints
│           ├── dto/             # Request/response records
│           ├── entity/          # JPA entities (Definition, Node, Edge)
│           ├── mapper/          # MapStruct mappers
│           ├── repository/      # Spring Data JPA repos
│           └── service/         # Business logic, DAG validation
├── frontend/
│   └── src/
│       ├── api/                 # Axios client, workflow API
│       ├── components/          # Layout, NodeConfigPanel
│       ├── pages/               # Dashboard, WorkflowList, WorkflowEditor
│       └── types/               # TypeScript interfaces
└── docker-compose-dev.yml
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/workflows` | Create workflow |
| `GET` | `/api/workflows` | List all workflows (latest versions) |
| `GET` | `/api/workflows/{id}` | Get workflow by ID |
| `PUT` | `/api/workflows/{id}` | Update workflow (creates new version) |
| `DELETE` | `/api/workflows/{id}` | Delete workflow |
| `POST` | `/api/workflows/{id}/validate` | Validate workflow DAG |

## Key Design Decisions

- **Immutable versioning** — updates create new versions, old versions are preserved
- **UUID primary keys** — no sequential ID leaks
- **JSONB columns** — flexible node config and retry policies
- **DAG validation** — cycle detection, duplicate node keys, disconnected nodes, self-edges, invalid edge references
- **Constructor injection** — no field injection, follows Spring best practices
- **Records for DTOs** — immutable, concise data transfer objects
- **Plugin architecture** — task executors via SPI (Phase 2)

## Roadmap

- [x] **Phase 1** — Workflow CRUD, DAG validation, React Flow editor
- [ ] **Phase 2** — Execution engine, workers, task executors (HTTP, SQL, Shell, Java)
- [ ] **Phase 2.5** — WebSocket real-time updates
- [ ] **Phase 3** — AI agents, event sourcing, approval gates, replay

## License

Private — All rights reserved.
