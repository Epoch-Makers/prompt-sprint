import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import { useAuth } from "../context/AuthContext";

export default function RegisterPage() {
  const [form, setForm] = useState({
    fullName: "",
    email: "",
    password: "",
    confirm: "",
  });
  const [err, setErr] = useState(null);
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleChange = (k) => (e) =>
    setForm({ ...form, [k]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErr(null);

    if (form.password.length < 6) {
      setErr("Parola en az 6 karakter olmalı.");
      return;
    }
    if (form.password !== form.confirm) {
      setErr("Parolalar eşleşmiyor.");
      return;
    }

    setLoading(true);
    try {
      await register({
        fullName: form.fullName,
        email: form.email,
        password: form.password,
      });
      navigate("/teams/new");
    } catch (e2) {
      setErr(
        e2?.response?.data?.message ||
          "Kayıt başarısız. Email zaten kayıtlı olabilir."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title="Yeni hesap oluştur">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">Ad Soyad</label>
          <input
            type="text"
            required
            value={form.fullName}
            onChange={handleChange("fullName")}
            className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Email</label>
          <input
            type="email"
            required
            value={form.email}
            onChange={handleChange("email")}
            className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Parola</label>
          <input
            type="password"
            required
            minLength={6}
            value={form.password}
            onChange={handleChange("password")}
            className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Parola (tekrar)</label>
          <input
            type="password"
            required
            value={form.confirm}
            onChange={handleChange("confirm")}
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
          {loading ? "Kaydediliyor..." : "Kayıt Ol"}
        </button>
      </form>

      <p className="text-sm text-slate-600 mt-4 text-center">
        Zaten hesabın var mı?{" "}
        <Link to="/login" className="text-brand-600 hover:underline">
          Giriş yap →
        </Link>
      </p>
    </AuthLayout>
  );
}
