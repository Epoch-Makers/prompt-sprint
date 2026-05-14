import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Trash2, ArrowLeft } from "lucide-react";
import { getTeam, addMember, removeMember } from "../api/teams";
import { useTeams } from "../context/TeamContext";

export default function TeamMembersPage() {
  const { teamId } = useParams();
  const id = Number(teamId);
  const { teams } = useTeams();
  const [team, setTeam] = useState(null);
  const [email, setEmail] = useState("");
  const [err, setErr] = useState(null);
  const [loading, setLoading] = useState(true);

  const isLeader = teams?.find((t) => t.id === id)?.myRole === "LEADER";

  const reload = async () => {
    try {
      const t = await getTeam(id);
      setTeam(t);
    } catch (e) {
      setErr(e?.response?.data?.message || "Ekip yüklenemedi");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handleAdd = async (e) => {
    e.preventDefault();
    setErr(null);
    try {
      await addMember(id, email);
      setEmail("");
      reload();
    } catch (e2) {
      setErr(e2?.response?.data?.message || "Üye eklenemedi.");
    }
  };

  const handleRemove = async (userId) => {
    if (!confirm("Bu üyeyi çıkarmak istiyor musun?")) return;
    try {
      await removeMember(id, userId);
      reload();
    } catch (e2) {
      setErr(e2?.response?.data?.message || "Çıkarma başarısız.");
    }
  };

  if (loading) return <div className="text-slate-500">Yükleniyor...</div>;

  return (
    <div className="space-y-4">
      <Link
        to={`/teams/${id}`}
        className="text-sm text-brand-600 hover:underline inline-flex items-center gap-1"
      >
        <ArrowLeft size={14} /> Ekip Dashboard'a Dön
      </Link>
      <h1 className="text-2xl font-bold">{team?.name} — Üyeler</h1>

      {isLeader && (
        <form
          onSubmit={handleAdd}
          className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm flex gap-2"
        >
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Üye email adresi"
            className="flex-1 border border-slate-300 rounded px-3 py-2 text-sm"
          />
          <button
            type="submit"
            className="bg-brand-600 text-white px-4 py-2 rounded text-sm hover:bg-brand-700"
          >
            Ekle
          </button>
        </form>
      )}

      {err && (
        <div className="text-red-600 text-sm bg-red-50 border border-red-200 rounded p-2">
          {err}
        </div>
      )}

      <div className="bg-white border border-slate-200 rounded-lg shadow-sm overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-xs text-slate-500">
            <tr>
              <th className="px-4 py-2">Ad Soyad</th>
              <th className="px-4 py-2">Email</th>
              <th className="px-4 py-2">Rol</th>
              {isLeader && <th className="px-4 py-2"></th>}
            </tr>
          </thead>
          <tbody>
            {(team?.members || []).map((m) => (
              <tr
                key={m.userId}
                className="border-b last:border-0 border-slate-100"
              >
                <td className="px-4 py-2 font-medium">{m.fullName}</td>
                <td className="px-4 py-2 text-slate-600">{m.email}</td>
                <td className="px-4 py-2">
                  <span
                    className={`text-xs px-2 py-0.5 rounded ${
                      m.role === "LEADER"
                        ? "bg-purple-100 text-purple-800"
                        : "bg-slate-100 text-slate-700"
                    }`}
                  >
                    {m.role}
                  </span>
                </td>
                {isLeader && (
                  <td className="px-4 py-2 text-right">
                    {m.role !== "LEADER" && (
                      <button
                        onClick={() => handleRemove(m.userId)}
                        className="text-red-600 hover:text-red-800"
                      >
                        <Trash2 size={14} />
                      </button>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
