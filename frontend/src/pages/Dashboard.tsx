import { useQuery } from '@tanstack/react-query';
import { workflowApi } from '../api/workflows';
import { runApi } from '../api/runs';
import { Workflow, CheckCircle, Play, Server, ShieldCheck, Bot } from 'lucide-react';
import { approvalApi } from '../api/approvals';
import { agentApi } from '../api/agents';

export default function Dashboard() {
  const { data: workflows, isLoading: wfLoading } = useQuery({
    queryKey: ['workflows'],
    queryFn: workflowApi.getAll,
  });

  const { data: runs, isLoading: runsLoading } = useQuery({
    queryKey: ['runs'],
    queryFn: runApi.getAll,
    refetchInterval: 5000,
  });

  const { data: workers, isLoading: wkLoading } = useQuery({
    queryKey: ['workers'],
    queryFn: runApi.getWorkers,
    refetchInterval: 5000,
  });

  const { data: pendingApprovals } = useQuery({
    queryKey: ['approvals'],
    queryFn: approvalApi.getPending,
  });

  const { data: agentTypes } = useQuery({
    queryKey: ['agent-types'],
    queryFn: agentApi.getTypes,
  });

  const activeRuns = runs?.filter((r) => r.status === 'RUNNING' || r.status === 'CREATED').length ?? 0;
  const succeededRuns = runs?.filter((r) => r.status === 'SUCCEEDED').length ?? 0;
  const failedRuns = runs?.filter((r) => r.status === 'FAILED').length ?? 0;

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Dashboard</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-indigo-100 rounded-lg">
              <Workflow className="w-5 h-5 text-indigo-600" />
            </div>
            <span className="text-sm font-medium text-gray-500">Workflows</span>
          </div>
          <p className="text-3xl font-bold text-gray-900">
            {wfLoading ? '...' : workflows?.length ?? 0}
          </p>
        </div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-yellow-100 rounded-lg">
              <Play className="w-5 h-5 text-yellow-600" />
            </div>
            <span className="text-sm font-medium text-gray-500">Active Runs</span>
          </div>
          <p className="text-3xl font-bold text-gray-900">
            {runsLoading ? '...' : activeRuns}
          </p>
          <p className="text-sm text-gray-500 mt-1">
            {succeededRuns} succeeded &middot; {failedRuns} failed
          </p>
        </div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-blue-100 rounded-lg">
              <Server className="w-5 h-5 text-blue-600" />
            </div>
            <span className="text-sm font-medium text-gray-500">Workers</span>
          </div>
          <p className="text-3xl font-bold text-gray-900">
            {wkLoading ? '...' : workers?.length ?? 0}
          </p>
          <p className="text-sm text-gray-500 mt-1">
            {workers?.filter((w) => w.status === 'ACTIVE').length ?? 0} active
          </p>
        </div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-amber-100 rounded-lg">
              <ShieldCheck className="w-5 h-5 text-amber-600" />
            </div>
            <span className="text-sm font-medium text-gray-500">Pending Approvals</span>
          </div>
          <p className="text-3xl font-bold text-gray-900">{pendingApprovals?.length ?? 0}</p>
        </div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-purple-100 rounded-lg">
              <Bot className="w-5 h-5 text-purple-600" />
            </div>
            <span className="text-sm font-medium text-gray-500">AI Agents</span>
          </div>
          <p className="text-3xl font-bold text-gray-900">{agentTypes?.length ?? 0}</p>
          <p className="text-sm text-gray-500 mt-1">{agentTypes?.join(', ')}</p>
        </div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-green-100 rounded-lg">
              <CheckCircle className="w-5 h-5 text-green-600" />
            </div>
            <span className="text-sm font-medium text-gray-500">Phase</span>
          </div>
          <p className="text-3xl font-bold text-gray-900">3</p>
          <p className="text-sm text-gray-500 mt-1">AI Agents + Governance</p>
        </div>
      </div>
    </div>
  );
}
