import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import { runApi } from '../api/runs';
import { STATUS_COLORS, type RunStatus, type TaskRunResponse } from '../types/execution';
import { StatusBadge } from './RunList';
import { ArrowLeft, Pause, Play, XCircle, RotateCcw } from 'lucide-react';
import { useState, useCallback } from 'react';
import { useStompSubscription } from '../hooks/useWebSocket';
import type { WorkflowRunResponse } from '../types/execution';

const NODE_STATUS_COLORS: Record<RunStatus, string> = {
  CREATED: '#e5e7eb',
  READY: '#bfdbfe',
  RUNNING: '#fef08a',
  RETRYING: '#fed7aa',
  SUCCEEDED: '#bbf7d0',
  FAILED: '#fecaca',
  PAUSED: '#e9d5ff',
  CANCELLED: '#d1d5db',
};

export default function RunDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [selectedTask, setSelectedTask] = useState<TaskRunResponse | null>(null);

  const { data: run, isLoading } = useQuery({
    queryKey: ['run', id],
    queryFn: () => runApi.getById(id!),
    refetchInterval: 10000,
    enabled: !!id,
  });

  const onRunUpdate = useCallback((data: WorkflowRunResponse) => {
    queryClient.setQueryData(['run', id], data);
  }, [queryClient, id]);

  useStompSubscription(`/topic/runs/${id}`, onRunUpdate, !!id);

  const pauseMut = useMutation({
    mutationFn: () => runApi.pause(id!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['run', id] }),
  });
  const resumeMut = useMutation({
    mutationFn: () => runApi.resume(id!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['run', id] }),
  });
  const cancelMut = useMutation({
    mutationFn: () => runApi.cancel(id!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['run', id] }),
  });
  const retryMut = useMutation({
    mutationFn: () => runApi.retry(id!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['run', id] }),
  });

  if (isLoading || !run) return <div className="p-8 text-gray-500">Loading...</div>;

  const succeeded = run.taskRuns.filter((t) => t.status === 'SUCCEEDED').length;
  const total = run.taskRuns.length;

  return (
    <div className="p-8">
      <button
        onClick={() => navigate('/runs')}
        className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-800 mb-4"
      >
        <ArrowLeft className="w-4 h-4" /> Back to Runs
      </button>

      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            {run.workflowName} <span className="text-gray-400 text-lg">v{run.workflowVersion}</span>
          </h1>
          <p className="text-sm text-gray-500 mt-1">Run {run.id.slice(0, 8)}</p>
        </div>
        <div className="flex items-center gap-2">
          <StatusBadge status={run.status} />
          <span className="text-sm text-gray-500">{succeeded}/{total} tasks completed</span>
        </div>
      </div>

      {/* Action buttons */}
      <div className="flex gap-2 mb-6">
        {run.status === 'RUNNING' && (
          <button onClick={() => pauseMut.mutate()} className="flex items-center gap-1 px-3 py-1.5 text-sm bg-purple-50 text-purple-700 rounded-lg border border-purple-200 hover:bg-purple-100">
            <Pause className="w-3.5 h-3.5" /> Pause
          </button>
        )}
        {run.status === 'PAUSED' && (
          <button onClick={() => resumeMut.mutate()} className="flex items-center gap-1 px-3 py-1.5 text-sm bg-green-50 text-green-700 rounded-lg border border-green-200 hover:bg-green-100">
            <Play className="w-3.5 h-3.5" /> Resume
          </button>
        )}
        {!['SUCCEEDED', 'FAILED', 'CANCELLED'].includes(run.status) && (
          <button onClick={() => cancelMut.mutate()} className="flex items-center gap-1 px-3 py-1.5 text-sm bg-red-50 text-red-700 rounded-lg border border-red-200 hover:bg-red-100">
            <XCircle className="w-3.5 h-3.5" /> Cancel
          </button>
        )}
        {run.status === 'FAILED' && (
          <button onClick={() => retryMut.mutate()} className="flex items-center gap-1 px-3 py-1.5 text-sm bg-yellow-50 text-yellow-700 rounded-lg border border-yellow-200 hover:bg-yellow-100">
            <RotateCcw className="w-3.5 h-3.5" /> Retry Failed
          </button>
        )}
      </div>

      <div className="grid grid-cols-3 gap-6">
        {/* Task list */}
        <div className="col-span-2 bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold">Tasks</h2>
          </div>
          <div className="divide-y divide-gray-100">
            {run.taskRuns.map((task) => (
              <div
                key={task.id}
                onClick={() => setSelectedTask(task)}
                className={`px-6 py-4 flex items-center justify-between cursor-pointer hover:bg-gray-50 ${
                  selectedTask?.id === task.id ? 'bg-indigo-50' : ''
                }`}
              >
                <div className="flex items-center gap-3">
                  <div
                    className="w-3 h-3 rounded-full"
                    style={{ backgroundColor: NODE_STATUS_COLORS[task.status] }}
                  />
                  <div>
                    <p className="text-sm font-medium text-gray-900">{task.nodeName}</p>
                    <p className="text-xs text-gray-500">{task.nodeKey} &middot; {task.nodeType}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-xs text-gray-400">
                    Attempt {task.attempt}/{task.maxAttempts}
                  </span>
                  <StatusBadge status={task.status} />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Task detail panel */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold">Task Detail</h2>
          </div>
          {selectedTask ? (
            <div className="p-6 space-y-4 text-sm">
              <div>
                <label className="text-xs font-medium text-gray-500 uppercase">Node</label>
                <p className="text-gray-900">{selectedTask.nodeName} ({selectedTask.nodeKey})</p>
              </div>
              <div>
                <label className="text-xs font-medium text-gray-500 uppercase">Type</label>
                <p className="text-gray-900">{selectedTask.nodeType}</p>
              </div>
              <div>
                <label className="text-xs font-medium text-gray-500 uppercase">Status</label>
                <div className="mt-1"><StatusBadge status={selectedTask.status} /></div>
              </div>
              <div>
                <label className="text-xs font-medium text-gray-500 uppercase">Attempts</label>
                <p className="text-gray-900">{selectedTask.attempt} / {selectedTask.maxAttempts}</p>
              </div>
              {selectedTask.startedAt && (
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase">Started</label>
                  <p className="text-gray-900">{new Date(selectedTask.startedAt).toLocaleString()}</p>
                </div>
              )}
              {selectedTask.completedAt && (
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase">Completed</label>
                  <p className="text-gray-900">{new Date(selectedTask.completedAt).toLocaleString()}</p>
                </div>
              )}
              {selectedTask.errorMessage && (
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase">Error</label>
                  <pre className="mt-1 p-3 bg-red-50 text-red-700 rounded-lg text-xs overflow-auto whitespace-pre-wrap">
                    {selectedTask.errorMessage}
                  </pre>
                </div>
              )}
              {selectedTask.output && Object.keys(selectedTask.output).length > 0 && (
                <div>
                  <label className="text-xs font-medium text-gray-500 uppercase">Output</label>
                  <pre className="mt-1 p-3 bg-gray-50 text-gray-800 rounded-lg text-xs overflow-auto whitespace-pre-wrap">
                    {JSON.stringify(selectedTask.output, null, 2)}
                  </pre>
                </div>
              )}
            </div>
          ) : (
            <div className="p-6 text-sm text-gray-400 text-center">
              Select a task to view details
            </div>
          )}
        </div>
      </div>

      {/* Error message */}
      {run.errorMessage && (
        <div className="mt-6 p-4 bg-red-50 border border-red-200 rounded-xl">
          <p className="text-sm font-medium text-red-700">Run Error</p>
          <p className="text-sm text-red-600 mt-1">{run.errorMessage}</p>
        </div>
      )}
    </div>
  );
}
