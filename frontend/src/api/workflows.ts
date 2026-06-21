import apiClient from './client';
import type { WorkflowCreateRequest, WorkflowUpdateRequest, WorkflowResponse, ValidationResultDto } from '../types/workflow';

export const workflowApi = {
  getAll: () =>
    apiClient.get<WorkflowResponse[]>('/workflows').then((res) => res.data),

  getById: (id: string) =>
    apiClient.get<WorkflowResponse>(`/workflows/${id}`).then((res) => res.data),

  create: (data: WorkflowCreateRequest) =>
    apiClient.post<WorkflowResponse>('/workflows', data).then((res) => res.data),

  update: (id: string, data: WorkflowUpdateRequest) =>
    apiClient.put<WorkflowResponse>(`/workflows/${id}`, data).then((res) => res.data),

  delete: (id: string) =>
    apiClient.delete(`/workflows/${id}`).then((res) => res.data),

  validate: (id: string) =>
    apiClient.post<ValidationResultDto>(`/workflows/${id}/validate`).then((res) => res.data),
};
