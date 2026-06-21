# AegisFlow

A production-grade **workflow orchestration platform** with a visual DAG editor, pluggable task execution, AI-powered failure analysis, human approval gates, and full event sourcing with replay.

Think **GitHub Actions / Airflow / Temporal** — but with a visual editor, AI agents, and human-in-the-loop approvals built in.

---

## What It Does

### The Core Loop

```
Define Workflow → Run It → Monitor in Real-Time → React to Failures
```

### 1. Define a Workflow

Create a **workflow** — a directed acyclic graph (DAG) of **nodes** (tasks) connected by **edges** (dependencies).

Example — a data pipeline:

```
Fetch Data (HTTP) → Validate (SQL) → Transform (Java) → Publish (HTTP)
```

Each node has a **type** that determines how it executes:

| Node Type | What It Does |
|-----------|-------------|
| **HTTP** | Makes an API call |
| **SQL** | Runs a database query |
| **SHELL** | Runs a terminal command |
| **JAVA** | Executes a Java class |
| **AGENT** | Invokes an AI agent (LLM) |
| **APPROVAL** | Pauses and waits for human approval |

Build workflows visually in the **drag-and-drop editor** (React Flow) or via REST API. The system validates the DAG — no cycles, no disconnected nodes.

### 2. Run It

Hit "Run" and the **Orchestration Engine** takes over:

1. Creates a `WorkflowRun` with one `TaskRun` per node
2. Identifies tasks with no upstream dependencies → marks them `READY`
3. **Workers** pick up `READY` tasks and execute them via the appropriate **TaskExecutor**
4. When a task succeeds → downstream tasks become `READY`
5. When a task fails → retries (configurable) or fails the run
6. Every state transition is recorded as an **immutable event**: `CREATED → READY → RUNNING → SUCCEEDED`

### 3. Monitor in Real-Time

- **WebSockets** push live status updates — tasks change status instantly in the UI
- **Dashboard** shows active runs, worker health, pending approvals, AI agent count
- **Run Detail** page shows each task's status, attempt count, output, and errors

### 4. AI + Human-in-the-Loop

**AI Agents:**
- **Recovery Agent** — analyzes task failures and returns structured diagnosis: root cause classification (NETWORK/TIMEOUT/CONFIG/etc.), confidence score, and recovery plan
- **Summary Agent** — generates human-readable summaries of workflow executions with highlights and recommendations
- Pluggable LLM backend — swap between Mock, OpenAI, Gemini, or Ollama via config

**Human Approval:**
- An `APPROVAL` node pauses the pipeline and creates an approval request
- A human reviews it in the **Approval Queue** UI and clicks Approve or Reject
- Approving unblocks the pipeline; rejecting fails the task

**Audit Trail:**
- Every significant action logged to `audit_log` via `@Audited` AOP annotation
- Queryable by entity type and ID

**Replay:**
- Reconstruct the full execution timeline of any run from its immutable events
- Visual timeline in the **Replay Viewer** page

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 21, Spring Boot 3.4, Maven |
| **Database** | PostgreSQL 16 (JSONB, Flyway migrations) |
| **Cache** | Redis 7 |
| **Frontend** | React 19, TypeScript, Vite, Tailwind CSS 4 |
| **Visual Editor** | React Flow (`@xyflow/react`) |
| **State Management** | TanStack React Query |
| **Real-Time** | STOMP over WebSocket |
| **AI** | Pluggable LLM providers (Mock/OpenAI/Gemini/Ollama) |
| **Mapping** | MapStruct, Lombok |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Infra** | Docker Compose |

---

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

Starts PostgreSQL (port 5432) and Redis (port 6379).

### 2. Run the backend

```bash
cd backend
mvn spring-boot:run
```

API starts at `http://localhost:8080`. Flyway runs migrations automatically.

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI spec:** http://localhost:8080/v3/api-docs

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

App starts at `http://localhost:5173`. API requests proxy to the backend.

### 4. Run tests

