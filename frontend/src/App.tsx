import { Refine } from "@refinedev/core";
import { ConfigProvider } from "antd";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { dataProvider } from "./api/dataProvider";
import { AuthProvider } from "./auth/AuthContext";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { AppErrorBoundary } from "./components/AppErrorBoundary";
import { AppShell } from "./components/AppShell";
import { staffAliasTheme } from "./config/theme";
import { AccessDeniedPage } from "./pages/AccessDeniedPage";
import { HealthPage } from "./pages/HealthPage";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { PlatformTenantDetailPage } from "./pages/PlatformTenantDetailPage";
import { PlatformTenantsPage } from "./pages/PlatformTenantsPage";
import { TenantHomePage } from "./pages/TenantHomePage";

export function App() {
  return (
    <AppErrorBoundary>
      <ConfigProvider theme={staffAliasTheme}>
        <BrowserRouter>
          <Refine dataProvider={dataProvider}>
            <AuthProvider>
              <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route element={<ProtectedRoute />}>
                  <Route element={<AppShell />}>
                    <Route index element={<HomePage />} />
                    <Route path="health" element={<HealthPage />} />
                    <Route path="access-denied" element={<AccessDeniedPage />} />
                    <Route element={<ProtectedRoute role="PLATFORM_ADMIN" />}>
                      <Route path="platform/tenants" element={<PlatformTenantsPage />} />
                      <Route path="platform/tenants/:id" element={<PlatformTenantDetailPage />} />
                    </Route>
                    <Route element={<ProtectedRoute role="TENANT_ADMIN" />}>
                      <Route path="tenant" element={<TenantHomePage />} />
                    </Route>
                    <Route path="*" element={<NotFoundPage />} />
                  </Route>
                </Route>
              </Routes>
            </AuthProvider>
          </Refine>
        </BrowserRouter>
      </ConfigProvider>
    </AppErrorBoundary>
  );
}
