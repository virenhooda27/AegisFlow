import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import { useStompSubscription } from '../hooks/useWebSocket';
import { useNavigate } from 'react-router-dom';
import { runApi } from '../api/runs';
import { STATUS_COLORS, type RunStatus } from '../types/execution';
import { Play, Clock, RefreshCw } from 'lucide-react';

export default function RunList() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { data: runs, isLoading, refetch } = useQuery({
    queryKey: ['runs'],
    queryFn: runApi.getAll,
    refetchInterval: 15000,
  });

  const onRunUpdate = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['runs'] });
  }, [queryClient]);

  useStompSubscription('/topic/runs', onRunUpdate);

  if (isLoading) return <div className="p-8 text-gray-500">Loading runs...</div>;

  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Workflow Runs</h1>
        <button
          onClick={() => refetch()}
          className="flex items-center gap-2 px-3 py-2 text-sm bg-white border border-gray-300 rounded-lg hover:bg-gray-50"
        >
          <RefreshCw className="w-4 h-4" />
          Refresh
        </button>
      </div>

      {!runs?.length ? (
        <div className="text-center py-16 text-gray-500">
          <Play className="w-12 h-12 mx-auto mb-4 text-gray-300" />
          <p className="text-lg font-medium">No runs yet</p>
          <p className="text-sm">Start a workflow from the Workflows page</p>
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Workflow</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Version</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Tasks</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Started</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Duration</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {runs.map((run) => {
                const succeeded = run.taskRuns.filter((t) => t.status === 'SUCCEEDED').length;
                const total = run.taskRuns.length;
                const duration = run.startedAt
                  ? formatDuration(run.startedAt, run.completedAt)
                  : '-';

                return (
                  <tr
                    key={run.id}
                    onClick={() => navigate(`/runs/${run.id}`)}
                    className="hover:bg-gray-50 cursor-pointer"
                  >
                    <td className="px-6 py-4 text-sm font-medium text-gray-900">{run.workflowName}</td>
                    <td className="px-6 py-4 text-sm text-gray-500">v{run.workflowVersion}</td>
                    <td className="px-6 py-4">
                      <StatusBadge status={run.status} />
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {succeeded}/{total}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {run.startedAt ? new Date(run.startedAt).toLocaleString() : '-'}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500 flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5" />
                      {duration}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export function StatusBadge({ status }: { status: RunStatus }) {
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${STATUS_COLORS[status]}`}>
      {status}
    </span>
  );
}

function formatDuration(start: string, end?: string | null): string {
  const s = new Date(start).getTime();
  const e = end ? new Date(end).getTime() : Date.now();
  const ms = e - s;
  if (ms < 1000) return `${ms}ms`;
  const sec = Math.floor(ms / 1000);
  if (sec < 60) return `${sec}s`;
  const min = Math.floor(sec / 60);
  return `${min}m ${sec % 60}s`;
}
