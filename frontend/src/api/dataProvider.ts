import type {
  BaseRecord,
  CreateParams,
  DataProvider,
  DeleteOneParams,
  GetListParams,
  GetOneParams,
  UpdateParams,
} from "@refinedev/core";
import { apiFetch } from "./client";
import { env } from "../config/env";

const getList = async <TData extends BaseRecord = BaseRecord>({
  resource,
}: GetListParams) => {
  const data = await apiFetch<TData[]>(`/api/${resource}`);
  return { data, total: data.length };
};

const getOne = async <TData extends BaseRecord = BaseRecord>({
  resource,
  id,
}: GetOneParams) => ({
  data: await apiFetch<TData>(`/api/${resource}/${id}`),
});

const create = async <TData extends BaseRecord = BaseRecord, TVariables = unknown>({
  resource,
  variables,
}: CreateParams<TVariables>) => ({
  data: await apiFetch<TData>(`/api/${resource}`, {
    method: "POST",
    body: JSON.stringify(variables),
  }),
});

const update = async <TData extends BaseRecord = BaseRecord, TVariables = unknown>({
  resource,
  id,
  variables,
}: UpdateParams<TVariables>) => ({
  data: await apiFetch<TData>(`/api/${resource}/${id}`, {
    method: "PUT",
    body: JSON.stringify(variables),
  }),
});

const deleteOne = async <TData extends BaseRecord = BaseRecord, TVariables = unknown>({
  resource,
  id,
}: DeleteOneParams<TVariables>) => ({
  data: await apiFetch<TData>(`/api/${resource}/${id}`, { method: "DELETE" }),
});

export const dataProvider: DataProvider = {
  getApiUrl: () => env.apiUrl,
  getList,
  getOne,
  create,
  update,
  deleteOne,
};
