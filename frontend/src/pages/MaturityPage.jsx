import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { maturity as fetchMaturity } from "../api/ai";
import { listRetros } from "../api/retros";
import MaturityScore from "../components/MaturityScore";

export default function MaturityPage() {
  const { teamId } = useParams();
  const id = Number(teamId);
  const [data, setData] = useState(null);
  const [retros, setRetros] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState(null);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setLoading(true);
      try {
        const list = await listRetros(id);
        if (cancelled) return;
        setRetros(list || []);
        const latest = list?.[0];
        if (latest) {
          const m = await fetchMaturity(latest.id);
          if (!cancelled) setData(m);
        }
      } catch (e) {
        setErr(e?.response?.data?.message || "Olgunluk skoru hesaplanamadı.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    load();
    return () => {
      cancelled = true;
    };
  }, [id]);

  if (loading) return <div className="text-slate-500">Hesaplanıyor...</div>;

  return (
    <div className="space-y-4">
      <Link
        to={`/teams/${id}`}
        className="text-sm text-brand-600 hover:underline inline-flex items-center gap-1"
      >
        <ArrowLeft size={14} /> Ekibe Dön
      </Link>

      <h1 className="text-2xl font-bold">Ekip Olgunluk Skoru</h1>

      {err && (
        <div className="text-red-600 text-sm bg-red-50 border border-red-200 rounded p-2">
          {err}
        </div>
      )}

      {data ? (
        <MaturityScore
          score={data.score}
          level={data.level}
          components={data.components}
          tips={data.tips}
        />
      ) : (
        <p className="text-slate-500">Henüz retro yok. Önce bir retro açın.</p>
      )}

      <div className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm">
        <h2 className="font-semibold mb-3">Retro Geçmişi</h2>
        {retros.length === 0 ? (
          <p className="text-sm text-slate-500">Retro yok.</p>
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
                <tr key={r.id} className="border-b last:border-0 border-slate-100">
                  <td className="py-2">{r.sprintName}</td>
                  <td>{r.status}</td>
                  <td className="text-slate-500">
                    {r.createdAt?.slice(0, 10)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
