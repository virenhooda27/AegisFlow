-- Workflow runs
CREATE TABLE workflow_run (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id         UUID NOT NULL REFERENCES workflow_definition(id),
    status              VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    started_at          TIMESTAMP WITH TIME ZONE,
    completed_at        TIMESTAMP WITH TIME ZONE,
    error_message       TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_workflow_run_workflow_id ON workflow_run(workflow_id);
CREATE INDEX idx_workflow_run_status ON workflow_run(status);

-- Task runs (one per workflow node per run)
CREATE TABLE task_run (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_run_id     UUID NOT NULL REFERENCES workflow_run(id) ON DELETE CASCADE,
    workflow_node_id    UUID NOT NULL REFERENCES workflow_node(id),
    node_key            VARCHAR(255) NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    attempt             INTEGER NOT NULL DEFAULT 0,
    max_attempts        INTEGER NOT NULL DEFAULT 3,
    started_at          TIMESTAMP WITH TIME ZONE,
    completed_at        TIMESTAMP WITH TIME ZONE,
    error_message       TEXT,
    output              JSONB DEFAULT '{}',
    assigned_worker_id  UUID,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uq_task_run_per_node UNIQUE (workflow_run_id, node_key)
);

CREATE INDEX idx_task_run_workflow_run_id ON task_run(workflow_run_id);
CREATE INDEX idx_task_run_status ON task_run(status);

-- Worker nodes
CREATE TABLE worker_node (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(255) NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    last_heartbeat      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    active_tasks        INTEGER NOT NULL DEFAULT 0,
    max_tasks           INTEGER NOT NULL DEFAULT 5,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Execution events (append-only log)
CREATE TABLE execution_event (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_run_id     UUID NOT NULL REFERENCES workflow_run(id) ON DELETE CASCADE,
    task_run_id         UUID REFERENCES task_run(id) ON DELETE CASCADE,
    event_type          VARCHAR(100) NOT NULL,
    status_from         VARCHAR(50),
    status_to           VARCHAR(50),
    message             TEXT,
    metadata            JSONB DEFAULT '{}',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_execution_event_run_id ON execution_event(workflow_run_id);
CREATE INDEX idx_execution_event_type ON execution_event(event_type);
