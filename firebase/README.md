# Firebase

This folder contains Cloud Functions and Firestore rules for Agenda Mobile.

## Setup
1) Install Firebase CLI: `npm i -g firebase-tools`
2) Login: `firebase login`
3) Initialize (if needed): `firebase init functions firestore`
4) Deploy:
   - Functions: `firebase deploy --only functions`
   - Rules: `firebase deploy --only firestore:rules`

## Admin claims
`createWard` requires a user with `admin: true` custom claim.
You can set the claim in the Firebase Console or via Admin SDK.
