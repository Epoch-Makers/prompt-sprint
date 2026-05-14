import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { joinLookup, joinRetro } from "../api/retros";
import { useAuth } from "../context/AuthContext";

export default function GuestJoinPage() {
  const { guestJoinToken } = useParams();
  const navigate = useNavigate();
  const { setGuest } = useAuth();

  const [lookup, setLookup] = useState(null);
  const [displayName, setDisplayName] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [err, setErr] = useState(null);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      if (!guestJoinToken) {
        setErr("Token eksik.");
        setLoading(false);
        return;
      }
      try {
        const data = await joinLookup(guestJoinToken);
        if (cancelled) return;
        if (!data?.tokenValid) {
          setErr("Geçersiz veya süresi dolmuş link.");
        } else {
          setLookup(data);
        }
      } catch (e) {
        const code = e?.response?.status;
        setErr(
          code === 410
            ? "Bu retro kapatılmış. Yeni bir retro açmasını isteyin."
            : code === 404
            ? "Geçersiz link."
            : e?.response?.data?.message || "Link doğrulanamadı."
        );
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    run();
    return () => {
      cancelled = true;
    };
  }, [guestJoinToken]);

  const validate = (name) => {
    const n = name.trim();
    if (n.length < 2 || n.length > 40) {
      return "İsim 2-40 karakter olmalı.";
    }
    if (!/^[\p{L}\p{N} ._-]+$/u.test(n)) {
      return "Sadece harf, rakam, boşluk ve . _ - karakterleri.";
    }
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErr(null);
    const ve = validate(displayName);
    if (ve) {
      setErr(ve);
      return;
    }
    setSubmitting(true);
    try {
      const res = await joinRetro(lookup.retroId, {
        token: guestJoinToken,
        displayName: displayName.trim(),
      });
      // Persist guest session — sessionStorage per spec
      if (res.guestSessionId) {
        sessionStorage.setItem("guestSessionId", res.guestSessionId);
        localStorage.setItem("guestSessionId", res.guestSessionId);
      }
      setGuest({
        retroId: res.retroId,
        guestSessionId: res.guestSessionId,
        displayName: res.displayName,
        fullName: res.displayName,
      });
      navigate(`/retros/${res.retroId}`, { replace: true });
    } catch (e2) {
      const code = e2?.response?.status;
      setErr(
        code === 409
          ? "Bu isim zaten kullanılıyor, başka bir isim deneyin."
          : code === 410
          ? "Retro kapatılmış."
          : e2?.response?.data?.message || "Katılım başarısız."
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-100 p-4">
      <div className="bg-white rounded-lg shadow border border-slate-200 w-full max-w-md p-8">
        <h1 className="text-2xl font-bold text-brand-700 mb-1">RetroAI</h1>
        <p className="text-sm text-slate-500 mb-6">Misafir Katılım</p>

        {loading ? (
          <p className="text-sm text-slate-500">Doğrulanıyor...</p>
        ) : err && !lookup ? (
          <>
            <div className="text-red-600 text-sm bg-red-50 border border-red-200 rounded p-2 mb-4">
              {err}
            </div>
            <button
              onClick={() => navigate("/login")}
              className="text-sm text-brand-600 hover:underline"
            >
              Girişe dön
            </button>
          </>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="bg-slate-50 border border-slate-200 rounded p-3 text-sm">
              <div className="font-semibold">{lookup.retroName}</div>
              <div className="text-xs text-slate-500">
                {lookup.teamName} · {lookup.sprintName}
              </div>
              <div className="text-xs mt-1">
                Faz:{" "}
                <span className="font-medium">{lookup.currentPhase}</span>
                {lookup.anonymousMode && (
                  <span className="ml-2 bg-amber-100 text-amber-800 px-2 py-0.5 rounded text-[10px]">
                    Anonim Mod
                  </span>
                )}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">
                Görünen Ad
              </label>
              <input
                type="text"
                required
                minLength={2}
                maxLength={40}
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
                placeholder="Ahmet K."
              />
              <p className="text-xs text-slate-400 mt-1">
                2-40 karakter, harf/rakam/boşluk ve . _ -
              </p>
            </div>

            {err && (
              <div className="text-red-600 text-sm bg-red-50 border border-red-200 rounded p-2">
                {err}
              </div>
            )}

            <button
              type="submit"
              disabled={submitting}
              className="w-full bg-brand-600 text-white font-semibold rounded py-2 hover:bg-brand-700 disabled:opacity-50"
            >
              {submitting ? "Katılıyor..." : "Retroya Katıl"}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
