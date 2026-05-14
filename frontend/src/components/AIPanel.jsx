import { Sparkles } from "lucide-react";

const urgencyColor = {
  LOW: "bg-green-100 text-green-800",
  MEDIUM: "bg-yellow-100 text-yellow-800",
  HIGH: "bg-red-100 text-red-800",
};

export default function AIPanel({ analysis, selectedActions, onToggleAction }) {
  if (!analysis) return null;

  return (
    <aside className="w-full lg:w-96 bg-white border border-slate-200 rounded-lg p-4 shadow-sm">
      <h3 className="text-lg font-semibold flex items-center gap-2 mb-3">
        <Sparkles size={18} className="text-brand-600" /> AI Analiz
      </h3>

      <section className="mb-4">
        <h4 className="text-sm font-semibold text-slate-700 mb-2">Tema Kümeleri</h4>
        <ul className="space-y-2">
          {(analysis.themes || []).map((t, i) => (
            <li
              key={i}
              className="border border-slate-200 rounded p-2 bg-slate-50"
            >
              <div className="flex items-center justify-between">
                <span className="font-medium text-sm">{t.title}</span>
                <span
                  className={`text-xs px-2 py-0.5 rounded ${
                    urgencyColor[t.urgency] || "bg-slate-200"
                  }`}
                >
                  {t.urgency}
                </span>
              </div>
              <div className="text-xs text-slate-500 mt-1">
                Moral: {t.moralScore} · {t.cardIds?.length || 0} kart
              </div>
            </li>
          ))}
        </ul>
      </section>

      <section>
        <h4 className="text-sm font-semibold text-slate-700 mb-2">
          Önerilen Aksiyonlar
        </h4>
        <ul className="space-y-2">
          {(analysis.actions || []).map((a, i) => (
            <li key={i} className="border border-slate-200 rounded p-2">
              <label className="flex items-start gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={selectedActions?.includes(i) ?? false}
                  onChange={() => onToggleAction?.(i)}
                  className="mt-1"
                />
                <div className="text-sm">
                  <div className="font-medium">{a.title}</div>
                  <div className="text-xs text-slate-500">{a.description}</div>
                  {a.suggestedDeadline && (
                    <div className="text-xs text-slate-400 mt-1">
                      Deadline: {a.suggestedDeadline}
                    </div>
                  )}
                </div>
              </label>
            </li>
          ))}
        </ul>
      </section>
    </aside>
  );
}
