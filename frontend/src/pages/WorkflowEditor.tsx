import { useCallback, useState, useRef, useMemo, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  addEdge,
  useNodesState,
  useEdgesState,
  type Connection,
  type Node,
  type Edge,
  type NodeTypes,
  Handle,
  Position,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { workflowApi } from '../api/workflows';
import { Save, CheckCircle, XCircle, ArrowLeft, Plus } from 'lucide-react';
import { NODE_TYPES } from '../types/workflow';
import type { NodeDto, EdgeDto, WorkflowCreateRequest, ValidationResultDto } from '../types/workflow';
import NodeConfigPanel from '../components/NodeConfigPanel';

function TaskNode({ data }: { data: Record<string, unknown> }) {
  return (
    <div className="bg-white border-2 border-gray-300 rounded-lg shadow-sm px-4 py-3 min-w-[150px]">
      <Handle type="target" position={Position.Top} className="!bg-indigo-500 !w-3 !h-3" />
      <div className="text-xs font-semibold text-indigo-600 uppercase mb-1">{data.taskType as string}</div>
      <div className="text-sm font-medium text-gray-900">{data.label as string}</div>
      <Handle type="source" position={Position.Bottom} className="!bg-indigo-500 !w-3 !h-3" />
    </div>
  );
}

const nodeTypes: NodeTypes = { task: TaskNode };

let nodeIdCounter = 0;
function getNextNodeId() {
  nodeIdCounter += 1;
  return `node_${nodeIdCounter}`;
}

export default function WorkflowEditor() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isNew = !id || id === 'new';
  const reactFlowWrapper = useRef<HTMLDivElement>(null);

  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);
  const [workflowName, setWorkflowName] = useState('');
  const [workflowDescription, setWorkflowDescription] = useState('');
  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  const [validation, setValidation] = useState<ValidationResultDto | null>(null);
  const [saveStatus, setSaveStatus] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle');

  const { data: workflowData, isLoading } = useQuery({
    queryKey: ['workflow', id],
    queryFn: () => workflowApi.getById(id!),
    enabled: !isNew,
  });

  useEffect(() => {
    if (!workflowData) return;
    setWorkflowName(workflowData.name);
    setWorkflowDescription(workflowData.description || '');

    const flowNodes: Node[] = workflowData.nodes.map((n: NodeDto) => ({
      id: n.nodeKey,
      type: 'task',
      position: { x: n.positionX, y: n.positionY },
      data: { label: n.name, taskType: n.type, config: n.config || {}, timeoutSeconds: n.timeoutSeconds, retryPolicy: n.retryPolicy || {} },
    }));

    const flowEdges: Edge[] = workflowData.edges.map((e: EdgeDto, i: number) => ({
      id: `edge_${i}`,
      source: e.sourceNodeKey,
      target: e.targetNodeKey,
      animated: true,
      style: { stroke: '#6366f1' },
    }));

    setNodes(flowNodes);
    setEdges(flowEdges);
  }, [workflowData]);

  const saveMutation = useMutation({
    mutationFn: (payload: WorkflowCreateRequest) =>
      isNew ? workflowApi.create(payload) : workflowApi.update(id!, payload),
    onSuccess: (data) => {
      setSaveStatus('saved');
      setTimeout(() => setSaveStatus('idle'), 2000);
      if (isNew) {
        navigate(`/workflows/${data.id}/edit`, { replace: true });
      }
    },
    onError: () => setSaveStatus('error'),
  });

  const onConnect = useCallback(
    (params: Connection) =>
      setEdges((eds) => addEdge({ ...params, animated: true, style: { stroke: '#6366f1' } }, eds)),
    [setEdges]
  );

  const onNodeClick = useCallback((_: React.MouseEvent, node: Node) => {
    setSelectedNode(node);
  }, []);

  const onPaneClick = useCallback(() => {
    setSelectedNode(null);
  }, []);

  const addNode = useCallback(
    (type: string) => {
      const nodeKey = getNextNodeId();
      const newNode: Node = {
        id: nodeKey,
        type: 'task',
        position: { x: 250 + Math.random() * 200, y: 100 + Math.random() * 200 },
        data: { label: `${type} Task`, taskType: type, config: {}, timeoutSeconds: null, retryPolicy: {} },
      };
      setNodes((nds) => [...nds, newNode]);
    },
    [setNodes]
  );

  const updateNodeData = useCallback(
    (nodeId: string, data: Record<string, unknown>) => {
      setNodes((nds) =>
        nds.map((n) => (n.id === nodeId ? { ...n, data: { ...n.data, ...data } } : n))
      );
      setSelectedNode((prev) => (prev && prev.id === nodeId ? { ...prev, data: { ...prev.data, ...data } } : prev));
    },
    [setNodes]
  );

  const buildPayload = useCallback((): WorkflowCreateRequest => {
    const nodeDtos: NodeDto[] = nodes.map((n) => ({
      nodeKey: n.id,
      name: (n.data as Record<string, unknown>).label as string,
      type: (n.data as Record<string, unknown>).taskType as string,
      config: ((n.data as Record<string, unknown>).config as Record<string, unknown>) || {},
      timeoutSeconds: (n.data as Record<string, unknown>).timeoutSeconds as number | undefined,
      retryPolicy: ((n.data as Record<string, unknown>).retryPolicy as Record<string, unknown>) || {},
      positionX: n.position.x,
      positionY: n.position.y,
    }));

    const edgeDtos: EdgeDto[] = edges.map((e) => ({
      sourceNodeKey: e.source,
      targetNodeKey: e.target,
    }));

    return {
      name: workflowName || 'Untitled Workflow',
      description: workflowDescription,
      nodes: nodeDtos,
      edges: edgeDtos,
    };
  }, [nodes, edges, workflowName, workflowDescription]);

  const handleSave = useCallback(() => {
    setSaveStatus('saving');
    saveMutation.mutate(buildPayload());
  }, [buildPayload, saveMutation]);

  const handleValidate = useCallback(async () => {
    if (isNew) {
      const payload = buildPayload();
      const res = await workflowApi.create(payload);
      navigate(`/workflows/${res.id}/edit`, { replace: true });
      const result = await workflowApi.validate(res.id);
      setValidation(result);
    } else {
      const result = await workflowApi.validate(id!);
      setValidation(result);
    }
  }, [id, isNew, buildPayload, navigate]);

  const memoizedNodeTypes = useMemo(() => nodeTypes, []);

  if (!isNew && isLoading) {
    return <div className="p-8 text-gray-500">Loading workflow...</div>;
  }

  return (
    <div className="h-full flex flex-col">
      {/* Toolbar */}
      <div className="bg-white border-b border-gray-200 px-6 py-3 flex items-center gap-4 shrink-0">
        <button onClick={() => navigate('/workflows')} className="text-gray-400 hover:text-gray-600">
          <ArrowLeft className="w-5 h-5" />
        </button>
        <input
          value={workflowName}
          onChange={(e) => setWorkflowName(e.target.value)}
          placeholder="Workflow Name"
          className="text-lg font-semibold text-gray-900 bg-transparent border-none outline-none flex-1 placeholder:text-gray-300"
        />
        <input
          value={workflowDescription}
          onChange={(e) => setWorkflowDescription(e.target.value)}
          placeholder="Description..."
          className="text-sm text-gray-500 bg-transparent border-none outline-none w-64 placeholder:text-gray-300"
        />
        <button
          onClick={handleValidate}
          className="flex items-center gap-1.5 px-3 py-1.5 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
        >
          <CheckCircle className="w-4 h-4" />
          Validate
        </button>
        <button
          onClick={handleSave}
          disabled={saveStatus === 'saving'}
          className="flex items-center gap-1.5 px-4 py-1.5 text-sm bg-primary text-white rounded-lg hover:bg-primary-dark transition-colors disabled:opacity-50"
        >
          <Save className="w-4 h-4" />
          {saveStatus === 'saving' ? 'Saving...' : saveStatus === 'saved' ? 'Saved!' : 'Save'}
        </button>
      </div>

      {/* Validation result */}
      {validation && (
        <div className={`px-6 py-2 text-sm flex items-center gap-2 shrink-0 ${validation.valid ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'}`}>
          {validation.valid ? <CheckCircle className="w-4 h-4" /> : <XCircle className="w-4 h-4" />}
          {validation.valid ? 'DAG is valid' : `Validation failed: ${validation.errors.join(', ')}`}
          {validation.warnings.length > 0 && (
            <span className="text-amber-600 ml-2">Warnings: {validation.warnings.join(', ')}</span>
          )}
          <button onClick={() => setValidation(null)} className="ml-auto text-gray-400 hover:text-gray-600">
            <XCircle className="w-4 h-4" />
          </button>
        </div>
      )}

      <div className="flex-1 flex">
        {/* Node palette */}
        <div className="w-48 bg-white border-r border-gray-200 p-4 space-y-2 shrink-0">
          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">Add Node</p>
          {NODE_TYPES.map((type) => (
            <button
              key={type}
              onClick={() => addNode(type)}
              className="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-700 bg-gray-50 rounded-lg hover:bg-indigo-50 hover:text-indigo-700 transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
              {type}
            </button>
          ))}
        </div>

        {/* Canvas */}
        <div className="flex-1" ref={reactFlowWrapper}>
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodeClick={onNodeClick}
            onPaneClick={onPaneClick}
            nodeTypes={memoizedNodeTypes}
            fitView
            deleteKeyCode="Delete"
          >
            <Background />
            <Controls />
            <MiniMap
              nodeColor="#6366f1"
              maskColor="rgba(0,0,0,0.1)"
            />
          </ReactFlow>
        </div>

        {/* Config panel */}
        {selectedNode && (
          <NodeConfigPanel
            node={selectedNode}
            onUpdate={updateNodeData}
            onClose={() => setSelectedNode(null)}
          />
        )}
      </div>
    </div>
  );
}
