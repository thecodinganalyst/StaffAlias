import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";

function jsonResponse(status: number, body?: unknown) {
  return new Response(body === undefined ? undefined : JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("role-aware admin routes", () => {
  it("redirects an unauthenticated user to login", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => jsonResponse(401)));
    window.history.pushState({}, "", "/platform/tenants");
    render(<App />);
    expect(await screen.findByRole("heading", { name: /sign in to staffalias/i })).toBeInTheDocument();
  });

  it("blocks a tenant admin from a platform route", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => jsonResponse(200, {
      userId: "user-a",
      username: "tenant-admin",
      role: "TENANT_ADMIN",
      tenantId: "tenant-a",
    })));
    window.history.pushState({}, "", "/platform/tenants");
    render(<App />);
    expect(await screen.findByText(/access denied/i)).toBeInTheDocument();
    expect(screen.queryByText("Tenants")).not.toBeInTheDocument();
  });

  it("exposes platform tenant navigation on a compact viewport", async () => {
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith("/api/auth/me")) {
        return jsonResponse(200, { userId: "platform", username: "platform", role: "PLATFORM_ADMIN" });
      }
      return jsonResponse(404);
    }));
    window.history.pushState({}, "", "/");
    render(<App />);

    const navigationButton = await screen.findByRole("button", { name: /open navigation/i });
    fireEvent.click(navigationButton);
    expect(await screen.findByRole("link", { name: "Tenants" })).toHaveAttribute("href", "/platform/tenants");
  });

  it("shows tenant management to a platform admin", async () => {
    vi.stubGlobal("fetch", vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith("/api/auth/me")) {
        return jsonResponse(200, { userId: "platform", username: "platform", role: "PLATFORM_ADMIN" });
      }
      if (url.endsWith("/api/platform/tenants")) return jsonResponse(200, []);
      return jsonResponse(404);
    }));
    window.history.pushState({}, "", "/platform/tenants");
    render(<App />);
    expect(await screen.findByRole("heading", { name: "Tenants" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /create tenant/i })).toBeInTheDocument();
  });
});
