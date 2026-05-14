import client from "./client";

export const bulkCreateActions = (payload) =>
  client.post("/api/actions/bulk", payload).then((r) => r.data);

export const listActions = (params) =>
  client.get("/api/actions", { params }).then((r) => r.data);

export const updateAction = (actionId, payload) =>
  client.patch(`/api/actions/${actionId}`, payload).then((r) => r.data);

export const deleteAction = (actionId) =>
  client.delete(`/api/actions/${actionId}`).then((r) => r.data);
