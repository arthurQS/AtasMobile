# research

Ecosystem knowledge (stack, features, architecture, pitfalls).

## Notes
- Firebase: avaliar modelo de dados, regras de seguranca e custo.
- Auth: fluxo anonimo + membership por unidade via Cloud Function.
- Firestore: `wards/{wardId}/agendas/{agendaId}` com `updatedAt`, `updatedBy`, `version`.
- Sync offline-first: estrategia de merge/conflito (baseline last-write-wins, evoluir para merge por campos).
- Firebase design detalhado: `research/firebase.md`.

