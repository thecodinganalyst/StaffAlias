export interface TenantSelection {
  tenantId: string;
  tenantCode: string;
  tenantName?: string;
}

export interface TenantState {
  currentTenant: TenantSelection | null;
}
