const dotColor = {
  GREEN: "bg-green-500",
  YELLOW: "bg-yellow-400",
  GREY: "bg-slate-300",
};

export default function ParticipationBar({ participants }) {
  if (!participants || participants.length === 0) return null;
  return (
    <div className="flex items-center gap-3 flex-wrap">
      <span className="text-xs text-slate-500">Katılım:</span>
      {participants.map((p) => (
        <div
          key={p.userId ?? p.guestSessionId ?? p.fullName}
          className="flex items-center gap-1 text-xs text-slate-700"
          title={`${p.cardCount} kart${p.isGuest ? " (misafir)" : ""}`}
        >
          <span
            className={`w-2.5 h-2.5 rounded-full ${dotColor[p.status] || "bg-slate-300"}`}
          />
          {p.fullName}
          {p.isGuest && (
            <span className="text-[10px] text-purple-700">·misafir</span>
          )}
        </div>
      ))}
    </div>
  );
}
