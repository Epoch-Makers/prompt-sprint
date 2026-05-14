import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, ExternalLink, ShieldAlert } from "lucide-react";
import { listActions, updateAction } from "../api/actions";
import { riskScore } from "../api/ai";
import { createIssue, getActiveConnection } from "../api/jira";
import ActionRiskBadge from "../components/ActionRiskBadge";

const STATUSES = ["", "OPEN", "IN_PROGRESS", "DONE", "AT_RISK"];

export default function ActionsPage() {
  const { teamId } = useParams();
  const id = Number(teamId);
  const [status, setStatus] = useState("");
  const [actions, setActions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [jiraConn, setJiraConn] = useState(null);

  const reload = async () => {
    setLoading(true);
    try {
      const list = await listActions({ teamId: id, ...(status && { status }) });
      setActions(list || []);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, status]);

  useEffect(() => {
    getActiveConnection(id).then(setJiraConn).catch(() => setJiraConn(null));
  }, [id]);

  const handleStatusChange = async (actionId, newStatus) => {
    try {
      await updateAction(actionId, { status: newStatus });
      reload();
    } catch (e) {
      alert(e?.response?.data?.message || "Durum değiştirilemedi");
    }
  };

  const applyRewrite = async (action) => {
    if (!action.rewriteSuggestion) return;
    try {
      await updateAction(action.id, { title: action.rewriteSuggestion });
      reload();
    } catch {
      alert("Rewrite uygulanamadı");
    }
  };

  const createJiraIssue = async (actionId) => {
    try {
      await createIssue(actionId);
      reload();
    } catch (e) {
      alert(e?.response?.data?.message || "Jira ticket yaratılamadı");
    }
  };

  const refreshRisk = async () => {
    const ids = actions.map((a) => a.id);
    if (ids.length === 0) return;
    try {
      await riskScore(ids);
      reload();
    } catch {
      alert("Risk skoru hesaplanamadı");
    }
  };

  const atRisk = actions.filter((a) => (a.riskScore ?? 0) >= 4);

  return (
    <div className="space-y-4">
      <Link
        to={`/teams/${id}`}
        className="text-sm text-brand-600 hover:underline inline-flex items-center gap-1"
      >
        <ArrowLeft size={14} /> Ekibe Dön
      </Link>

      <div className="flex items-center justify-between flex-wrap gap-2">
        <h1 className="text-2xl font-bold">Aksiyonlar</h1>
        <button
          onClick={refreshRisk}
          className="text-sm bg-slate-700 text-white px-3 py-1.5 rounded hover:bg-slate-800"
        >
          Risk Skorlarını Güncelle
        </button>
      </div>

      {/* Risk Radar */}
      <section className="bg-red-50 border border-red-200 rounded-lg p-4">
        <h2 className="font-semibold text-red-800 flex items-center gap-2 mb-2">
          <ShieldAlert size={18} /> Risk Radarı
        </h2>
        {atRisk.length === 0 ? (
          <p className="text-sm text-red-700">Risk altında aksiyon yok.</p>
        ) : (
          <ul className="space-y-2">
            {atRisk.map((a) => (
              <li
                key={a.id}
                className="bg-white border border-red-200 rounded p-3 flex items-start justify-between gap-3"
              >
                <div className="flex-1">
                  <div className="font-medium">{a.title}</div>
                  <div className="text-xs text-red-700 mt-1">
                    {a.riskReason || "Yüksek risk"}
                  </div>
                  {a.rewriteSuggestion && (
                    <div className="text-xs text-slate-600 italic mt-1">
                      Öneri: {a.rewriteSuggestion}
                    </div>
                  )}
                </div>
                <div className="flex flex-col items-end gap-1">
                  <ActionRiskBadge score={a.riskScore} />
                  {a.rewriteSuggestion && (
                    <button
                      onClick={() => applyRewrite(a)}
                      className="text-xs bg-brand-600 text-white px-2 py-1 rounded hover:bg-brand-700"
                    >
                      Rewrite'ı Uygula
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* Filter */}
      <div className="flex items-center gap-2">
        <label className="text-sm text-slate-600">Durum:</label>
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          className="border border-slate-300 rounded px-2 py-1 text-sm bg-white"
        >
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s || "Hepsi"}
            </option>
          ))}
        </select>
      </div>

      {/* Table */}
      <div className="bg-white border border-slate-200 rounded-lg shadow-sm overflow-x-auto">
        {loading ? (
          <div className="p-6 text-slate-500">Yükleniyor...</div>
        ) : actions.length === 0 ? (
          <div className="p-6 text-slate-500">Aksiyon yok.</div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-left text-xs text-slate-500">
              <tr>
                <th className="px-3 py-2">Durum</th>
                <th className="px-3 py-2">Başlık</th>
                <th className="px-3 py-2">Atanan</th>
                <th className="px-3 py-2">Deadline</th>
                <th className="px-3 py-2">Risk</th>
                <th className="px-3 py-2">Jira</th>
              </tr>
            </thead>
            <tbody>
              {actions.map((a) => (
                <tr
                  key={a.id}
                  className="border-b last:border-0 border-slate-100"
                >
                  <td className="px-3 py-2">
                    <select
                      value={a.status}
                      onChange={(e) =>
                        handleStatusChange(a.id, e.target.value)
                      }
                      className="border border-slate-300 rounded px-1 py-0.5 text-xs bg-white"
                    >
                      <option value="OPEN">OPEN</option>
                      <option value="IN_PROGRESS">IN_PROGRESS</option>
                      <option value="DONE">DONE</option>
                      <option value="AT_RISK">AT_RISK</option>
                    </select>
                  </td>
                  <td className="px-3 py-2">
                    <div className="font-medium">{a.title}</div>
                    {a.carriedFromSprint && (
                      <span className="inline-block text-xs bg-purple-100 text-purple-700 px-2 py-0.5 rounded mt-1">
                        {a.carriedFromSprint}'den devreden
                      </span>
                    )}
                  </td>
                  <td className="px-3 py-2 text-slate-600">
                    {a.assigneeName || "—"}
                  </td>
                  <td className="px-3 py-2 text-slate-600">
                    {a.deadline || "—"}
                  </td>
                  <td className="px-3 py-2">
                    <ActionRiskBadge score={a.riskScore} />
                  </td>
                  <td className="px-3 py-2">
                    {a.jiraKey ? (
                      <a
                        href={
                          jiraConn?.jiraDomain
                            ? `https://${jiraConn.jiraDomain}/browse/${a.jiraKey}`
                            : "#"
                        }
                        target="_blank"
                        rel="noreferrer"
                        className="text-brand-600 hover:underline inline-flex items-center gap-1 text-xs"
                      >
                        {a.jiraKey} <ExternalLink size={12} />
                      </a>
                    ) : (
                      <button
                        onClick={() => createJiraIssue(a.id)}
                        className="text-xs bg-slate-700 text-white px-2 py-1 rounded hover:bg-slate-800"
                      >
                        + Jira Ticket
                      </button>
                    )}
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
