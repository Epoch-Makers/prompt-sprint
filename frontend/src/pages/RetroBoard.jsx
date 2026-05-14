import { useEffect, useState, useCallback, useMemo, useRef } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Sparkles, ArrowLeft, Plus, Trophy } from "lucide-react";
import {
  getRetro,
  listCards,
  createCard,
  updateCard,
  deleteCard,
  voteCard,
  unvoteCard,
  getParticipation,
  setPhase,
} from "../api/retros";
import { getSprintContext } from "../api/jira";
import { briefing as fetchBriefing, silentPrompt, analyze, maturity } from "../api/ai";
import RetroCard from "../components/RetroCard";
import CarryOverBanner from "../components/CarryOverBanner";
import SprintContextBand from "../components/SprintContextBand";
import ParticipationBar from "../components/ParticipationBar";
import PhaseProgressBar from "../components/PhaseProgressBar";
import PhaseControlButtons from "../components/PhaseControlButtons";
import Toast from "../components/Toast";
import ActionApprovalPanel from "../components/ActionApprovalPanel";
import MaturityScore from "../components/MaturityScore";
import { useAuth } from "../context/AuthContext";

const COLUMNS = [
  { key: "GOOD", title: "İyi Giden", color: "border-green-200 bg-green-50" },
  { key: "IMPROVE", title: "Geliştirilebilir", color: "border-amber-200 bg-amber-50" },
  { key: "DISCUSS", title: "Tartışılacak", color: "border-blue-200 bg-blue-50" },
  { key: "NEXT_STEPS", title: "Devam Adımları", color: "border-purple-200 bg-purple-50" },
];

