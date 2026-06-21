import { useQuery, useMutation } from '@tanstack/react-query';
import { agentApi } from '../api/agents';
import type { AgentResponse } from '../types/agent';
import { Bot, Send, Zap } from 'lucide-react';
import { useState } from 'react';

export default function AgentConsole() {
  const [agentType, setAgentType] = useState('RECOVERY');
  const [prompt, setPrompt] = useState('');
  const [results, setResults] = useState<AgentResponse[]>([]);

  const { data: agentTypes } = useQuery({
    queryKey: ['agent-types'],
    queryFn: agentApi.getTypes,
  });

  const invokeMut = useMutation({
    mutationFn: () => agentApi.invoke(agentType, prompt),
    onSuccess: (data) => {
      setResults((prev) => [data, ...prev]);
      setPrompt('');
    },
  });

  return (
    <div className="p-8">
      <div className="flex items-center gap-3 mb-6">
        <Bot className="w-7 h-7 text-indigo-600" />
        <h1 className="text-2xl font-bold text-gray-900">Agent Console</h1>
      </div>

      <div className="bg-white rounded-xl border border-gray-200 p-6 shadow-sm mb-6">
        <div className="flex items-end gap-4">
          <div className="flex-shrink-0">
            <label className="block text-sm font-medium text-gray-700 mb-1">Agent</label>
            <select
              value={agentType}
              onChange={(e) => setAgentType(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white"
            >
              {agentTypes?.map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
          </div>
          <div className="flex-1">
            <label className="block text-sm font-medium text-gray-700 mb-1">Prompt</label>
            <input
              type="text"
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && !invokeMut.isPending && invokeMut.mutate()}
              placeholder="Describe the task failure, ask for a summary, or request a plan..."
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
            />
          </div>
          <button
            onClick={() => invokeMut.mutate()}
            disabled={invokeMut.isPending || !prompt.trim()}
            className="flex items-center gap-1.5 px-4 py-2 bg-indigo-600 text-white rounded-lg text-sm hover:bg-indigo-700 disabled:opacity-50"
          >
            <Send className="w-4 h-4" /> {invokeMut.isPending ? 'Running...' : 'Invoke'}
          </button>
        </div>
      </div>

      <div className="space-y-4">
        {results.map((r, i) => (
          <div key={i} className="bg-white rounded-xl border border-gray-200 p-6 shadow-sm">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Zap className="w-4 h-4 text-amber-500" />
                <span className="font-semibold text-gray-900">{r.agentType}</span>
              </div>
              <div className="flex items-center gap-3 text-xs text-gray-500">
                <span>{r.tokensUsed} tokens</span>
                <span>{r.executionTimeMs}ms</span>
              </div>
            </div>
            <pre className="bg-gray-50 rounded-lg p-4 text-sm text-gray-800 overflow-x-auto whitespace-pre-wrap">
              {(() => {
                try { return JSON.stringify(JSON.parse(r.result), null, 2); }
                catch { return r.result; }
              })()}
            </pre>
          </div>
        ))}

        {results.length === 0 && (
          <div className="text-center py-12 text-gray-500">
            <Bot className="w-10 h-10 mx-auto mb-3 text-gray-300" />
            <p>Invoke an agent to see results here</p>
          </div>
        )}
      </div>
    </div>
  );
}
