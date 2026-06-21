import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import WorkflowList from './pages/WorkflowList';
import WorkflowEditor from './pages/WorkflowEditor';
import RunList from './pages/RunList';
import RunDetail from './pages/RunDetail';
import WorkerDashboard from './pages/WorkerDashboard';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="workflows" element={<WorkflowList />} />
          <Route path="workflows/new" element={<WorkflowEditor />} />
          <Route path="workflows/:id/edit" element={<WorkflowEditor />} />
          <Route path="runs" element={<RunList />} />
          <Route path="runs/:id" element={<RunDetail />} />
          <Route path="workers" element={<WorkerDashboard />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
