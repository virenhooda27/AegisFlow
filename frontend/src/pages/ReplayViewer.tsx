import { useQuery } from '@tanstack/react-query';
import { useParams, useNavigate } from 'react-router-dom';
import { replayApi } from '../api/audit';
import { STATUS_COLORS, type RunStatus } from '../types/execution';
import { ArrowLeft, History, Clock } from 'lucide-react';

export default function ReplayViewer() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: replay, isLoading } = useQuery({
    queryKey: ['replay', id],
    queryFn: () => replayApi.replay(id!),
    enabled: !!id,
  });

  if (isLoading) return <div className="p-8 text-gray-500">Replaying events...</div>;
  if (!replay) return <div className="p-8 text-gray-500">No replay data</div>;

  return (
    <div className="p-8">
      <button
        onClick={() => navigate(-1)}
        className="flex items-center gap-1 text-sm text-gray-600 hover:text-gray-900 mb-4"
      >
        <ArrowLeft className="w-4 h-4" /> Back
      </button>

      <div className="flex items-center gap-3 mb-6">
        <History className="w-6 h-6 text-indigo-600" />
        <h1 className="text-2xl font-bold text-gray-900">Event Replay</h1>
        <span className={`px-2.5 py-1 rounded-full text-xs font-medium ${STATUS_COLORS[replay.runStatus as RunStatus] || 'bg-gray-100 text-gray-800'}`}>
          {replay.runStatus}
        </span>
      </div>

      {/* Task states */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-8">
        {Object.values(replay.tasks).map((task) => (
          <div key={task.nodeKey} className="bg-white rounded-lg border border-gray-200 p-4">
            <div className="text-sm font-medium text-gray-900 mb-1">{task.nodeKey}</div>
            <span className={`px-2 py-0.5 rounded text-xs font-medium ${STATUS_COLORS[task.status as RunStatus] || 'bg-gray-100 text-gray-800'}`}>
              {task.status}
            </span>
            <div className="text-xs text-gray-500 mt-1">Attempt: {task.attempt}</div>
            {task.errorMessage && (
              <div className="text-xs text-red-600 mt-1 truncate">{task.errorMessage}</div>
            )}
          </div>
        ))}
      </div>

      {/* Timeline */}
      <h2 className="text-lg font-semibold text-gray-900 mb-4">Event Timeline</h2>
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="divide-y divide-gray-100">
          {replay.timeline.map((event, i) => (
            <div key={i} className="flex items-start gap-4 px-5 py-3 hover:bg-gray-50">
              <div className="flex-shrink-0 mt-0.5">
                <Clock className="w-4 h-4 text-gray-400" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-0.5">
                  <span className="text-sm font-medium text-gray-900">{event.eventType}</span>
                  {event.nodeKey && (
                    <span className="text-xs text-gray-500 bg-gray-100 px-1.5 py-0.5 rounded">{event.nodeKey}</span>
                  )}
                </div>
                <div className="flex items-center gap-2 text-xs text-gray-500">
                  {event.fromStatus && event.toStatus && (
                    <span>
                      <span className={`px-1.5 py-0.5 rounded ${STATUS_COLORS[event.fromStatus as RunStatus] || ''}`}>{event.fromStatus}</span>
                      {' → '}
                      <span className={`px-1.5 py-0.5 rounded ${STATUS_COLORS[event.toStatus as RunStatus] || ''}`}>{event.toStatus}</span>
                    </span>
                  )}
                  {event.message && <span className="truncate">— {event.message}</span>}
                </div>
              </div>
              <div className="text-xs text-gray-400 flex-shrink-0">
                {new Date(event.timestamp).toLocaleTimeString()}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
