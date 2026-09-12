import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { App } from "./App";

describe("App", () => {
  it("renders the StaffAlias home page", () => {
    window.history.pushState({}, "", "/");
    render(<App />);

    expect(screen.getByRole("heading", { name: "StaffAlias" })).toBeInTheDocument();
    expect(screen.getByText(/track the staff lifecycle/i)).toBeInTheDocument();
  });
});
