const apiUrl = import.meta.env.VITE_API_URL?.trim();

if (!apiUrl) {
  throw new Error("VITE_API_URL must be configured for the frontend environment");
}

export const env = {
  apiUrl,
  appName: import.meta.env.VITE_APP_NAME?.trim() || "StaffAlias",
} as const;