export default function RetroBoard() {
  const { retroId } = useParams();
  const navigate = useNavigate();
  const { user, guestUser } = useAuth();
  const currentUserId = user?.id ?? null;

  const [retro, setRetro] = useState(null);
  const [cards, setCards] = useState([]);
  const [participation, setParticipation] = useState([]);
  const [sprintCtx, setSprintCtx] = useState(null);
  const [briefing, setBriefing] = useState(null);
  const [showBriefing, setShowBriefing] = useState(true);
  const [silent, setSilent] = useState(null);

  const [analysis, setAnalysis] = useState(null);
  const [analyzing, setAnalyzing] = useState(false);
  const [analyzeErr, setAnalyzeErr] = useState(null);

  const [maturityData, setMaturityData] = useState(null);
  const [maturityLoading, setMaturityLoading] = useState(false);

  const [newText, setNewText] = useState({
    GOOD: "",
    IMPROVE: "",
    DISCUSS: "",
    NEXT_STEPS: "",
  });

  const [toast, setToast] = useState({ open: false, message: "", variant: "info" });
  const prevPhaseRef = useRef(null);

  const isBoardOwner = useMemo(() => {
    if (!retro || !currentUserId) return false;
    return retro.createdByUserId === currentUserId;
  }, [retro, currentUserId]);

  const isGuest = !user && !!guestUser;
  const phase = retro?.currentPhase || "WRITING";
  const isClosed = retro?.status === "CLOSED" || phase === "CLOSED";

  const reloadRetro = useCallback(async () => {
    try {
      const r = await getRetro(retroId);
      setRetro((prev) => {
        if (prev && prev.currentPhase !== r.currentPhase) {
          // detected phase change
          prevPhaseRef.current = prev.currentPhase;
          if (!isBoardOwner) {
            setToast({
              open: true,
              message: `Board sahibi faz değiştirdi: ${r.currentPhase} başladı.`,
              variant: "info",
            });
          }
        }
        return r;
      });
    } catch {
      /* ignore */
    }
  }, [retroId, isBoardOwner]);

  const reloadCards = useCallback(async () => {
    try {
      const list = await listCards(retroId);
      setCards(list || []);
    } catch {
      /* ignore */
    }
  }, [retroId]);

  const reloadParticipation = useCallback(async () => {
    try {
      const list = await getParticipation(retroId);
      setParticipation(list || []);
    } catch {
      /* ignore */
    }
  }, [retroId]);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const r = await getRetro(retroId);
        if (cancelled) return;
        setRetro(r);
      } catch {
        if (!isGuest) navigate("/");
        return;
      }
      reloadCards();
      reloadParticipation();
      getSprintContext(retroId).then((c) => !cancelled && setSprintCtx(c)).catch(() => {});
      if (!isGuest) {
        fetchBriefing(retroId).then((b) => !cancelled && setBriefing(b)).catch(() => {});
      }
    };
    load();
    return () => {
      cancelled = true;
    };
  }, [retroId, navigate, reloadCards, reloadParticipation, isGuest]);

  // Polling: retro (3s) + cards (3s) + participation (5s)
  useEffect(() => {
    const retroInt = setInterval(reloadRetro, 3000);
    const cardInt = setInterval(reloadCards, 3000);
    const partInt = setInterval(reloadParticipation, 5000);
    return () => {
      clearInterval(retroInt);
      clearInterval(cardInt);
      clearInterval(partInt);
    };
  }, [reloadRetro, reloadCards, reloadParticipation]);

  // Silent prompt — only in WRITING phase, only logged-in user
  useEffect(() => {
    if (!currentUserId || phase !== "WRITING") return;
    let cancelled = false;
    const fetchPrompt = async () => {
      try {
        const p = await silentPrompt(retroId, currentUserId);
        if (!cancelled && p?.shouldShow) setSilent(p);
      } catch {
        /* ignore */
      }
    };
    const tid = setInterval(fetchPrompt, 60000);
    fetchPrompt();
    return () => {
      cancelled = true;
      clearInterval(tid);
    };
  }, [retroId, currentUserId, phase]);

  // Phase change
  const handlePhaseChange = async (targetPhase) => {
    try {
      const res = await setPhase(retroId, targetPhase);
      setRetro((r) => (r ? { ...r, currentPhase: res.currentPhase } : r));
      setToast({
        open: true,
        message: `Faz değiştirildi: ${res.currentPhase}`,
        variant: "success",
      });
    } catch (e) {
      setToast({
        open: true,
        message: e?.response?.data?.message || "Faz değiştirilemedi.",
        variant: "error",
      });
    }
  };

  // Card actions
  const handleAdd = async (column) => {
    const content = newText[column].trim();
    if (!content) return;
    try {
      await createCard(retroId, { content, column, source: "USER" });
      setNewText({ ...newText, [column]: "" });
      reloadCards();
    } catch (e) {
      if (e?.response?.status === 423) {
        setToast({
          open: true,
          message: "Bu faz kapalı, kart eklenemez.",
          variant: "warn",
        });
      }
    }
  };

  const handleEdit = async (cardId, payload) => {
    try {
      await updateCard(cardId, payload);
      reloadCards();
    } catch (e) {
      if (e?.response?.status === 423) {
        setToast({
          open: true,
          message: "Bu faz kapalı, kart düzenlenemez.",
          variant: "warn",
        });
      }
    }
  };

  const handleDelete = async (cardId) => {
    if (!confirm("Kartı silmek istiyor musun?")) return;
    try {
      await deleteCard(cardId);
      reloadCards();
    } catch {
      /* ignore */
    }
  };

  const handleVote = async (cardId) => {
    try {
      await voteCard(cardId);
      reloadCards();
      reloadRetro();
    } catch (e) {
      const code = e?.response?.status;
      if (code === 423) {
        setToast({ open: true, message: "Bu faz kapalı, oylama yok.", variant: "warn" });
      } else {
        setToast({
          open: true,
          message: e?.response?.data?.message || "Oy verilemedi.",
          variant: "error",
        });
      }
    }
  };
  const handleUnvote = async (cardId) => {
    try {
      await unvoteCard(cardId);
      reloadCards();
      reloadRetro();
    } catch {
      /* ignore */
    }
  };

  // GROUPING: board owner moves cards between columns via dropdown
  const handleMove = async (cardId, newColumn) => {
    try {
      await updateCard(cardId, { column: newColumn });
      reloadCards();
    } catch {
      setToast({ open: true, message: "Kart taşınamadı.", variant: "error" });
    }
  };

  // AI Analyze (GROUPING, board owner)
  const runAnalyze = async () => {
    setAnalyzing(true);
    setAnalyzeErr(null);
    try {
      const a = await analyze(retroId);
      setAnalysis(a);
      reloadCards(); // AI may have created NEXT_STEPS cards
    } catch (e) {
      setAnalyzeErr(
        e?.response?.data?.message || "AI analizi başarısız."
      );
    } finally {
      setAnalyzing(false);
    }
  };

  const runMaturity = async () => {
    setMaturityLoading(true);
    try {
      const m = await maturity(retroId);
      setMaturityData(m);
    } catch (e) {
      setToast({
        open: true,
        message: e?.response?.data?.message || "Olgunluk skoru hesaplanamadı.",
        variant: "error",
      });
    } finally {
      setMaturityLoading(false);
    }
  };

  if (!retro) return <div className="text-slate-500">Yükleniyor...</div>;

  const myRemaining = retro?.myRemainingVotes ?? 3;
  const nextStepsCards = cards.filter((c) => c.column === "NEXT_STEPS");
  const topVoted = [...cards]
    .filter((c) => (c.voteCount ?? 0) > 0)
    .sort((a, b) => (b.voteCount ?? 0) - (a.voteCount ?? 0))
    .slice(0, 5);

  // --- Sub-Bar (sticky, all phases) ---
  const subBar = (
    <div className="bg-white border-b border-slate-200 -mx-6 px-6 py-3 mb-4 space-y-3 sticky top-[57px] z-20">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div className="min-w-0">
          <div className="font-semibold truncate">{retro.retroName}</div>
          <div className="text-xs text-slate-500">{retro.sprintName}</div>
        </div>

        <div className="flex items-center gap-3 flex-wrap text-xs">
          {phase === "VOTING" && (
            <span className="bg-brand-50 border border-brand-200 text-brand-700 px-2 py-1 rounded font-medium">
              Kalan oyunuz: {myRemaining}/3
            </span>
          )}
          {sprintCtx?.mock && (
            <span className="bg-amber-100 text-amber-800 px-2 py-1 rounded">
              Mock Mode
            </span>
          )}
          {isGuest && (
            <span className="bg-purple-100 text-purple-800 px-2 py-1 rounded">
              Misafir: {guestUser.displayName || guestUser.fullName || "—"}
            </span>
          )}
          {isClosed && (
            <span className="bg-slate-200 text-slate-700 px-2 py-1 rounded">
              Bu retro kapatıldı
            </span>
          )}
        </div>
      </div>

      <PhaseProgressBar currentPhase={phase} />

      <div className="flex items-center justify-between flex-wrap gap-3">
        <ParticipationBar participants={participation} />
        {isBoardOwner && !isClosed && (
          <PhaseControlButtons
            currentPhase={phase}
            onChange={handlePhaseChange}
          />
        )}
      </div>
    </div>
  );

  // --- Column renderer (compact for DISCUSSION) ---
  const renderColumn = (col, opts = {}) => {
    const colCards = cards.filter((c) => c.column === col.key);
    const allowAdd = phase === "WRITING" && !isClosed;
    const allowVote = phase === "VOTING" && !isClosed;
    const allowEditDelete = (phase === "WRITING" || phase === "GROUPING") && !isClosed;
    const allowMove = phase === "GROUPING" && isBoardOwner;
    const highlight = opts.highlight;

    return (
      <section
        key={col.key}
        className={`border rounded-lg p-3 ${col.color} ${
          highlight ? "ring-2 ring-purple-500" : ""
        }`}
      >
        <h2 className="font-semibold mb-3 flex items-center justify-between">
          <span>
            {col.title}
            {highlight && (
              <span className="ml-2 text-xs text-purple-700">
                · Bu retronun çıktıları
              </span>
            )}
          </span>
          <span className="text-xs text-slate-500">{colCards.length} kart</span>
        </h2>

        <div className="min-h-[80px]">
          {colCards.map((c) => (
            <div key={c.id} className="relative">
              <RetroCard
                card={{
                  ...c,
                  // disable voting if not in VOTING phase
                  myVoted: allowVote ? c.myVoted : c.myVoted,
                }}
                currentUserId={allowEditDelete ? currentUserId : null}
                onVote={allowVote ? handleVote : () => {}}
                onUnvote={allowVote ? handleUnvote : () => {}}
                onEdit={allowEditDelete ? handleEdit : () => {}}
                onDelete={allowEditDelete ? handleDelete : () => {}}
              />
              {allowMove && (
                <div className="-mt-1 mb-2 ml-1">
                  <select
                    value={c.column}
                    onChange={(e) => handleMove(c.id, e.target.value)}
                    className="text-xs border border-slate-300 rounded px-1 py-0.5 bg-white"
                  >
                    {COLUMNS.map((cc) => (
                      <option key={cc.key} value={cc.key}>
                        ↪ {cc.title}
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>
          ))}
        </div>

        {allowAdd && (
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleAdd(col.key);
            }}
            className="mt-2 flex gap-1"
          >
            <input
              type="text"
              value={newText[col.key]}
              onChange={(e) =>
                setNewText({ ...newText, [col.key]: e.target.value })
              }
              placeholder="Yeni kart..."
              className="flex-1 border border-slate-300 rounded px-2 py-1 text-sm bg-white"
            />
            <button
              type="submit"
              className="bg-slate-700 text-white px-2 rounded hover:bg-slate-800"
              title="Ekle"
            >
              <Plus size={14} />
            </button>
          </form>
        )}
      </section>
    );
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-2">
        {user ? (
          <Link
            to={`/teams/${retro.teamId}`}
            className="text-sm text-brand-600 hover:underline inline-flex items-center gap-1"
          >
            <ArrowLeft size={14} /> Ekibe Dön
          </Link>
        ) : (
          <span />
        )}
      </div>

      {subBar}

      <CarryOverBanner count={retro.carriedOverActionCount} />
      <SprintContextBand context={sprintCtx} />

      {/* DISCUSSION top-voted */}
      {phase === "DISCUSSION" && topVoted.length > 0 && (
        <section className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm">
          <h3 className="font-semibold flex items-center gap-2 mb-2">
            <Trophy size={16} className="text-amber-500" />
            En çok oy alan kartlar
          </h3>
          <ul className="space-y-1">
            {topVoted.map((c) => (
              <li key={c.id} className="flex justify-between text-sm">
                <span className="truncate">{c.content}</span>
                <span className="ml-3 text-xs bg-brand-50 text-brand-700 px-2 py-0.5 rounded">
                  {c.voteCount} oy
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* GROUPING — AI panel */}
      {phase === "GROUPING" && (
        <div className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold flex items-center gap-2">
              <Sparkles size={16} className="text-brand-600" /> AI Tema Analizi
            </h3>
            {isBoardOwner && (
              <button
                onClick={runAnalyze}
                disabled={analyzing}
                className="bg-brand-600 text-white text-sm px-3 py-1.5 rounded hover:bg-brand-700 disabled:opacity-50"
              >
                {analyzing ? "Analiz ediliyor..." : "AI Analiz Et"}
              </button>
            )}
          </div>
          {analyzeErr && (
            <div className="text-red-600 text-sm bg-red-50 border border-red-200 rounded p-2">
              {analyzeErr}
            </div>
          )}
          {analysis?.themes && analysis.themes.length > 0 ? (
            <ul className="grid grid-cols-1 md:grid-cols-2 gap-2">
              {analysis.themes.map((t, i) => (
                <li
                  key={i}
                  className="border border-slate-200 rounded p-2 bg-slate-50"
                >
                  <div className="flex items-center justify-between">
                    <span className="font-medium text-sm">{t.title}</span>
                    <span className="text-xs bg-white border border-slate-300 px-2 py-0.5 rounded">
                      {t.urgency}
                    </span>
                  </div>
                  <div className="text-xs text-slate-500 mt-1">
                    Moral: {t.moralScore} · {t.cardIds?.length || 0} kart
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-slate-500">
              Analiz çalıştırıldığında tema kümeleri ve SMART aksiyonlar burada listelenir; her aksiyon NEXT_STEPS sütununa otomatik kart düşer.
            </p>
          )}
        </div>
      )}

      {/* Columns grid */}
      <div className={`grid grid-cols-1 md:grid-cols-2 ${phase === "DISCUSSION" ? "xl:grid-cols-4" : "xl:grid-cols-4"} gap-4`}>
        {COLUMNS.map((col) =>
          renderColumn(col, {
            highlight: phase === "DISCUSSION" && col.key === "NEXT_STEPS",
          })
        )}
      </div>

      {/* DISCUSSION — action approval panel */}
      {phase === "DISCUSSION" && isBoardOwner && (
        <ActionApprovalPanel
          retroId={retroId}
          teamId={retro.teamId}
          members={participation
            .filter((p) => p.userId)
            .map((p) => ({ userId: p.userId, fullName: p.fullName }))}
          aiActions={analysis?.actions || []}
          nextStepsCards={nextStepsCards}
          onSaved={() => reloadCards()}
        />
      )}

      {/* DISCUSSION — maturity score */}
      {phase === "DISCUSSION" && isBoardOwner && (
        <div className="space-y-3">
          <div className="flex justify-end">
            <button
              onClick={runMaturity}
              disabled={maturityLoading}
              className="bg-slate-700 text-white text-sm px-3 py-2 rounded hover:bg-slate-800 disabled:opacity-50"
            >
              {maturityLoading ? "Hesaplanıyor..." : "Olgunluk Skoru Hesapla"}
            </button>
          </div>
          {maturityData && (
            <MaturityScore
              score={maturityData.score}
              level={maturityData.level}
              components={maturityData.components}
              tips={maturityData.tips}
            />
          )}
        </div>
      )}

      {/* Briefing modal */}
      {showBriefing && briefing && !isGuest && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg p-6 max-w-lg w-full shadow-xl">
            <h2 className="text-xl font-bold mb-2">Açılış Brifingi</h2>
            <p className="text-sm text-slate-600 mb-4">
              {briefing.prevRetroSummary || "Bu ekibin ilk retrosu — başlayalım!"}
            </p>

            {briefing.prevRetroSummary && (
              <ul className="text-sm space-y-1 mb-4">
                <li>✔ Tamamlanan: <strong>{briefing.doneCount}</strong></li>
                <li>⏳ Devam eden: <strong>{briefing.inProgressCount}</strong></li>
                <li>⚠ Risk altında: <strong className="text-red-600">{briefing.atRiskCount}</strong></li>
              </ul>
            )}

            {(briefing.recurringThemes || []).length > 0 && (
              <div className="bg-amber-50 border border-amber-200 rounded p-3 text-sm mb-4">
                <strong>Tekrar eden temalar:</strong>
                <ul className="list-disc list-inside mt-1">
                  {briefing.recurringThemes.map((t, i) => (
                    <li key={i}>
                      "{t.title}" — {t.occurrenceCount}. sprint
                    </li>
                  ))}
                </ul>
              </div>
            )}

            <button
              onClick={() => setShowBriefing(false)}
              className="w-full bg-brand-600 text-white rounded py-2 hover:bg-brand-700"
            >
              Anladım, devam et
            </button>
          </div>
        </div>
      )}

      {/* Silent prompt modal */}
      {silent?.shouldShow && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-40 p-4">
          <div className="bg-white rounded-lg p-6 max-w-md w-full shadow-xl">
            <h3 className="font-semibold mb-2">Sana özel başlangıç sorusu</h3>
            <p className="text-sm text-slate-700 mb-4">{silent.prompt}</p>
            <button
              onClick={() => setSilent(null)}
              className="bg-brand-600 text-white rounded px-4 py-2 text-sm hover:bg-brand-700"
            >
              Tamam, başlıyorum
            </button>
          </div>
        </div>
      )}

      <Toast
        open={toast.open}
        message={toast.message}
        variant={toast.variant}
        onClose={() => setToast({ ...toast, open: false })}
      />
    </div>
  );
}
