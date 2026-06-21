export interface AgentResponse {
  agentType: string;
  result: string;
  structuredOutput: Record<string, unknown>;
  tokensUsed: number;
  executionTimeMs: number;
}

export interface AgentMemory {
  id: string;
  workflowRunId: string | null;
  taskRunId: string | null;
  agentType: string;
  memoryType: string;
  content: string;
  metadata: Record<string, unknown>;
  createdAt: string;
}

export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface ApprovalResponse {
  id: string;
  workflowRunId: string;
  taskRunId: string;
  title: string;
  description: string;
  status: ApprovalStatus;
  requestedAt: string;
  resolvedAt: string | null;
  resolvedBy: string | null;
  resolutionNote: string | null;
}

export interface AuditLog {
  id: string;
  entityType: string;
  entityId: string;
  action: string;
  actor: string | null;
  beforeState: Record<string, unknown> | null;
  afterState: Record<string, unknown> | null;
  createdAt: string;
}

export interface ReplayState {
  runId: string;
  runStatus: string;
  tasks: Record<string, { nodeKey: string; status: string; attempt: number; errorMessage: string | null }>;
  timeline: Array<{
    eventType: string;
    nodeKey: string | null;
    fromStatus: string | null;
    toStatus: string | null;
    message: string | null;
    timestamp: string;
  }>;
}

export const APPROVAL_STATUS_COLORS: Record<ApprovalStatus, string> = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  APPROVED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
};
