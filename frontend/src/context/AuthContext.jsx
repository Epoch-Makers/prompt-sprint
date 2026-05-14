import { createContext, useContext, useEffect, useState, useCallback } from "react";
import { me as apiMe, login as apiLogin, logout as apiLogout, register as apiRegister } from "../api/auth";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [guestUser, setGuestUser] = useState(() => {
    try {
      const raw = localStorage.getItem("guestUser");
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  });
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    const token = localStorage.getItem("token");
    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const u = await apiMe();
      setUser(u);
    } catch {
      setUser(null);
      localStorage.removeItem("token");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const login = async (email, password) => {
    const data = await apiLogin({ email, password });
    if (data?.token) localStorage.setItem("token", data.token);
    setUser(data.user);
    return data.user;
  };

  const register = async (payload) => {
    await apiRegister(payload);
    return login(payload.email, payload.password);
  };

  const logout = async () => {
    try {
      await apiLogout();
    } catch {
      /* ignore */
    }
    localStorage.removeItem("token");
    localStorage.removeItem("guestToken"); // legacy
    localStorage.removeItem("guestRetroId");
    localStorage.removeItem("guestUser");
    localStorage.removeItem("guestSessionId");
    sessionStorage.removeItem("guestSessionId");
    setUser(null);
    setGuestUser(null);
  };

  const setGuest = useCallback((u) => {
    if (u) {
      localStorage.setItem("guestUser", JSON.stringify(u));
    } else {
      localStorage.removeItem("guestUser");
    }
    setGuestUser(u);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        guestUser,
        loading,
        login,
        register,
        logout,
        refresh,
        setGuest,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
