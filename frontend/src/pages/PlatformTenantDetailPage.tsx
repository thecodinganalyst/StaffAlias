import { Button, Card, Descriptions, Result, Spin, Typography } from "antd";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ApiError, apiFetch } from "../api/http";

interface Tenant {
  id: string;
  code: string;
  name: string;
}

export function PlatformTenantDetailPage() {
  const { id } = useParams();
  const [tenant, setTenant] = useState<Tenant>();
  const [missing, setMissing] = useState(false);

  useEffect(() => {
    if (!id) return;
    apiFetch<Tenant>(`/api/platform/tenants/${id}`)
      .then(setTenant)
      .catch((error) => {
        if (error instanceof ApiError && error.status === 404) setMissing(true);
        else throw error;
      });
  }, [id]);

  if (missing) return <Result status="404" title="Tenant not found" extra={<Button><Link to="/platform/tenants">Back to tenants</Link></Button>} />;
  if (!tenant) return <div style={{ display: "grid", minHeight: "40vh", placeItems: "center" }}><Spin /></div>;

  return (
    <Card>
      <Typography.Title level={2}>{tenant.name}</Typography.Title>
      <Descriptions bordered column={1}>
        <Descriptions.Item label="Tenant ID">{tenant.id}</Descriptions.Item>
        <Descriptions.Item label="Code">{tenant.code}</Descriptions.Item>
        <Descriptions.Item label="Name">{tenant.name}</Descriptions.Item>
      </Descriptions>
      <Button style={{ marginTop: 16 }}><Link to="/platform/tenants">Back to tenants</Link></Button>
    </Card>
  );
}
