import { Button, Card, Descriptions, Form, Input, Modal, Result, Space, Spin, Typography, message } from "antd";
import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError, apiFetch } from "../api/http";

interface Tenant {
  id: string;
  code: string;
  name: string;
}

export function PlatformTenantDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [editing, setEditing] = useState(false);
  const [form] = Form.useForm();
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

  async function updateTenant(values: { name: string }) {
    if (!id) return;
    const updated = await apiFetch<Tenant>(`/api/platform/tenants/${id}`, { method: "PUT", body: JSON.stringify(values) });
    setTenant(updated);
    setEditing(false);
    message.success("Tenant updated.");
  }

  async function deleteTenant() {
    if (!id) return;
    await apiFetch(`/api/platform/tenants/${id}`, { method: "DELETE" });
    message.success("Tenant deleted.");
    navigate("/platform/tenants", { replace: true });
  }

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
      <Space wrap style={{ marginTop: 16 }}>
        <Button><Link to="/platform/tenants">Back to tenants</Link></Button>
        <Button onClick={() => { form.setFieldsValue({ name: tenant.name }); setEditing(true); }}>Edit tenant</Button>
        <Button danger onClick={() => void deleteTenant()}>Delete tenant</Button>
      </Space>
      <Modal title="Edit tenant" open={editing} onCancel={() => setEditing(false)} footer={null} destroyOnHidden>
        <Form form={form} layout="vertical" onFinish={updateTenant}>
          <Form.Item name="name" label="Tenant name" rules={[{ required: true }, { max: 200 }]}><Input /></Form.Item>
          <Button type="primary" htmlType="submit" block>Save changes</Button>
        </Form>
      </Modal>
    </Card>
  );
}
