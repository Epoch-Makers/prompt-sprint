import { useEffect } from "react";
import { X } from "lucide-react";

const variants = {
  info: "bg-blue-600 text-white",
  success: "bg-green-600 text-white",
  error: "bg-red-600 text-white",
  warn: "bg-amber-500 text-white",
};

export default function Toast({ open, message, variant = "info", onClose, duration = 4000 }) {
  useEffect(() => {
    if (!open) return undefined;
    const t = setTimeout(() => onClose?.(), duration);
    return () => clearTimeout(t);
  }, [open, duration, onClose]);

  if (!open) return null;

  return (
    <div className="fixed bottom-6 right-6 z-[100]">
      <div
        className={`flex items-center gap-3 px-4 py-3 rounded shadow-lg ${
          variants[variant] || variants.info
        }`}
      >
        <span className="text-sm">{message}</span>
        <button
          onClick={onClose}
          className="hover:opacity-80"
          aria-label="Kapat"
        >
          <X size={14} />
        </button>
      </div>
    </div>
  );
}
