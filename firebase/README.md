# Firebase

This folder contains Cloud Functions and Firestore rules for Agenda Mobile.

## Setup
1) Install Firebase CLI: `npm i -g firebase-tools`
2) Login: `firebase login`
3) Initialize (if needed): `firebase init functions firestore`
4) Deploy:
   - Functions: `firebase deploy --only functions`
   - Rules: `firebase deploy --only firestore:rules`

## Emulators
- Require Java 21+ (use `JAVA_HOME` apontando para JDK 21/25).
- Run:
  - `firebase emulators:exec --config firebase.json --project demo-atasmobile "node tests/rules.test.js"`

## Node
- Use Node 18 for Functions and tests (`firebase/.nvmrc`).

## Audit fix
- Em `firebase/tests`, `npm audit fix` foi executado.
- Permanecem avisos moderados em dependencias do `firebase` (undici); revisar ao atualizar o SDK.

## Admin claims
`createWard` requires a user with `admin: true` custom claim.
You can set the claim in the Firebase Console or via Admin SDK.
