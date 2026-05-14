import { createContext, useContext, useEffect, useState, useCallback } from "react";
import { listTeams } from "../api/teams";
import { useAuth } from "./AuthContext";

const TeamContext = createContext(null);

export function TeamProvider({ children }) {
  const { user } = useAuth();
  const [teams, setTeams] = useState([]);
  const [activeTeamId, setActiveTeamId] = useState(() => {
    const stored = localStorage.getItem("activeTeamId");
    return stored ? Number(stored) : null;
  });
  const [loading, setLoading] = useState(false);

  const reload = useCallback(async () => {
    if (!user) {
      setTeams([]);
      return;
    }
    setLoading(true);
    try {
      const list = await listTeams();
      setTeams(list || []);
      if ((!activeTeamId || !list.find((t) => t.id === activeTeamId)) && list.length > 0) {
        setActiveTeamId(list[0].id);
        localStorage.setItem("activeTeamId", String(list[0].id));
      }
    } catch {
      setTeams([]);
    } finally {
      setLoading(false);
    }
  }, [user, activeTeamId]);

  useEffect(() => {
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  const selectTeam = (id) => {
    setActiveTeamId(id);
    localStorage.setItem("activeTeamId", String(id));
  };

  return (
    <TeamContext.Provider value={{ teams, activeTeamId, selectTeam, reload, loading }}>
      {children}
    </TeamContext.Provider>
  );
}

export function useTeams() {
  return useContext(TeamContext);
}
