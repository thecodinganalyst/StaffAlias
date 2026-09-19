import { Alert, Button, Card, Form, Input, Typography } from "antd";
import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { apiFetch } from "../api/http";

export function ActivationPage() {
  const [params] = useSearchParams();
  const token = params.get("token") ?? "";
  const [done, setDone] = useState(false);
  const [error, setError] = useState("");

  async function activate(values: { password: string }) {
    setError("");
    try {
      await apiFetch<void>("/api/auth/activation", {
        method: "POST",
        body: JSON.stringify({ token, password: values.password }),
      });
      setDone(true);
    } catch {
      setError("This activation link is invalid or expired. Contact your platform administrator for a new invitation.");
    }
  }

  return <Card style={{ maxWidth: 480, margin: "64px auto" }}>
    <Typography.Title level={2}>Activate StaffAlias account</Typography.Title>
    {done ? <Alert type="success" showIcon message="Account activated"
      description={<span>Your password has been set. <Link to="/login">Sign in to StaffAlias</Link>.</span>} /> :
      <>
        {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} />}
        {!token && <Alert type="error" showIcon message="Activation token is missing." style={{ marginBottom: 16 }} />}
        <Form layout="vertical" onFinish={activate}>
          <Form.Item name="password" label="New password"
            rules={[{ required: true }, { min: 12, message: "Use at least 12 characters." }]}>
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" disabled={!token} block>Set password</Button>
        </Form>
      </>}
  </Card>;
}
