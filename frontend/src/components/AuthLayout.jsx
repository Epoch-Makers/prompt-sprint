export default function AuthLayout({ title, children }) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-100 p-4">
      <div className="bg-white rounded-lg shadow border border-slate-200 w-full max-w-md p-8">
        <h1 className="text-2xl font-bold text-brand-700 mb-1">RetroAI</h1>
        <p className="text-sm text-slate-500 mb-6">{title}</p>
        {children}
      </div>
    </div>
  );
}
