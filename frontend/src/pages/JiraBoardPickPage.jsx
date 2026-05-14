import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Check } from "lucide-react";
import { jiraConnectBoard, listTeamBoards } from "../api/jira";

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

export default function JiraBoardPickPage() {
  const { teamId } = useParams();
  const id = Number(teamId);
  const navigate = useNavigate();
  const location = useLocation();
  const stateBoards = location.state?.boards;
  const stateConnectionId = location.state?.connectionId;

  const [boards, setBoards] = useState(stateBoards || []);
  const [connectionId, setConnectionId] = useState(stateConnectionId || null);
  const [selected, setSelected] = useState("");
  const [currentBoardId, setCurrentBoardId] = useState(null);
  const [loading, setLoading] = useState(!stateBoards);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState(null);

  useEffect(() => {
    if (stateBoards && stateBoards.length > 0) return;
    let cancelled = false;
    (async () => {
      try {
        const data = await listTeamBoards(id);
        if (cancelled) return;
        setBoards(data.boards || []);
        setCurrentBoardId(data.currentBoardId || null);
        if (data.currentBoardId)
          setSelected(String(data.currentBoardId));
      } catch (e) {
        setErr(
          e?.response?.status === 404
            ? "Bu ekibin Jira bağlantısı yok. Önce bağlantıyı kurun."
            : e?.response?.data?.message || "Board listesi alınamadı."
        );
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id, stateBoards]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErr(null);
    if (!selected) {
      setErr("Lütfen bir board seçin.");
      return;
    }
    setSaving(true);
    try {
      await jiraConnectBoard({
        connectionId,
        boardId: Number(selected),
      });
      navigate(`/teams/${id}/jira`, { replace: true });
    } catch (e2) {
      setErr(e2?.response?.data?.message || "Board kaydedilemedi.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-xl space-y-4">
      <Link
        to={`/teams/${id}/jira`}
        className="text-sm text-brand-600 hover:underline inline-flex items-center gap-1"
      >
        <ArrowLeft size={14} /> Geri
      </Link>

      <h1 className="text-2xl font-bold">Jira Board Seçimi</h1>

      <Stepper current="BOARDS" />

      <form
        onSubmit={handleSubmit}
        className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm space-y-3"
      >
        {loading ? (
          <p className="text-sm text-slate-500">Board'lar yükleniyor...</p>
        ) : boards.length === 0 ? (
          <p className="text-sm text-slate-500">
            Hesabınızda erişilebilir board bulunamadı.
          </p>
        ) : (
          <ul className="space-y-1 max-h-80 overflow-auto">
            {boards.map((b) => (
              <li key={b.id}>
                <label
                  className={`flex items-center gap-2 p-2 rounded border cursor-pointer ${
                    String(b.id) === selected
                      ? "border-brand-600 bg-brand-50"
                      : "border-slate-200 hover:bg-slate-50"
                  }`}
                >
                  <input
                    type="radio"
                    name="board"
                    value={b.id}
                    checked={String(b.id) === selected}
                    onChange={() => setSelected(String(b.id))}
                  />
                  <div className="flex-1">
                    <div className="text-sm font-medium">{b.name}</div>
                    <div className="text-xs text-slate-500">
                      {b.type} · {b.projectKey}
                      {currentBoardId === b.id && (
                        <span className="ml-2 text-brand-600">(mevcut)</span>
                      )}
                    </div>
                  </div>
                </label>
              </li>
            ))}
          </ul>
        )}

        {err && (
          <div className="text-red-600 text-sm bg-red-50 border border-red-200 rounded p-2">
            {err}
          </div>
        )}

        <button
          type="submit"
          disabled={saving || boards.length === 0 || !selected}
          className="w-full bg-brand-600 text-white font-semibold rounded py-2 hover:bg-brand-700 disabled:opacity-50"
        >
          {saving ? "Kaydediliyor..." : "Board'u Kaydet"}
        </button>
      </form>
    </div>
  );
}
