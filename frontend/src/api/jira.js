import client from "./client";

// Two-step flow (preferred)
export const jiraConnect = (payload) =>
  client.post("/api/jira/connect", payload).then((r) => r.data);

export const jiraConnectBoard = (payload) =>
  client.post("/api/jira/connect/board", payload).then((r) => r.data);

export const listTeamBoards = (teamId) =>
  client.get("/api/jira/boards", { params: { teamId } }).then((r) => r.data);

// Legacy one-shot
export const createConnection = (payload) =>
  client.post("/api/jira/connections", payload).then((r) => r.data);

export const listBoardsByConnection = (connectionId) =>
  client
    .get(`/api/jira/connections/${connectionId}/boards`)
    .then((r) => r.data);

export const getActiveConnection = (teamId) =>
  client
    .get("/api/jira/connections/active", { params: { teamId } })
    .then((r) => r.data);

export const deleteConnection = (connectionId) =>
  client.delete(`/api/jira/connections/${connectionId}`).then((r) => r.data);

export const getSprintContext = (retroId) =>
  client
    .get("/api/jira/sprint-context", { params: { retroId } })
    .then((r) => r.data);

export const createIssue = (actionId) =>
  client.post("/api/jira/issue", { actionId }).then((r) => r.data);

export const bulkCreateIssues = (retroId) =>
  client.post("/api/jira/bulk-create", { retroId }).then((r) => r.data);
