export interface NodeDto {
  id?: string;
  nodeKey: string;
  name: string;
  type: string;
  config?: Record<string, unknown>;
  timeoutSeconds?: number;
  retryPolicy?: Record<string, unknown>;
  positionX: number;
  positionY: number;
}

export interface EdgeDto {
  sourceNodeKey: string;
  targetNodeKey: string;
}

export interface WorkflowCreateRequest {
  name: string;
  description?: string;
  nodes: NodeDto[];
  edges: EdgeDto[];
}

export interface WorkflowUpdateRequest {
  name: string;
  description?: string;
  nodes: NodeDto[];
  edges: EdgeDto[];
}

export interface WorkflowResponse {
  id: string;
  name: string;
  description?: string;
  version: number;
  nodes: NodeDto[];
  edges: EdgeDto[];
  createdAt: string;
  updatedAt: string;
}

export interface ValidationResultDto {
  valid: boolean;
  errors: string[];
  warnings: string[];
}

export const NODE_TYPES = ['HTTP', 'SQL', 'SHELL', 'JAVA'] as const;
export type NodeType = (typeof NODE_TYPES)[number];
