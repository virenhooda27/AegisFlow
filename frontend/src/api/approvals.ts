import axios from 'axios';
import type { ApprovalResponse } from '../types/agent';

const API = '/api/approvals';

export const approvalApi = {
  getPending: () =>
    axios.get<ApprovalResponse[]>(API).then(r => r.data),

  getByRun: (runId: string) =>
    axios.get<ApprovalResponse[]>(`${API}/run/${runId}`).then(r => r.data),

  getById: (id: string) =>
    axios.get<ApprovalResponse>(`${API}/${id}`).then(r => r.data),

  approve: (id: string, resolvedBy?: string, note?: string) =>
    axios.post<ApprovalResponse>(`${API}/${id}/approve`, { resolvedBy, note }).then(r => r.data),

  reject: (id: string, resolvedBy?: string, note?: string) =>
    axios.post<ApprovalResponse>(`${API}/${id}/reject`, { resolvedBy, note }).then(r => r.data),
};
