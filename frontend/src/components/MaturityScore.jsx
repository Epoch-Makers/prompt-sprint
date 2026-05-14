const levelColor = {
  FORMING: "bg-slate-200 text-slate-800",
  NORMING: "bg-blue-200 text-blue-900",
  PERFORMING: "bg-green-200 text-green-900",
  MASTERY: "bg-purple-200 text-purple-900",
};

export default function MaturityScore({ score, level, components, tips }) {
  const pct = Math.max(0, Math.min(100, score ?? 0));

  return (
    <div className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
      <div className="flex items-center gap-6">
        <div className="relative w-32 h-32 flex items-center justify-center">
          <svg viewBox="0 0 36 36" className="w-32 h-32 rotate-[-90deg]">
            <path
              d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              fill="none"
              stroke="#e2e8f0"
              strokeWidth="3"
            />
            <path
              d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              fill="none"
              stroke="#2563eb"
              strokeWidth="3"
              strokeDasharray={`${pct}, 100`}
            />
          </svg>
          <span className="absolute text-2xl font-bold text-slate-800">{pct}</span>
        </div>

        <div>
          <span
            className={`inline-block px-3 py-1 rounded-full text-sm font-semibold ${
              levelColor[level] || "bg-slate-200 text-slate-800"
            }`}
          >
            {level || "—"}
          </span>
          <h3 className="text-lg font-semibold mt-2">Ekip Olgunluk Skoru</h3>
        </div>
      </div>

      {components && (
        <div className="mt-6 space-y-3">
          {Object.entries(components).map(([key, val]) => {
            const pctVal = Math.round((val ?? 0) * 100);
            return (
              <div key={key}>
                <div className="flex justify-between text-xs text-slate-600 mb-1">
                  <span>{key}</span>
                  <span>%{pctVal}</span>
                </div>
                <div className="w-full bg-slate-100 rounded-full h-2">
                  <div
                    className="bg-brand-500 h-2 rounded-full"
                    style={{ width: `${pctVal}%` }}
                  ></div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {tips && tips.length > 0 && (
        <div className="mt-6">
          <h4 className="text-sm font-semibold mb-2">İyileştirme Önerileri</h4>
          <ul className="list-disc list-inside text-sm text-slate-700 space-y-1">
            {tips.map((t, i) => (
              <li key={i}>{t}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
