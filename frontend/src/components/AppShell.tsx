import { HomeOutlined, HeartOutlined, LogoutOutlined, TeamOutlined } from "@ant-design/icons";
import { Button, Grid, Layout, Menu, Space, Tag, Typography } from "antd";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { env } from "../config/env";

const { Header, Sider, Content } = Layout;

export function AppShell() {
  const location = useLocation();
  const navigate = useNavigate();
  const screens = Grid.useBreakpoint();
  const compact = !screens.md;
  const { user, logout } = useAuth();

  const selectedKey = location.pathname.startsWith("/platform/tenants")
    ? "/platform/tenants"
    : location.pathname.startsWith("/tenant")
      ? "/tenant"
      : location.pathname.startsWith("/health")
        ? "/health"
        : "/";

  const items = [
    { key: "/", icon: <HomeOutlined />, label: <Link to="/">Home</Link> },
    ...(user?.role === "PLATFORM_ADMIN"
      ? [{ key: "/platform/tenants", icon: <TeamOutlined />, label: <Link to="/platform/tenants">Tenants</Link> }]
      : []),
    ...(user?.role === "TENANT_ADMIN"
      ? [{ key: "/tenant", icon: <TeamOutlined />, label: <Link to="/tenant">Tenant workspace</Link> }]
      : []),
    { key: "/health", icon: <HeartOutlined />, label: <Link to="/health">Backend health</Link> },
  ];

  async function signOut() {
    await logout();
    navigate("/login", { replace: true });
  }

  return (
    <Layout style={{ minHeight: "100vh" }}>
      {!compact && (
        <Sider width={220} theme="light" breakpoint="lg">
          <div style={{ padding: 20 }}>
            <Typography.Title level={4} style={{ margin: 0 }}>{env.appName}</Typography.Title>
          </div>
          <Menu mode="inline" selectedKeys={[selectedKey]} items={items} />
        </Sider>
      )}
      <Layout>
        <Header style={{ display: "flex", alignItems: "center", justifyContent: "space-between", paddingInline: compact ? 16 : 24, borderBottom: "1px solid #f0f0f0" }}>
          <Typography.Text strong>{compact ? env.appName : "Staff lifecycle management"}</Typography.Text>
          <Space>
            <Typography.Text>{user?.username}</Typography.Text>
            {user && <Tag>{user.role === "PLATFORM_ADMIN" ? "Platform admin" : "Tenant admin"}</Tag>}
            {user?.tenantId && <Tag>{user.tenantId}</Tag>}
            <Button type="text" icon={<LogoutOutlined />} onClick={() => void signOut()}>Logout</Button>
          </Space>
        </Header>
        <Content style={{ padding: compact ? 16 : 24 }}><Outlet /></Content>
      </Layout>
    </Layout>
  );
}
