import { useQuery } from '@tanstack/react-query';
import { workflowApi } from '../api/workflows';
import { Workflow, CheckCircle } from 'lucide-react';

export default function Dashboard() {
  const { data: workflows, isLoading } = useQuery({
    queryKey: ['workflows'],
    queryFn: workflowApi.getAll,
  });

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Dashboard</h1>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-indigo-100 rounded-lg">
              <Workflow className="w-5 h-5 text-indigo-600" />
            </div>
            <span className="text-sm font-medium text-gray-500">Workflows</span>
          </div>
          <p className="text-3xl font-bold text-gray-900">
            {isLoading ? '...' : workflows?.length ?? 0}
          </p>
        </div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-green-100 rounded-lg">
              <CheckCircle className="w-5 h-5 text-green-600" />
            </div>
            <span className="text-sm font-medium text-gray-500">Phase</span>
          </div>
          <p className="text-3xl font-bold text-gray-900">1</p>
          <p className="text-sm text-gray-500 mt-1">Workflow Engine</p>
        </div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-purple-100 rounded-lg">
              <Workflow className="w-5 h-5 text-purple-600" />
            </div>
            <span className="text-sm font-medium text-gray-500">Status</span>
          </div>
          <p className="text-lg font-bold text-green-600">Operational</p>
        </div>
      </div>
    </div>
  );
}
