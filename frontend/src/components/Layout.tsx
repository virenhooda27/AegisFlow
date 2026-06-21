import { Outlet, NavLink } from 'react-router-dom';
import { Workflow, LayoutDashboard, Play, Server, Wifi, WifiOff } from 'lucide-react';
import { useConnectionStatus } from '../hooks/useWebSocket';

export default function Layout() {
  const wsConnected = useConnectionStatus();

  return (
    <div className="flex h-screen overflow-hidden">
      <aside className="w-64 bg-sidebar text-white flex flex-col shrink-0">
        <div className="p-6 border-b border-sidebar-hover">
          <h1 className="text-xl font-bold tracking-tight flex items-center gap-2">
            <Workflow className="w-6 h-6 text-indigo-400" />
            AegisFlow
          </h1>
        </div>
        <nav className="flex-1 p-4 space-y-1">
          <NavLink
            to="/"
            end
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                isActive ? 'bg-sidebar-hover text-white' : 'text-indigo-200 hover:bg-sidebar-hover hover:text-white'
              }`
            }
          >
            <LayoutDashboard className="w-4 h-4" />
            Dashboard
          </NavLink>
          <NavLink
            to="/workflows"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                isActive ? 'bg-sidebar-hover text-white' : 'text-indigo-200 hover:bg-sidebar-hover hover:text-white'
              }`
            }
          >
            <Workflow className="w-4 h-4" />
            Workflows
          </NavLink>
          <NavLink
            to="/runs"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                isActive ? 'bg-sidebar-hover text-white' : 'text-indigo-200 hover:bg-sidebar-hover hover:text-white'
              }`
            }
          >
            <Play className="w-4 h-4" />
            Runs
          </NavLink>
          <NavLink
            to="/workers"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                isActive ? 'bg-sidebar-hover text-white' : 'text-indigo-200 hover:bg-sidebar-hover hover:text-white'
              }`
            }
          >
            <Server className="w-4 h-4" />
            Workers
          </NavLink>
        </nav>
        <div className="p-4 border-t border-sidebar-hover text-xs text-indigo-300 space-y-1">
          <div className="flex items-center gap-1.5">
            {wsConnected ? (
              <><Wifi className="w-3 h-3 text-green-400" /> <span className="text-green-300">Live</span></>
            ) : (
              <><WifiOff className="w-3 h-3 text-red-400" /> <span className="text-red-300">Polling</span></>
            )}
          </div>
          <div>v0.2.5 &middot; Phase 2.5</div>
        </div>
      </aside>
      <main className="flex-1 overflow-auto bg-gray-50">
        <Outlet />
      </main>
    </div>
  );
}
