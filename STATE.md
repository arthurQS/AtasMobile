# STATE

## Position
- Documentacao base criada e alinhada ao escopo atual.
- UI e docs migradas para terminologia "Agenda".

## Decisions
- Offline-first com Room/DataStore.
- Sincronizacao via Firebase (planejada).
 - Senha master validada via Cloud Function.

## Blockers
- Falta implementar Firebase para sincronizacao colaborativa.
- Definir modelo de dados e regras do Firebase.

## Next actions
- Definir colecoes Firestore e regras de seguranca por unidade.
- Planejar estrategia de conflito (versionamento/merge).

