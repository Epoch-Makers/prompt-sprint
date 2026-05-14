import client from "./client";

export const listTeams = () =>
  client.get("/api/teams").then((r) => r.data);

export const getTeam = (teamId) =>
  client.get(`/api/teams/${teamId}`).then((r) => r.data);

export const createTeam = (payload) =>
  client.post("/api/teams", payload).then((r) => r.data);

export const addMember = (teamId, email) =>
  client.post(`/api/teams/${teamId}/members`, { email }).then((r) => r.data);

export const removeMember = (teamId, userId) =>
  client.delete(`/api/teams/${teamId}/members/${userId}`).then((r) => r.data);
