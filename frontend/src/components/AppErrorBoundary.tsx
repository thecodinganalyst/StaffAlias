import { Component, type ErrorInfo, type ReactNode } from "react";
import { Alert, Button, Space } from "antd";

interface Props { children: ReactNode }
interface State { hasError: boolean }

export class AppErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("Unhandled application error", error, info);
  }

  render() {
    if (this.state.hasError) {
      return (
        <Space direction="vertical" style={{ padding: 24 }}>
          <Alert type="error" showIcon message="Something went wrong" description="The application encountered an unexpected error." />
          <Button onClick={() => window.location.reload()}>Reload</Button>
        </Space>
      );
    }
    return this.props.children;
  }
}