```bash
cd backend
mvn test          # All 36 tests (requires dev containers running)
```

---

## Project Structure

```
AegisFlow/
├── backend/src/main/java/com/aegisflow/
│   ├── common/                # BaseEntity, exceptions, global error handler, WebSocket config
│   ├── workflow/
│   │   ├── controller/        # Workflow CRUD REST endpoints
│   │   ├── entity/            # WorkflowDefinition, WorkflowNode, WorkflowEdge
│   │   ├── service/           # DAG validation, workflow service
│   │   └── mapper/            # MapStruct mappers
│   ├── execution/
│   │   ├── controller/        # Run controller, Replay controller
│   │   ├── entity/            # WorkflowRun, TaskRun, ExecutionEvent, WorkerNode
│   │   └── service/           # OrchestrationEngine, WorkerService, ReplayEngine
│   ├── task/executor/         # TaskExecutor SPI (HTTP, SQL, Shell, Java, Agent, Approval)
│   ├── agent/
│   │   ├── core/              # Agent interface, AgentRegistry
│   │   ├── agents/            # RecoveryAgent, SummaryAgent
│   │   ├── llm/               # LLMProvider interface + Mock/OpenAI/Gemini/Ollama
│   │   ├── service/           # ApprovalService, AgentMemoryService
│   │   └── controller/        # Agent + Approval REST endpoints
│   └── audit/                 # AuditLog entity, @Audited AOP aspect, AuditController
├── frontend/src/
│   ├── api/                   # Axios clients (workflows, runs, agents, approvals, audit)
│   ├── components/            # Layout, NodeConfigPanel, ErrorBoundary
│   ├── hooks/                 # useWebSocket (STOMP)
│   ├── pages/                 # Dashboard, WorkflowList, WorkflowEditor, RunList,
│   │                          # RunDetail, Workers, ApprovalQueue, AgentConsole,
│   │                          # AuditLogPage, ReplayViewer
│   └── types/                 # TypeScript interfaces (workflow, execution, agent)
├── docs/                      # Sample workflows
└── docker-compose-dev.yml
```

---

## API Endpoints (23 total)

### Workflows
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/workflows` | Create workflow |
| `GET` | `/api/workflows` | List all workflows (latest versions) |
| `GET` | `/api/workflows/{id}` | Get workflow by ID |
| `PUT` | `/api/workflows/{id}` | Update workflow (creates new version) |
| `DELETE` | `/api/workflows/{id}` | Delete workflow |
| `POST` | `/api/workflows/{id}/validate` | Validate workflow DAG |

### Execution
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/runs` | Start a workflow run |
| `GET` | `/api/runs` | List all runs |
| `GET` | `/api/runs/{id}` | Get run with task details |
| `GET` | `/api/runs/{id}/tasks` | Get tasks for a run |
| `POST` | `/api/runs/{id}/pause` | Pause a running run |
| `POST` | `/api/runs/{id}/resume` | Resume a paused run |
| `POST` | `/api/runs/{id}/cancel` | Cancel a run |
| `POST` | `/api/runs/{id}/retry` | Retry failed tasks |
| `GET` | `/api/runs/workers` | List worker nodes |

### AI Agents
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/agents/invoke` | Invoke an AI agent |
| `GET` | `/api/agents/types` | List registered agent types |
| `GET` | `/api/agents/memory/run/{runId}` | Get agent memory for a run |
| `GET` | `/api/agents/memory/task/{taskId}` | Get agent memory for a task |

### Approvals
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/approvals` | List pending approvals |
| `GET` | `/api/approvals/{id}` | Get approval by ID |
| `POST` | `/api/approvals/{id}/approve` | Approve with optional note |
| `POST` | `/api/approvals/{id}/reject` | Reject with optional note |
| `GET` | `/api/approvals/run/{runId}` | Get approvals for a run |

