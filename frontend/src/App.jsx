import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { TeamProvider } from "./context/TeamContext";
import ProtectedRoute from "./components/ProtectedRoute";
import AppLayout from "./components/AppLayout";

import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import DashboardPage from "./pages/DashboardPage";
import NewTeamPage from "./pages/NewTeamPage";
import TeamMembersPage from "./pages/TeamMembersPage";
import RetroNew from "./pages/RetroNew";
import RetroBoard from "./pages/RetroBoard";
import ActionsPage from "./pages/ActionsPage";
import JiraConnectPage from "./pages/JiraConnectPage";
import JiraBoardPickPage from "./pages/JiraBoardPickPage";
import MaturityPage from "./pages/MaturityPage";
import GuestJoinPage from "./pages/GuestJoinPage";
import AtlassianCallbackPage from "./pages/AtlassianCallbackPage";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <TeamProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/join/:guestJoinToken" element={<GuestJoinPage />} />
            <Route path="/auth/atlassian/callback" element={<AtlassianCallbackPage />} />

            {/* Guest-accessible retro */}
            <Route
              element={
                <ProtectedRoute allowGuest>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              <Route path="/retros/:retroId" element={<RetroBoard />} />
            </Route>

            {/* Authenticated-only routes */}
            <Route
              element={
                <ProtectedRoute>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              <Route path="/" element={<DashboardPage />} />
              <Route path="/teams/new" element={<NewTeamPage />} />
              <Route path="/teams/:teamId" element={<DashboardPage />} />
              <Route path="/teams/:teamId/members" element={<TeamMembersPage />} />
              <Route path="/teams/:teamId/retros/new" element={<RetroNew />} />
              <Route path="/teams/:teamId/actions" element={<ActionsPage />} />
              <Route path="/teams/:teamId/jira" element={<JiraConnectPage />} />
              <Route
                path="/teams/:teamId/jira/board"
                element={<JiraBoardPickPage />}
              />
              <Route path="/teams/:teamId/maturity" element={<MaturityPage />} />
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </TeamProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
