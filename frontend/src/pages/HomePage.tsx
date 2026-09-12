import { Card, Space, Typography } from "antd";

export function HomePage() {
  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <div>
        <Typography.Title level={2}>StaffAlias</Typography.Title>
        <Typography.Paragraph>
          Track the staff lifecycle from joining through changes, transfers, and leaving while keeping tenant data isolated.
        </Typography.Paragraph>
      </div>
      <Card title="Frontend foundation ready">
        React, Refine Core, Ant Design, routing, environment configuration, and API connectivity are configured.
      </Card>
    </Space>
  );
}
