import axios from 'axios';
import type { AuditLog, ReplayState } from '../types/agent';

export const auditApi = {
  getRecent: () =>
    axios.get<AuditLog[]>('/api/audit').then(r => r.data),

  getByEntity: (entityType: string, entityId: string) =>
    axios.get<AuditLog[]>(`/api/audit/${entityType}/${entityId}`).then(r => r.data),
};

export const replayApi = {
  replay: (runId: string) =>
    axios.get<ReplayState>(`/api/replay/${runId}`).then(r => r.data),
};
