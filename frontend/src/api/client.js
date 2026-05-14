import axios from "axios";

const baseURL = import.meta.env.VITE_API_URL || "http://localhost:8080";

const client = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json",
  },
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  const guestSession =
    sessionStorage.getItem("guestSessionId") ||
    localStorage.getItem("guestSessionId") ||
    localStorage.getItem("guestToken"); // legacy
  if (guestSession) {
    config.headers["X-Guest-Session"] = guestSession;
  }
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err?.response?.status === 401) {
      // session invalid → drop token (keep guest session, may be different scope)
      localStorage.removeItem("token");
    }
    return Promise.reject(err);
  }
);

export default client;
