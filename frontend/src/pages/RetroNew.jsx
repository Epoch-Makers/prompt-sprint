import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { createRetro } from "../api/retros";

export default function RetroNew() {
  const { teamId } = useParams();
  const id = Number(teamId);
  const navigate = useNavigate();
  const [form, setForm] = useState({
    sprintName: "",
    retroName: "",
    anonymousMode: true,
  });
  const [err, setErr] = useState(null);
  const [loading, setLoading] = useState(false);

  const change = (k) => (e) =>
    setForm({
      ...form,
      [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value,
    });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErr(null);
    setLoading(true);
    try {
      const retro = await createRetro({ teamId: id, ...form });
      navigate(`/retros/${retro.id}`);
    } catch (e2) {
      setErr(
        e2?.response?.data?.message ||
          "Retro oluşturulamadı. Aktif retro var olabilir."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-lg mx-auto space-y-4">
      <Link
        to={`/teams/${id}`}
        className="text-sm text-brand-600 hover:underline inline-flex items-center gap-1"
      >
        <ArrowLeft size={14} /> Geri
      </Link>
      <div className="bg-white border border-slate-200 rounded-lg p-6 shadow-sm">
        <h1 className="text-xl font-bold mb-4">Yeni Retro Aç</h1>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-1">Sprint Adı</label>
            <input
              type="text"
              required
              value={form.sprintName}
              onChange={change("sprintName")}
              className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
              placeholder="Sprint 24"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Retro Adı</label>
            <input
              type="text"
              required
              value={form.retroName}
              onChange={change("retroName")}
              className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
              placeholder="Sprint 24 Retro"
            />
          </div>

          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={form.anonymousMode}
              onChange={change("anonymousMode")}
            />
            Anonim mod (önerilir)
          </label>

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
            {loading ? "Açılıyor..." : "Retroyu Başlat"}
          </button>
        </form>
      </div>
    </div>
  );
}
