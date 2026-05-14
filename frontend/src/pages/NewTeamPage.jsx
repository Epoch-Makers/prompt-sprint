import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { createTeam } from "../api/teams";
import { useTeams } from "../context/TeamContext";

export default function NewTeamPage() {
  const [name, setName] = useState("");
  const [err, setErr] = useState(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { reload, selectTeam } = useTeams();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErr(null);
    setLoading(true);
    try {
      const team = await createTeam({ name });
      await reload();
      selectTeam(team.id);
      navigate(`/teams/${team.id}`);
    } catch (e2) {
      setErr(e2?.response?.data?.message || "Ekip oluşturulamadı.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
      <h2 className="text-xl font-bold mb-4">Yeni Ekip Oluştur</h2>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">Ekip Adı</label>
          <input
            type="text"
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
            placeholder="Payment Squad"
          />
        </div>
        {err && (
          <div className="text-red-600 text-sm bg-red-50 border border-red-200 rounded p-2">
            {err}
          </div>
        )}
        <button
          type="submit"
          disabled={loading}
          className="w-full bg-brand-600 text-white font-semibold rounded py-2 hover:bg-brand-700 disabled:opacity-50"
        >
          {loading ? "Oluşturuluyor..." : "Ekibi Oluştur"}
        </button>
      </form>
    </div>
  );
}
