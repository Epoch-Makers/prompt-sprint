import { ChevronLeft, ChevronRight } from "lucide-react";
import { PHASES } from "./PhaseProgressBar";

export default function PhaseControlButtons({ currentPhase, onChange, disabled }) {
  const idx = PHASES.findIndex((p) => p.key === currentPhase);
  const prev = idx > 0 ? PHASES[idx - 1] : null;
  const next = idx >= 0 && idx < PHASES.length - 1 ? PHASES[idx + 1] : null;

  return (
    <div className="flex items-center gap-2">
      <button
        type="button"
        disabled={!prev || disabled}
        onClick={() => prev && onChange(prev.key)}
        className="flex items-center gap-1 text-xs px-2 py-1 border border-slate-300 rounded hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed"
      >
        <ChevronLeft size={14} />
        Geri
      </button>
      <button
        type="button"
        disabled={!next || disabled}
        onClick={() => next && onChange(next.key)}
        className="flex items-center gap-1 text-xs px-3 py-1 bg-brand-600 text-white rounded hover:bg-brand-700 disabled:opacity-40 disabled:cursor-not-allowed"
      >
        İleri: {next ? next.label : "Bitti"}
        <ChevronRight size={14} />
      </button>
    </div>
  );
}
