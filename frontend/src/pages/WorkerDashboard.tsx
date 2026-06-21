import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import { useStompSubscription } from '../hooks/useWebSocket';
import { runApi } from '../api/runs';
import { Server, RefreshCw, Activity } from 'lucide-react';

export default function WorkerDashboard() {
  const queryClient = useQueryClient();
  const { data: workers, isLoading, refetch } = useQuery({
    queryKey: ['workers'],
    queryFn: runApi.getWorkers,
    refetchInterval: 15000,
  });

  const onWorkerUpdate = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['workers'] });
  }, [queryClient]);

  useStompSubscription('/topic/workers', onWorkerUpdate);

  if (isLoading) return <div className="p-8 text-gray-500">Loading workers...</div>;

  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Worker Dashboard</h1>
        <button
          onClick={() => refetch()}
          className="flex items-center gap-2 px-3 py-2 text-sm bg-white border border-gray-300 rounded-lg hover:bg-gray-50"
        >
          <RefreshCw className="w-4 h-4" />
          Refresh
        </button>
      </div>

      {!workers?.length ? (
        <div className="text-center py-16 text-gray-500">
          <Server className="w-12 h-12 mx-auto mb-4 text-gray-300" />
          <p className="text-lg font-medium">No workers registered</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {workers.map((worker) => {
            const statusColor = worker.status === 'ACTIVE'
              ? 'bg-green-100 text-green-700'
              : worker.status === 'STALE'
              ? 'bg-red-100 text-red-700'
              : 'bg-gray-100 text-gray-700';

            const loadPct = worker.maxTasks > 0
              ? Math.round((worker.activeTasks / worker.maxTasks) * 100)
              : 0;

            return (
              <div key={worker.id} className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <Server className="w-5 h-5 text-gray-400" />
                    <h3 className="font-semibold text-gray-900">{worker.name}</h3>
                  </div>
                  <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${statusColor}`}>
                    {worker.status}
                  </span>
                </div>

                <div className="space-y-3">
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-gray-500">Active Tasks</span>
                    <span className="font-medium">{worker.activeTasks} / {worker.maxTasks}</span>
                  </div>

                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div
                      className={`h-2 rounded-full transition-all ${
                        loadPct > 80 ? 'bg-red-500' : loadPct > 50 ? 'bg-yellow-500' : 'bg-green-500'
                      }`}
                      style={{ width: `${loadPct}%` }}
                    />
                  </div>

                  <div className="flex items-center gap-1 text-xs text-gray-400">
                    <Activity className="w-3 h-3" />
                    Last heartbeat: {new Date(worker.lastHeartbeat).toLocaleTimeString()}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