### Audit & Replay
| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/audit` | Recent audit entries |
| `GET` | `/api/audit/{entityType}/{entityId}` | Audit trail for an entity |
| `GET` | `/api/replay/{runId}` | Replay run from events |

---

## Key Design Decisions

- **Immutable versioning** — workflow updates create new versions; old versions are preserved
- **UUID primary keys** — no sequential ID leaks
- **JSONB columns** — flexible node config and retry policies
- **DAG validation** — cycle detection, duplicate node keys, disconnected nodes, self-edges
- **Constructor injection** — no field injection, follows Spring best practices
- **Records for DTOs** — immutable, concise data transfer objects
- **Plugin architecture** — task executors registered via Spring component scan
- **Event sourcing** — all state transitions stored as immutable events
- **LLM abstraction** — swap AI providers via `aegisflow.llm.provider` config

---

## Roadmap

- [x] **Phase 1** — Workflow CRUD, DAG validation, React Flow visual editor
- [x] **Phase 2** — Execution engine, workers, task executors (HTTP, SQL, Shell, Java)
- [x] **Phase 2.5** — WebSocket real-time updates, live connection indicator
- [x] **Phase 3** — AI agents, event sourcing, human approval gates, audit logging, replay

---

## Why Use AegisFlow?

Most workflow tools make you choose: either you get a visual editor (but no real execution engine), or you get a powerful runtime (but you're writing YAML/code). AegisFlow gives you both — plus AI and human-in-the-loop built in from day one.

| Pain Point | How AegisFlow Solves It |
|-----------|------------------------|
| **"Our pipelines are YAML files nobody understands"** | Visual drag-and-drop DAG editor — anyone can see the flow |
| **"When a task fails, we have no idea why"** | Recovery Agent analyzes failures, classifies root cause, suggests fixes |
| **"We need manager sign-off before deploying"** | Approval nodes pause the pipeline until a human approves |
| **"We can't trace what happened in a past run"** | Event sourcing + Replay reconstructs any run's full history |
| **"Our CI/CD tool can't orchestrate business logic"** | Pluggable executors — run HTTP calls, SQL queries, shell scripts, Java code, or AI agents |
| **"We want real-time visibility, not log tailing"** | WebSocket live updates — watch tasks execute in the browser |

---

## Example Use Cases

### 1. AI-Assisted Code Deployment Pipeline

```
Code Scan (AGENT) → Build (SHELL) → Test (SHELL) → Security Review (AGENT) → Manager Approval (APPROVAL) → Deploy (SHELL)
```

An AI agent scans the code for issues, another reviews security. A manager approves before deploy. If deploy fails, the Recovery Agent diagnoses why.

### 2. Data Pipeline with Quality Gates

```
Ingest from S3 (HTTP) → Validate Schema (SQL) → Transform (JAVA) → Quality Check (AGENT) → Publish to Warehouse (SQL)
```

Data flows through validation and transformation. An AI agent checks data quality before publishing. Full audit trail of every run.

### 3. Incident Response Automation

```
Alert Received (HTTP) → Gather Logs (SHELL) → Root Cause Analysis (AGENT) → Generate Fix (AGENT) → Approval (APPROVAL) → Apply Fix (SHELL) → Verify (HTTP)
```

When an alert fires, the system gathers logs, uses AI to find the root cause and generate a fix. A human approves before the fix is applied.

### 4. ML Model Training & Deployment

```
Fetch Dataset (HTTP) → Preprocess (JAVA) → Train Model (SHELL) → Evaluate (AGENT) → Stakeholder Approval (APPROVAL) → Deploy to Production (HTTP)
```

AI evaluates model metrics and generates a summary. Stakeholder reviews the summary and approves deployment.

### 5. Multi-Step Business Process

```
Receive Order (HTTP) → Fraud Check (AGENT) → Payment (HTTP) → Fulfillment (HTTP) → Notify Customer (HTTP)
```

An AI agent checks for fraud patterns. If flagged, it can route to a human approval step. Every state transition is auditable.

---

## License

Private — All rights reserved.
