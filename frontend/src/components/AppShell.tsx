import { HomeOutlined, HeartOutlined } from "@ant-design/icons";
import { Layout, Menu, Typography, Grid } from "antd";
import { Link, Outlet, useLocation } from "react-router-dom";
import { env } from "../config/env";

const { Header, Sider, Content } = Layout;

export function AppShell() {
  const location = useLocation();
  const screens = Grid.useBreakpoint();
  const compact = !screens.md;

  const selectedKey = location.pathname.startsWith("/health") ? "/health" : "/";

  return (
    <Layout style={{ minHeight: "100vh" }}>
      {!compact && (
        <Sider width={220} theme="light" breakpoint="lg">
          <div style={{ padding: 20 }}>
            <Typography.Title level={4} style={{ margin: 0 }}>
              {env.appName}
            </Typography.Title>
          </div>
          <Menu
            mode="inline"
            selectedKeys={[selectedKey]}
            items={[
              { key: "/", icon: <HomeOutlined />, label: <Link to="/">Home</Link> },
              { key: "/health", icon: <HeartOutlined />, label: <Link to="/health">Backend health</Link> },
            ]}
          />
        </Sider>
      )}
      <Layout>
        <Header style={{ display: "flex", alignItems: "center", paddingInline: compact ? 16 : 24, borderBottom: "1px solid #f0f0f0" }}>
          <Typography.Text strong>{compact ? env.appName : "Staff lifecycle management"}</Typography.Text>
        </Header>
        <Content style={{ padding: compact ? 16 : 24 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
