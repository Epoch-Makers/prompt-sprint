import { useState } from "react";
import { ThumbsUp, Pencil, Trash2, Bot, Check, X } from "lucide-react";

export default function RetroCard({ card, currentUserId, onVote, onUnvote, onEdit, onDelete }) {
  const [editing, setEditing] = useState(false);
  const [text, setText] = useState(card.content);
  const isOwner = card.authorId === currentUserId;

  const saveEdit = async () => {
    await onEdit(card.id, { content: text });
    setEditing(false);
  };

  return (
    <div className="bg-white rounded-md border border-slate-200 p-3 mb-2 shadow-sm">
      <div className="flex items-start justify-between gap-2">
        {editing ? (
          <textarea
            value={text}
            onChange={(e) => setText(e.target.value)}
            className="flex-1 border border-slate-300 rounded p-1 text-sm"
            rows={2}
          />
        ) : (
          <p className="text-sm text-slate-800 whitespace-pre-wrap flex-1">
            {card.source === "JIRA_AI" && (
              <span className="inline-flex items-center text-blue-600 mr-1" title="Jira-AI">
                <Bot size={14} />
              </span>
            )}
            {card.content}
          </p>
        )}
      </div>

      <div className="flex items-center justify-between mt-2 text-xs">
        <span className="text-slate-500">{card.authorName || "Anonim üye"}</span>
        <div className="flex items-center gap-2">
          <button
            onClick={() =>
              card.myVoted ? onUnvote(card.id) : onVote(card.id)
            }
            className={`flex items-center gap-1 px-2 py-1 rounded ${
              card.myVoted
                ? "bg-brand-600 text-white"
                : "bg-slate-100 text-slate-700 hover:bg-slate-200"
            }`}
          >
            <ThumbsUp size={12} /> {card.voteCount ?? 0}
          </button>

          {isOwner && !editing && (
            <>
              <button
                onClick={() => setEditing(true)}
                className="p-1 text-slate-500 hover:text-slate-800"
                title="Düzenle"
              >
                <Pencil size={14} />
              </button>
              <button
                onClick={() => onDelete(card.id)}
                className="p-1 text-slate-500 hover:text-red-600"
                title="Sil"
              >
                <Trash2 size={14} />
              </button>
            </>
          )}

          {isOwner && editing && (
            <>
              <button onClick={saveEdit} className="p-1 text-green-600">
                <Check size={14} />
              </button>
              <button
                onClick={() => {
                  setText(card.content);
                  setEditing(false);
                }}
                className="p-1 text-slate-500"
              >
                <X size={14} />
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
