import { useEffect, useState } from "react";
import { Alert, Card, Spin, Typography } from "antd";
import { apiFetch } from "../api/client";

type Health = { status: string };

export function HealthPage() {
  const [health, setHealth] = useState<Health | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<Health>("/actuator/health")
      .then(setHealth)
      .catch((cause: unknown) => setError(cause instanceof Error ? cause.message : "Unable to reach backend"));
  }, []);

  return (
    <Card title="Backend connectivity">
      {!health && !error && <Spin />}
      {health && <Alert type="success" showIcon message={`Backend status: ${health.status}`} />}
      {error && <Alert type="error" showIcon message="Backend unavailable" description={error} />}
      <Typography.Paragraph style={{ marginTop: 16 }}>
        This check uses the configured API base URL and Spring Boot Actuator health endpoint.
      </Typography.Paragraph>
    </Card>
  );
}
