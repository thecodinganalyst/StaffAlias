import type { BaseRecord, DataProvider } from "@refinedev/core";
import { apiFetch } from "./client";
import { env } from "../config/env";

export const dataProvider: DataProvider = {
  getApiUrl: () => env.apiUrl,
  getList: async ({ resource }) => {
    const data = await apiFetch<BaseRecord[]>(`/api/${resource}`);
    return { data, total: data.length };
  },
  getOne: async ({ resource, id }) => ({
    data: await apiFetch<BaseRecord>(`/api/${resource}/${id}`),
  }),
  create: async ({ resource, variables }) => ({
    data: await apiFetch<BaseRecord>(`/api/${resource}`, {
      method: "POST",
      body: JSON.stringify(variables),
    }),
  }),
  update: async ({ resource, id, variables }) => ({
    data: await apiFetch<BaseRecord>(`/api/${resource}/${id}`, {
      method: "PUT",
      body: JSON.stringify(variables),
    }),
  }),
  deleteOne: async ({ resource, id }) => ({
    data: await apiFetch<BaseRecord>(`/api/${resource}/${id}`, { method: "DELETE" }),
  }),
};
