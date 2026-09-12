import { Result, Button } from "antd";
import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <Result
      status="404"
      title="Page not found"
      subTitle="The page you requested does not exist."
      extra={<Button type="primary"><Link to="/">Back home</Link></Button>}
    />
  );
}
