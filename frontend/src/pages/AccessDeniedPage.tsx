import { Button, Result } from "antd";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function AccessDeniedPage() {
  const { user } = useAuth();
  const target = user?.role === "PLATFORM_ADMIN" ? "/platform/tenants" : "/tenant";
  return <Result status="403" title="Access denied" subTitle="Your account does not have access to this area." extra={<Button type="primary"><Link to={target}>Go to your workspace</Link></Button>} />;
}
