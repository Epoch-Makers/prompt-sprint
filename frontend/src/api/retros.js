import client from "./client";

export const createRetro = (payload) =>
  client.post("/api/retros", payload).then((r) => r.data);

export const listRetros = (teamId) =>
  client.get(`/api/retros`, { params: { teamId } }).then((r) => r.data);

export const getRetro = (retroId) =>
  client.get(`/api/retros/${retroId}`).then((r) => r.data);

export const updateRetro = (retroId, payload) =>
  client.patch(`/api/retros/${retroId}`, payload).then((r) => r.data);

export const setPhase = (retroId, targetPhase) =>
  client
    .post(`/api/retros/${retroId}/phase`, { targetPhase })
    .then((r) => r.data);

export const getParticipation = (retroId) =>
  client.get(`/api/retros/${retroId}/participation`).then((r) => r.data);

// Guest flow
export const joinLookup = (token) =>
  client
    .get(`/api/retros/join/lookup`, { params: { token } })
    .then((r) => r.data);

export const joinRetro = (retroId, payload) =>
  client.post(`/api/retros/${retroId}/join`, payload).then((r) => r.data);

// Cards
export const listCards = (retroId) =>
  client.get(`/api/retros/${retroId}/cards`).then((r) => r.data);

export const createCard = (retroId, payload) =>
  client.post(`/api/retros/${retroId}/cards`, payload).then((r) => r.data);

export const updateCard = (cardId, payload) =>
  client.patch(`/api/cards/${cardId}`, payload).then((r) => r.data);

export const deleteCard = (cardId) =>
  client.delete(`/api/cards/${cardId}`).then((r) => r.data);

export const voteCard = (cardId) =>
  client.post(`/api/cards/${cardId}/vote`).then((r) => r.data);

export const unvoteCard = (cardId) =>
  client.delete(`/api/cards/${cardId}/vote`).then((r) => r.data);

export const actionFromCard = (payload) =>
  client.post(`/api/actions/from-card`, payload).then((r) => r.data);
