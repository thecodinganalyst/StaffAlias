import { HomeOutlined, HeartOutlined, LogoutOutlined, MenuOutlined, TeamOutlined } from "@ant-design/icons";
import { Button, Drawer, Grid, Layout, Menu, Space, Tag, Typography } from "antd";
import { useState } from "react";
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
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

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
        <Header className={compact ? "app-header app-header--compact" : "app-header"} style={{ paddingInline: compact ? 12 : 24 }}>
          <Space className="app-header__brand" size={compact ? 4 : 8}>
            {compact && (
              <Button
                type="text"
                icon={<MenuOutlined />}
                aria-label="Open navigation"
                onClick={() => setMobileMenuOpen(true)}
              />
            )}
            <Typography.Text className="app-header__title" strong>{compact ? env.appName : "Staff lifecycle management"}</Typography.Text>
          </Space>
          <Space className="app-header__account" size={compact ? 4 : 8}>
            {!compact && <Typography.Text>{user?.username}</Typography.Text>}
            {user && !compact && <Tag>{user.role === "PLATFORM_ADMIN" ? "Platform admin" : "Tenant admin"}</Tag>}
            {user?.tenantId && !compact && <Tag>{user.tenantId}</Tag>}
            <Button aria-label="Logout" type="text" icon={<LogoutOutlined />} onClick={() => void signOut()}>{compact ? null : "Logout"}</Button>
          </Space>
        </Header>
        <Content style={{ padding: compact ? 16 : 24 }}><Outlet /></Content>
        {compact && (
          <Drawer
            title={env.appName}
            placement="left"
            open={mobileMenuOpen}
            onClose={() => setMobileMenuOpen(false)}
            styles={{ body: { padding: 0 } }}
          >
            <Menu
              mode="inline"
              selectedKeys={[selectedKey]}
              items={items}
              onClick={() => setMobileMenuOpen(false)}
            />
          </Drawer>
        )}
      </Layout>
    </Layout>
  );
}
