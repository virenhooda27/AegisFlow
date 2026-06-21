import axios from 'axios';
import type { AgentResponse, AgentMemory } from '../types/agent';

const API = '/api/agents';

export const agentApi = {
  invoke: (agentType: string, prompt: string, workflowRunId?: string, taskRunId?: string) =>
    axios.post<AgentResponse>(`${API}/invoke`, {
      agentType, prompt, workflowRunId, taskRunId, context: {},
    }).then(r => r.data),

  getTypes: () =>
    axios.get<string[]>(`${API}/types`).then(r => r.data),

  getMemoryByRun: (runId: string) =>
    axios.get<AgentMemory[]>(`${API}/memory/run/${runId}`).then(r => r.data),

  getMemoryByTask: (taskId: string) =>
    axios.get<AgentMemory[]>(`${API}/memory/task/${taskId}`).then(r => r.data),
};
