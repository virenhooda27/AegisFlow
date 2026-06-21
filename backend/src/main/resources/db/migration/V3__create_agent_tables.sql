-- Agent memory tables
CREATE TABLE agent_memory (
    id            UUID PRIMARY KEY,
    workflow_run_id UUID REFERENCES workflow_run(id),
    task_run_id   UUID REFERENCES task_run(id),
    agent_type    VARCHAR(50)  NOT NULL,
    memory_type   VARCHAR(50)  NOT NULL,
    content       TEXT         NOT NULL,
    metadata      JSONB        DEFAULT '{}',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_agent_memory_run ON agent_memory(workflow_run_id);
CREATE INDEX idx_agent_memory_task ON agent_memory(task_run_id);
CREATE INDEX idx_agent_memory_type ON agent_memory(agent_type, memory_type);

-- Approval tracking
CREATE TABLE approval_request (
    id            UUID PRIMARY KEY,
    workflow_run_id UUID NOT NULL REFERENCES workflow_run(id),
    task_run_id   UUID NOT NULL REFERENCES task_run(id),
    title         VARCHAR(500) NOT NULL,
    description   TEXT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    requested_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    resolved_at   TIMESTAMP WITH TIME ZONE,
    resolved_by   VARCHAR(255),
    resolution_note TEXT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_approval_status ON approval_request(status);
CREATE INDEX idx_approval_run ON approval_request(workflow_run_id);

-- Audit log
CREATE TABLE audit_log (
    id            UUID PRIMARY KEY,
    entity_type   VARCHAR(100) NOT NULL,
    entity_id     UUID         NOT NULL,
    action        VARCHAR(50)  NOT NULL,
    actor         VARCHAR(255),
    before_state  JSONB,
    after_state   JSONB,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_time ON audit_log(created_at);
