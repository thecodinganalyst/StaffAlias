export interface AuthenticatedUser {
  id: string;
  displayName: string;
  tenantCode?: string;
}

export interface AuthState {
  user: AuthenticatedUser | null;
  isAuthenticated: boolean;
}
