import { Button, Card, Form, Grid, Input, List, Modal, Space, Table, Typography, message } from "antd";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, apiFetch } from "../api/http";

interface Tenant {
  id: string;
  code: string;
  name: string;
}

interface ProvisioningResponse {
  tenant: Tenant;
  tenantAdmin: { id: string; username: string; role: string; tenantId: string };
  activationEmailSent: boolean;
}

export function PlatformTenantsPage() {
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();
  const screens = Grid.useBreakpoint();
  const compact = !screens.md;

  async function load() {
    setLoading(true);
    try {
      setTenants(await apiFetch<Tenant[]>("/api/platform/tenants"));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    let active = true;

    async function loadInitialTenants() {
      try {
        const currentTenants = await apiFetch<Tenant[]>("/api/platform/tenants");
        if (active) setTenants(currentTenants);
      } finally {
        if (active) setLoading(false);
      }
    }

    void loadInitialTenants();
    return () => {
      active = false;
    };
  }, []);

  async function createTenant(values: { code: string; name: string; adminEmail: string }) {
    setSubmitting(true);
    try {
      const result = await apiFetch<ProvisioningResponse>("/api/platform/tenants", {
        method: "POST",
        body: JSON.stringify(values),
      });
      if (result.activationEmailSent) {
        message.success(`Tenant ${result.tenant.code} created. Activation email sent to ${result.tenantAdmin.username}.`);
      } else {
        message.warning(`Tenant ${result.tenant.code} created, but activation email was not sent. Configure Resend before inviting tenant admins.`);
      }
      setOpen(false);
      form.resetFields();
      await load();
    } catch (error) {
      message.error(error instanceof ApiError ? `Unable to create tenant: ${error.message}` : "Unable to create tenant.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Card className="tenant-card">
      <Space direction="vertical" size="large" style={{ width: "100%" }}>
        <Space className="tenant-page__heading" style={{ width: "100%", justifyContent: "space-between" }}>
          <div>
            <Typography.Title level={2} style={{ marginBottom: 0 }}>Tenants</Typography.Title>
            <Typography.Text type="secondary">Platform-wide tenant administration</Typography.Text>
          </div>
          <Button type="primary" onClick={() => setOpen(true)} block={compact}>Create tenant</Button>
        </Space>
        {compact ? (
          <List
            loading={loading}
            dataSource={tenants}
            locale={{ emptyText: "No tenants" }}
            renderItem={(tenant) => (
              <List.Item actions={[<Link key="view" to={`/platform/tenants/${tenant.id}`}>View</Link>]}>
                <List.Item.Meta title={tenant.name} description={tenant.code} />
              </List.Item>
            )}
          />
        ) : (
          <Table
            rowKey="id"
            loading={loading}
            dataSource={tenants}
            pagination={false}
            columns={[
              { title: "Code", dataIndex: "code" },
              { title: "Name", dataIndex: "name" },
              { title: "", key: "actions", render: (_, tenant: Tenant) => <Link to={`/platform/tenants/${tenant.id}`}>View</Link> },
            ]}
          />
        )}
      </Space>
      <Modal className="tenant-modal" title="Create tenant and initial admin" open={open} onCancel={() => setOpen(false)} footer={null} destroyOnHidden>
        <Form form={form} layout="vertical" onFinish={createTenant} requiredMark={false}>
          <Form.Item name="code" label="Tenant code" rules={[{ required: true }, { max: 64 }]}><Input /></Form.Item>
          <Form.Item name="name" label="Tenant name" rules={[{ required: true }, { max: 200 }]}><Input /></Form.Item>
          <Form.Item name="adminEmail" label="Tenant admin email"
            extra="An activation link will be emailed when Resend is configured."
            rules={[{ required: true }, { type: "email" }, { max: 320 }]}>
            <Input type="email" autoComplete="email" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={submitting} block>Create tenant</Button>
        </Form>
      </Modal>
    </Card>
  );
}
