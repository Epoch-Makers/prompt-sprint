import { Link, useNavigate } from "react-router-dom";
import { LogOut, Users } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { useTeams } from "../context/TeamContext";

export default function Navbar() {
  const { user, guestUser, logout } = useAuth();
  const { teams, activeTeamId, selectTeam } = useTeams();
  const navigate = useNavigate();

  const isGuest = !user && !!guestUser;

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  const handleTeamChange = (e) => {
    const id = Number(e.target.value);
    selectTeam(id);
    navigate(`/teams/${id}`);
  };

  return (
    <header className="sticky top-0 z-30 bg-white border-b border-slate-200 shadow-sm">
      <div className="px-4 py-3 flex items-center gap-4">
        <Link to={isGuest ? "#" : "/"} className="text-xl font-bold text-brand-700 tracking-tight">
          RetroAI
        </Link>

        {!isGuest && teams && teams.length > 0 && (
          <div className="flex items-center gap-2">
            <Users size={16} className="text-slate-500" />
            <select
              value={activeTeamId ?? ""}
              onChange={handleTeamChange}
              className="text-sm border border-slate-300 rounded px-2 py-1 bg-white"
            >
              {teams.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name} {t.myRole === "LEADER" ? "👑" : ""}
                </option>
              ))}
            </select>
          </div>
        )}

        {!isGuest && (
          <Link
            to="/teams/new"
            className="text-sm text-brand-600 hover:underline ml-2"
          >
            + Ekip Oluştur
          </Link>
        )}

        {isGuest && (
          <span className="text-xs bg-amber-100 text-amber-800 px-2 py-0.5 rounded">
            Misafir Mod
          </span>
        )}

        <div className="ml-auto flex items-center gap-3 text-sm text-slate-700">
          {user && <span className="font-medium">{user.fullName}</span>}
          {isGuest && (
            <span className="font-medium">
              {guestUser.fullName || guestUser.name || "Misafir"}
            </span>
          )}
          <button
            onClick={handleLogout}
            className="flex items-center gap-1 text-slate-500 hover:text-red-600"
          >
            <LogOut size={16} /> Çıkış
          </button>
        </div>
      </div>
    </header>
  );
}
