import { Refine } from "@refinedev/core";
import { ConfigProvider } from "antd";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { dataProvider } from "./api/dataProvider";
import { AppErrorBoundary } from "./components/AppErrorBoundary";
import { AppShell } from "./components/AppShell";
import { staffAliasTheme } from "./config/theme";
import { HealthPage } from "./pages/HealthPage";
import { HomePage } from "./pages/HomePage";
import { NotFoundPage } from "./pages/NotFoundPage";

export function App() {
  return (
    <AppErrorBoundary>
      <ConfigProvider theme={staffAliasTheme}>
        <BrowserRouter>
          <Refine dataProvider={dataProvider}>
            <Routes>
              <Route element={<AppShell />}>
                <Route index element={<HomePage />} />
                <Route path="health" element={<HealthPage />} />
                <Route path="*" element={<NotFoundPage />} />
              </Route>
            </Routes>
          </Refine>
        </BrowserRouter>
      </ConfigProvider>
    </AppErrorBoundary>
  );
}
