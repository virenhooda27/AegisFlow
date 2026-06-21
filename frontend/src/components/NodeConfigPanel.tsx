import { useState } from 'react';
import { X } from 'lucide-react';
import { NODE_TYPES } from '../types/workflow';
import type { Node } from '@xyflow/react';

interface NodeConfigPanelProps {
  node: Node;
  onUpdate: (nodeId: string, data: Record<string, unknown>) => void;
  onClose: () => void;
}

export default function NodeConfigPanel({ node, onUpdate, onClose }: NodeConfigPanelProps) {
  const data = node.data as Record<string, unknown>;
  const [label, setLabel] = useState((data.label as string) || '');
  const [taskType, setTaskType] = useState((data.taskType as string) || 'HTTP');
  const [timeoutSeconds, setTimeoutSeconds] = useState<string>(
    data.timeoutSeconds != null ? String(data.timeoutSeconds) : ''
  );
  const [configJson, setConfigJson] = useState(
    JSON.stringify(data.config || {}, null, 2)
  );

  const handleApply = () => {
    let config = {};
    try {
      config = JSON.parse(configJson);
    } catch {
      // keep empty if invalid
    }
    onUpdate(node.id, {
      label,
      taskType,
      timeoutSeconds: timeoutSeconds ? parseInt(timeoutSeconds) : null,
      config,
    });
  };

  return (
    <div className="w-72 bg-white border-l border-gray-200 p-4 overflow-y-auto shrink-0">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-gray-900">Node Configuration</h3>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
          <X className="w-4 h-4" />
        </button>
      </div>

      <div className="space-y-4">
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">Node ID</label>
          <input
            value={node.id}
            disabled
            className="w-full px-3 py-1.5 text-sm bg-gray-50 border border-gray-200 rounded-lg text-gray-400"
          />
        </div>

        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">Name</label>
          <input
            value={label}
            onChange={(e) => setLabel(e.target.value)}
            className="w-full px-3 py-1.5 text-sm border border-gray-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
          />
        </div>

        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">Type</label>
          <select
            value={taskType}
            onChange={(e) => setTaskType(e.target.value)}
            className="w-full px-3 py-1.5 text-sm border border-gray-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
          >
            {NODE_TYPES.map((t) => (
              <option key={t} value={t}>{t}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">Timeout (seconds)</label>
          <input
            type="number"
            value={timeoutSeconds}
            onChange={(e) => setTimeoutSeconds(e.target.value)}
            placeholder="No timeout"
            className="w-full px-3 py-1.5 text-sm border border-gray-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
          />
        </div>

        <div>
          <label className="block text-xs font-medium text-gray-500 mb-1">Config (JSON)</label>
          <textarea
            value={configJson}
            onChange={(e) => setConfigJson(e.target.value)}
            rows={5}
            className="w-full px-3 py-1.5 text-sm border border-gray-200 rounded-lg font-mono focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
          />
        </div>

        <button
          onClick={handleApply}
          className="w-full px-3 py-2 text-sm bg-primary text-white rounded-lg hover:bg-primary-dark transition-colors font-medium"
        >
          Apply Changes
        </button>
      </div>
    </div>
  );
}
