import client from "./client";

export const analyze = (retroId) =>
  client.post("/api/ai/analyze", { retroId }).then((r) => r.data);

export const riskScore = (actionIds) =>
  client.post("/api/ai/risk-score", { actionIds }).then((r) => r.data);

export const maturity = (retroId) =>
  client.post("/api/ai/maturity", { retroId }).then((r) => r.data);

export const briefing = (retroId) =>
  client.get("/api/ai/briefing", { params: { retroId } }).then((r) => r.data);

export const silentPrompt = (retroId, userId) =>
  client
    .get("/api/ai/silent-prompt", { params: { retroId, userId } })
    .then((r) => r.data);

export const jiraHistory = (retroId) =>
  client.post("/api/ai/jira-history", { retroId }).then((r) => r.data);
