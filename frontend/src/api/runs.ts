import apiClient from './client';
import type { WorkflowRunResponse, TaskRunResponse, WorkerNodeResponse } from '../types/execution';

export const runApi = {
  start: (workflowId: string) =>
    apiClient.post<WorkflowRunResponse>('/runs', { workflowId }).then((res) => res.data),

  getAll: () =>
    apiClient.get<WorkflowRunResponse[]>('/runs').then((res) => res.data),

  getById: (id: string) =>
    apiClient.get<WorkflowRunResponse>(`/runs/${id}`).then((res) => res.data),

  getTasks: (id: string) =>
    apiClient.get<TaskRunResponse[]>(`/runs/${id}/tasks`).then((res) => res.data),

  pause: (id: string) =>
    apiClient.post<WorkflowRunResponse>(`/runs/${id}/pause`).then((res) => res.data),

  resume: (id: string) =>
    apiClient.post<WorkflowRunResponse>(`/runs/${id}/resume`).then((res) => res.data),

  cancel: (id: string) =>
    apiClient.post<WorkflowRunResponse>(`/runs/${id}/cancel`).then((res) => res.data),

  retry: (id: string) =>
    apiClient.post<WorkflowRunResponse>(`/runs/${id}/retry`).then((res) => res.data),

  getWorkers: () =>
    apiClient.get<WorkerNodeResponse[]>('/runs/workers').then((res) => res.data),
};
