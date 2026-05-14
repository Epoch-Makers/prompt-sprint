import { Outlet } from "react-router-dom";
import Navbar from "./Navbar";

export default function AppLayout() {
  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />
      <main className="flex-1 p-6 max-w-7xl w-full mx-auto">
        <Outlet />
      </main>
      <footer className="border-t border-slate-200 px-4 py-2 text-xs text-slate-500 text-center">
        RetroAI v0.1.0 · {new Date().toISOString().slice(0, 10)}
      </footer>
    </div>
  );
}
