import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { LinkIcon } from "lucide-react";
import AuthLayout from "../components/AuthLayout";
import { useAuth } from "../context/AuthContext";
import { atlassianLoginUrl } from "../api/auth";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [err, setErr] = useState(null);
  const [loading, setLoading] = useState(false);
  const [showJoin, setShowJoin] = useState(false);
  const [joinLink, setJoinLink] = useState("");
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErr(null);
    setLoading(true);
    try {
      await login(email, password);
      navigate("/");
    } catch (e2) {
      setErr(
        e2?.response?.data?.message ||
          "Giriş başarısız. Bilgilerinizi kontrol edin."
      );
    } finally {
      setLoading(false);
    }
  };

  const handleAtlassianLogin = () => {
    window.location.href = atlassianLoginUrl();
  };

  const handleJoinLink = (e) => {
    e.preventDefault();
    setErr(null);
    // Parse a link in the form:
    //   https://app/join/<uuid>
    //   /join/<uuid>
    //   <uuid>
    try {
      let token = "";
      const trimmed = joinLink.trim();
      const m = trimmed.match(/\/join\/([^/?#]+)/);
      if (m) {
        token = m[1];
      } else if (/^[A-Za-z0-9._-]{8,}$/.test(trimmed)) {
        token = trimmed;
      }

      if (!token) {
        setErr("Geçerli bir retro linki yapıştırın.");
        return;
      }

      navigate(`/join/${encodeURIComponent(token)}`);
    } catch {
      setErr("Link parse edilemedi.");
    }
  };

  return (
    <AuthLayout title="Giriş yap">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">Email</label>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
            placeholder="ad@firma.com"
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Parola</label>
          <input
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
          />
        </div>

        {err && (
          <div className="text-red-600 text-sm bg-red-50 border border-red-200 rounded p-2">
            {err}
          </div>
        )}

        <button
          type="submit"
          disabled={loading}
          className="w-full bg-brand-600 text-white font-semibold rounded py-2 hover:bg-brand-700 disabled:opacity-50"
        >
          {loading ? "Giriş yapılıyor..." : "Giriş Yap"}
        </button>
      </form>

      <div className="my-4 flex items-center gap-2 text-xs text-slate-400">
        <div className="flex-1 h-px bg-slate-200" />
        <span>veya</span>
        <div className="flex-1 h-px bg-slate-200" />
      </div>

      <button
        type="button"
        onClick={handleAtlassianLogin}
        className="w-full flex items-center justify-center gap-2 border border-slate-300 rounded py-2 text-sm font-medium hover:bg-slate-50"
      >
        <svg viewBox="0 0 32 32" className="w-5 h-5" aria-hidden="true">
          <defs>
            <linearGradient
              id="atl-gradient"
              x1="50%"
              y1="0%"
              x2="50%"
              y2="100%"
            >
              <stop offset="0%" stopColor="#0052CC" />
              <stop offset="100%" stopColor="#2684FF" />
            </linearGradient>
          </defs>
          <path
            fill="url(#atl-gradient)"
            d="M10.4 16.2L4 26h13.2c.5 0 .8-.3.8-.7 0-.1 0-.2-.1-.3l-6.1-8.7c-.3-.4-.9-.5-1.4-.1zM15.5 6c-.5-.4-1.1-.3-1.4.1L8 14.8l-3.5 5.1c-.3.4-.2 1 .2 1.3l8.3 6c.4.3 1 .2 1.3-.2l5.5-7.9c.3-.4.2-1-.2-1.3z"
          />
        </svg>
        Atlassian ile Giriş Yap
      </button>

      <button
        type="button"
        onClick={() => setShowJoin((s) => !s)}
        className="mt-3 w-full flex items-center justify-center gap-2 border border-slate-300 rounded py-2 text-sm font-medium hover:bg-slate-50"
      >
        <LinkIcon size={14} /> Retro linkiyle katıl
      </button>

      {showJoin && (
        <form onSubmit={handleJoinLink} className="mt-3 space-y-2">
          <input
            type="text"
            value={joinLink}
            onChange={(e) => setJoinLink(e.target.value)}
            placeholder="https://.../retros/10/join?token=..."
            className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
          />
          <button
            type="submit"
            className="w-full bg-slate-700 text-white text-sm rounded py-2 hover:bg-slate-800"
          >
            Misafir Olarak Katıl
          </button>
        </form>
      )}

      <p className="text-sm text-slate-600 mt-4 text-center">
        Hesabın yok mu?{" "}
        <Link to="/register" className="text-brand-600 hover:underline">
          Kayıt ol →
        </Link>
      </p>
    </AuthLayout>
  );
}
