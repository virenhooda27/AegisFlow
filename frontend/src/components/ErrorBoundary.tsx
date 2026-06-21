import { Component, type ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="p-8 max-w-xl mx-auto mt-20">
          <h1 className="text-2xl font-bold text-red-600 mb-4">Something went wrong</h1>
          <pre className="bg-red-50 border border-red-200 rounded-lg p-4 text-sm text-red-800 whitespace-pre-wrap">
            {this.state.error?.message}
            {'\n\n'}
            {this.state.error?.stack}
          </pre>
          <button
            onClick={() => window.location.reload()}
            className="mt-4 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
          >
            Reload
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
