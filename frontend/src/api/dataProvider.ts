import type { DataProvider } from "@refinedev/core";
import { apiFetch } from "./client";
import { env } from "../config/env";

export const dataProvider: DataProvider = {
  getApiUrl: () => env.apiUrl,
  getList: async ({ resource }) => {
    const data = await apiFetch<unknown[]>(`/api/${resource}`);
    return { data, total: data.length };
  },
  getOne: async ({ resource, id }) => ({
    data: await apiFetch(`/api/${resource}/${id}`),
  }),
  create: async ({ resource, variables }) => ({
    data: await apiFetch(`/api/${resource}`, {
      method: "POST",
      body: JSON.stringify(variables),
    }),
  }),
  update: async ({ resource, id, variables }) => ({
    data: await apiFetch(`/api/${resource}/${id}`, {
      method: "PUT",
      body: JSON.stringify(variables),
    }),
  }),
  deleteOne: async ({ resource, id }) => ({
    data: await apiFetch(`/api/${resource}/${id}`, { method: "DELETE" }),
  }),
};
