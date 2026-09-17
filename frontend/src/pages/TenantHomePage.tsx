import { Card, Descriptions, Typography } from "antd";
import { useAuth } from "../auth/AuthContext";

export function TenantHomePage() {
  const { user } = useAuth();
  return (
    <Card>
      <Typography.Title level={2}>Tenant administration</Typography.Title>
      <Typography.Paragraph type="secondary">This workspace is scoped to your authenticated tenant. Platform administration is not available here.</Typography.Paragraph>
      <Descriptions bordered column={1}>
        <Descriptions.Item label="Username">{user?.username}</Descriptions.Item>
        <Descriptions.Item label="Role">{user?.role}</Descriptions.Item>
        <Descriptions.Item label="Tenant ID">{user?.tenantId}</Descriptions.Item>
      </Descriptions>
    </Card>
  );
}
