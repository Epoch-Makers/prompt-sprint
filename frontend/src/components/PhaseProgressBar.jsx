import { Check } from "lucide-react";

export const PHASES = [
  { key: "WRITING", label: "Yazma" },
  { key: "GROUPING", label: "Gruplama" },
  { key: "VOTING", label: "Oylama" },
  { key: "DISCUSSION", label: "Final Değerlendirme" },
];

export default function PhaseProgressBar({ currentPhase }) {
  const currentIdx = PHASES.findIndex((p) => p.key === currentPhase);

  return (
    <ol className="flex items-center w-full gap-1 sm:gap-2">
      {PHASES.map((p, i) => {
        const isDone = i < currentIdx;
        const isCurrent = i === currentIdx;
        return (
          <li key={p.key} className="flex-1 flex items-center min-w-0">
            <div className="flex items-center gap-1 sm:gap-2 min-w-0">
              <span
                className={`w-7 h-7 shrink-0 rounded-full flex items-center justify-center text-xs font-semibold border ${
                  isDone
                    ? "bg-brand-600 text-white border-brand-600"
                    : isCurrent
                    ? "bg-white text-brand-700 border-brand-600 ring-2 ring-brand-200"
                    : "bg-slate-100 text-slate-500 border-slate-300"
                }`}
              >
                {isDone ? <Check size={14} /> : i + 1}
              </span>
              <span
                className={`text-xs sm:text-sm truncate ${
                  isCurrent
                    ? "font-semibold text-slate-800"
                    : isDone
                    ? "text-brand-700"
                    : "text-slate-500"
                }`}
              >
                {p.label}
              </span>
            </div>
            {i < PHASES.length - 1 && (
              <div
                className={`flex-1 h-0.5 mx-1 sm:mx-2 ${
                  isDone ? "bg-brand-600" : "bg-slate-200"
                }`}
              />
            )}
          </li>
        );
      })}
    </ol>
  );
}
