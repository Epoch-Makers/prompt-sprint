import { RotateCcw } from "lucide-react";

export default function CarryOverBanner({ count }) {
  if (!count || count <= 0) return null;
  return (
    <div className="flex items-center gap-2 bg-blue-50 border border-blue-200 text-blue-800 rounded-md px-4 py-2 text-sm mb-3">
      <RotateCcw size={16} />
      <span>
        <strong>{count}</strong> aksiyon önceki retrodan otomatik taşındı.
      </span>
    </div>
  );
}
