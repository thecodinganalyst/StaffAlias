import { expect, test } from "@playwright/test";

test("platform admin login rejects invalid credentials", async ({ page }) => {
  await page.route("**/api/auth/me", route => route.fulfill({ status: 401 }));
  await page.route("**/api/auth/login", route => route.fulfill({ status: 401, contentType: "application/json", body: "{}" }));
  await page.goto("/login");
  await page.getByLabel("Username").fill("platform");
  await page.getByLabel("Password").fill("wrong");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page.getByText("Invalid username or password.")).toBeVisible();
});

test("platform admin can login and complete tenant CRUD", async ({ page }) => {
  const id = "11111111-1111-4111-8111-111111111111";
  const code = `E2E${Date.now()}`;
  let tenant: { id: string; code: string; name: string } | undefined;
  await page.route("**/api/auth/**", async route => {
    if (route.request().url().endsWith("/api/auth/logout")) return route.fulfill({ status: 204 });
    return route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ userId: "platform", username: "platform", role: "PLATFORM_ADMIN" }) });
  });
  await page.route("**/api/platform/tenants**", async route => {
    const request = route.request(); const detail = new URL(request.url()).pathname.endsWith(`/${id}`);
    if (request.method() === "GET" && detail) return route.fulfill({ status: tenant ? 200 : 404, contentType: "application/json", body: JSON.stringify(tenant ?? {}) });
    if (request.method() === "GET") return route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(tenant ? [tenant] : []) });
    if (request.method() === "POST") { const body = request.postDataJSON(); tenant = { id, code: body.code, name: body.name }; return route.fulfill({ status: 201, contentType: "application/json", body: JSON.stringify({ tenant, tenantAdmin: { id: "admin", username: body.adminEmail, role: "TENANT_ADMIN", tenantId: id }, activationEmailSent: false }) }); }
    if (request.method() === "PUT") { const body = request.postDataJSON(); tenant = { ...tenant!, name: body.name }; return route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(tenant) }); }
    if (request.method() === "DELETE") { tenant = undefined; return route.fulfill({ status: 204 }); }
    return route.abort();
  });
  await page.goto("/login");
  await page.getByLabel("Username").fill("platform"); await page.getByLabel("Password").fill("secret");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page.getByRole("heading", { name: "Tenants" })).toBeVisible();
  await page.getByRole("button", { name: "Create tenant" }).click();
  await page.getByLabel("Tenant code").fill(code); await page.getByLabel("Tenant name").fill("E2E Tenant");
  await page.getByLabel("Tenant admin email").fill(`${code.toLowerCase()}@example.test`);
  await page.getByRole("button", { name: "Create tenant", exact: true }).last().click();
  await expect(page.getByText("E2E Tenant")).toBeVisible();
  await page.getByRole("link", { name: "View" }).click(); await expect(page.getByText(code)).toBeVisible();
  await page.getByRole("button", { name: "Edit tenant" }).click(); await page.getByLabel("Tenant name").fill("E2E Tenant Updated");
  await page.getByRole("button", { name: "Save changes" }).click(); await expect(page.getByRole("heading", { name: "E2E Tenant Updated" })).toBeVisible();
  await page.getByRole("button", { name: "Delete tenant" }).click(); await expect(page.getByRole("heading", { name: "Tenants" })).toBeVisible();
  await expect(page.getByText("E2E Tenant Updated")).not.toBeVisible();
});
