import { Alert, Button, Card, Form, Input, Typography } from "antd";
import { useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { ApiError } from "../api/http";
import { useAuth } from "../auth/AuthContext";

export function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState<string>();
  const [submitting, setSubmitting] = useState(false);

  if (user) {
    return <Navigate to={user.role === "PLATFORM_ADMIN" ? "/platform/tenants" : "/tenant"} replace />;
  }

  async function submit(values: { username: string; password: string }) {
    setSubmitting(true);
    setError(undefined);
    try {
      const authenticated = await login(values.username, values.password);
      const requested = (location.state as { from?: string } | null)?.from;
      const defaultPath = authenticated.role === "PLATFORM_ADMIN" ? "/platform/tenants" : "/tenant";
      navigate(requested && requested !== "/login" ? requested : defaultPath, { replace: true });
    } catch (cause) {
      setError(cause instanceof ApiError && cause.status === 401 ? "Invalid username or password." : "Unable to sign in.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div style={{ minHeight: "100vh", display: "grid", placeItems: "center", padding: 24 }}>
      <Card style={{ width: "100%", maxWidth: 420 }}>
        <Typography.Title level={2}>Sign in to StaffAlias</Typography.Title>
        <Typography.Paragraph type="secondary">Use your platform or tenant administrator account.</Typography.Paragraph>
        {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} />}
        <Form layout="vertical" onFinish={submit} requiredMark={false}>
          <Form.Item name="username" label="Username" rules={[{ required: true }]}>
            <Input autoComplete="username" />
          </Form.Item>
          <Form.Item name="password" label="Password" rules={[{ required: true }]}>
            <Input.Password autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={submitting} block>Sign in</Button>
        </Form>
      </Card>
    </div>
  );
}
