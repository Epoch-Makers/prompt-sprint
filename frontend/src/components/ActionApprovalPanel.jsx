import { useEffect, useState } from "react";
import { bulkCreateActions } from "../api/actions";
import { actionFromCard } from "../api/retros";
import { bulkCreateIssues } from "../api/jira";

export default function ActionApprovalPanel({
  retroId,
  teamId,
  members = [],
  aiActions = [],
  nextStepsCards = [],
  onSaved,
}) {
  // Merge AI suggestions and NEXT_STEPS cards into a single editable list.
  const [rows, setRows] = useState([]);
  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState(null);
  const [err, setErr] = useState(null);

  useEffect(() => {
    const base = [];
    aiActions.forEach((a) => {
      base.push({
        kind: "AI",
        selected: true,
        title: a.title,
        description: a.description || "",
        assigneeUserId: a.suggestedAssigneeUserId || "",
        deadline: a.suggestedDeadline || "",
        themeTitle: a.themeTitle || "",
      });
    });
    nextStepsCards.forEach((c) => {
      base.push({
        kind: "CARD",
        cardId: c.id,
        selected: true,
        title: c.content,
        description: "",
        assigneeUserId: "",
        deadline: "",
        themeTitle: "NEXT_STEPS",
      });
    });
    setRows(base);
  }, [aiActions, nextStepsCards]);

  const toggle = (i) =>
    setRows((rs) => rs.map((r, idx) => (idx === i ? { ...r, selected: !r.selected } : r)));
  const update = (i, k, v) =>
    setRows((rs) => rs.map((r, idx) => (idx === i ? { ...r, [k]: v } : r)));

  const saveAll = async () => {
    setSaving(true);
    setErr(null);
    setMsg(null);
    const selected = rows.filter((r) => r.selected);
    if (selected.length === 0) {
      setErr("En az bir aksiyon seçin.");
      setSaving(false);
      return null;
    }
    try {
      // AI bulk
      const aiPayload = selected
        .filter((r) => r.kind === "AI")
        .map((r) => ({
          title: r.title,
          description: r.description,
          assigneeUserId: r.assigneeUserId ? Number(r.assigneeUserId) : null,
          deadline: r.deadline || null,
        }));
      if (aiPayload.length > 0) {
        await bulkCreateActions({ retroId: Number(retroId), actions: aiPayload });
      }
      // Card-based
      const cardSelected = selected.filter((r) => r.kind === "CARD");
      for (const r of cardSelected) {
        await actionFromCard({
          cardId: r.cardId,
          assigneeUserId: r.assigneeUserId ? Number(r.assigneeUserId) : null,
          deadline: r.deadline || null,
        });
      }
      setMsg("Aksiyonlar kaydedildi.");
      onSaved?.();
      return selected;
    } catch (e) {
      setErr(e?.response?.data?.message || "Kaydetme başarısız.");
      return null;
    } finally {
      setSaving(false);
    }
  };

  const saveAndJira = async () => {
    const res = await saveAll();
    if (!res) return;
    try {
      const jira = await bulkCreateIssues(Number(retroId));
      setMsg(
        `Kaydedildi. Jira: ${jira.successCount} başarılı, ${jira.failedCount} hata.`
      );
    } catch (e) {
      setErr(e?.response?.data?.message || "Jira'ya gönderme başarısız.");
    }
  };

  if (rows.length === 0) {
    return (
      <div className="bg-white border border-slate-200 rounded p-4 text-sm text-slate-500">
        Onaylanacak aksiyon yok. AI analizini çalıştırın veya NEXT_STEPS sütununa kart ekleyin.
      </div>
    );
  }

  return (
    <div className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm">
      <h3 className="font-semibold mb-3">Aksiyon Onaylama Paneli</h3>

      <ul className="space-y-3">
        {rows.map((a, i) => (
          <li
            key={i}
            className="border border-slate-200 rounded p-3 flex gap-3 items-start"
          >
            <input
              type="checkbox"
              checked={a.selected}
              onChange={() => toggle(i)}
              className="mt-1"
            />
            <div className="flex-1 grid grid-cols-1 md:grid-cols-3 gap-2">
              <input
                type="text"
                value={a.title}
                onChange={(e) => update(i, "title", e.target.value)}
                className="md:col-span-3 border border-slate-300 rounded px-2 py-1 text-sm"
              />
              <select
                value={a.assigneeUserId || ""}
                onChange={(e) => update(i, "assigneeUserId", e.target.value)}
                className="border border-slate-300 rounded px-2 py-1 text-sm"
              >
                <option value="">— Atanmamış —</option>
                {members.map((m) => (
                  <option key={m.userId} value={m.userId}>
                    {m.fullName}
                  </option>
                ))}
              </select>
              <input
                type="date"
                value={a.deadline || ""}
                onChange={(e) => update(i, "deadline", e.target.value)}
                className="border border-slate-300 rounded px-2 py-1 text-sm"
              />
              <div className="text-xs text-slate-500 px-2 py-1">
                {a.kind === "AI" ? `Tema: ${a.themeTitle || "—"}` : "NEXT_STEPS kartı"}
              </div>
            </div>
          </li>
        ))}
      </ul>

      {err && (
        <div className="mt-3 text-sm text-red-600 bg-red-50 border border-red-200 rounded p-2">
          {err}
        </div>
      )}
      {msg && (
        <div className="mt-3 text-sm bg-blue-50 border border-blue-200 rounded p-2">
          {msg}
        </div>
      )}

      <div className="mt-4 flex gap-2 justify-end">
        <button
          onClick={saveAll}
          disabled={saving}
          className="bg-slate-700 text-white text-sm px-3 py-2 rounded hover:bg-slate-800 disabled:opacity-50"
        >
          Sadece Kaydet
        </button>
        <button
          onClick={saveAndJira}
          disabled={saving}
          className="bg-brand-600 text-white text-sm px-3 py-2 rounded hover:bg-brand-700 disabled:opacity-50"
        >
          Kaydet &amp; Jira'ya Gönder
        </button>
      </div>
    </div>
  );
}
