import { Activity, Bug, Target } from "lucide-react";

export default function SprintContextBand({ context }) {
  if (!context) return null;
  return (
    <div className="flex flex-wrap items-center gap-4 bg-slate-50 border border-slate-200 rounded-md px-4 py-3 text-sm mb-3">
      <div className="font-semibold text-slate-700">{context.sprintName}</div>

      <div className="flex items-center gap-1 text-slate-600">
        <Target size={14} />
        {context.doneStories}/{context.plannedStories} story
      </div>

      <div className="flex items-center gap-1 text-red-600">
        <Bug size={14} /> {context.openBugs} açık bug
      </div>

      <div className="flex items-center gap-1 text-slate-600">
        <Activity size={14} /> Velocity %{context.velocityPct}
      </div>

      {context.topBugService && (
        <div className="text-slate-500">
          En çok bug: <strong>{context.topBugService}</strong>
        </div>
      )}

      {context.mock && (
        <span className="ml-auto bg-amber-100 text-amber-800 px-2 py-0.5 rounded text-xs">
          Mock Mode
        </span>
      )}
    </div>
  );
}
