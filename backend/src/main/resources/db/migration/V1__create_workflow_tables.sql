CREATE TABLE workflow_definition (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    version         INTEGER NOT NULL DEFAULT 1,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uq_workflow_name_version UNIQUE (name, version)
);

CREATE INDEX idx_workflow_definition_name ON workflow_definition(name);

CREATE TABLE workflow_node (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id         UUID NOT NULL REFERENCES workflow_definition(id) ON DELETE CASCADE,
    node_key            VARCHAR(255) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    type                VARCHAR(100) NOT NULL,
    config              JSONB DEFAULT '{}',
    timeout_seconds     INTEGER,
    retry_policy        JSONB DEFAULT '{}',
    position_x          DOUBLE PRECISION NOT NULL DEFAULT 0,
    position_y          DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uq_workflow_node_key UNIQUE (workflow_id, node_key)
);

CREATE INDEX idx_workflow_node_workflow_id ON workflow_node(workflow_id);

CREATE TABLE workflow_edge (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id     UUID NOT NULL REFERENCES workflow_definition(id) ON DELETE CASCADE,
    source_node_id  UUID NOT NULL REFERENCES workflow_node(id) ON DELETE CASCADE,
    target_node_id  UUID NOT NULL REFERENCES workflow_node(id) ON DELETE CASCADE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uq_workflow_edge UNIQUE (workflow_id, source_node_id, target_node_id),
    CONSTRAINT chk_no_self_edge CHECK (source_node_id <> target_node_id)
);

CREATE INDEX idx_workflow_edge_workflow_id ON workflow_edge(workflow_id);
