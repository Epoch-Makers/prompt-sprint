import client from "./client";

export const register = (payload) =>
  client.post("/api/auth/register", payload).then((r) => r.data);

export const login = (payload) =>
  client.post("/api/auth/login", payload).then((r) => r.data);

export const me = () => client.get("/api/auth/me").then((r) => r.data);

export const logout = () => client.post("/api/auth/logout").then((r) => r.data);

export const atlassianLoginUrl = () => {
  const base = import.meta.env.VITE_API_URL || "";
  return `${base}/api/auth/atlassian`;
};
