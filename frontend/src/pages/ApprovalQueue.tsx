import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { approvalApi } from '../api/approvals';
import { APPROVAL_STATUS_COLORS, type ApprovalStatus } from '../types/agent';
import { CheckCircle, XCircle, Clock, RefreshCw } from 'lucide-react';
import { useState } from 'react';

export default function ApprovalQueue() {
  const queryClient = useQueryClient();
  const { data: approvals, isLoading, refetch } = useQuery({
    queryKey: ['approvals'],
    queryFn: approvalApi.getPending,
    refetchInterval: 10000,
  });

  const [actionNote, setActionNote] = useState('');

  const approveMut = useMutation({
    mutationFn: ({ id }: { id: string }) => approvalApi.approve(id, 'user', actionNote),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['approvals'] });
      setActionNote('');
    },
  });

  const rejectMut = useMutation({
    mutationFn: ({ id }: { id: string }) => approvalApi.reject(id, 'user', actionNote),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['approvals'] });
      setActionNote('');
    },
  });

  if (isLoading) return <div className="p-8 text-gray-500">Loading approvals...</div>;

  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Approval Queue</h1>
        <button
          onClick={() => refetch()}
          className="flex items-center gap-2 px-3 py-2 text-sm bg-white border border-gray-300 rounded-lg hover:bg-gray-50"
        >
          <RefreshCw className="w-4 h-4" /> Refresh
        </button>
      </div>

      {!approvals?.length ? (
        <div className="text-center py-16 text-gray-500">
          <CheckCircle className="w-12 h-12 mx-auto mb-3 text-green-400" />
          <p className="text-lg font-medium">No pending approvals</p>
          <p className="text-sm">All clear! Check back later.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {approvals.map((approval) => (
            <div key={approval.id} className="bg-white rounded-xl border border-gray-200 p-6 shadow-sm">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">{approval.title}</h3>
                  {approval.description && (
                    <p className="text-sm text-gray-600 mt-1">{approval.description}</p>
                  )}
                </div>
                <span className={`px-2.5 py-1 rounded-full text-xs font-medium ${APPROVAL_STATUS_COLORS[approval.status as ApprovalStatus]}`}>
                  {approval.status}
                </span>
              </div>

              <div className="flex items-center gap-4 text-xs text-gray-500 mb-4">
                <span className="flex items-center gap-1">
                  <Clock className="w-3 h-3" />
                  {new Date(approval.requestedAt).toLocaleString()}
                </span>
                <span>Run: {approval.workflowRunId.slice(0, 8)}...</span>
                <span>Task: {approval.taskRunId.slice(0, 8)}...</span>
              </div>

              <div className="flex items-center gap-3">
                <input
                  type="text"
                  placeholder="Note (optional)"
                  value={actionNote}
                  onChange={(e) => setActionNote(e.target.value)}
                  className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm"
                />
                <button
                  onClick={() => approveMut.mutate({ id: approval.id })}
                  disabled={approveMut.isPending}
                  className="flex items-center gap-1.5 px-4 py-2 bg-green-600 text-white rounded-lg text-sm hover:bg-green-700 disabled:opacity-50"
                >
                  <CheckCircle className="w-4 h-4" /> Approve
                </button>
                <button
                  onClick={() => rejectMut.mutate({ id: approval.id })}
                  disabled={rejectMut.isPending}
                  className="flex items-center gap-1.5 px-4 py-2 bg-red-600 text-white rounded-lg text-sm hover:bg-red-700 disabled:opacity-50"
                >
                  <XCircle className="w-4 h-4" /> Reject
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
