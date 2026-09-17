import { Spin } from "antd";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";
import type { ApplicationRole } from "./types";

export function ProtectedRoute({ role }: { role?: ApplicationRole }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <div style={{ display: "grid", minHeight: "50vh", placeItems: "center" }}><Spin size="large" /></div>;
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (role && user.role !== role) {
    return <Navigate to="/access-denied" replace />;
  }

  return <Outlet />;
}
