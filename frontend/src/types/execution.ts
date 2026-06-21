export interface TaskRunResponse {
  id: string;
  nodeKey: string;
  nodeName: string;
  nodeType: string;
  status: RunStatus;
  attempt: number;
  maxAttempts: number;
  startedAt?: string;
  completedAt?: string;
  errorMessage?: string;
  output?: Record<string, unknown>;
  assignedWorkerId?: string;
}

export interface WorkflowRunResponse {
  id: string;
  workflowId: string;
  workflowName: string;
  workflowVersion: number;
  status: RunStatus;
  startedAt?: string;
  completedAt?: string;
  errorMessage?: string;
  taskRuns: TaskRunResponse[];
  createdAt: string;
}

export interface WorkerNodeResponse {
  id: string;
  name: string;
  status: string;
  lastHeartbeat: string;
  activeTasks: number;
  maxTasks: number;
}

export type RunStatus =
  | 'CREATED'
  | 'READY'
  | 'RUNNING'
  | 'RETRYING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'PAUSED'
  | 'CANCELLED';

export const STATUS_COLORS: Record<RunStatus, string> = {
  CREATED: 'bg-gray-200 text-gray-700',
  READY: 'bg-blue-100 text-blue-700',
  RUNNING: 'bg-yellow-100 text-yellow-800',
  RETRYING: 'bg-orange-100 text-orange-700',
  SUCCEEDED: 'bg-green-100 text-green-700',
  FAILED: 'bg-red-100 text-red-700',
  PAUSED: 'bg-purple-100 text-purple-700',
  CANCELLED: 'bg-gray-300 text-gray-600',
};
