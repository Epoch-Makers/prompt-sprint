import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Check, Trash2 } from "lucide-react";
import {
  getActiveConnection,
  jiraConnect,
  deleteConnection,
} from "../api/jira";

const STEPS = [
  { key: "FORM", label: "Bağlantı Bilgileri" },
  { key: "BOARDS", label: "Board Seçimi" },
  { key: "DONE", label: "Tamamlandı" },
];

function Stepper({ current }) {
  const currentIdx = STEPS.findIndex((s) => s.key === current);
  return (
    <ol className="flex items-center w-full mb-6">
      {STEPS.map((s, i) => {
        const isDone = i < currentIdx;
        const isCurrent = i === currentIdx;
        return (
          <li
            key={s.key}
            className={`flex-1 flex items-center ${
              i < STEPS.length - 1
                ? "after:content-[''] after:flex-1 after:h-0.5 after:mx-2 after:bg-slate-200"
                : ""
            } ${isDone ? "after:bg-brand-600" : ""}`}
          >
            <div className="flex items-center gap-2">
              <span
                className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-semibold border ${
                  isDone
                    ? "bg-brand-600 text-white border-brand-600"
                    : isCurrent
                    ? "bg-white text-brand-700 border-brand-600"
                    : "bg-slate-100 text-slate-500 border-slate-300"
                }`}
              >
                {isDone ? <Check size={14} /> : i + 1}
              </span>
              <span
                className={`text-xs whitespace-nowrap ${
                  isCurrent
                    ? "font-semibold text-slate-800"
                    : isDone
                    ? "text-brand-700"
                    : "text-slate-500"
                }`}
              >
                {s.label}
              </span>
            </div>
          </li>
        );
      })}
    </ol>
  );
}

export default function JiraConnectPage() {
  const { teamId } = useParams();
  const id = Number(teamId);
  const navigate = useNavigate();
  const [conn, setConn] = useState(null);
  const [form, setForm] = useState({
    email: "",
    apiToken: "",
    jiraDomain: "",
  });
  const [err, setErr] = useState(null);
  const [msg, setMsg] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const reload = async () => {
    setLoading(true);
    try {
      const c = await getActiveConnection(id);
      setConn(c);
    } catch {
      setConn(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const change = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const handleConnect = async (e) => {
    e.preventDefault();
    setErr(null);
    setMsg(null);
    setSubmitting(true);
    try {
      const res = await jiraConnect({ teamId: id, ...form });
      navigate(`/teams/${id}/jira/board`, {
        state: {
          connectionId: res.connectionId,
          boards: res.boards || [],
        },
      });
    } catch (e2) {
      setErr(e2?.response?.data?.message || "Bağlantı kurulamadı.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDisconnect = async () => {
    if (!conn || !confirm("Bağlantıyı kaldır?")) return;
    try {
      await deleteConnection(conn.id);
      setConn(null);
      setMsg("Bağlantı kaldırıldı.");
    } catch (e) {
      setErr(e?.response?.data?.message || "Kaldırma başarısız.");
    }
  };

  if (loading) return <div className="text-slate-500">Yükleniyor...</div>;

  const isConnected = conn?.status === "CONNECTED";
  const isPendingBoard = conn?.status === "PENDING_BOARD";
  const stepKey = isConnected ? "DONE" : isPendingBoard ? "BOARDS" : "FORM";

  return (
    <div className="max-w-xl space-y-4">
      <Link
        to={`/teams/${id}`}
        className="text-sm text-brand-600 hover:underline inline-flex items-center gap-1"
      >
        <ArrowLeft size={14} /> Ekibe Dön
      </Link>
      <h1 className="text-2xl font-bold">Jira Bağlantısı</h1>

      <Stepper current={stepKey} />

      {isConnected && (
        <div className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm">
          <h2 className="font-semibold mb-2">Aktif Bağlantı</h2>
          <ul className="text-sm space-y-1 text-slate-700">
            <li>Email: <strong>{conn.email}</strong></li>
            <li>Domain: <strong>{conn.jiraDomain}</strong></li>
            <li>Project Key: <strong>{conn.projectKey}</strong></li>
            <li>
              Board:{" "}
              <strong>{conn.boardName || `#${conn.boardId}`}</strong>
            </li>
            <li>
              Durum:{" "}
              <span className="text-xs bg-green-100 text-green-800 px-2 py-0.5 rounded">
                {conn.status}
              </span>
            </li>
          </ul>
          <div className="mt-3 flex gap-2">
            <Link
              to={`/teams/${id}/jira/board`}
              className="text-sm text-brand-600 hover:underline"
            >
              Board'u Değiştir
            </Link>
            <button
              onClick={handleDisconnect}
              className="inline-flex items-center gap-1 text-sm text-red-600 hover:text-red-800 ml-auto"
            >
              <Trash2 size={14} /> Bağlantıyı Kaldır
            </button>
          </div>
        </div>
      )}

      {isPendingBoard && (
        <div className="bg-amber-50 border border-amber-200 rounded-lg p-4 text-sm">
          Bağlantı kuruldu ama board seçilmedi.{" "}
          <Link
            to={`/teams/${id}/jira/board`}
            className="text-brand-600 hover:underline font-semibold"
          >
            Board Seç →
          </Link>
        </div>
      )}

      {!conn && (
        <form
          onSubmit={handleConnect}
          className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm space-y-3"
        >
          <p className="text-xs text-slate-500">
            API token nasıl alınır?{" "}
            <a
              href="https://id.atlassian.com/manage-profile/security/api-tokens"
              target="_blank"
              rel="noreferrer"
              className="text-brand-600 hover:underline"
            >
              Atlassian Dokümanı →
            </a>
          </p>
          <div>
            <label className="block text-sm font-medium mb-1">
              Atlassian Email
            </label>
            <input
              type="email"
              required
              value={form.email}
              onChange={change("email")}
              className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">API Token</label>
            <input
              type="password"
              required
              value={form.apiToken}
              onChange={change("apiToken")}
              className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Jira Domain</label>
            <input
              type="text"
              required
              value={form.jiraDomain}
              onChange={change("jiraDomain")}
              placeholder="mycompany.atlassian.net"
              className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
            />
          </div>
          <button
            type="submit"
            disabled={submitting}
            className="w-full bg-brand-600 text-white font-semibold rounded py-2 hover:bg-brand-700 disabled:opacity-50"
          >
            {submitting ? "Bağlanılıyor..." : "Devam Et: Board Seçimi"}
          </button>
        </form>
      )}

      {err && (
        <div className="text-red-600 text-sm bg-red-50 border border-red-200 rounded p-2">
          {err}
        </div>
      )}
      {msg && (
        <div className="text-sm bg-blue-50 border border-blue-200 rounded p-2">
          {msg}
        </div>
      )}
    </div>
  );
}
