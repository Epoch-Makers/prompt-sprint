import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function AtlassianCallbackPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { refresh } = useAuth();
  const [err, setErr] = useState(null);

  useEffect(() => {
    const token = params.get("token");
    const error = params.get("error");
    if (error) {
      setErr(error);
      return;
    }
    if (!token) {
      setErr("Token yok.");
      return;
    }
    localStorage.setItem("token", token);
    refresh().finally(() => {
      navigate("/", { replace: true });
    });
  }, [params, navigate, refresh]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-100 p-4">
      <div className="bg-white rounded-lg shadow border border-slate-200 w-full max-w-md p-8 text-center">
        <h1 className="text-2xl font-bold text-brand-700 mb-2">RetroAI</h1>
        {err ? (
          <>
            <p className="text-sm text-red-600 mb-4">
              Atlassian girişi başarısız: {err}
            </p>
            <button
              onClick={() => navigate("/login")}
              className="text-sm text-brand-600 hover:underline"
            >
              Girişe dön
            </button>
          </>
        ) : (
          <p className="text-sm text-slate-500">Atlassian girişi tamamlanıyor...</p>
        )}
      </div>
    </div>
  );
}
