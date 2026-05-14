export default function ActionRiskBadge({ score }) {
  if (score == null) {
    return <span className="text-xs text-slate-400">—</span>;
  }
  const color =
    score >= 4
      ? "bg-red-100 text-red-700 border-red-300"
      : score === 3
      ? "bg-yellow-100 text-yellow-700 border-yellow-300"
      : "bg-green-100 text-green-700 border-green-300";

  return (
    <span
      className={`inline-flex items-center justify-center w-7 h-7 rounded-full border text-xs font-semibold ${color}`}
      title={`Risk skoru: ${score}/5`}
    >
      {score}
    </span>
  );
}
