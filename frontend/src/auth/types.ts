export type ApplicationRole = "PLATFORM_ADMIN" | "TENANT_ADMIN";

export interface AuthenticatedUser {
  userId: string;
  username: string;
  role: ApplicationRole;
  tenantId?: string;
}

export interface AuthState {
  user: AuthenticatedUser | null;
  loading: boolean;
}
