import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function ProtectedRoute({ children, allowGuest = false }) {
  const { user, guestUser, loading } = useAuth();
  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen text-slate-500">
        Yükleniyor...
      </div>
    );
  }
  if (!user && !(allowGuest && guestUser)) {
    return <Navigate to="/login" replace />;
  }
  return children;
}
