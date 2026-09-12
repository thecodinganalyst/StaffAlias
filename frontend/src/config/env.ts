const configuredApiUrl = import.meta.env.VITE_API_URL?.trim();

export const env = {
  apiUrl: configuredApiUrl || "http://localhost:8080",
  appName: import.meta.env.VITE_APP_NAME?.trim() || "StaffAlias",
} as const;
