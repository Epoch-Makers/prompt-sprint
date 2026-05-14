import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Plus, ShieldAlert, Settings, Users as UsersIcon } from "lucide-react";
import { getTeam } from "../api/teams";
import { listRetros } from "../api/retros";
import { listActions } from "../api/actions";
import { useTeams } from "../context/TeamContext";

export default function DashboardPage() {
  const { teamId } = useParams();
  const navigate = useNavigate();
  const { activeTeamId, teams } = useTeams();
  const id = teamId ? Number(teamId) : activeTeamId;

  const [team, setTeam] = useState(null);
  const [retros, setRetros] = useState([]);
  const [openActions, setOpenActions] = useState([]);
  const [atRiskCount, setAtRiskCount] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) {
      navigate("/teams/new");
      return;
    }
    if (teamId && Number(teamId) !== activeTeamId) {
      // sync selector when navigating via URL
    }
    let cancelled = false;
    const load = async () => {
      setLoading(true);
      try {
        const [t, r, a, ar] = await Promise.all([
          getTeam(id).catch(() => null),
          listRetros(id).catch(() => []),
          listActions({ teamId: id, status: "OPEN" }).catch(() => []),
          listActions({ teamId: id, status: "AT_RISK" }).catch(() => []),
        ]);
        if (cancelled) return;
        setTeam(t);
        setRetros(r || []);
        setOpenActions(a || []);
        setAtRiskCount((ar || []).length);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    load();
    return () => {
      cancelled = true;
    };
  }, [id, navigate, teamId, activeTeamId]);

  const currentTeamInfo = teams?.find((t) => t.id === id);
  const isLeader = currentTeamInfo?.myRole === "LEADER";

  if (loading) {
    return <div className="text-slate-500">Yükleniyor...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-bold">{team?.name || "Ekip"}</h1>
          <p className="text-sm text-slate-500">
            {team?.members?.length || 0} üye
          </p>
        </div>

        <div className="flex gap-2">
          <Link
            to={`/teams/${id}/members`}
            className="px-3 py-2 text-sm border border-slate-300 rounded flex items-center gap-1 hover:bg-slate-50"
          >
            <UsersIcon size={14} /> Üyeler
          </Link>
          {isLeader && (
            <Link
              to={`/teams/${id}/jira`}
              className="px-3 py-2 text-sm border border-slate-300 rounded flex items-center gap-1 hover:bg-slate-50"
            >
              <Settings size={14} /> Jira
            </Link>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white border border-slate-200 rounded-lg p-5 shadow-sm md:col-span-2">
          <h2 className="font-semibold text-lg mb-3">Yeni Retro</h2>
          <Link
            to={`/teams/${id}/retros/new`}
            className="inline-flex items-center gap-2 bg-brand-600 text-white px-4 py-2 rounded hover:bg-brand-700"
          >
            <Plus size={16} /> Yeni Retro Aç
          </Link>
        </div>

        <div className="bg-white border border-slate-200 rounded-lg p-5 shadow-sm">
          <h2 className="font-semibold text-sm text-slate-600 mb-2">
            Açık Aksiyon
          </h2>
          <p className="text-3xl font-bold text-slate-800">
            {openActions.length}
          </p>
          {atRiskCount > 0 && (
            <p className="mt-1 text-sm text-red-600 flex items-center gap-1">
              <ShieldAlert size={14} /> {atRiskCount} risk altında
            </p>
          )}
          <Link
            to={`/teams/${id}/actions`}
            className="text-sm text-brand-600 hover:underline mt-3 inline-block"
          >
            Aksiyonlara git →
          </Link>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-white border border-slate-200 rounded-lg p-5 shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <h2 className="font-semibold text-lg">Geçmiş Retrolar</h2>
            <Link
              to={`/teams/${id}/maturity`}
              className="text-sm text-brand-600 hover:underline"
            >
              Olgunluk Skoru →
            </Link>
          </div>
          {retros.length === 0 ? (
            <p className="text-sm text-slate-500">Henüz retro yok.</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-slate-500 border-b border-slate-200">
                  <th className="py-2">Sprint</th>
                  <th>Durum</th>
                  <th>Tarih</th>
                </tr>
              </thead>
              <tbody>
                {retros.map((r) => (
                  <tr
                    key={r.id}
                    onClick={() => navigate(`/retros/${r.id}`)}
                    className="border-b last:border-0 border-slate-100 hover:bg-slate-50 cursor-pointer"
                  >
                    <td className="py-2 font-medium">{r.sprintName}</td>
                    <td>
                      <span
                        className={`text-xs px-2 py-0.5 rounded ${
                          r.status === "ACTIVE"
                            ? "bg-green-100 text-green-800"
                            : "bg-slate-100 text-slate-600"
                        }`}
                      >
                        {r.status}
                      </span>
                    </td>
                    <td className="text-slate-500">
                      {r.createdAt?.slice(0, 10)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="bg-white border border-slate-200 rounded-lg p-5 shadow-sm">
          <h2 className="font-semibold text-lg mb-3">Hızlı Bağlantılar</h2>
          <ul className="space-y-2 text-sm">
            <li>
              <Link
                to={`/teams/${id}/actions`}
                className="text-brand-600 hover:underline"
              >
                Tüm Aksiyonlar (Risk Radarı)
              </Link>
            </li>
            <li>
              <Link
                to={`/teams/${id}/maturity`}
                className="text-brand-600 hover:underline"
              >
                Olgunluk Skoru
              </Link>
            </li>
            <li>
              <Link
                to={`/teams/${id}/members`}
                className="text-brand-600 hover:underline"
              >
                Ekip Üyeleri
              </Link>
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}
